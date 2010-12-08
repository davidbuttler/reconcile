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
 *    SetInstancesPanel.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package reconcile.weka.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import reconcile.weka.core.Instances;


/** 
 * A panel that displays an instance summary for a set of instances and
 * lets the user open a set of instances from either a file or URL.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class SetInstancesPanel extends JPanel {
  
  /** Click to open instances from a file */
  protected JButton m_OpenFileBut = new JButton("Open file...");

  /** Click to open instances from a URL */
  protected JButton m_OpenURLBut = new JButton("Open URL...");

  /** The instance summary component */
  protected InstancesSummaryPanel m_Summary = new InstancesSummaryPanel();
  
  /** Filter to ensure only arff files are selected */  
  protected FileFilter m_ArffFilter =
    new ExtensionFileFilter(Instances.FILE_EXTENSION, "Arff data files");

  /** The file chooser for selecting arff files */
  protected JFileChooser m_FileChooser
    = new JFileChooser(new File(System.getProperty("user.dir")));

  /** Stores the last URL that instances were loaded from */
  protected String m_LastURL = "http://";

  /** The thread we do loading in */
  protected Thread m_IOThread;

  /**
   * Manages sending notifications to people when we change the set of
   * working instances.
   */
  protected PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** The current set of instances loaded */
  protected Instances m_Instances;
  
  /**
   * Create the panel.
   */
  public SetInstancesPanel() {

    m_OpenFileBut.setToolTipText("Open a set of instances from a file");
    m_OpenURLBut.setToolTipText("Open a set of instances from a URL");
    m_FileChooser.setFileFilter(m_ArffFilter);
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    m_OpenURLBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setInstancesFromURLQ();
      }
    });
    m_OpenFileBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setInstancesFromFileQ();
      }
    });
    m_Summary.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

    JPanel buttons = new JPanel();
    buttons.setLayout(new GridLayout(1, 2));
    buttons.add(m_OpenFileBut);
    buttons.add(m_OpenURLBut);
    
    setLayout(new BorderLayout());
    add(m_Summary, BorderLayout.CENTER);
    add(buttons, BorderLayout.SOUTH);
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
	final File selected = m_FileChooser.getSelectedFile();
	m_IOThread = new Thread() {
	  public void run() {
	    setInstancesFromFile(selected);
	    m_IOThread = null;
	  }
	};
	m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
	m_IOThread.start();
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
	  final URL url = new URL(urlName);
	  m_IOThread = new Thread() {
	    public void run() {
	      setInstancesFromURL(url);
	      m_IOThread = null;
	    }
	  };
	  m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
	  m_IOThread.start();
	}
      } catch (Exception ex) {
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
   * Loads results from a set of instances contained in the supplied
   * file.
   *
   * @param f a value of type 'File'
   */
  protected void setInstancesFromFile(File f) {
      
    try {
      Reader r = new BufferedReader(new FileReader(f));
      setInstances(new Instances(r));
      r.close();
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this,
				    "Couldn't read from file:\n"
				    + f.getName(),
				    "Load Instances",
				    JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Loads instances from a URL.
   *
   * @param u the URL to load from.
   */
  protected void setInstancesFromURL(URL u) {

    try {
      Reader r = new BufferedReader(new InputStreamReader(u.openStream()));
      setInstances(new Instances(r));
      r.close();
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this,
				    "Couldn't read from URL:\n"
				    + u,
				    "Load Instances",
				    JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Updates the set of instances that is currently held by the panel
   *
   * @param i a value of type 'Instances'
   */
  public void setInstances(Instances i) {

    m_Instances = i;
    m_Summary.setInstances(m_Instances);
    // Fire property change event for those interested.
    m_Support.firePropertyChange("", null, null);
  }

  /**
   * Gets the set of instances currently held by the panel
   *
   * @return a value of type 'Instances'
   */
  public Instances getInstances() {
    
    return m_Instances;
  }

  /**
   * Gets the instances summary panel associated with
   * this panel
   * @return the instances summary panel
   */
  public InstancesSummaryPanel getSummary() {
    return m_Summary;
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
}
