/**
 * SummaryViewer.java Aug 18, 2004
 * 
 * @author aek23
 */

package reconcile.visualTools.annotator;

import java.io.File;
import java.io.IOException;

import javax.swing.UIManager;

import reconcile.data.Corpus;
import reconcile.data.CorpusFile;
import reconcile.data.Document;

import com.google.common.collect.Lists;

/**
 * This class contains the main() method but all it really does is create and call the MainWindow class
 */

public class CorpusAnnotationEditor {

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
    CorpusAnnotationEditor editor = new CorpusAnnotationEditor();
    editor.run(args);
  }
  catch (Exception e) {
    e.printStackTrace();
  }


}

Corpus c = null;

/**
 * @param args
 * @throws IOException
 */
private void run(String[] args)
    throws IOException
{
  File file = new File(args[0]);
  if (file.isDirectory()) {
    c = new CorpusFile(file);
  }
  else {
    c = new CorpusFile(new File("."), Lists.newArrayList(file));
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
