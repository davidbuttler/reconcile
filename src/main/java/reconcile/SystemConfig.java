package reconcile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import reconcile.general.Constants;
import reconcile.general.Utils;


/**
 * @author nathan
 * 
 */

public class SystemConfig
    extends PropertiesConfiguration {

// Some defaults
private String TRAIN_DIR = null;
private String TRAIN_FILELIST = null;
private String TEST_DIR = null;
private String TEST_FILELIST = null;
private String VALID_DIR = null;
private String VALID_FILELIST = null;
private boolean GENERATE_FEATURES = true;
private boolean PREPROCESS = true;
private boolean CROSS_VALIDATE = false;
private boolean VALIDATE = false;
private String CROSS_VALIDATOR = null;
private String VALIDATOR = null;
private String SCORER = null;
private boolean SCORE = true;
private String[] SCORER_SET = null;
private int NUM_FOLDS = 10;
private int RATIO = 10;
private boolean TRAIN_ONLY = false;
private boolean TRAIN = true;
private boolean TEST = true;
private boolean TESTONLY = false;
private boolean PRINTINDIESCORES = false;
private String[] FEATURE_NAMES = {};
private String PAIR_GEN_NAME = "AllPairs";
// public boolean HYSOM = false;
public String[] NERMODELS = {};
public boolean MUC6 = true;
public String DATASET = "muc6";
public String FEAT_SET_NAME = null;
public String CLASSIFIER;
public String CLUSTERER;
public String MODEL_NAME;
public String MUC_SCORER_PATH;
public String ANNOTDIR;
public String ANNOTFILES;
public String tagChunk;
public String tagChunkLists;
public boolean VERBOSE;

private HashMap<String, String> AnnotationSetNames;
private ArrayList<String> PreprocessingElements;
private HashMap<String, String[]> PreprocessingElSetNames;
private String[] Scorers;

public SystemConfig()
    throws ConfigurationException {
  this(Utils.getResourceStream("default.config"));
}

public SystemConfig(InputStream is)
    throws ConfigurationException {
  super();
  load(new BufferedReader(new InputStreamReader(is)));
  init();
}

public SystemConfig(String fn)
    throws ConfigurationException {
  super(fn);
  init();
}


@SuppressWarnings("unchecked")
private void init()
{
  // Set up defaults for the names of all annotation sets are loaded
  AnnotationSetNames = new HashMap<String, String>();
  AnnotationSetNames.put(Constants.SENT, "sentence");
  AnnotationSetNames.put(Constants.PAR, "paragraph");
  AnnotationSetNames.put(Constants.POS, "postag");
  AnnotationSetNames.put(Constants.TOKEN, "token");
  AnnotationSetNames.put(Constants.PARSE, "parse");
  AnnotationSetNames.put(Constants.DEP, "dep");
  //AnnotationSetNames.put(Constants.ORIG, "gold_annots");
  AnnotationSetNames.put(Constants.NE, "ne");
  AnnotationSetNames.put(Constants.NP, "basenp");
  //AnnotationSetNames.put(Constants.COREF, "coref");

  TRAIN_DIR = getString("TRAIN_DIR");
  TRAIN_FILELIST = getString("TRAIN_FILELIST");
  TEST_DIR = getString("TEST_DIR");
  TEST_FILELIST = getString("TEST_FILELIST");
  VALID_DIR = getString("VALID_DIR");
  VALID_FILELIST = getString("VALID_FILELIST");

  // String trainDirStr = getString("TRAIN_DIR");
  // String trainListStr = getString("TRAIN_FILELIST");
  // if (trainDirStr != null && trainListStr != null) {
  // TRAIN_DIR = new File(trainDirStr);
  // TRAIN_FILELIST = new File(trainListStr);
  // }
  // String testDirStr = getString("TEST_DIR");
  // String testListStr = getString("TEST_FILELIST");
  // if (testDirStr != null && testListStr != null) {
  // TEST_DIR = new File(testDirStr);
  // TEST_FILELIST = new File(testListStr);
  // }
  // String validDirStr = getString("VALID_DIR");
  // String validListStr = getString("VALID_FILELIST");
  // if (validDirStr != null && validListStr != null) {
  // VALID_DIR = new File(validDirStr);
  // VALID_FILELIST = new File(validListStr);
  // }

  DATASET = getString("DATASET");
  FEAT_SET_NAME = getString("FEAT_SET_NAME");
  GENERATE_FEATURES = getBoolean("RUN_FEATURE_GENERATION", GENERATE_FEATURES);
  PREPROCESS = getBoolean("PREPROCESS", PREPROCESS);
  CROSS_VALIDATE = getBoolean("CROSS_VALIDATE", CROSS_VALIDATE);
  CROSS_VALIDATOR = getString("CROSS_VALIDATOR");
  VALIDATE = getBoolean("VALIDATE", VALIDATE);
  VALIDATOR = getString("VALIDATOR");
  SCORER = getString("OPTIMIZE_SCORER");
  SCORER_SET = getStringArray("OPTIMIZE_SCORER_SET");
  NUM_FOLDS = getInt("NUM_FOLDS", NUM_FOLDS);
  SCORE = getBoolean("SCORE", SCORE);
  TRAIN_ONLY = getBoolean("TRAIN_ONLY", true);
  TRAIN = getBoolean("TRAIN", TRAIN);
  TEST = getBoolean("TEST", TEST);
  TESTONLY = getBoolean("TESTONLY", TESTONLY);
  FEATURE_NAMES = getStringArray("FEATURE_NAMES");
  MUC6 = getBoolean("MUC6", MUC6);
  VERBOSE = getBoolean("VERBOSE", VERBOSE);
  CLASSIFIER = getString("CLASSIFIER");
  MODEL_NAME = getString("MODEL_NAME");
  MUC_SCORER_PATH = getString("MUC_SCORER_PATH");
  CLUSTERER = getString("CLUSTERER", "SingleLink");
  RATIO = getInt("RATIO", RATIO);

  tagChunk = getString("TAGCHUNK");
  tagChunkLists = getString("TAGCHUNK_LISTS");
  ANNOTDIR = getString("ANNOT_DIR");
  ANNOTFILES = getString("ANNOT_FILELIST");
  PAIR_GEN_NAME = getString("INSTANCE_GENERATOR", PAIR_GEN_NAME);
  PRINTINDIESCORES = getBoolean("INDIESCORES", PRINTINDIESCORES);

  String data_dir = getString("DATA_DIR");
  if (data_dir != null && data_dir.length() > 0) {
    Utils.setDataDirectory(data_dir);
  }

  Configuration annotSets = subset("AnnotationSet");
  Iterator<String> keysIter = annotSets.getKeys();
  while (keysIter.hasNext()) {
    String key = keysIter.next();
    AnnotationSetNames.put(key, annotSets.getString(key));
  }

  PreprocessingElements = new ArrayList<String>();
  PreprocessingElSetNames = new HashMap<String, String[]>();
  Scorers = new String[0];
  Configuration preprocessEl = subset("PreprocessingElement");
  Iterator<String> preprocessIter = preprocessEl.getKeys();
  while (preprocessIter.hasNext()) {
    String element = preprocessIter.next();
    PreprocessingElements.add(element);
    PreprocessingElSetNames.put(element, preprocessEl.getStringArray(element));
  }
  Scorers = getStringArray("SCORERS");
}


public void addConfig(InputStream in)
{
  try {
    PropertiesConfiguration tmp = new PropertiesConfiguration();
    tmp.load(in);
    addConfig(tmp);
  }
  catch (ConfigurationException ce) {
    throw new RuntimeException(ce);
  }

}

@SuppressWarnings("unchecked")
public void addConfig(Configuration cfg)
{
  Iterator<String> it = cfg.getKeys();
  while (it.hasNext()) {
    String s = it.next();
    clearProperty(s);
    setProperty(s, cfg.getProperty(s));
  }
  init();
}

public void addConfig(String fn)
{
  System.out.println("add filename: " + fn);
  File f = new File(fn);
  addConfig(f);
}

public void addConfig(File f)
{
  try {
    System.out.println("add file: " + f.getAbsolutePath());
    FileInputStream fin = new FileInputStream(f);
    addConfig(fin);
  }
  catch (FileNotFoundException e) {
    throw new RuntimeException(e);
  }
}

public HashMap<String, String> getAnnotSetNames()
{
  return AnnotationSetNames;
}

public String getAnnotationSetName(String annotSet)
{
  return AnnotationSetNames.get(annotSet);
}

public boolean testOnly() 
{
	return TESTONLY;
}

public boolean getVerbose(){ 
	return VERBOSE;
}

public String getTagChunk()
{
  return tagChunk;
}

public String getTagChunkLists()
{
  return tagChunkLists;
}

public int getRatio() {
	return RATIO;
}

public String getAnnotDir()
{
  return ANNOTDIR;
}

public String getAnnotLst()
{
  return ANNOTFILES;
}

public boolean getScore() {
	return SCORE;
}

public String getTrLst()
{
  return TRAIN_FILELIST;
}

public String getTrDir()
{
  return TRAIN_DIR;
}

public String getTestDir()
{
  return TEST_DIR;
}

public String getValidDir()
{
  return VALID_DIR;
}

public boolean getMUC6()
{
  return MUC6;
}

public boolean getIndieScores()
{
	return PRINTINDIESCORES;
}

public String[] getNERModels(String ner)
{
  NERMODELS = getStringArray(ner);
  return NERMODELS;
}

public String getTestLst()
{
  return TEST_FILELIST;
}

public String getValidLst()
{
  return VALID_FILELIST;
}

public boolean getTrainOnly()
{
  return TRAIN_ONLY;
}

public boolean getTrain()
{
  return TRAIN;
}

public boolean getTest()
{
  return TEST;
}

public boolean getGenerateFeatures()
{
  return GENERATE_FEATURES;
}

public boolean getPreProcess()
{
  return PREPROCESS;
}

public boolean getCrossValidate()
{
  return CROSS_VALIDATE;
}

public String getCrossValidator()
{
  return CROSS_VALIDATOR;
}

public boolean getValidate()
{
  return VALIDATE;
}

public String getValidator()
{
  return VALIDATOR;
}

public String getOptimizeScorer()
{
  return SCORER;
}

public void setOptimizeScorer(String scorer)
{
  SCORER = scorer;
}

public String[] getOptimizeScorerSet()
{
  return SCORER_SET;
}

public String getPairGenName()
{
  return PAIR_GEN_NAME;
}

public String[] getScorers()
{
  return Scorers;
}

public int getNumFolds()
{
  return NUM_FOLDS;
}


public String[] getFeatureNames()
{
  return FEATURE_NAMES;
}

public ArrayList<String> getPreprocessingElements()
{
  return PreprocessingElements;
}

public HashMap<String, String[]> getPreprocessingElSetNames()
{
  return PreprocessingElSetNames;
}

public String getDataset()
{
  return DATASET;
}

public String getFeatSetName()
{
  return FEAT_SET_NAME;
}

public String getClassifier()
{
  return CLASSIFIER;
}

public String getModelName()
{
  return MODEL_NAME;
}

public String getMUCScorerPath()
{
  return MUC_SCORER_PATH;
}

public String getClusterer()
{
  return CLUSTERER;
}

/**
 * @param responseNps
 * @param string
 */
public void setAnnotationSetName(String key, String fileName)
{
  AnnotationSetNames.put(key, fileName);
}
/**
 * @param string
 */
public void setFeatureSetName(String string)
{
  FEAT_SET_NAME = string;
  
}

public void setDataset(String dataset) {
	DATASET = dataset;
}

public void setClassifier(String classifier) {
	CLASSIFIER = classifier;
}

public void setModelName(String modelName) {
	MODEL_NAME = modelName;
	
}

public void setClusterer(String clusterer) {
	CLUSTERER = clusterer;
	
}
}
