/**
 * MainMenuBar.java Aug 18, 2004
 * 
 * @author aek23
 */

package reconcile.visualTools.npPairViewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * This class creates the Menubar at the top of the screen as well as implement its functionality
 */

public class MainMenuBar
    extends JMenuBar
    implements ActionListener {

/**
   * 
   */
private static final long serialVersionUID = 1L;

JMenu fileMenu, editMenu, modeMenu, layoutMenu;

/**
 * Null
 * 
 * JMenu combineNullTargetsMenu;
 */
// File
JMenuItem openFile, exitProgram; // nullTargetsCombined,

/** Null */
JFileChooser fileChooser, saveFileChooser;

int returnVal;

String filename;

// Summary summaryContents;
MainWindow mainWindow;

ButtonGroup modeGroup, layoutGroup;

/**
 * Null ButtonGroup combineNullTargetsGroup;
 */

public MainMenuBar(MainWindow parentWindow) {

  mainWindow = parentWindow;

  fileMenu = new JMenu("File");

  add(fileMenu);

  // File Menu
  openFile = new JMenuItem("Open File");
  exitProgram = new JMenuItem("Exit");

  fileMenu.add(openFile);
  fileMenu.addSeparator();
  // fileMenu.add(recalculate);
  // fileMenu.addSeparator();
  fileMenu.add(exitProgram);

  openFile.addActionListener(this);
  // recalculate.addActionListener(this);
  exitProgram.addActionListener(this);
  // file chooser stuff
  fileChooser = new JFileChooser();
  // fileChooser.addChoosableFileFilter(new AnnotationFileFilter("xml",
  // "XML Annotation Files"));
  // fileChooser.addChoosableFileFilter(new AnnotationFileFilter("txt",
  // "Text Files"));
}

public void actionPerformed(ActionEvent e)
{
  if (e.getSource() instanceof JMenuItem) {
    JMenuItem source = (JMenuItem) e.getSource();

    if ((source.getText().compareTo("Open File")) == 0) {

      returnVal = fileChooser.showOpenDialog(mainWindow);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        try {
          filename = fileChooser.getSelectedFile().getCanonicalPath();
          System.out.println("You chose to open this file: " + filename);

          mainWindow.openFile(filename);
          mainWindow.displayRawText();
          mainWindow.resetFileDisplayState(false);

        }
        catch (IOException ex) {
          System.out.println("Error: Java IOException");
          JOptionPane.showMessageDialog(null, "Error Opening File: " + ex, "Error", JOptionPane.WARNING_MESSAGE);
        }
        // open the file
      }
    }
    else if ((source.getText().compareTo("Exit")) == 0) {

      System.exit(0);
    }
  }
}
}
