 /*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    PreprocessPanel.java
 *    Copyright (C) 2003 Richard Kirkby, Len Trigg
 *
 */

package reconcile.weka.gui.explorer;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import reconcile.weka.core.Attribute;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.converters.C45Loader;
import reconcile.weka.core.converters.CSVLoader;
import reconcile.weka.core.converters.Loader;
import reconcile.weka.experiment.InstanceQuery;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.SupervisedFilter;
import reconcile.weka.filters.unsupervised.attribute.Remove;
import reconcile.weka.gui.AttributeSelectionPanel;
import reconcile.weka.gui.AttributeSummaryPanel;
import reconcile.weka.gui.AttributeVisualizationPanel;
import reconcile.weka.gui.ExtensionFileFilter;
import reconcile.weka.gui.FileEditor;
import reconcile.weka.gui.GenericObjectEditor;
import reconcile.weka.gui.InstancesSummaryPanel;
import reconcile.weka.gui.Logger;
import reconcile.weka.gui.PropertyDialog;
import reconcile.weka.gui.PropertyPanel;
import reconcile.weka.gui.SysErrLog;
import reconcile.weka.gui.TaskLogger;
import reconcile.weka.gui.ViewerDialog;


/** 
 * This panel controls simple preprocessing of instances. Summary
 * information on instances and attributes is shown. Filters may be
 * configured to alter the set of instances. Altered instances may
 * also be saved.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class PreprocessPanel extends JPanel {
  
  /** Displays simple stats on the working instances */
  protected InstancesSummaryPanel m_InstSummaryPanel =
    new InstancesSummaryPanel();

  /** Click to load base instances from a file */
  protected JButton m_OpenFileBut = new JButton("Open file...");

  /** Click to load base instances from a URL */
  protected JButton m_OpenURLBut = new JButton("Open URL...");

  /** Click to load base instances from a Database */
  protected JButton m_OpenDBBut = new JButton("Open DB...");

  /** Lets the user enter a DB query */
  protected GenericObjectEditor m_DatabaseQueryEditor = 
    new GenericObjectEditor();

  /** Click to revert back to the last saved point */
  protected JButton m_UndoBut = new JButton("Undo");

  /** Click to open the current instances in a viewer */
  protected JButton m_EditBut = new JButton("Edit...");

  /** Click to apply filters and save the results */
  protected JButton m_SaveBut = new JButton("Save...");
  
  /** Panel to let the user toggle attributes */
  protected AttributeSelectionPanel m_AttPanel = new AttributeSelectionPanel();

  /** Button for removing attributes */
  JButton m_RemoveButton = new JButton("Remove");

  /** Displays summary stats on the selected attribute */
  protected AttributeSummaryPanel m_AttSummaryPanel =
    new AttributeSummaryPanel();

  /** Lets the user configure the filter */
  protected GenericObjectEditor m_FilterEditor =
    new GenericObjectEditor();

  /** Filter configuration */
  protected PropertyPanel m_FilterPanel = new PropertyPanel(m_FilterEditor);

  /** Click to apply filters and save the results */
  protected JButton m_ApplyFilterBut = new JButton("Apply");

  /** The file chooser for selecting arff files */
  protected JFileChooser m_FileChooser 
    = new JFileChooser(new File(System.getProperty("user.dir")));

  /** File filters for various file types */
  protected ExtensionFileFilter m_bsiFileFilter = 
    new ExtensionFileFilter(Instances.SERIALIZED_OBJ_FILE_EXTENSION,
			    "Binary serialized instances");

  protected ExtensionFileFilter m_c45FileFilter = 
    new ExtensionFileFilter(C45Loader.FILE_EXTENSION,
			    "C45 names files");

  protected ExtensionFileFilter m_csvFileFilter = 
    new ExtensionFileFilter(CSVLoader.FILE_EXTENSION,
			    "CSV data files");

  protected ExtensionFileFilter m_arffFileFilter = 
    new ExtensionFileFilter(Instances.FILE_EXTENSION,
			    "Arff data files");

  /** Stores the last URL that instances were loaded from */
  protected String m_LastURL = "http://";
  
  /** Stores the last sql query executed */
  protected String m_SQLQ = new String("SELECT * FROM ?");
 
  /** The working instances */
  protected Instances m_Instances;

  /** The visualization of the attribute values */
  protected AttributeVisualizationPanel m_AttVisualizePanel = 
    new AttributeVisualizationPanel();

  /** Keeps track of undo points */
  protected File[] m_tempUndoFiles = new File[20]; // set number of undo ops here

  /** The next available slot for an undo point */
  protected int m_tempUndoIndex = 0;
  
  /**
   * Manages sending notifications to people when we change the set of
   * working instances.
   */
  protected PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** A thread for loading/saving instances from a file or URL */
  protected Thread m_IOThread;

  /** The message logger */
  protected Logger m_Log = new SysErrLog();
  
  static {
    java.beans.PropertyEditorManager
      .registerEditor(java.io.File.class,
                      FileEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.core.SelectedTag.class,
		      reconcile.weka.gui.SelectedTagEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.filters.Filter.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.attributeSelection.ASSearch.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.attributeSelection.ASEvaluation.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
      .registerEditor(reconcile.weka.experiment.InstanceQuery.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
       .registerEditor(reconcile.weka.core.converters.Loader.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
       .registerEditor(reconcile.weka.core.converters.Saver.class,
		      reconcile.weka.gui.GenericObjectEditor.class);
  }
  
  /**
   * Creates the instances panel with no initial instances.
   */
  public PreprocessPanel() {

    // Create/Configure/Connect components
    try {
    m_DatabaseQueryEditor.setClassType(reconcile.weka.experiment.InstanceQuery.class);
    m_DatabaseQueryEditor.setValue(new reconcile.weka.experiment.InstanceQuery());
    ((GenericObjectEditor.GOEPanel)m_DatabaseQueryEditor.getCustomEditor())
      .addOkListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    setInstancesFromDBQ();
	  }
	});
    } catch (Exception ex) {
    }
    m_FilterEditor.setClassType(reconcile.weka.filters.Filter.class);
    m_OpenFileBut.setToolTipText("Open a set of instances from a file");
    m_OpenURLBut.setToolTipText("Open a set of instances from a URL");
    m_OpenDBBut.setToolTipText("Open a set of instances from a database");
    m_UndoBut.setToolTipText("Undo the last change to the dataset");
    m_EditBut.setToolTipText("Open the current dataset in a Viewer for editing");
    m_SaveBut.setToolTipText("Save the working relation to a file");
    m_ApplyFilterBut.setToolTipText("Apply the current filter to the data");

    m_FileChooser.
      addChoosableFileFilter(m_bsiFileFilter);
    m_FileChooser.
      addChoosableFileFilter(m_c45FileFilter);
    m_FileChooser.
      addChoosableFileFilter(m_csvFileFilter);
    m_FileChooser.
      addChoosableFileFilter(m_arffFileFilter);

    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    m_OpenURLBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setInstancesFromURLQ();
      }
    });
    m_OpenDBBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	PropertyDialog pd = new PropertyDialog(m_DatabaseQueryEditor,100,100);
      }
    });
    m_OpenFileBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setInstancesFromFileQ();
      }
    });
    m_UndoBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	undo();
      }
    });
    m_EditBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        edit();
      }
    });
    m_SaveBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	saveWorkingInstancesToFileQ();
      }
    });
    m_ApplyFilterBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  applyFilter((Filter) m_FilterEditor.getValue());
	}
      });
    m_AttPanel.getSelectionModel()
      .addListSelectionListener(new ListSelectionListener() {
	public void valueChanged(ListSelectionEvent e) {
	  if (!e.getValueIsAdjusting()) {	  
	    ListSelectionModel lm = (ListSelectionModel) e.getSource();
	    for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
	      if (lm.isSelectedIndex(i)) {
		m_AttSummaryPanel.setAttribute(i);
		m_AttVisualizePanel.setAttribute(i);
		break;
	      }
	    }
	  }
	}
    });


    m_InstSummaryPanel.setBorder(BorderFactory
				 .createTitledBorder("Current relation"));
    JPanel attStuffHolderPanel = new JPanel();
    attStuffHolderPanel.setBorder(BorderFactory
				  .createTitledBorder("Attributes"));
    attStuffHolderPanel.setLayout(new BorderLayout());
    attStuffHolderPanel.add(m_AttPanel, BorderLayout.CENTER);
    m_RemoveButton.setEnabled(false);
    m_RemoveButton.setToolTipText("Remove selected attributes.");
    m_RemoveButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  try {
	    Remove r = new Remove();
	    int [] selected = m_AttPanel.getSelectedAttributes();
	    if (selected.length == 0) {
	      return;
	    }
	    if (selected.length == m_Instances.numAttributes()) {
	      // Pop up an error optionpane
	      JOptionPane.showMessageDialog(PreprocessPanel.this,
					    "Can't remove all attributes from data!\n",
					    "Remove Attributes",
					    JOptionPane.ERROR_MESSAGE);
	      m_Log.logMessage("Can't remove all attributes from data!");
	      m_Log.statusMessage("Problem removing attributes");
	      return;
	    }
	    r.setAttributeIndicesArray(selected);
	    applyFilter(r);
	  } catch (Exception ex) {
	    if (m_Log instanceof TaskLogger) {
	      ((TaskLogger)m_Log).taskFinished();
	    }
	    // Pop up an error optionpane
	    JOptionPane.showMessageDialog(PreprocessPanel.this,
					  "Problem filtering instances:\n"
					  + ex.getMessage(),
					  "Remove Attributes",
					  JOptionPane.ERROR_MESSAGE);
	    m_Log.logMessage("Problem removing attributes: " + ex.getMessage());
	    m_Log.statusMessage("Problem removing attributes");
	  }
	}
      });

    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    p1.setLayout(new BorderLayout());
    p1.add(m_RemoveButton, BorderLayout.CENTER);
    attStuffHolderPanel.add(p1, BorderLayout.SOUTH);
    m_AttSummaryPanel.setBorder(BorderFactory
		    .createTitledBorder("Selected attribute"));
    m_UndoBut.setEnabled(false);
    m_EditBut.setEnabled(false);
    m_SaveBut.setEnabled(false);
    m_ApplyFilterBut.setEnabled(false);
    
    // Set up the GUI layout
    JPanel buttons = new JPanel();
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    buttons.setLayout(new GridLayout(1, 6, 5, 5));
    buttons.add(m_OpenFileBut);
    buttons.add(m_OpenURLBut);
    buttons.add(m_OpenDBBut);
    buttons.add(m_UndoBut);
    buttons.add(m_EditBut);
    buttons.add(m_SaveBut);

    JPanel attInfo = new JPanel();

    attInfo.setLayout(new BorderLayout());
    attInfo.add(attStuffHolderPanel, BorderLayout.CENTER);

    JPanel filter = new JPanel();
    filter.setBorder(BorderFactory
		    .createTitledBorder("Filter"));
    filter.setLayout(new BorderLayout());
    filter.add(m_FilterPanel, BorderLayout.CENTER);
    filter.add(m_ApplyFilterBut, BorderLayout.EAST); 

    JPanel attVis = new JPanel();
    attVis.setLayout( new GridLayout(2,1) );
    attVis.add(m_AttSummaryPanel);

    JComboBox colorBox = m_AttVisualizePanel.getColorBox();
    colorBox.setToolTipText("The chosen attribute will also be used as the " +
			    "class attribute when a filter is applied.");
    final JButton visAllBut = new JButton("Visualize All");
    visAllBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent ae) {
	  if (m_Instances != null) {
	    try {
	      final reconcile.weka.gui.beans.AttributeSummarizer as = 
		new reconcile.weka.gui.beans.AttributeSummarizer();
	      as.setColoringIndex(m_AttVisualizePanel.getColoringIndex());
	      as.setInstances(m_Instances);
	      
	      final javax.swing.JFrame jf = new javax.swing.JFrame();
	      jf.getContentPane().setLayout(new java.awt.BorderLayout());
	      
	      jf.getContentPane().add(as, java.awt.BorderLayout.CENTER);
	      jf.addWindowListener(new java.awt.event.WindowAdapter() {
		  public void windowClosing(java.awt.event.WindowEvent e) {
		    visAllBut.setEnabled(true);
		    jf.dispose();
		  }
		});
	      jf.setSize(830,600);
	      jf.setVisible(true);
	    } catch (Exception ex) {
	      ex.printStackTrace();
	    }
	  }
	}
      });
    JPanel histoHolder = new JPanel();
    histoHolder.setLayout(new BorderLayout());
    histoHolder.add(m_AttVisualizePanel, BorderLayout.CENTER);
    JPanel histoControls = new JPanel();
    histoControls.setLayout(new BorderLayout());
    histoControls.add(colorBox, BorderLayout.CENTER);
    histoControls.add(visAllBut, BorderLayout.EAST);
    histoHolder.add(histoControls, BorderLayout.NORTH);
    attVis.add(histoHolder);

    JPanel lhs = new JPanel();
    lhs.setLayout(new BorderLayout());
    lhs.add(m_InstSummaryPanel, BorderLayout.NORTH);
    lhs.add(attInfo, BorderLayout.CENTER);

    JPanel rhs = new JPanel();
    rhs.setLayout(new BorderLayout());
    rhs.add(attVis, BorderLayout.CENTER);

    JPanel relation = new JPanel();
    relation.setLayout(new GridLayout(1, 2));
    relation.add(lhs);
    relation.add(rhs);

    JPanel middle = new JPanel();
    middle.setLayout(new BorderLayout());
    middle.add(filter, BorderLayout.NORTH);
    middle.add(relation, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(buttons, BorderLayout.NORTH);
    add(middle, BorderLayout.CENTER);
  }

  /**
   * Sets the Logger to receive informational messages
   *
   * @param newLog the Logger that will now get info messages
   */
  public void setLog(Logger newLog) {

    m_Log = newLog;
  }
  
  /**
   * Tells the panel to use a new base set of instances.
   *
   * @param inst a set of Instances
   */
  public void setInstances(Instances inst) {

    m_Instances = inst;
    try {
      Runnable r = new Runnable() {
	public void run() {
	  m_InstSummaryPanel.setInstances(m_Instances);
	  m_AttPanel.setInstances(m_Instances);
	  m_RemoveButton.setEnabled(true);
	  m_AttSummaryPanel.setInstances(m_Instances);
	  m_AttVisualizePanel.setInstances(m_Instances);

	  // select the first attribute in the list
	  m_AttPanel.getSelectionModel().setSelectionInterval(0, 0);
	  m_AttSummaryPanel.setAttribute(0);
	  m_AttVisualizePanel.setAttribute(0);

	  m_ApplyFilterBut.setEnabled(true);

	  m_Log.logMessage("Base relation is now "
			   + m_Instances.relationName()
			   + " (" + m_Instances.numInstances()
			   + " instances)");
	  m_SaveBut.setEnabled(true);
	  m_EditBut.setEnabled(true);
	  m_Log.statusMessage("OK");
	  // Fire a propertychange event
	  m_Support.firePropertyChange("", null, null);
	}
      };
      if (SwingUtilities.isEventDispatchThread()) {
	r.run();
      } else {
	SwingUtilities.invokeAndWait(r);
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this,
				    "Problem setting base instances:\n"
				    + ex.getMessage(),
				    "Instances",
				    JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Gets the working set of instances.
   *
   * @return the working instances
   */
  public Instances getInstances() {

    return m_Instances;
  }
  
  /**
   * Adds a PropertyChangeListener who will be notified of value changes.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {

    m_Support.addPropertyChangeListener(l);
  }

  /**
   * Removes a PropertyChangeListener.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {

    m_Support.removePropertyChangeListener(l);
  }
  
  /**
   * Passes the dataset through the filter that has been configured for use.
   */
  protected void applyFilter(final Filter filter) {

    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  try {

	    if (filter != null) {
	    
	      if (m_Log instanceof TaskLogger) {
		((TaskLogger)m_Log).taskStarted();
	      }
	      m_Log.statusMessage("Passing dataset through filter "
				  + filter.getClass().getName());
	      int classIndex = m_AttVisualizePanel.getColoringIndex();
	      if ((classIndex < 0) && (filter instanceof SupervisedFilter)) {
		throw new IllegalArgumentException("Class (colour) needs to " +
						   "be set for supervised " +
						   "filter.");
	      }
	      Instances copy = new Instances(m_Instances);
	      copy.setClassIndex(classIndex);
	      filter.setInputFormat(copy);
	      Instances newInstances = filter.useFilter(copy, filter);
	      if (newInstances == null || newInstances.numAttributes() < 1) {
		throw new Exception("Dataset is empty.");
	      }
	      m_Log.statusMessage("Saving undo information");
	      addUndoPoint();
	      m_AttVisualizePanel.setColoringIndex(copy.classIndex());
	      m_Instances = newInstances;
	      setInstances(m_Instances);
	      if (m_Log instanceof TaskLogger) {
		((TaskLogger)m_Log).taskFinished();
	      }
	    }
	    
	  } catch (Exception ex) {
	
	    if (m_Log instanceof TaskLogger) {
	      ((TaskLogger)m_Log).taskFinished();
	    }
	    // Pop up an error optionpane
	    JOptionPane.showMessageDialog(PreprocessPanel.this,
					  "Problem filtering instances:\n"
					  + ex.getMessage(),
					  "Apply Filter",
					  JOptionPane.ERROR_MESSAGE);
	    m_Log.logMessage("Problem filtering instances: " + ex.getMessage());
	    m_Log.statusMessage("Problem filtering instances");
	  }
	  m_IOThread = null;
	}
      };
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't apply filter at this time,\n"
				    + "currently busy with other IO",
				    "Apply Filter",
				    JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Queries the user for a file to save instances as, then saves the
   * instances in a background process. This is done in the IO
   * thread, and an error message is popped up if the IO thread is busy.
   */
  public void saveWorkingInstancesToFileQ() {
    
    if (m_IOThread == null) {
      m_FileChooser.setAcceptAllFileFilterUsed(false);
      int returnVal = m_FileChooser.showSaveDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	File sFile = m_FileChooser.getSelectedFile();
	if (m_FileChooser.getFileFilter() == m_arffFileFilter) {
	  if (!sFile.getName().toLowerCase().endsWith(Instances.FILE_EXTENSION)) {
	    sFile = new File(sFile.getParent(), sFile.getName() 
			     + Instances.FILE_EXTENSION);
	  }
	  File selected = sFile;
	  saveInstancesToFile(selected, m_Instances, true);
	} else if (m_FileChooser.getFileFilter() == m_csvFileFilter) {
	  if (!sFile.getName().toLowerCase().endsWith(CSVLoader.FILE_EXTENSION)) {
	    sFile = new File(sFile.getParent(), sFile.getName() 
			     + CSVLoader.FILE_EXTENSION);
	  }
	  File selected = sFile;
	  saveInstancesToFile(selected, m_Instances, false);
	} else if (m_FileChooser.getFileFilter() == m_c45FileFilter) {	 
	  File selected = sFile;
	  saveInstancesToC45File(selected, m_Instances);
	} else if (m_FileChooser.getFileFilter() == m_bsiFileFilter) {
	  if (!sFile.getName().toLowerCase().
	      endsWith(Instances.SERIALIZED_OBJ_FILE_EXTENSION)) {
	    sFile = new File(sFile.getParent(), sFile.getName() 
			     + Instances.SERIALIZED_OBJ_FILE_EXTENSION);
	  }
	  File selected = sFile;
	  saveSerializedInstancesToFile(selected, m_Instances);
	}
      }
      FileFilter temp = m_FileChooser.getFileFilter();
      m_FileChooser.setAcceptAllFileFilterUsed(true);
      m_FileChooser.setFileFilter(temp);
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't save at this time,\n"
				    + "currently busy with other IO",
				    "Save Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }
  
  /**
   * Queries the user for a file to load instances from, then loads the
   * instances in a background process. This is done in the IO
   * thread, and an error message is popped up if the IO thread is busy.
   */
  public void setInstancesFromFileQ() {
    
    if (m_IOThread == null) {
      int returnVal = m_FileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	File selected = m_FileChooser.getSelectedFile();
	try {
	  addUndoPoint();
	} catch (Exception ignored) {}
	setInstancesFromFile(selected);
      }
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Queries the user for a URL to a database to load instances from, 
   * then loads the instances in a background process. This is done in the IO
   * thread, and an error message is popped up if the IO thread is busy.
   */
  public void setInstancesFromDBQ() {
    if (m_IOThread == null) {
      try {
	InstanceQuery InstQ = 
	  (InstanceQuery)m_DatabaseQueryEditor.getValue();
	
        // we have to disconnect, otherwise we can't change the DB!
        if (InstQ.isConnected())
          InstQ.disconnectFromDatabase();

	InstQ.connectToDatabase();      
	try {
	  addUndoPoint();
	} catch (Exception ignored) {}
	setInstancesFromDB(InstQ);
      } catch (Exception ex) {
	JOptionPane.showMessageDialog(this,
				      "Problem connecting to database:\n"
				      + ex.getMessage(),
				      "Load Instances",
				      JOptionPane.ERROR_MESSAGE);
      }
      
    } else {
      JOptionPane.showMessageDialog(this,
				     "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }
    
  /**
   * Queries the user for a URL to load instances from, then loads the
   * instances in a background process. This is done in the IO
   * thread, and an error message is popped up if the IO thread is busy.
   */
  public void setInstancesFromURLQ() {
    
    if (m_IOThread == null) {
      try {
	String urlName = (String) JOptionPane.showInputDialog(this,
			"Enter the source URL",
			"Load Instances",
			JOptionPane.QUESTION_MESSAGE,
			null,
			null,
			m_LastURL);
	if (urlName != null) {
	  m_LastURL = urlName;
	  URL url = new URL(urlName);
	  try {
	    addUndoPoint();
	  } catch (Exception ignored) {}
	  setInstancesFromURL(url);
	}
      } catch (Exception ex) {
	ex.printStackTrace();
	JOptionPane.showMessageDialog(this,
				      "Problem with URL:\n"
				      + ex.getMessage(),
				      "Load Instances",
				      JOptionPane.ERROR_MESSAGE);
      }
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }
  

  /**
   * Saves the current instances in C45 names and data file format
   *
   * @param f a value of type 'File'
   * @param inst the instances to save
   */
  protected void saveInstancesToC45File(final File f, final Instances inst) {
    if (m_IOThread == null) {
      final int classIndex = m_AttVisualizePanel.getColoringIndex();
      if (inst.attribute(classIndex).isNumeric()) {	      
	JOptionPane.showMessageDialog(this,
				      "Can't save in C45 format,\n"
				      + "as the selected class is numeric.",
				      "Save Instances",
				      JOptionPane.ERROR_MESSAGE);
	return;
      }
      m_IOThread = new Thread() {
	  public void run() {
	    try {
	      m_Log.statusMessage("Saving to file...");
	      String name = f.getAbsolutePath();
	      if (name.lastIndexOf('.') != -1) {
		name = name.substring(0, name.lastIndexOf('.'));
	      }
	      File fData = new File(name+".data");
	      File fNames = new File(name+".names");
	      Writer w = new BufferedWriter(new FileWriter(fNames));
	      Writer w2 =  new BufferedWriter(new FileWriter(fData));	      
	      
	      // write the names file
	      for (int i = 0; i < inst.attribute(classIndex).numValues(); i++) {
		w.write(inst.attribute(classIndex).value(i));
		if (i < inst.attribute(classIndex).numValues()-1) {
		  w.write(",");
		} else {
		  w.write(".\n");
		}
	      }
	      for (int i = 0; i < inst.numAttributes(); i++) {
		if (i != classIndex) {
		  w.write(inst.attribute(i).name()+": ");
		  if (inst.attribute(i).isNumeric() || inst.attribute(i).isDate()) {
		    w.write("continuous.\n");
		  } else {
		    Attribute temp = inst.attribute(i);
		    for (int j = 0; j < temp.numValues(); j++) {
		      w.write(temp.value(j));
		      if (j < temp.numValues()-1) {
			w.write(",");
		      } else {
			w.write(".\n");
		      }
		    }
		  }
		}
	      }
	      w.close();
	      
	      // write the data file
	      for (int i = 0; i < inst.numInstances(); i++) {
		Instance tempI = inst.instance(i);
		for (int j = 0; j < inst.numAttributes(); j++) {
		  if (j != classIndex) {
		    if (tempI.isMissing(j)) {
		      w2.write("?,");
		    } else if (inst.attribute(j).isNominal() || 
			       inst.attribute(j).isString()) {
		      w2.write(inst.attribute(j).value((int)tempI.value(j))+",");
		    } else {
		      w2.write(""+tempI.value(j)+",");
		    }
		  }
		}
		//		w2.write(inst.instance(i).toString());
		// write the class value
		if (tempI.isMissing(classIndex)) {
		  w2.write("?");
		} else {
		  w2.write(inst.attribute(classIndex).
			   value((int)tempI.value(classIndex)));
		}
		w2.write("\n");
	      }
	      w2.close();
	      m_Log.statusMessage("OK");
	 
	    } catch (Exception ex) {
	      ex.printStackTrace();
	      m_Log.logMessage(ex.getMessage());
	    }
	    m_IOThread = null;
	  }
	};
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't save at this time,\n"
				    + "currently busy with other IO",
				    "Save c45 format",
				    JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Saves the current instances in binary serialized form to a file
   *
   * @param f a value of type 'File'
   * @param inst the instances to save
   */
  protected void saveSerializedInstancesToFile(final File f, 
					       final Instances inst) {
    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	  public void run() {
	    try {
	      m_Log.statusMessage("Saving to file...");

	      ObjectOutputStream oos = 
		  new ObjectOutputStream(
		  new BufferedOutputStream(
		  new FileOutputStream(f)));

	      oos.writeObject(inst);
	      oos.flush();
	      oos.close();

	      m_Log.statusMessage("OK");
	    } catch (Exception ex) {
	      ex.printStackTrace();
	      m_Log.logMessage(ex.getMessage());
	    }
	    m_IOThread = null;
	  }
	};
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't save at this time,\n"
				    + "currently busy with other IO",
				    "Save binary serialized instances",
				    JOptionPane.WARNING_MESSAGE);
    } 
  }

  /**
   * Saves the current instances to the supplied file.
   *
   * @param f a value of type 'File'
   * @param inst the instances to save
   * @param saveHeader true to save in arff format, false to save in csv
   */
  protected void saveInstancesToFile(final File f, final Instances inst,
				     final boolean saveHeader) {
      
    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  try {
	    m_Log.statusMessage("Saving to file...");
	    Writer w = new BufferedWriter(new FileWriter(f));
	    if (saveHeader) {
	      Instances h = new Instances(inst, 0);
	      w.write(h.toString());
	      w.write("\n");
	    } else {
	      // csv - write attribute names as first row
	      for (int i = 0; i < inst.numAttributes(); i++) {
		w.write(inst.attribute(i).name());
		if (i < inst.numAttributes()-1) {
		  w.write(",");
		}
	      }
	      w.write("\n");
	    }
	    for (int i = 0; i < inst.numInstances(); i++) {
	      w.write(inst.instance(i).toString());
	      w.write("\n");
	    }
	    w.close();
	    m_Log.statusMessage("OK");
	  } catch (Exception ex) {
	    ex.printStackTrace();
	    m_Log.logMessage(ex.getMessage());
	  }
	  m_IOThread = null;
	}
      };
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't save at this time,\n"
				    + "currently busy with other IO",
				    "Save Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Pops up generic object editor with list of conversion filters
   *
   * @param f the File
   */
  private void converterQuery(final File f) {
    final GenericObjectEditor convEd = new GenericObjectEditor(true);

    try {
      convEd.setClassType(reconcile.weka.core.converters.Loader.class);
      convEd.setValue(new reconcile.weka.core.converters.CSVLoader());
      ((GenericObjectEditor.GOEPanel)convEd.getCustomEditor())
	.addOkListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      tryConverter((Loader)convEd.getValue(), f);
	    }
	  });
    } catch (Exception ex) {
    }

    PropertyDialog pd = new PropertyDialog(convEd, 100, 100);
  }

  /**
   * Applies the selected converter
   *
   * @param cnv the converter to apply to the input file
   * @param f the input file
   */
  private void tryConverter(final Loader cnv, final File f) {

    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	  public void run() {
	    try {
	      cnv.setSource(f);
	      Instances inst = cnv.getDataSet();
	      setInstances(inst);
	    } catch (Exception ex) {
	      m_Log.statusMessage(cnv.getClass().getName()+" failed to load "
				 +f.getName());
	      JOptionPane.showMessageDialog(PreprocessPanel.this,
					    cnv.getClass().getName()+" failed to load '"
					    + f.getName() + "'.\n"
					    + "Reason:\n" + ex.getMessage(),
					    "Convert File",
					    JOptionPane.ERROR_MESSAGE);
	      m_IOThread = null;
	      converterQuery(f);
	    }
	    m_IOThread = null;
	  }
	};
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    }
  }

  /**
   * Loads results from a set of instances contained in the supplied
   * file. This is started in the IO thread, and a dialog is popped up
   * if there's a problem.
   *
   * @param f a value of type 'File'
   */
  public void setInstancesFromFile(final File f) {
      
    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  String fileType = f.getName();
	  try {
	    m_Log.statusMessage("Reading from file...");
	    if (f.getName().toLowerCase().endsWith(Instances.FILE_EXTENSION)) {	    
	      fileType = "arff";
	      Reader r = new BufferedReader(new FileReader(f));
	      setInstances(new Instances(r));
	      r.close();
	    } else if (f.getName().toLowerCase().endsWith(CSVLoader.FILE_EXTENSION)) {
	      fileType = "csv";
	      CSVLoader cnv = new CSVLoader();
	      cnv.setSource(f);
	      Instances inst = cnv.getDataSet();
	      setInstances(inst);
	    } else if (f.getName().toLowerCase().endsWith(C45Loader.FILE_EXTENSION)) {
	      fileType = "C45 names";
	      C45Loader cnv = new C45Loader();
	      cnv.setSource(f);
	      Instances inst = cnv.getDataSet();
	      setInstances(inst);
	    } else if (f.getName().toLowerCase().
		       endsWith(Instances.SERIALIZED_OBJ_FILE_EXTENSION)
		       || f.getName().toLowerCase().endsWith(".tmp")) {
	      ObjectInputStream ois = 
		new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));
	      setInstances((Instances)ois.readObject());
	      ois.close();
	    } else {
	      throw new Exception("Unrecognized file type");
	    }
	  } catch (Exception ex) {
	    m_Log.statusMessage("File '" + f.getName() + "' not recognised as an "
				+fileType+" file.");
	    m_IOThread = null;
	    if (JOptionPane.showOptionDialog(PreprocessPanel.this,
					     "File '" + f.getName()
					     + "' not recognised as an "
					     +fileType+" file.\n"
					     + "Reason:\n" + ex.getMessage(),
					     "Load Instances",
					     0,
					     JOptionPane.ERROR_MESSAGE,
					     null,
					     new String[] {"OK", "Use Converter"},
					     null) == 1) {
	    
	      converterQuery(f);
	    }
	  }
	  m_IOThread = null;
	}
      };
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }
  
  /**
   * Loads instances from a database
   *
   * @param iq the InstanceQuery object to load from (this is assumed
   * to have been already connected to a valid database).
   */
  public void setInstancesFromDB(final InstanceQuery iq) {
    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  
	  try {
	    m_Log.statusMessage("Reading from database...");
	    final Instances i = iq.retrieveInstances();
	    SwingUtilities.invokeAndWait(new Runnable() {
	      public void run() {
		setInstances(new Instances(i));
	      }
	    });
	    iq.disconnectFromDatabase();
	  } catch (Exception ex) {
	    m_Log.statusMessage("Problem executing DB query "+m_SQLQ);
	    JOptionPane.showMessageDialog(PreprocessPanel.this,
					  "Couldn't read from database:\n"
					  + ex.getMessage(),
					  "Load Instances",
					  JOptionPane.ERROR_MESSAGE);
	  }

	   m_IOThread = null;
	}
      };

      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
       JOptionPane.showMessageDialog(this,
				    "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Loads instances from a URL.
   *
   * @param u the URL to load from.
   */
  public void setInstancesFromURL(final URL u) {

    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {

	  try {
	    m_Log.statusMessage("Reading from URL...");
	    Reader r = new BufferedReader(
		       new InputStreamReader(u.openStream()));
	    setInstances(new Instances(r));
	    r.close();
	  } catch (Exception ex) {
	    ex.printStackTrace();
	    m_Log.statusMessage("Problem reading " + u);
	    JOptionPane.showMessageDialog(PreprocessPanel.this,
					  "Couldn't read from URL:\n"
					  + u + "\n"
					  + ex.getMessage(),
					  "Load Instances",
					  JOptionPane.ERROR_MESSAGE);
	  }

	  m_IOThread = null;
	}
      };
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Backs up the current state of the dataset, so the changes can be undone.
   */
  public void addUndoPoint() throws Exception {
    
    if (m_Instances != null) {
      // create temporary file
      File tempFile = File.createTempFile("weka", null);
      tempFile.deleteOnExit();

      ObjectOutputStream oos = 
	new ObjectOutputStream(
	new BufferedOutputStream(
	new FileOutputStream(tempFile)));
    
      oos.writeObject(m_Instances);
      oos.flush();
      oos.close();

      // update undo file list
      if (m_tempUndoFiles[m_tempUndoIndex] != null) {
	// remove undo points that are too old
	m_tempUndoFiles[m_tempUndoIndex].delete();
      }
      m_tempUndoFiles[m_tempUndoIndex] = tempFile;
      if (++m_tempUndoIndex >= m_tempUndoFiles.length) {
	// wrap pointer around
	m_tempUndoIndex = 0;
      }

      m_UndoBut.setEnabled(true);
    }
  }

  /**
   * Reverts to the last backed up version of the dataset.
   */
  public void undo() {

    if (--m_tempUndoIndex < 0) {
      // wrap pointer around
      m_tempUndoIndex = m_tempUndoFiles.length-1;
    }
    
    if (m_tempUndoFiles[m_tempUndoIndex] != null) {
      // load instances from the temporary file
      setInstancesFromFile(m_tempUndoFiles[m_tempUndoIndex]);

      // update undo file list
      m_tempUndoFiles[m_tempUndoIndex] = null;
    }
    
    // update undo button
    int temp = m_tempUndoIndex-1;
    if (temp < 0) {
      temp = m_tempUndoFiles.length-1;
    }
    m_UndoBut.setEnabled(m_tempUndoFiles[temp] != null);
  }
  
  /**
   * edits the current instances object in the viewer 
   */
  public void edit() {
    ViewerDialog        dialog;
    int                 result;
    
    dialog = new ViewerDialog(null);
    result = dialog.showDialog(m_Instances);
    if (result == ViewerDialog.APPROVE_OPTION) {
      try {
        addUndoPoint();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      setInstances(dialog.getInstances());
    }
  }
  
  /**
   * Tests out the instance-preprocessing panel from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      final JFrame jf = new JFrame("Weka Explorer: Preprocess");
      jf.getContentPane().setLayout(new BorderLayout());
      final PreprocessPanel sp = new PreprocessPanel();
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      reconcile.weka.gui.LogPanel lp = new reconcile.weka.gui.LogPanel();
      sp.setLog(lp);
      jf.getContentPane().add(lp, BorderLayout.SOUTH);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setSize(800, 600);
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
