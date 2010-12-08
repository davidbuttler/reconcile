package reconcile;

import gov.llnl.text.util.MapUtil;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import reconcile.data.Annotation;
import reconcile.data.AnnotationComparatorNestedLast;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.FeatureWriter;
import reconcile.featureVector.FeatureWriterARFF;
import reconcile.featureVector.FeatureWriterARFFBinarized;
import reconcile.featureVector.individualFeature.DocNo;
import reconcile.features.properties.Property;
import reconcile.filter.PairGenerator;
import reconcile.general.Constants;
import reconcile.general.Utils;
import reconcile.scorers.Matcher;

import com.google.common.collect.Maps;

public class FeatureVectorGenerator {

static HashMap<Feature, Long> runTimes;
private static List<Feature> mFeatures;
private static PairGenerator mPairGen;

public static HashMap<Feature, String> makeVector(Annotation np1, Annotation np2, List<Feature> featureList,
    Document doc)
{
  HashMap<Feature, String> result = new HashMap<Feature, String>();

  for (Feature feat : featureList) {
    // String value =
    feat.getValue(np1, np2, doc, result);
  }

  return result;
}

public static void initializeTimingStructure(List<Feature> feats)
{
  runTimes = new HashMap<Feature, Long>();
  for (Feature f : feats) {
    runTimes.put(f, 0L);
  }
}

public static void numberAnnotations(AnnotationSet an)
{
  Annotation[] ordered = an.getOrderedAnnots(new AnnotationComparatorNestedLast());
  int counter = 1;
  for (Annotation a : ordered) {
    a.setAttribute(Constants.CE_ID, Integer.toString(counter));
    counter++;
  }
}

public static HashMap<Feature, String> makeVectorTimed(Annotation np1, Annotation np2, List<Feature> featureList,
    Document doc)
{
  HashMap<Feature, String> result = new HashMap<Feature, String>();
  for (Feature feat : featureList) {
    long stTime = System.currentTimeMillis();
    feat.getValue(np1, np2, doc, result);
    long elapsedTime = System.currentTimeMillis() - stTime;
    runTimes.put(feat, runTimes.get(feat) + elapsedTime);
    
    /*
    if (feat.getName() == "class") {
    	System.out.println("Pairing: " + doc.getAnnotString(np1).replace("\n", " ") + " & " + doc.getAnnotString(np2).replace("\n", " ") + " " + feat.produceValue(np1, np2, doc, result) );    
    }
    */
  }
  return result;
}

public static void printFeatTiming()
{
  System.out.println("\nRuntime broken down by feature:");
  TreeMap<Long, List<String>> q = new TreeMap<Long, List<String>>();
  for (Feature f : runTimes.keySet()) {
    MapUtil.addToMapList(q, runTimes.get(f).longValue(), f.getName());
  }
  for (Long time : q.keySet()) {
    TreeSet<String> set = new TreeSet<String>(q.get(time));
    for (String name : set) {
      System.out.printf("%s: %2.1f\n", name, (time.longValue() / (float) 1000));
    }
  }

}

public static void makeFeatures(Iterable<Document> docs, boolean training)
{
  makeFeatures(docs, training, null);
}


public static void makeFeatures(Iterable<Document> docs, boolean training, ExperimentRecord rec)
{
  String[] featureNames = Utils.getConfig().getFeatureNames();
  makeFeatures(docs, featureNames, training, rec);
}

public static void makeTestFeatures(Iterable<Document> files)
{
  String[] featureNames = Utils.getConfig().getFeatureNames();
  for (int i = 0; i < featureNames.length; i++) {
    if (featureNames[i].equals("instClass")) {
      featureNames[i] = "unknwnClass";
    }
  }
  makeFeatures(files, featureNames, false, null);
}


private static void outputNPProperties(Document doc, AnnotationSet nps)
{
  // Output all the NP properties that were computed
  AnnotationSet properties = new AnnotationSet(Constants.PROPERTIES_FILE_NAME);

  for (Annotation np : nps) {
    Map<String, String> npProps = new TreeMap<String, String>();
    Map<Property, Object> props = np.getProperties();
    if (props != null && props.keySet() != null) {
      for (Property p : props.keySet()) {
        npProps.put(p.toString(), Utils.printProperty(props.get(p)));
      }
    }

    String num = np.getAttribute(Constants.CE_ID);
    npProps.put(Constants.CE_ID, num);
    npProps.put("Text", doc.getAnnotText(np));
    properties.add(np.getStartOffset(), np.getEndOffset(), "np", npProps);
  }
  
  doc.writeAnnotationSet(properties);

}

private static void outputMatchedKey(Document doc, AnnotationSet gs)
{
  // Output all the NP properties that were computed
  //AnnotationSet properties = new AnnotationSet(Constants.GS_OUTPUT_FILE);
  AnnotationSet properties = new AnnotationSet("props");
  
  for (Annotation np : gs) {
    Map<String, String> npProps = np.getFeatures();
    Map<Property, Object> props = np.getProperties();
    if (props != null && props.keySet() != null) {
      for (Property p : props.keySet()) {
        npProps.put(p.toString(), Utils.printProperty(props.get(p)));
      }
    }
  
	 //Let's just put all the features in the gsNPs file...? 
	 /* 
	 String num = np.getAttribute("ID");
    npProps.put(Constants.CE_ID, num);

	 String coref = np.getAttribute("CorefID");
    npProps.put(Constants.CLUSTER_ID, coref);

	 String min = np.getAttribute("MIN");
    npProps.put("MIN", min);
	 
	 String head_start = np.getAttribute("HEAD_START");
    npProps.put("HEAD_START", head_start);

	 String head_end = np.getAttribute("HEAD_END");
    npProps.put("HEAD_END", head_start);
	 */
	 
    properties.add(np.getStartOffset(), np.getEndOffset(), "np", npProps);
  }

  AnnotationSet newProperties = new AnnotationSet(Constants.GS_OUTPUT_FILE, properties);
  doc.writeAnnotationSet(newProperties);
}


/**
 * @param files
 *          - an array of the directories containing the raw text
 */
public static void makeFeatures(Iterable<Document> docs, String[] featureNames, boolean training, ExperimentRecord rec)
{
  List<Feature> featureList = Constructor.createFeatures(featureNames);
  initializeTimingStructure(featureList);
  int numNPs = 0;

  // Create the pair (instance) generator
  String pairGenName = Utils.getConfig().getPairGenName();
  PairGenerator pairGen = Constructor.makePairGenClass(pairGenName);
  int i = 0;

  for (Document doc : docs) {

    long stTime = System.currentTimeMillis();
    doc.loadAnnotationSets(i++);
    AnnotationSet basenp = makeFeatures(training, featureList, pairGen, i, doc);
    long elapsedTime = System.currentTimeMillis() - stTime;
    System.out.println("Finished: " + doc.getAbsolutePath() + " in " + Long.toString(elapsedTime / 1000) + " seconds.");
    numNPs += basenp.size();
  }

  printFeatTiming();
  System.out.println("Markables: " + numNPs + " found, -- " + Matcher.totalNPsMatched + " matched");
  System.out.println("Markables: " + Matcher.totalKey + " in key, -- " + Matcher.numMatchedKey + " matched");
  System.out.println("Markables: " + Matcher.doubleMatches + " double matches.");

  if (rec != null) {
    PrintWriter recOut = rec.getOutput();
    recOut.println("Markables: " + numNPs + " found, -- " + Matcher.totalNPsMatched + " matched");
    recOut.println("Markables: " + Matcher.totalKey + " in key, -- " + Matcher.numMatchedKey + " matched");
    recOut.println("Markables: " + Matcher.doubleMatches + " double matches.");
  }

  /*
  try {
  	FileWriter fw = new FileWriter("/home/ngilbert/xspace/regMarksMatched-expanded-nes3.txt", true);
  	fw.write("Total key matched "+((CorefID)FeatureUtils.COREF_ID).numMatchedKey+" of "+((CorefID)FeatureUtils.COREF_ID).totalKey);
  	fw.flush();
  	fw.close();
  } 
  catch(IOException ioe) {
  	ioe.printStackTrace();
  }
  */

  Matcher.nullifyCounters();
}

public static AnnotationSet makeFeatures(Document doc, boolean training)
{
  List<Feature> featureList = getFeatures();
  if (runTimes == null) initializeTimingStructure(featureList);
  PairGenerator pairGen = getPairGenerator();
  initializeTimingStructure(featureList);

  return makeFeatures(training, featureList, pairGen, 0, doc);
}

public static AnnotationSet makeFeatures(Document doc)
{
  List<Feature> featureList = getFeatures();
  if (runTimes == null) initializeTimingStructure(featureList);
  PairGenerator pairGen = getPairGenerator();
  initializeTimingStructure(featureList);

  return makeFeatures(false, featureList, pairGen, 0, doc);
}

/**
 * @return
 */
private static PairGenerator getPairGenerator()
{
  if (mPairGen == null) {
    // Create the pair (instance) generator
    String pairGenName = Utils.getConfig().getPairGenName();
    mPairGen = Constructor.makePairGenClass(pairGenName);
  }
  return mPairGen;
}

/**
 * @return
 */
private static List<Feature> getFeatures()
{
  if (mFeatures == null) {
    String[] featureNames = Utils.getConfig().getFeatureNames();
    mFeatures = Constructor.createFeatures(featureNames);
  }
  return mFeatures;
}

private static AnnotationSet makeFeatures(boolean training, List<Feature> featureList, PairGenerator pairGen, int i, Document doc)
{
  OutputStream output = doc.writeFeatureFile();
  boolean write_binary = Utils.getConfig().getBoolean("WRITE_BINARIZED_FEATURE_FILE", true);
  FeatureWriter writer;
  if (write_binary) {
    writer = new FeatureWriterARFFBinarized(featureList, output);
  }
  else {
    writer = new FeatureWriterARFF(featureList, output);
  }

  AnnotationSet docNo = doc.getAnnotationSet(DocNo.ID);
  if (docNo == null || docNo.size() == 0) {
    docNo = new AnnotationSet(DocNo.ID);
    Map<String, String> features = Maps.newHashMap();
    features.put(DocNo.ID, String.valueOf(i));
    docNo.add(0, doc.length(), DocNo.ID, features);
    // don't write to disk
    doc.addAnnotationSet(docNo, DocNo.ID, false);
  }

  writer.printHeader();
  AnnotationSet basenp = doc.getAnnotationSet(Constants.NP);
  //AnnotationSet basenp = new AnnotationReaderBytespan().read(doc.getAnnotationDir(), Constants.PROPERTIES_FILE_NAME);
  Annotation[] basenpArray = basenp.toArray();
  System.out.println("Document " + (i + 1) + ": " + doc.getAbsolutePath() + " (" + basenpArray.length + " nps)");

  /*
  try {
  	pprintMarkables(basenp, text);
    	}
    	catch(IOException ex) {
  	ex.printStackTrace();
    	} 
  */

  // Initialize the pair generator with the new document
  pairGen.initialize(basenpArray, doc, training);
  
  while (pairGen.hasNext()) {
    Annotation[] pair = pairGen.nextPair();
    Annotation np1 = pair[0], np2 = pair[1];
    HashMap<Feature, String> values = makeVectorTimed(np1, np2, featureList, doc);
    writer.printInstanceVector(values);
  }

  // for (int j = basenpArray.length - 1; j >= 0; j--) {
  // Annotation np2 = basenpArray[j];
  // for (int k = j - 1; k >= 0; k--) {
  // Annotation np1 = basenpArray[k];
  // HashMap<Feature, String> values = makeVectorTimed(np1, np2, featureList, doc);
  // writer.printInstanceVector(values);
  // }
  // }
  outputNPProperties(doc, basenp);

  if (training) {
	  outputMatchedKey(doc, doc.getAnnotationSet(Constants.GS_NP));
  }
  return basenp;
}

}
