/**
 * MainMenuBar.java Aug 18, 2004
 * 
 * @author aek23
 */

package reconcile.visualTools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import reconcile.data.Annotation;
import reconcile.data.AnnotationReaderBytespan;
import reconcile.data.AnnotationReaderTipster;
import reconcile.data.AnnotationSet;


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
JMenuItem openTextFile, exitProgram, openAnnSet, annSetDiff; // nullTargetsCombined,
String openTextFileString = "Open Text File", exitProgramString = "Exit", openAnnSetString = "Open Annotation Set",
    annSetDiffString = "Compute Annotation Set Diff";
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
  openTextFile = new JMenuItem(openTextFileString);
  openAnnSet = new JMenuItem(openAnnSetString);
  annSetDiff = new JMenuItem(annSetDiffString);
  exitProgram = new JMenuItem(exitProgramString);

  fileMenu.add(openTextFile);
  fileMenu.add(openAnnSet);
  fileMenu.add(annSetDiff);
  fileMenu.addSeparator();
  // fileMenu.add(recalculate);
  // fileMenu.addSeparator();
  fileMenu.add(exitProgram);

  openTextFile.addActionListener(this);
  openAnnSet.addActionListener(this);
  annSetDiff.addActionListener(this);
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

    if ((source.getText().compareTo(openTextFileString)) == 0) {

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
    else if ((source.getText().compareTo(openAnnSetString)) == 0) {
      String dirName = mainWindow.getDirName();
      if (dirName == null) {
        JOptionPane.showMessageDialog(this, "No text file open", "Opening error", JOptionPane.ERROR_MESSAGE);
      }
      else {
        // fileChooser.setCurrentDirectory(new File(dirName));
        returnVal = fileChooser.showOpenDialog(mainWindow);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          try {
            filename = fileChooser.getSelectedFile().getCanonicalPath();
            System.out.println("You chose to open this file: " + filename);
            // distinguish file to be opened (.xml, .sum1, and .mpqa )
            String separator = System.getProperty("file.separator");
            AnnotationSet newAn;
            if (filename.endsWith(".ng")) {
              newAn = (new AnnotationReaderTipster()).read(new FileInputStream(filename), filename
                  .substring(filename.lastIndexOf(separator) + 1));
            }
            else {
              newAn = (new AnnotationReaderBytespan()).read(new FileInputStream(filename), filename.substring(filename
                  .lastIndexOf(separator) + 1));
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
    else if ((source.getText().compareTo(annSetDiffString)) == 0) {
      String dirName = mainWindow.getDirName();
      if (dirName == null) {
        JOptionPane.showMessageDialog(this, "No text file open", "Opening error", JOptionPane.ERROR_MESSAGE);
      }
      else {
        // fileChooser.setCurrentDirectory(new File(dirName));
        returnVal = fileChooser.showOpenDialog(mainWindow);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          try {
            filename = fileChooser.getSelectedFile().getCanonicalPath();
            System.out.println("You chose to open this file: " + filename);
            // distinguish file to be opened (.xml, .sum1, and .mpqa )
            String separator = System.getProperty("file.separator");
            AnnotationSet newAn1, newAn2;
            newAn1 = (new AnnotationReaderBytespan()).read(new FileInputStream(filename), filename
                .substring(filename.lastIndexOf(separator) + 1));

            // fileChooser.setCurrentDirectory(new File(dirName));
            returnVal = fileChooser.showOpenDialog(mainWindow);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
              filename = fileChooser.getSelectedFile().getCanonicalPath();
              System.out.println("You chose to open this file: " + filename);
              // distinguish file to be opened (.xml, .sum1, and .mpqa )
              newAn2 = (new AnnotationReaderBytespan()).read(new FileInputStream(filename), filename.substring(filename
                  .lastIndexOf(separator) + 1));

              AnnotationSet diff1 = new AnnotationSet(newAn1.getName() + "/" + newAn2.getName());
              for (Annotation a : newAn1) {
                if (!newAn2.containsSpan(a)) {
                  diff1.add(a);
                }
              }
              AnnotationSet diff2 = new AnnotationSet(newAn2.getName() + "/" + newAn1.getName());
              for (Annotation a : newAn2) {
                if (!newAn1.containsSpan(a)) {
                  diff2.add(a);
                }
              }
              mainWindow.addAnnotationSet(diff1);
              mainWindow.addAnnotationSet(diff2);
              mainWindow.resetFileDisplayState(false);
            }
          }
          catch (IOException ex) {
            System.out.println("Error: Java IOException");
            JOptionPane.showMessageDialog(null, "Error Opening File: " + ex, "Error", JOptionPane.WARNING_MESSAGE);
          }
        }
      }

    }
    else if ((source.getText().compareTo(exitProgramString)) == 0) {

      System.exit(0);
    }
  }
}
}
