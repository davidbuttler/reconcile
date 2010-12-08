/**
 * SummaryViewer.java Aug 18, 2004
 * 
 * @author aek23
 */

package reconcile.visualTools.annotator;

import gov.llnl.text.util.LineIterable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.UIManager;

import reconcile.data.Corpus;
import reconcile.data.CorpusFile;
import reconcile.data.Document;

import com.google.common.collect.Lists;

/**
 * This class contains the main() method but all it really does is create and call the MainWindow class
 */

public class FileCorpusAnnotationEditor {

public static MainWindow mw;
Corpus c = null;

private static void createAndShowGUI(Iterable<Document> corpus)
{
  // MainWindow mw = new MainWindow();
  mw = new MainWindow(corpus);
}

public static void main(String[] args)
{

  try {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

    FileCorpusAnnotationEditor editor = new FileCorpusAnnotationEditor();
    editor.run(args);
  }
  catch (Exception e) {
    e.printStackTrace();
  }

}

/**
 * @param args
 * @throws IOException
 */
private void run(String[] args)
    throws IOException
{
  File file = new File(args[0]);
  List<File> files = Lists.newArrayList();
  for (String line : LineIterable.iterate(file)) {
    files.add(new File(line));
  }
  System.out.println("there are "+files.size()+" files to view");
  c = new CorpusFile(new File("."), files);

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
