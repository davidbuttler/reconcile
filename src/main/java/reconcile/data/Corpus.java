package reconcile.data;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Stack;

import reconcile.SystemConfig;
import reconcile.general.Utils;

public abstract class Corpus
    implements Iterable<Document> {

public static final String STOP_FILE = "stop_word_file";
protected File mRootDir;
protected Iterable<Document> mIterable;

/**
 * allow sub classes to override behavior
 */
protected Corpus() {

}

public Corpus(File rootDir, Iterable<Document> corpusDocs) {
  mRootDir = rootDir;
  mIterable = corpusDocs;
}

public abstract Iterable<Document> iterable()
    throws IOException;


/**
 * Given a corpus of text, move it into the system format so that we can store various types of metadata and annotations
 * as files surrounding the original text.
 * 
 * @param originalCorpus
 * @throws IOException
 */
public abstract void reformatCorpus(File originalDir)
    throws IOException;

/**
 * @param outputDir
 * @throws IOException
 */
public static void mkdir(File dir)
    throws IOException
{
  // File.umount();
  if (dir.exists() && dir.isDirectory()) return;
  boolean success = dir.mkdirs();
  if (!success) // System.out.println("could not make dir: "+dir.getAbsolutePath());
    throw new IOException("could not make dir: " + dir.getAbsolutePath());

}

/**
 * @param string
 * @return
 */
public abstract File getDataFile(String fileName);

/**
 * @return
 */
public static SystemConfig getConfig()
{
  return Utils.getConfig();
}

/**
 * @param configFile
 */
public static void setConfig(String configFile)
{
  Utils.setConfig(configFile);
}

/**
 * @return
 */
public File getRootDir()
{
  return mRootDir;
}

/* (non-Javadoc)
 * @see java.lang.Iterable#iterator()
 */
public Iterator<Document> iterator()
{
  try {
    return iterable().iterator();
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }

}

/**
 * @param f
 * @throws IOException
 */
public static void delete(File f)
    throws IOException
{
  boolean s = f.delete();
  if (!s) throw new IOException("failed to delete " + f.getAbsolutePath());

}

/**
 * utility method to remove all of the junk that reconcile puts into the Document directories (like annotations, cluster
 * results, feature vectors, etc.)
 * 
 * @throws IOException
 */
public int clean()
    throws IOException
{
  int count = 0;
  for (Document d : this) {
    count += d.clean();
  }
  return count;
}

/**
 * utility method to delete feature files
 * 
 * @throws IOException
 */
public void cleanTrain()
    throws IOException
{
  System.out.println("Corpus: running cleanTrain()");
  for (Document d : this) {
    d.deleteFeatureFile();
  }
}

/**
 * utility method to delete prediction, cluster, and feature files
 * 
 * @throws IOException
 */
public void cleanTest()
    throws IOException
{
  System.out.println("Corpus: running cleanTest()");
  for (Document d : this) {
    d.deleteClusterFile();
    d.deletePredictionFile();
    d.deleteFeatureFile();
  }
}

/**
 * @param f
 * @throws IOException
 */
public static int recursivelyDelete(File fileToDelete)
    throws IOException
{
  Stack<File> s = new Stack<File>();
  s.push(fileToDelete);
  int count = 0;

  while (s.size() > 0) {
    File f = s.pop();
    if (f.exists()) {
      if (f.isDirectory()) {
        java.io.File[] list = f.listFiles();
        if (list.length > 0) {
          s.push(f);
          for (java.io.File child : list) {
            s.push(child);
          }
        }
        else {
          delete(f);
          count++;
        }
      }
      else {
        delete(f);
        count++;
      }
    }
  }

  return count;
}
}
