/**
 * SummaryViewer.java Aug 18, 2004
 * 
 * @author aek23
 */

package reconcile.visualTools.predictionViewer;

import java.io.File;

import javax.swing.UIManager;

import reconcile.data.Corpus;
import reconcile.data.CorpusFile;
import reconcile.data.Document;

/**
 * This class contains the main() method but all it really does is create and call the MainWindow class
 */

public class CorpusPredictionViewer {

public static MainWindow mw;

private static void createAndShowGUI(Iterable<Document> corpus)
{
  // MainWindow mw = new MainWindow();
  mw = new MainWindow(corpus);
}

public static void main(String[] args)
{

  try {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
  }
  catch (Exception e) {
  }

  CorpusPredictionViewer editor = new CorpusPredictionViewer();
  editor.run(args);

}

Corpus c = null;
/**
 * @param args
 */
private void run(String[] args)
{
  File file = new File(args[0]);
  if (file.isDirectory()) {
    c = new CorpusFile(file);
  }
  else {
    throw new RuntimeException("must specify a directory that contains documents or a raw.txt file");
  }

  // creating and showing this application's GUI.
  javax.swing.SwingUtilities.invokeLater(new Runnable() {

    public void run()
    {
      createAndShowGUI(c);
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
