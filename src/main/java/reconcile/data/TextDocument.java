package reconcile.data;

import gov.llnl.text.util.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Set;

import reconcile.general.Utils;


public class TextDocument extends Document{
	
public static String getText(String fileName)
{
  // Find the raw.txt file in the directory
  String rawTextFileName = fileName;
  return Utils.getTextFromFile(rawTextFileName);
}
private File mFilename;
private String features;
private ByteArrayOutputStream out;
private StringWriter predictions;

public TextDocument(File filename) {
  mFilename = filename;
  mDir = filename;
  mId=filename.toString();
  init();
}

public TextDocument(String filename) {
  this(new File(filename));
  mId = filename;
  init();
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
      throw new RuntimeException("Cannot write annotations for simple text file yet");
//      File f = new File(getAnnotationDir(), annSetName);
//      if (DEBUG) {
//        System.out.println("Document.addAnnotationSet: " + f);
//      }
//
//      PrintWriter out = new PrintWriter(f);
//      Writer.write(set, out);
//      out.flush();
//      out.close();
    }
    mAnnotationSets.put(annSetName, set);
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

public void addAnnotationSet(AnnotationSet set, boolean write)
{
	addAnnotationSet(set, set.getName(), write);
}

public void writeAnnotationSet(AnnotationSet anSet)
{
  addAnnotationSet(anSet, anSet.getName(), false);
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
  String annSetName = getCannonicalAnnotationSetName(annotationSetName);
  AnnotationSet set = mAnnotationSets.get(annSetName);
  if (set == null) {
   throw new RuntimeException("Annotation file does not exist: " + annSetName+"("+annotationSetName+")");
}
return set;
}

public void writeFeatureFile(String content)
{
  features=content;
}
public OutputStream writeFeatureFile()
{
   out = new ByteArrayOutputStream();
   return out;
}
public Reader getFeatureReader()
{
  if(features==null&&out!=null)
	  features = out.toString();
  return new StringReader(features);
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


/**
 * @return
 */
public File getRawFile()
{
  return mFilename;
}



public Writer getPredictionWriter()
{
	predictions = new StringWriter();
	return predictions;
}

public Reader getPredictionReader(){
	String preds = predictions.toString();
	return new StringReader(preds);
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

}
