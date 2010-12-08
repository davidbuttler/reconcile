package reconcile.data;

import gov.llnl.text.util.FileUtils;
import gov.llnl.text.util.RecursiveFileIterable;
import gov.llnl.text.util.Timer;

import java.io.File;
import java.io.IOException;

import reconcile.SystemConfig;
import reconcile.general.Utils;
import reconcile.util.File2DocIterable;
import reconcile.util.Filters;
import reconcile.util.ParentDocument;

public class CorpusFile
    extends Corpus
    implements Iterable<Document> {

private File mFileMetaDataDir;

/**
 * Create a corups with a given root directory, and configure the system with the given configuration file
 * 
 * @param rootDir
 * @param configFile
 */
public CorpusFile(File rootDir, File configFile) {
  mRootDir = rootDir;
  Utils.setConfig(configFile.getAbsolutePath());
}


/**
 * Create a corups with a given root directory, and configure the system with the given configuration file
 * 
 * @param rootDir
 * @param configFilename
 */
public CorpusFile(File rootDir, String configFilename) {
  mRootDir = rootDir;
  Utils.setConfig(configFilename);
}

/**
 * Create a corpus with a given root directory. Note that this assumes that the system has already been configured or is
 * using the default config
 * 
 * @param rootDir
 */
public CorpusFile(File rootDir) {
  mRootDir = rootDir;
}


/**
 * @param rootDir
 *          the root dir of the corpus (in case you want to store corpus metadata
 * @param fileNames
 *          a list of files that comprise the corpus
 * @throws IOException
 */
public CorpusFile(File rootDir, Iterable<File> fileNames)
    throws IOException {
  mIterable = new File2DocIterable(fileNames);
  mRootDir = rootDir;
}

/**
 * @param docs
 *          a list of documents that comprise the corpus
 * @param rootDir
 *          the root dir of the corpus (in case you want to store corpus metadata
 * @throws IOException
 */
public CorpusFile(Iterable<Document> docs, File rootDir)
    throws IOException {
  mIterable = docs;
  mRootDir = rootDir;
}

/**
 * @param rootDir
 * @param cfg
 */
public CorpusFile(File rootDir, SystemConfig cfg) {
  mRootDir = rootDir;
  Utils.setConfig(cfg);
}

@Override
public Iterable<Document> iterable()
    throws IOException
{
  if (mIterable == null) {
    mIterable = new ParentDocument(RecursiveFileIterable.iterate(mRootDir, Filters.rawFileFilter));
  }
  return mIterable;
}


/**
 * Given a corpus of text, move it into the system format so that we can store various types of metadata and annotations
 * as files surrounding the original text.
 * 
 * @param originalCorpus
 * @throws IOException
 */
@Override
public void reformatCorpus(File originalDir)
    throws IOException
{
  Iterable<File> originalCorpus = RecursiveFileIterable.iterate(originalDir);

  Timer t = new Timer(10);
  for (File f : originalCorpus) {
    if (f.length() < 5) {
      // System.out.println("ORIG file too small");
      continue;
    }

    File outDir = FileUtils.getParallelOutputFile(originalDir, f, mRootDir);
    Document doc = new Document(outDir);
    doc.setRawText(FileUtils.readFile(f));
    t.increment();

  }
  t.end();

}

///**
// * @param outputDir
// * @throws IOException
// */
//public static void mkdir(File dir)
//    throws IOException
//{
//  // File.umount();
//  if (dir.exists() && dir.isDirectory()) return;
//  boolean success = dir.mkdirs();
//  if (!success) // System.out.println("could not make dir: "+dir.getAbsolutePath());
//    throw new IOException("could not make dir: " + dir.getAbsolutePath());
//
//}

/**
 * @param string
 * @return
 */
@Override
public File getDataFile(String fileName)
{
  File f = new File(getFileMetadataDir(), fileName);
  return f;
}

/**
 * @return
 */
private File getFileMetadataDir()
{
  try {
    if (mFileMetaDataDir == null) {
      mFileMetaDataDir = new File(mRootDir, "fileMetaData");
      if (!mFileMetaDataDir.exists()) {
        mkdir(mFileMetaDataDir);
      }
    }
    return mFileMetaDataDir;
  }
  catch (IOException e) {
    e.printStackTrace();
  }
  return null;
}

}
