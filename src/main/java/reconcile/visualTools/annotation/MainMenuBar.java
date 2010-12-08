/**
 * MainMenuBar.java Aug 18, 2004
 * 
 * @author aek23
 */

package reconcile.visualTools.annotation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import reconcile.data.AnnotationReaderBytespan;
import reconcile.data.AnnotationReaderTipster;
import reconcile.data.AnnotationSet;
import reconcile.visualTools.util.AnnotationFileFilter;

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
JMenuItem openTextFile, exitProgram, openAnnSet; // nullTargetsCombined,

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
  openTextFile = new JMenuItem("Open Text File");
  openAnnSet = new JMenuItem("Open Annotation Set");
  exitProgram = new JMenuItem("Exit");

  fileMenu.add(openTextFile);
  fileMenu.add(openAnnSet);
  fileMenu.addSeparator();
  // fileMenu.add(recalculate);
  // fileMenu.addSeparator();
  fileMenu.add(exitProgram);

  openTextFile.addActionListener(this);
  openAnnSet.addActionListener(this);
  // recalculate.addActionListener(this);
  exitProgram.addActionListener(this);
  // file chooser stuff
  fileChooser = new JFileChooser();
  fileChooser.addChoosableFileFilter(new AnnotationFileFilter("xml", "XML Annotation Files"));
  fileChooser.addChoosableFileFilter(new AnnotationFileFilter("txt", "Text Files"));
}

public void actionPerformed(ActionEvent e)
{
  if (e.getSource() instanceof JMenuItem) {
    JMenuItem source = (JMenuItem) e.getSource();

    if ((source.getText().compareTo("Open Text File")) == 0) {

      returnVal = fileChooser.showOpenDialog(mainWindow);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        try {
          filename = fileChooser.getSelectedFile().getCanonicalPath();
          System.out.println("You chose to open this file: " + filename);

          mainWindow.setFilename(filename);
          mainWindow.clearAnnotations();
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
    else if ((source.getText().compareTo("Open Annotation Set")) == 0) {
      String dirName = mainWindow.getDoc().getDocumentId();
      if (dirName == null) {
        JOptionPane.showMessageDialog(this, "No text file open", "Opening error", JOptionPane.ERROR_MESSAGE);
      }
      else {
        fileChooser.setCurrentDirectory(new File(dirName));
        returnVal = fileChooser.showOpenDialog(mainWindow);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          try {
            filename = fileChooser.getSelectedFile().getCanonicalPath();
            System.out.println("You chose to open this file: " + filename);
            // distinguish file to be opened (.xml, .sum1, and .mpqa )
            String separator = System.getProperty("file.separator");
            AnnotationSet newAn;
            if (filename.endsWith(".ng")) {
              newAn = (new AnnotationReaderTipster()).read(new FileInputStream(filename), filename.substring(filename
                  .lastIndexOf(separator)));
            }
            else {
              newAn = (new AnnotationReaderBytespan()).read(new FileInputStream(filename), filename.substring(filename
                  .lastIndexOf(separator)));
            }

            mainWindow.addAnnotationSet(newAn);
            mainWindow.resetFileDisplayState(false);

          }
          catch (IOException ex) {
            System.out.println("Error: Java IOException");
            JOptionPane.showMessageDialog(null, "Error Opening File: " + ex, "Error", JOptionPane.WARNING_MESSAGE);
          }
        }
      }
    }
    else if ((source.getText().compareTo("Exit")) == 0) {

      System.exit(0);
    }
  }
}
}
