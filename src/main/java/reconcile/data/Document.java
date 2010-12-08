package reconcile.data;

import gov.llnl.text.util.FileUtils;
import gov.llnl.text.util.InputStreamLineIterable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import reconcile.SystemConfig;
import reconcile.featureVector.individualFeature.DocNo;
import reconcile.general.Constants;
import reconcile.general.Utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class Document {

public static final Pattern pLeadingNonWord = Pattern.compile("\\A\\W*");
public static final Pattern pNonWordSplit = Pattern.compile("\\W+|\\-");

/**
 * Raw text file name
 */
public final static String RAW_TXT = "raw.txt";
public static final String METADATA_FILE = "metadata.csv";
protected static boolean DEBUG = Constants.DEBUG;
protected static final String FEATURE_FORMAT = "FEATURE_FORMAT";

public static final AnnotationReaderBytespan AnReader = new AnnotationReaderBytespan();
public static final AnnotationWriterBytespan AnWriter = new AnnotationWriterBytespan();

public static String getText(String dirName)
{
  // Find the raw.txt file in the directory
  String rawTextFileName = dirName + Utils.SEPARATOR + RAW_TXT;
  return Utils.getTextFromFile(rawTextFileName);
}
public static String[] getWords(String textSpan)
{
  // remove leading non-word characters
  textSpan = pLeadingNonWord.matcher(textSpan).replaceAll("");
  return pNonWordSplit.split(textSpan);
}

protected File mDir;
private File mClusterSubDir;
private File mClusterFile;
private File mFeatureFile;
private File mFeatureDir;
private File mPredictionDir;
private File mPredictionFile;
private File mAnnotationDir;
protected Map<String, String> mMetaData;
protected String mText;

protected String mDocId;

protected Map<String, AnnotationSet> mAnnotationSets;
protected String mId;

/**
 * This constructor is dangerous because it does not initialize the underlying directory. So, if a subclass is needed,
 * it had better override every method that accesses the mDir member variable.
 */
protected Document() {
  init();
}
public Document(File dir) {
  mDir = dir;
  init();
}

public Document(String dirName) {
  this(new File(dirName));
}

/**
 * all constructors must call this method
 */
protected void init()
{
  mAnnotationSets = Maps.newHashMap();
  mMetaData = Maps.newHashMap();
  try {
    File metadataFile = new File(mDir, METADATA_FILE);
    if (metadataFile.exists() && metadataFile.length() > 0) {
      FileInputStream in = new FileInputStream(metadataFile);
      readMetaData(in, mMetaData);
    }
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }

}

/**
 * add an annotation set to the document directory with the given name. Depending on the <code>write</code> parameter,
 * the annotation set may be written to disk, or just stored in memory
 * 
 * @param set
 * @param annotationSetName
 *          This name will be checked against the config file to see if it needs to be mapped to a different name
 * @param write
 *          A flag determining whether the annotation set should be written to disk or just cached in memory
 * @throws IOException
 */
public void addAnnotationSet(AnnotationSet set, String annotationSetName, boolean write)
{
  try {
    String annSetName = getCannonicalAnnotationSetName(annotationSetName);
    if (write) {
      File f = new File(getAnnotationDir(), annSetName);
      if (DEBUG) {
        System.out.println("Document.addAnnotationSet: " + f);
      }

      PrintWriter out = new PrintWriter(f);
      AnWriter.write(set, out);
      out.flush();
      out.close();
    }
    mAnnotationSets.put(annSetName, set);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

public void addAnnotationSet(AnnotationSet set, boolean write)
{
	addAnnotationSet(set, set.getName(), write);
}
/**
 * Delete all of the data in the prediction directory, feature directory, cluster directory, and annotation directory
 * 
 * @throws IOException
 * 
 */
public int clean()
{
  try {
    List<File> delList = Lists.newArrayList(getClusterDir(), getPredictionDir(), getAnnotationDir(), getFeatureDir());
    mClusterFile = null;
    mClusterSubDir = null;
    mFeatureDir = null;
    mFeatureFile = null;
    mPredictionDir = null;
    mPredictionFile = null;
    mAnnotationDir = null;
    int count = 0;
    for (File f : delList) {
      try {
        count += Corpus.recursivelyDelete(f);
      }
      catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }
    return count;
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

public void deleteAnnotation(String name)
    throws IOException
{
  File f = getAnnotationDir();
  String canName = getCannonicalAnnotationSetName(name);
  File df = new File(f, canName);
  try {
    FileUtils.delete(df);
  }
  catch (IOException e) {
    e.printStackTrace();
  }
}

/**
 * delete the cluster file associated with this document
 * 
 * @throws IOException
 */
public void deleteClusterFile()
    throws IOException
{
  File f = getClusterFile();
  FileUtils.delete(f);
}

public void deleteFeatureFile()
    throws IOException
{
  File f = getFeatureFile();
  FileUtils.delete(f);
}

public void deletePredictionFile()
    throws IOException
{
  File f = getPredictionFile();
  FileUtils.delete(f);
}

/**
 * @param responseNps
 * @return
 */

public boolean existsAnnotationSetFile(String asName)
{
  String name = getCannonicalAnnotationSetName(asName);
  File dir = getAnnotationSetFile(name);
  return dir.exists();
}

public boolean existsClusterFile()
{
  File dir = getClusterFile();
  return dir.exists();
}

public boolean existsFeatureFile()
{
  File dir = getFeatureFile();
  return dir.exists();
}

/* (non-Javadoc)
 * @see reconcile.Document#existsFile(java.lang.String)
 */
private boolean existsFile(File f)
{
  return f.exists();
}

public boolean existsFile(String name)
{
  return existsFile(new File(getRootDir(), name));
}

public boolean existsPredictionFile()
{
  File dir = getPredictionFile();
  return dir.exists();
}

/**
 * @return
 */
public String getAbsolutePath()
{
  return mDir.getAbsolutePath();
}

File getAnnotationDir()
{
  if (mAnnotationDir == null) {
    mAnnotationDir = new File(mDir, Constants.ANNOT_DIR_NAME);
    if (!mAnnotationDir.exists()) {
      try {
        FileUtils.mkdir(mAnnotationDir);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
  return mAnnotationDir;
}

/**
 * Returns an annotation set. If it is a keyed annotation set from the config file (e.g. one of the X_ANNOTATION
 * constants in this class), the name may be switched by different configuration options. Else, the given name is used.
 * AnnotationSets are cached in memory so that there is no additional IO for repeated gets.
 * <p>
 * If the annotation set does not exist, this method returns null. Should it return an empty set? Or throw and
 * exception?
 * 
 * @param annotationSetName
 * @return
 * @throws IOException
 */
public AnnotationSet getAnnotationSet(String annotationSetName)
{
  try {
    String annSetName = getCannonicalAnnotationSetName(annotationSetName);
    AnnotationSet set = mAnnotationSets.get(annSetName);
    if (set == null) {
      File annFile = new File(getAnnotationDir(), annSetName);
      if (!annFile.exists()) {
        return null;
        // throw new RuntimeException("Annotation file does not exist: " + annFile.getAbsolutePath());
      }
      FileInputStream in = new FileInputStream(annFile);
//      set = clean(Reader.read(in, annSetName));
      set = AnReader.read(in, annSetName);
      mAnnotationSets.put(annSetName, set);
    }
    return set;
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/**
 * Returns the file path for an annotation set. If it is a keyed annotation set from the config file (e.g. one of the
 * X_ANNOTATION constants in this class), the name may be switched by different configuration options. Else, the given
 * name is used.
 * 
 * @param annotationSetName
 * @return File
 * @throws IOException
 */
public File getAnnotationSetFile(String annotationSetName)
{
  String annSetName = getCannonicalAnnotationSetName(annotationSetName);
    File annFile = new File(getAnnotationDir(), annSetName);

    return annFile;
}

public Set<String> getAnnotationSetNames()
{
  return mAnnotationSets.keySet();
}

public String getAnnotString(Annotation a)
{
  if (a.strContent == null) {
    a.strContent = getAnnotString(a.getStartOffset(), a.getEndOffset());
  }
  return a.strContent;
}



public String getAnnotString(int start, int end)
{
  String result = null;

  try {
    result = getText().substring(start, end);
  }
  catch (StringIndexOutOfBoundsException siobe) {
    int st = mText.length() - 20;
    for (int i = st; i < mText.length(); i++) {
      System.err.println(i + ":" + mText.charAt(i));

    }
    throw new RuntimeException(siobe);
  }
  return result;
}

public String getAnnotText(Annotation a)
{
  if (a.textContent == null) {
    String result = getAnnotString(a);
    if (result == null) return null;
    result = result.trim().replaceAll("(\\s|\\n)+", " ");
    result = result.replaceAll("\\A[\\s\\\"'`\\[\\(\\-]+", "").replaceAll("\\W+\\z", "");
    a.textContent = result;
  }
  return a.textContent;
}

public String getAnnotText(int start, int end)
{
  String result = getAnnotString(start, end);
  if (result == null) return null;
  result = result.trim().replaceAll("(\\s|\\n)+", " ");
  result = result.replaceAll("\\A[\\s\\\"'`\\[\\(\\-]+", "").replaceAll("\\W+\\z", "");
  return result;
}

/**
 * Given an annotation set name, check to see if there is a cannonicalization for it, and if so, return that
 * canonicalization
 * 
 * @param annotationSetName
 * @return
 */
public String getCannonicalAnnotationSetName(String annotationSetName)
{
  String annSetName = Utils.getConfig().getAnnotationSetName(annotationSetName);
  String name = null;
  if (annSetName != null) {
    name = annSetName;
  }
  else {
    name = annotationSetName;
  }
  return name;
}

/**
 * get the cluster directory associated with this document
 */
File getClusterDir()
{
  if (mClusterSubDir == null) {
    SystemConfig cfg = Utils.getConfig();
    String clName = cfg.getClusterer();
    File predDir = getPredictionDir();
    mClusterSubDir = new File(predDir, clName);
    if (!mClusterSubDir.exists()) {
      try {
        FileUtils.mkdir(mClusterSubDir);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

  }
  return mClusterSubDir;
}

/**
 * Get the cluster file associated with this document
 * 
 * @return
 */
public File getClusterFile()
{
  if (mClusterFile == null) {
    File dir = getClusterDir();
    if (DEBUG) {
      System.out.println("Document.getClusterFile: fn=" + new File(dir, Constants.CLUSTER_FILE_NAME));
    }

    mClusterFile = new File(dir, Constants.CLUSTER_FILE_NAME);
  }
  return mClusterFile;
}

public String getClusterName()
{
  return Constants.CLUSTER_FILE_NAME;
}
/**
 * @return
 */
File getFeatureDir()
{
  if (mFeatureDir == null) {
    SystemConfig cfg = Utils.getConfig();
    String featSetName = cfg.getFeatSetName();
    mFeatureDir = new File(mDir, Constants.FEAT_DIR_NAME + "." + featSetName);
    if (!mFeatureDir.exists()) {
      try {
        FileUtils.mkdir(mFeatureDir);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
  return mFeatureDir;
}

public File getFeatureFile()
{
  if (mFeatureFile == null) {
    File dir = getFeatureDir();
    SystemConfig cfg = Utils.getConfig();
    String featureFormat = cfg.getString(FEATURE_FORMAT, "arff");

    mFeatureFile = new File(dir, Constants.FEAT_FILE_NAME + "." + featureFormat);
  }
  return mFeatureFile;
}
public Reader getFeatureReader(){
	try {
		return new java.io.FileReader(getFeatureFile());
	} catch (FileNotFoundException e) {
		throw new RuntimeException(e);
	}
}

File getPredictionDir()
{
  if (mPredictionDir == null) {
    SystemConfig cfg = Utils.getConfig();
    String classifierName = cfg.getClassifier();
    String modelName = cfg.getModelName();
    File featDir = getFeatureDir();
    mPredictionDir = new File(featDir, Constants.PRED_DIR_NAME + "." + classifierName + "." + modelName);
    if (!mPredictionDir.exists()) {
      try {
        FileUtils.mkdir(mPredictionDir);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
  return mPredictionDir;
}

public File getPredictionFile()
{
  if (mPredictionFile == null) {
    mPredictionFile = new File(getPredictionDir(), Constants.PRED_FILE_NAME);
  }
  return mPredictionFile;
}
public Writer getPredictionWriter(){
	try {
		return new java.io.FileWriter(getPredictionFile());
	} catch (IOException e) {
		throw new RuntimeException(e);
	}
}
public Reader getPredictionReader(){
	try {
		return new FileReader(getPredictionFile());
	} catch (IOException e) {
		throw new RuntimeException(e);
	}
}
/**
 * @return
 */
public File getRawFile()
{
  File f = new File(mDir, RAW_TXT);
  return f;
}

/**
 * 
 * @return the directory that encapsulates all of the information for this particular document
 */
public String getDocumentId()
{
  if (mId == null) {
    return mDir.getAbsolutePath();
  }
  return mId;
}

/**
 * 
 * @return the directory that encapsulates all of the information for this particular document
 */
public File getRootDir()
{
  return mDir;
}

/* (non-Javadoc)
 * @see reconcile.Document#readAnnotationDirFile(java.lang.String)
 */

/**
 * @return
 */

public String getText()
{

  if (mText == null) {
    try {
      mText = FileUtils.readFile(getRawFile());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (mText.length() == 0)
      throw new RuntimeException("raw.txt file is of length 0.  This will cause all sorts of problems. document dir: "
          + getAbsolutePath());
  }
  return mText;
}

/* (non-Javadoc)
 * @see reconcile.Document#readClusterDirFile(java.lang.String)
 */

public String[] getWords(Annotation a)
{
  if (a.words == null) {
    a.words = getWords(getAnnotText(a));
  }
  return a.words;
}

/* (non-Javadoc)
 * @see reconcile.Document#readClusterFile()
 */

public String[] getWords(int start, int end)
{
  String textSpan = getAnnotText(start, end);
  return getWords(textSpan);
}

/* (non-Javadoc)
 * @see reconcile.Document#readFeatureDirFile(java.lang.String)
 */

public List<String> listAnnotationTypes()
{
  List<String> result = new ArrayList<String>();
  File annDir = getAnnotationDir();
  String[] files = annDir.list();
  for (String f : files) {
    result.add(f);
  }
  return result;
}

/* (non-Javadoc)
 * @see reconcile.Document#readFeatureFile()
 */

public void loadAnnotationSets()
{
  loadAnnotationSets(0);
}

/* (non-Javadoc)
 * @see reconcile.Document#readPredictionDirFile(java.lang.String)
 */

public void loadAnnotationSets(int docNum)
{
  HashMap<String, String> annotSets = Utils.getConfig().getAnnotSetNames();

  for (String anSetKey : annotSets.keySet()) {
    getAnnotationSet(anSetKey);
  }

  // This is a hack to pass along the document number without introducing an additional
  // parameter
  AnnotationSet docNo = new AnnotationSet(DocNo.ID);
  HashMap<String, String> feat = new HashMap<String, String>();
  feat.put(DocNo.ID, String.valueOf(docNum));
  docNo.add(0, 0, DocNo.ID, feat);
  mAnnotationSets.put(DocNo.ID, docNo);
}

/* (non-Javadoc)
 * @see reconcile.Document#readPredictionFile()
 */

public void loadAnnotationSetsByName(String[] annotSets)
{
  for (String anSetKey : annotSets) {
    getAnnotationSet(anSetKey);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#readFile(java.lang.String)
 */

public InputStream readAnnotationDirFile(String filename)
{
  try {
    File f = getAnnotationSetFile(filename);
    return new FileInputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#writeAnnotationDirFile(java.lang.String, java.io.InputStream)
 */

public InputStream readClusterDirFile(String fileName)
{
  try {
    File f = new File(getClusterDir(), fileName);
    return new FileInputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#writeAnnotationDirFile(java.lang.String, java.io.InputStream)
 */

public InputStream readClusterFile()
{
  try {
    File f = getClusterFile();
    if (!f.exists()) return null;
    return new FileInputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

public InputStream readFeatureDirFile(String filename)
{
  try {
    File f = new File(getFeatureDir(), filename);
    if (!f.exists()) return null;
    return new FileInputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

public InputStream readFeatureFile()
{
  try {
    File f = getFeatureFile();
    if (!f.exists()) return null;
    return new FileInputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#writeClusterDirFile(java.lang.String)
 */

public InputStream readFile(String name)
{
  try {
    File f = new File(getRootDir(), name);
    if (!f.exists()) return null;
    return new FileInputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#writeClusterFile()
 */

public InputStream readPredictionDirFile(String fileName)
{
  try {
    File f = new File(getPredictionDir(), fileName);
    if (!f.exists()) return null;
    return new FileInputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#writeClusterFile()
 */

public InputStream readPredictionFile()
{
  try {
    File f = getPredictionFile();
    if (!f.exists()) return null;
    return new FileInputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#writeFeatureDirFile(java.lang.String)
 */

/**
 * This is a hack to pass along the document number without introducing an additional parameter
 */
public void setDocNumber(int docNum)
{
  AnnotationSet docNo = new AnnotationSet("docNo");
  HashMap<String, String> feat = new HashMap<String, String>();
  feat.put("docNo", String.valueOf(docNum));
  docNo.add(0, 0, "docNo", feat);
  mAnnotationSets.put("docNo", docNo);
}

public void setDocumentId(String id)
{
  mId = id;
}

/**
 * @param inputTextFile
 * @throws IOException
 */
public void setRawText(File inputTextFile)
    throws IOException
{
  try {
    FileUtils.write(getRawFile(), new FileInputStream(inputTextFile));
  }
  catch (RuntimeException e) {
    e.printStackTrace();
  }

}

/**
 * @param articleNoTags
 * @throws IOException
 */
public void setRawText(String text)
{
  try {
    FileUtils.write(getRawFile(), text);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}
/* (non-Javadoc)
 * @see reconcile.Document#writeFeatureDirFile(java.lang.String)
 */

public OutputStream writeAnnotationDirFile(String filename)
{
  try {
    File f = getAnnotationSetFile(filename);
    return new FileOutputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#writeFeatureFile()
 */

public void writeAnnotationDirFile(String filename, String content)
{
  File f = getAnnotationSetFile(filename);
  writeFile(f, content);
}

/* (non-Javadoc)
 * @see reconcile.Document#writeFeatureFile()
 */

public void writeAnnotationSet(AnnotationSet anSet)
{
  addAnnotationSet(anSet, anSet.getName(), true);
}
/* (non-Javadoc)
 * @see reconcile.Document#writeClusterDirFile(java.lang.String)
 */

/* (non-Javadoc)
 * @see reconcile.Document#writePredictionDirFile(java.lang.String)
 */

public OutputStream writeClusterDirFile(String fileName)
{
  try {
    File f = new File(getClusterDir(), fileName);
    return new FileOutputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#writePredictionDirFile(java.lang.String)
 */

public void writeClusterDirFile(String fileName, String content)
{
  File f = new File(getClusterDir(), fileName);
  writeFile(f, content);
}

/* (non-Javadoc)
 * @see reconcile.Document#writePredictionFile()
 */

public OutputStream writeClusterFile()
{
  try {
    File f = getClusterFile();
    return new FileOutputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#writePredictionFile()
 */

public void writeClusterFile(String content)
{
  File f = getClusterFile();
  writeFile(f, content);
}

/* (non-Javadoc)
 * @see reconcile.Document#writeFile(java.lang.String)
 */

public OutputStream writeFeatureDirFile(String filename)
{
  try {
    File f = new File(getFeatureDir(), filename);
    return new FileOutputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#writeFile(java.lang.String)
 */

public void writeFeatureDirFile(String filename, String content)
{
  File f = new File(getFeatureDir(), filename);
  writeFile(f, content);
}

public OutputStream writeFeatureFile()
{
  try {
    File f = getFeatureFile();
    return new FileOutputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

public void writeFeatureFile(String content)
{
  File f = getFeatureFile();
  writeFile(f, content);
}

/* (non-Javadoc)
 * @see reconcile.Document#existsClusterFile()
 */

/* (non-Javadoc)
 * @see reconcile.Document#writeFile(java.lang.String)
 */
public void writeFile(File file, String content)
{
  try {
    FileUtils.write(file, content);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#existsFeatureFile()
 */

public OutputStream writeFile(String name)
{
  try {
    File f = new File(getRootDir(), name);
    return new FileOutputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#existsFile(java.lang.String)
 */

public void writeFile(String name, String content)
{
  File f = new File(getRootDir(), name);
  writeFile(f, content);
}

public OutputStream writePredictionDirFile(String fileName)
{
  try {
    File f = new File(getPredictionDir(), fileName);
    return new FileOutputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#existsPredictionFile()
 */

public void writePredictionDirFile(String fileName, String content)
{
  File f = new File(getPredictionDir(), fileName);
  writeFile(f, content);
}

/* (non-Javadoc)
 * @see reconcile.Document#deleteAnnotation(java.lang.String)
 */

public OutputStream writePredictionFile()
{
  try {
    File f = getPredictionFile();
    return new FileOutputStream(f);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

/* (non-Javadoc)
 * @see reconcile.Document#listAnnotationTypes()
 */

public void writePredictionFile(String content)
{
  File f = getPredictionFile();
  writeFile(f, content);
}

/**
 * @return
 */
public int length()
{
  return getText().length();
}

/**
 * Add field meta data to the document.
 * 
 * @param key
 * @param val
 * @throws IOException
 */
public synchronized void putMetaData(String key, String val)
    throws IOException
{
  mMetaData.put(key, val);
  writeMetaData();
}

public synchronized void removeMetaData(String key)
    throws IOException
{
  mMetaData.remove(key);
  writeMetaData();
}

/**
 * Add field meta data to the document.
 * 
 * @param data
 *          a map containing several values that should be added
 * @throws IOException
 */
public synchronized void addMetaData(Map<String, String> data)
    throws IOException
{
  mMetaData.putAll(data);
  writeMetaData();
}

/**
 * @throws IOException
 * @throws FileNotFoundException
 * 
 */
private void writeMetaData()
    throws IOException
{
  File metadataFile = new File(mDir, METADATA_FILE);
  writeMetaData(new FileOutputStream(metadataFile), mMetaData);

}

/**
 * given an input stream, put the key value pairs into a map
 * 
 * @param in
 * @param map
 * @throws IOException
 */
public static void readMetaData(InputStream in, Map<String, String> map)
    throws IOException
{
  try {
    for (String line : InputStreamLineIterable.iterateOverCommentedLines(in)) {
      int index = line.indexOf('\t');
      if (index > 0) {
        String key = line.substring(0, index).trim();
        String val = line.substring(index).trim();
        map.put(key, val);
      }
    }
  }
  catch (IOException e) {
    System.err.println("error in reading meta data file: " + e.getMessage());
    throw new RuntimeException(e);
  }
}

/**
 * given an input stream, put the key value pairs into a map
 * 
 * @param in
 * @param map
 * @throws IOException
 */
public static void writeMetaData(OutputStream outStream, Map<String, String> map)
    throws IOException
{
    PrintWriter out = new PrintWriter(outStream);
    for (String key : map.keySet()) {
      out.println(key + "\t" + map.get(key));
    }
    out.flush();
}

public String getMetaData(String key)
{
  return mMetaData.get(key);
}

public Set<String> getMetaDataKeys()
{
  return ImmutableSet.copyOf(mMetaData.keySet());
}

}
