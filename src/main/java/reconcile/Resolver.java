package reconcile;

import gov.llnl.text.util.LineIterable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.AnnotationWriterEmbedded;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;
import reconcile.util.File2TextDocIterable;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;


public class Resolver {

/**
 * @param args
 */
public static void main(String[] args)
{
  //System.out.println(Arrays.toString(args));
  if(args.length<1){
	  System.out.println("Please supply filenames for the files to be resolved.");
	  return;
  }
  /* Default configuration */
  String configFilename = "config/resolver.default";

  Utils.setConfig(configFilename);
  SystemConfig cfg = Utils.getConfig();

  long systemTime = System.currentTimeMillis();

  List<File> testFiles = getFiles(args);
  Iterable<Document> testNames = new File2TextDocIterable(testFiles);
  int length = Iterables.size(testNames);

  Preprocessor preprocessor = new Preprocessor(cfg);
  long time = startStage("preprocess", "Total of " + length + " files");
  preprocessor.preprocess(testNames, true);
  endStage("preprocess", time);


  time = startStage("future generation & classification", "Total of " + length + " files");
  String[] featureNames = Utils.getConfig().getFeatureNames();
  for (int i = 0; i < featureNames.length; i++) {
    if (featureNames[i].equals("instClass")) {
      featureNames[i] = "unknwnClass";
    }
  }
  FeatureVectorGenerator.makeTestFeatures(testNames);
  endStage("future generation & classification", time);

  time = startStage("resolve", "Resolving " + length + " files");
  String featSetName = cfg.getFeatSetName();
  if (featSetName == null)
    throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");
  String modelName = cfg.getModelName();
  if (modelName == null) throw new RuntimeException("Model name needs to be specified (parameter MODEL_NAME)");

  Tester.resolve(testNames,true);

  for(Document doc:testNames){
	  PrintWriter out;
	try {
		out = new PrintWriter(doc.getAbsolutePath()+".coref");
		AnnotationSet result = new AnnotationSet("result");
		for(Annotation an : doc.getAnnotationSet(Constants.RESPONSE_NPS)){
			HashMap<String, String> features = new HashMap<String, String>();
			features.put(Constants.CLUSTER_ID, an.getAttribute(Constants.CLUSTER_ID));
			features.put(Constants.CE_ID, an.getAttribute(Constants.CE_ID));
			result.add(an.getStartOffset(), an.getEndOffset(), "NP", features);
		}
    	new AnnotationWriterEmbedded().write(result, out, doc.getText());
    	out.flush();
	} catch (FileNotFoundException e) {
		throw new RuntimeException(e);
	}

  }
  endStage("test", time);

  long totalTime = System.currentTimeMillis() - systemTime;
  System.out.println("The system ran in " + Long.toString(totalTime / 1000) + " seconds");

}

public static List<File>  getFiles(String[] files)
{
  ArrayList<File> fileNames = Lists.newArrayList();
  for (int i=0; i<files.length; i++){
	String file = files[i];
    if (file != null && file.length() > 0) {
      fileNames.add(new File(file)); 
    }
  }
  return fileNames;
}

public static String[] getFileNames(String root, String fileListFileName)
{
  if (root == null || fileListFileName == null) return null;
  // read in and create an array of directory names
  ArrayList<String> fileNames = new ArrayList<String>();
  try {
    for (String file : LineIterable.iterateOverCommentedLines(new File(fileListFileName))) {
      file = file.trim();
      if (file != null && file.length() > 0) {
        fileNames.add(root + System.getProperty("file.separator") + file);
      }
    }
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }

  return fileNames.toArray(new String[0]);
}
public static List<File> getFiles(String root, String fileListFileName)
{
  if (root == null || fileListFileName == null) return null;
  // read in and create an array of directory names
  ArrayList<File> fileNames = Lists.newArrayList();
  try {
    for (String file : LineIterable.iterateOverCommentedLines(new File(fileListFileName))) {
      file = file.trim();
      if (file != null && file.length() > 0) {
        fileNames.add(new File(root, file));
      }
    }
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }

  return fileNames;
}

public static String[] joinArrays(String[] a1, String[] a2)
{
  int len = (a1 == null ? 0 : a1.length) + (a2 == null ? 0 : a2.length);
  String[] result = new String[len];
  int offset = a1 == null ? 0 : a1.length;
  if (a1 != null) {
    for (int i = 0; i < a1.length; i++) {
      result[i] = a1[i];
    }
  }
  if (a2 != null) {
    for (int i = 0; i < a2.length; i++) {
      result[offset + i] = a2[i];
    }
  }
  return result;
}

public static long startStage(String name, String comment)
{
  System.out.println("---------------------Starting stage " + name + "------------------");
  if (comment != null) {
    System.out.println(comment);
  }
  return System.currentTimeMillis();
}

public static long startStage(String name)
{
  return startStage(name, null);
}

public static long endStage(String name, long stTime)
{
  long opTime = System.currentTimeMillis() - stTime;
  System.out.println("---------------Stage " + name + " completed in " + Long.toString(opTime / 1000)
      + " seconds---------------");
  return stTime;
}

}
