/**
 * MainWindow.java Aug 3, 2007
 * 
 * @author ves
 */

package reconcile.visualTools;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JSplitPane;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.general.Utils;


/**
 * The main window for the annotation visualization application
 * 
 */

public class MainWindow
    extends JFrame {

/**
   * 
   */
private static final long serialVersionUID = 1L;
ArrayList<AnnotationSet> annotSets;
// The directory main directory name
public String dirName = null;

/* ----------------------------------
   GUI layout
 ------------------------------------
 */
TextViewPanel tvPanel;
AnnotationListPanel aPanel;

/* These split panes are used to layout the various panels in the GUI interface
 */
JSplitPane splitPane1, splitPane2, splitPane3, splitPane4, splitPane5;

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

/**
 * This constructor sets up much of the functionality of the GUI, including the actual layout of the components of the
 * GUI, as well as handling the graph implementation.
 * 
 */

public MainWindow() {

  /*
   * GUI Layout Creation
   */

  // create the file menus
  MainMenuBar mainMenu = new MainMenuBar(this);

  // create the OverviewPanel which shows a reduced size snapshot of the entire graph
  aPanel = new AnnotationListPanel(this);
  tvPanel = new TextViewPanel();

  splitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, aPanel, tvPanel);
  splitPane1.setDividerLocation(300);

  getContentPane().add(splitPane1);
  // splitPane1.setDividerLocation(0.8);
  this.setTitle("Annotation Viewer");
  setJMenuBar(mainMenu);
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  pack();
  setSize(1200, 900);
  setVisible(true);
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
  if (annotSets != null) return true;
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

  rawText = Utils.getText(dirName);
  tvPanel.setInitialText(rawText);

  fileDisplayed = true;
}

/**
 * New data -- diplay the annotations
 */

public void addAnnotationSet(AnnotationSet a)
{
  if (annotSets == null) {
    annotSets = new ArrayList<AnnotationSet>();
  }
  annotSets.add(a);
  aPanel.redraw();
}

public void clearAnnotations()
{
  annotSets = new ArrayList<AnnotationSet>();
  aPanel.redraw();
}

public void setFilename(String filename)
{
  String separator = System.getProperty("file.separator");
  dirName = filename.substring(0, filename.lastIndexOf(separator));
}

public String getDirName()
{
  return dirName;
}

/**
 * parses the filename to return the correct file name
 * 
 * @param name
 *          the filename
 * @param format
 *          either "Unix" or "Windows"
 */

public String getFilteredFileName(String name, String format)
{
  String[] tokens = name.split("\\\\");

  String toReturn = "";
  if (format.equals("Unix")) {
    toReturn = tokens[tokens.length - 2] + "/" + (tokens[tokens.length - 1].split(".xml")[0]);
  }
  else {
    toReturn = tokens[tokens.length - 2] + "\\" + (tokens[tokens.length - 1].split(".xml")[0]);
  }
  System.out.println("Getting : " + toReturn);
  return toReturn;
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
