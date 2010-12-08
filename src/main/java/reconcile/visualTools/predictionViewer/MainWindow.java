/**
 * MainWindow.java Aug 3, 2007
 * 
 * @author ves
 */

package reconcile.visualTools.predictionViewer;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.visualTools.util.DocHolder;
import reconcile.visualTools.util.HighlightListener;
import reconcile.visualTools.util.MouseController;
import reconcile.visualTools.util.TextViewPanel;

/**
 * The main window for the annotation visualization application
 * 
 */

public class MainWindow
    extends JFrame
    implements DocHolder, HighlightListener {

/**
   * 
   */
private static final long serialVersionUID = 1L;

public static Font font = new Font(null, Font.PLAIN, 16);

// The directory main directory name
public Document doc = null;

/* ----------------------------------
   GUI layout
 ------------------------------------
 */
TextViewPanel tvPanel;

PredictionListPanel predictionPanel;

/* These split panes are used to layout the various panels in the GUI interface
 */
private JSplitPane textSplitPane;
private JSplitPane mainSplitPane;

/* This is a controller that responds to mouse movements
 */
MouseController mc;

/* AbstractAction - binds certain keys and key combinations to certain action, so that pressing the
               <DELETE> key deletes the selected node.
 */
AbstractAction actionMap;

// have the xml file data been put into sources, opinions, entities, targets?
boolean dataCollected = false;

// has the appropriate raw text file been retrieved (either through FTP or network mapping) and displayed
// in the panel?
boolean fileDisplayed = false;

private JPanel buttonPanel;

private JTextField fileNameField;


private Iterable<Document> corpus;

private Iterator<Document> corpusIndex;

PredictionEditPanel editPanel;

/**
 * This constructor sets up much of the functionality of the GUI, including the actual layout of the components of the
 * GUI, as well as handling the graph implementation.
 * 
 */

public MainWindow(Iterable<Document> corp) {

  corpus = corp;
  corpusIndex = corpus.iterator();
  doc = corpusIndex.next();
  /*
   * GUI Layout Creation
   */

  // create the OverviewPanel which shows a reduced size snapshot of the entire graph
  predictionPanel = new PredictionListPanel(this);
  predictionPanel.setFont(font);
  tvPanel = new TextViewPanel(this);
  tvPanel.setFont(font);

  textSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, predictionPanel, tvPanel);
  textSplitPane.setDividerLocation(300);

  buttonPanel = createForwardBackPanel(doc);
  buttonPanel.setFont(font);
  editPanel = new PredictionEditPanel(this);
  editPanel.setFont(font);

  JSplitPane bottomSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editPanel, buttonPanel);
  bottomSplitPane.setDividerLocation(120);
  mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, textSplitPane, bottomSplitPane);
  mainSplitPane.setDividerLocation(700);

  getContentPane().add(mainSplitPane);
  // splitPane1.setDividerLocation(0.8);
  this.setTitle("Prediction Viewer");
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  displayRawText();
  pack();
  setSize(1200, 900);
  setVisible(true);
}

public void nextFile()
{
  if (!corpusIndex.hasNext()) {
    corpusIndex = corpus.iterator();
  }
  setDoc(corpusIndex.next());
    
}

public void previousFile()
{
  corpusIndex = corpus.iterator();
  setDoc(corpusIndex.next());
}

/**
 * @param doc
 * @return
 */
private JPanel createForwardBackPanel(Document doc)
{
  JPanel panel = new JPanel();
  JButton prev = new JButton("Prev");
  prev.setFont(font);
  prev.addActionListener(new ActionListener() {

    public void actionPerformed(java.awt.event.ActionEvent e)
    {
      previousFile();
    };
  });
  JButton next = new JButton("Next");
  next.setFont(font);
  next.addActionListener(new ActionListener() {

    public void actionPerformed(java.awt.event.ActionEvent e)
    {
      nextFile();
    };
  });
  fileNameField = new JTextField(doc.getDocumentId());
  fileNameField.setEditable(false);
  fileNameField.setFont(font);

  panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
  panel.add(prev);
  panel.add(Box.createHorizontalGlue());
  panel.add(fileNameField);
  panel.add(Box.createHorizontalGlue());
  panel.add(next);
  return panel;
}

/**
 * keeps track of whether a file is opened or not
 */

public void resetFileDisplayState(boolean state)
{
  fileDisplayed = state;
}

/**
 * is there summary data available yet?
 */

public boolean dataAvailable()
{
  if (doc != null) return true;
  return false;
}

/**
 * displays the raw text of the document in the text panel
 * 
 * @param xmlFile
 *          the XML Summary File
 */

public void displayRawText()
{
  tvPanel.clearPanel();
  tvPanel.setInitialText(doc.getText());
  fileDisplayed = true;
}

public void setDoc(Document document)
{
  predictionPanel.clear();

  doc = document;

  tvPanel.clearHighlights();
  displayRawText();

  predictionPanel.redraw();
  fileNameField.setText(doc.getDocumentId());
}

public Document getDoc()
{
  return doc;
}

/**
 * given an AgentID, find the text span associated with this Agent and highlight the appropriate areas in the raw text
 * panel
 * 
 * @param agentRef
 *          the AgentID
 */
public void highlightText(Annotation an, Color highlightTextColor)
{

  if (an != null) {
    // highlight the annotation
    // also define color to the highlight
    tvPanel.highlightSpan(an.getStartOffset(), an.getEndOffset() - an.getStartOffset(), highlightTextColor);

  }
}

/**
 * highlights text
 * 
 * @param start
 *          the offset at which to start highlighting
 * @param end
 *          the offset at which to finish highlighting
 */
public void highlightText(int start, int end, Color highlightTextColor)
{
  // also define color to the highlight
  tvPanel.highlightSpan(start, end - start, highlightTextColor);
}

/**
 * clears the raw text panel of all highlights
 */
public void clearHighlights()
{
  tvPanel.clearHighlights();
}

/* (non-Javadoc)
 * @see reconcile.visualTools.util.HighlightListener#setHighlight(int, int)
 */
public void setHighlight(int min, int max)
{

}
}
