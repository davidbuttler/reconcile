/**
 * SummaryViewer.java Aug 18, 2004
 * 
 * @author aek23
 */

package reconcile.visualTools.annotation;

import javax.swing.UIManager;

/**
 * This class contains the main() method but all it really does is create and call the MainWindow class
 */

public class AnnotationViewer {

public static MainWindow mw;

private static void createAndShowGUI()
{
  // MainWindow mw = new MainWindow();
  mw = new MainWindow();
}

public static void main(String[] args)
{

  try {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
  }
  catch (Exception e) {
  }

  // creating and showing this application's GUI.
  javax.swing.SwingUtilities.invokeLater(new Runnable() {

    public void run()
    {
      createAndShowGUI();
    }
  });
}

/**
 * @return Returns the mw.
 */
public static MainWindow getMw()
{
  return mw;
}
}
