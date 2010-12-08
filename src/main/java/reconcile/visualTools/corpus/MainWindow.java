/**
 * MainWindow.java Aug 3, 2007
 * 
 * @author ves
 */

package reconcile.visualTools.corpus;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.List;

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
import reconcile.visualTools.util.MouseController;
import reconcile.visualTools.util.TextViewPanel;

/**
 * The main window for the annotation visualization application
 * 
 */

public class MainWindow
    extends JFrame
    implements DocHolder {

/**
   * 
   */
private static final long serialVersionUID = 1L;

// The directory main directory name
public Document doc = null;

/* ----------------------------------
   GUI layout
 ------------------------------------
 */
TextViewPanel tvPanel;

AnnotationListPanel aPanel;

/* These split panes are used to layout the various panels in the GUI interface
 */
private JSplitPane annotationSplitPane;
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

AnnotationSetListPanel aSetPanel;

private List<Document> corpus;

private int corpusIndex;

/**
 * This constructor sets up much of the functionality of the GUI, including the actual layout of the components of the
 * GUI, as well as handling the graph implementation.
 * 
 */

public MainWindow(List<Document> corp) {

  corpus = corp;
  corpusIndex = 0;
  doc = corp.get(corpusIndex);
  /*
   * GUI Layout Creation
   */

  // create the OverviewPanel which shows a reduced size snapshot of the entire graph
  aSetPanel = new AnnotationSetListPanel(this);
  aPanel = new AnnotationListPanel(this);
  annotationSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, aSetPanel, aPanel);
  annotationSplitPane.setDividerLocation(200);
  tvPanel = new TextViewPanel(null);

  textSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, annotationSplitPane, tvPanel);
  textSplitPane.setDividerLocation(300);

  buttonPanel = createForwardBackPanel(doc);
  mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, textSplitPane, buttonPanel);
  mainSplitPane.setDividerLocation(840);

  getContentPane().add(mainSplitPane);
  // splitPane1.setDividerLocation(0.8);
  this.setTitle("Annotation Viewer");
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  displayRawText();
  pack();
  setSize(1200, 900);
  setVisible(true);
}

public void nextFile()
{
  corpusIndex = (corpusIndex + 1) % corpus.size();
  setDoc(corpus.get(corpusIndex));
}

public void previousFile()
{
  corpusIndex = (corpusIndex - 1);
  if (corpusIndex < 0) {
    corpusIndex = corpus.size() - 1;
  }
  setDoc(corpus.get(corpusIndex));
}

/**
 * @param doc
 * @return
 */
private JPanel createForwardBackPanel(Document doc)
{
  JPanel panel = new JPanel();
  JButton prev = new JButton("Prev");
  prev.addActionListener(new ActionListener() {

    public void actionPerformed(java.awt.event.ActionEvent e)
    {
      previousFile();
    };
  });
  JButton next = new JButton("Next");
  next.addActionListener(new ActionListener() {

    public void actionPerformed(java.awt.event.ActionEvent e)
    {
      nextFile();
    };
  });
  fileNameField = new JTextField(doc.getDocumentId());
  fileNameField.setEditable(false);

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

/* ----------------------------------------------------
 
 NOTE:  The rest of the methods have to do with interface layout
 
 ------------------------------------------------------ */

/**
 * takes an xml summary file name and displays the raw text of the file in the text panel
 * 
 * @param xmlFile
 *          the XML Summary File
 */

public void displayRawText()
{
  String rawText = "";

  tvPanel.clearPanel();

  rawText = doc.getText();
  tvPanel.setInitialText(rawText);

  fileDisplayed = true;
}

public void setDoc(Document document)
{
  doc = document;
  aSetPanel.redraw();
  aPanel.redraw();
  displayRawText();
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

}
