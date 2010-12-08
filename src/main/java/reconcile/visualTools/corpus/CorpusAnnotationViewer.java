/**
 * SummaryViewer.java Aug 18, 2004
 * 
 * @author aek23
 */

package reconcile.visualTools.corpus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import reconcile.data.Corpus;
import reconcile.data.CorpusFile;
import reconcile.data.Document;

/**
 * This class contains the main() method but all it really does is create and call the MainWindow class
 */

public class CorpusAnnotationViewer {

public static MainWindow mw;

private static void createAndShowGUI(List<Document> corpus)
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

  Corpus c = new CorpusFile(new File(args[0]));
  Corpus.setConfig(args[1]);
  final List<Document> list = new ArrayList<Document>();
  for (Document d : c) {
    list.add(d);
  }

  // creating and showing this application's GUI.
  javax.swing.SwingUtilities.invokeLater(new Runnable() {

    public void run()
    {
      createAndShowGUI(list);
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
