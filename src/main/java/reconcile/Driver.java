package reconcile;

import gov.llnl.text.util.LineIterable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import reconcile.data.Document;
import reconcile.general.Utils;
import reconcile.util.File2DocIterable;
import reconcile.validation.CrossValidator;
import reconcile.validation.Randomizer;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;


public class Driver {

private SystemConfig cfg;

/**
 * @param cfg
 */
public Driver(SystemConfig systemConfig) {
  this.cfg = systemConfig;
}

/**
 * @param args
 */
public static void main(String[] args)
{
  // boolean webstats = false;
  if (args.length > 1) {
    System.out.println("Too many arguments to driver. Only using the first one");
  }

  /* Default configuration */
  String configFilename = "config/coref.cfg";

  if (args.length >= 1) {
    configFilename = args[0];
    System.out.println("Config name specified to " + configFilename);
  }
  else {
    System.out.println("Configuring system from default location");
  }

  Utils.setConfig(configFilename);
  SystemConfig cfg = Utils.getConfig();
  Driver d = new Driver(cfg);

  boolean resolve = cfg.getBoolean("RESOLUTION_MODE", false);
  if (resolve) {
    d.runResolution();
  }
  else if (cfg.getCrossValidate()) {
    d.crossValidate();
  }
  else {
    d.runSystem();
  }
}

public void runSystem()
{
  long systemTime = System.currentTimeMillis();
  ExperimentRecord rec = new ExperimentRecord();
  
  //should the system only worry about the test files?
  boolean testOnly = cfg.testOnly();

  List<File> trainFileNames = null;
  List<File> validFileNames = null;
  
  if(!testOnly) { 
	  trainFileNames = getTrainingFiles();
	  validFileNames = getValidFiles();
  }
  List<File> testFileNames = getTestingFiles();

  Iterable<Document> trainNames = null;
  Iterable<Document> validNames = null;
  
  if(!testOnly) { 
	  trainNames = new File2DocIterable(trainFileNames);
	  validNames = new File2DocIterable(validFileNames);
  }
  
  Iterable<Document> testNames = new File2DocIterable(testFileNames);
  Iterable<Document> allFilenames;

  // System.out.println("Read from "+validDirectory+" - "+validFiles+" "+validNames.length+" files");

  // Should the system overwrite existing files
  boolean overwrite = cfg.getBoolean("OVERWRITE_FILES", true);

  /* run learners */
  /* comment out by NG
  Iterable<Document> allFilenames = trainNames;
  if (cfg.getBoolean("GENERATE_VALID_FEATURES", true)) {
    allFilenames = Iterables.concat(trainNames, validNames);
  }
  allFilenames = Iterables.concat(allFilenames, testNames);
  int allFilesSize = Iterables.size(allFilenames);
  */
  
  boolean test = cfg.getTest();
  if(!testOnly) {
	  allFilenames = trainNames;
	  if (cfg.getBoolean("GENERATE_VALID_FEATURES", true)) {
		 allFilenames = Iterables.concat(trainNames, validNames);
	  }

	  allFilenames = Iterables.concat(allFilenames, testNames);
  }
  else {
	  allFilenames = testNames;
  }
  int allFilesSize = Iterables.size(allFilenames);


  /* preprocess */
  boolean prepocess = cfg.getPreProcess();
  if (prepocess) {
	Preprocessor preprocessor = new Preprocessor(cfg);
	boolean verbose = cfg.getVerbose();
    long time = startStage("preprocess", "Total of " + allFilesSize + " files");
	preprocessor.setVerbose(verbose);
    preprocessor.preprocess(allFilenames, overwrite);
    endStage("preprocess", time);
  }
  /* make feature vector */
  // generate feature files
  boolean generateFeatures = cfg.getGenerateFeatures();
  String DATASET = cfg.getDataset();
  String featSetName = cfg.getFeatSetName();

  if (generateFeatures) {
    long time = startStage("feature vector generation", "Total of " + allFilesSize + " files");
    // FeatureVectorGenerator.makeFeatures(allFilenames);
    if (featSetName == null)
      throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");

	 if(!testOnly) {
		 rec.getOutput().println("Train features:");
		 FeatureVectorGenerator.makeFeatures(trainNames, true, rec);
		 if (validNames != null && cfg.getBoolean("GENERATE_VALID_FEATURES", true)) {
			rec.getOutput().println("Validation features:");
			FeatureVectorGenerator.makeFeatures(validNames, true, rec);
		 }
	 }
    rec.getOutput().println("Test features:");
    FeatureVectorGenerator.makeFeatures(testNames, false, rec);

    endStage("feature vector generation", time);
  }

  /* run learners */
  boolean learn = cfg.getTrain();
  String modelName = cfg.getModelName();
  if (learn && !testOnly) {
	 int trainSize = Iterables.size(trainNames);
    long time = startStage("learn", "Training on " + trainSize + " files");

    if (featSetName == null)
      throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");
    if (modelName == null) throw new RuntimeException("Model name needs to be specified (parameter MODEL_NAME)");

    // merge feature files together
    File trainFeatures = formTrainFeatFilename(DATASET, featSetName);
    // String testFeatures = WorkDir + Utils.SEPARATOR + DATASET + "." + featSetName + ".test.arff";
    // FeatureMerger.merge(trainDirectory, trainFiles, trainFeatures, fStructure.getFeatExtension());
    
    try {
    	FeatureMerger.combine(new FileOutputStream(trainFeatures), trainNames);
    }
    catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    // FeatureMerger.merge(testDirectory, testFiles, testFeatures);
    // String trainFeatures = FileStructure.formTrainFeatFilename(DATASET,featSetName);

    Learner.learn(trainFeatures, modelName);
    endStage("learn", time);
  }

  // Stage validate -- if the VALIDATE parameter is true and validate filenames are specified
  boolean validate = cfg.getValidate();
  if (validate && validNames != null) {
    if (featSetName == null) 
      throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");
    if (modelName == null) throw new RuntimeException("Model name needs to be specified (parameter MODEL_NAME)");
    String[] scorerSet = cfg.getOptimizeScorerSet();
    String modelFilename = Utils.getWorkDirectory() + Utils.SEPARATOR + modelName;
    if (scorerSet == null || scorerSet.length < 1) {
      CrossValidator cv = validateData(validNames, modelFilename);
      cv.classify(testNames, modelFilename);
      Scorer.score(false, testNames, rec.getOutput());
    }
    else {
      for (String scorer : scorerSet) {
        cfg.setOptimizeScorer(scorer);
        CrossValidator cv = validateData(validNames, modelFilename);
        cv.classify(testNames, modelFilename);
        System.out.println("====================================================");
        Scorer.score(false, testNames, rec.getOutput(), scorer);
        System.out.println("====================================================");
      }
    }
  }

  if (test) {
    long time = startStage("test", "Testing on " + Iterables.size(testNames) + " files");
    if (featSetName == null)
      throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");
    if (modelName == null) throw new RuntimeException("Model name needs to be specified (parameter MODEL_NAME)");

    if (validate && validNames == null) {
      String modelFilename = Utils.getWorkDirectory() + Utils.SEPARATOR + modelName;
      validateResultsOnTestData(testNames, modelFilename);
      Scorer.score(false, testNames, rec.getOutput());
    }
    else {
		boolean printAllDocScores = cfg.getIndieScores();
      Tester.test(testNames, rec.getOutput(), printAllDocScores);
    }

    /*	
    if (DATASET.contains("ace04") || DATASET.contains("ace05")) {
    	for (String t : testNames) {
    		try {
    			Scorers.ACEScorer.readInClusters(t, fStructure);	
    			Scorers.ACEScorer.writeACEFile(t, fStructure);	
    		}
    		catch(IOException ex) {
    			ex.printStackTrace();
    		}
    	}
    }
     */

    endStage("test", time);
  }

  rec.commitRecord();

  long totalTime = System.currentTimeMillis() - systemTime;
  System.out.println("The system ran in " + Long.toString(totalTime / 1000) + " seconds");

}

public List<File> getValidFiles()
{
  String validDirectory = cfg.getValidDir();
  String validFiles = cfg.getValidLst();
  if (validDirectory != null && validFiles != null) {
    return getFiles(validDirectory, validFiles);
  }
  else {
    return Lists.newArrayList();
  }
}

public List<File>  getTestingFiles()
{
  String testDirectory = cfg.getTestDir();
  String testFiles = cfg.getTestLst();
  if (testDirectory != null && testFiles != null) {
    return getFiles(testDirectory, testFiles);
  }
  else {
	    return Lists.newArrayList();
  }
}

public List<File> getTrainingFiles()
{
  String trainDirectory = cfg.getTrDir();
  String trainFiles = cfg.getTrLst();
  if (trainDirectory != null && trainFiles != null) {
    return getFiles(trainDirectory, trainFiles);
  }
  else {
	    return Lists.newArrayList();
  }
}

public void runResolution()
{
  long systemTime = System.currentTimeMillis();

  List<File> testFiles = getTestingFiles();
  Iterable<Document> testNames = new File2DocIterable(testFiles);
  int length = Iterables.size(testNames);

  // Should the system overwrite existing files
  boolean overwrite = cfg.getBoolean("OVERWRITE_FILES", true);

  /* preprocess */
  boolean prepocess = cfg.getPreProcess();
  if (prepocess) {
	Preprocessor preprocessor = new Preprocessor(cfg);
    long time = startStage("preprocess", "Total of " + length + " files");
    preprocessor.preprocess(testNames, overwrite);
    endStage("preprocess", time);
  }

  /* make feature vector */
  // generate feature files
  boolean generateFeatures = cfg.getGenerateFeatures();
  // String DATASET = cfg.getDataset();
  String featSetName = cfg.getFeatSetName();

  if (generateFeatures) {
    long time = startStage("future vector generation", "Total of " + length + " files");
    // FeatureVectorGenerator.makeFeatures(allFilenames);
    if (featSetName == null)
      throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");

    FeatureVectorGenerator.makeTestFeatures(testNames);

    endStage("future vector generation", time);
  }

  long time = startStage("resolve", "Resolving " + length + " files");
  if (featSetName == null)
    throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");
  String modelName = cfg.getModelName();
  if (modelName == null) throw new RuntimeException("Model name needs to be specified (parameter MODEL_NAME)");

  Tester.resolve(testNames);

  endStage("test", time);

  long totalTime = System.currentTimeMillis() - systemTime;
  System.out.println("The system ran in " + Long.toString(totalTime / 1000) + " seconds");

}

public void crossValidate()
{
  long systemTime = System.currentTimeMillis();
  // ExperimentRecord rec = new ExperimentRecord();

  List<File> trainFiles = getTrainingFiles();
  Iterable<Document> trainNames = new File2DocIterable(trainFiles);
  List<File> testFiles = getTestingFiles();
  Iterable<Document> testNames = new File2DocIterable(testFiles);

  Iterable<Document> filenames = Iterables.concat(trainNames, testNames);
  int length = Iterables.size(filenames);

  // Should the system overwrite existing files
  boolean overwrite = cfg.getBoolean("OVERWRITE_FILES", true);

  /* preprocess */
  boolean prepocess = cfg.getPreProcess();
  if (prepocess) {
    long time = startStage("preprocess", "Total of " + length + " files");
    Preprocessor preprocessor = new Preprocessor(cfg);
    preprocessor.preprocess(filenames, overwrite);
    endStage("preprocess", time);
  }

  /* make feature vector */
  // generate feature files
  boolean generateFeatures = cfg.getGenerateFeatures();
  String DATASET = cfg.getDataset();
  String featSetName = cfg.getFeatSetName();
  if (generateFeatures) {
    long time = startStage("future vector generation", "Total of " + length + " files");
    if (featSetName == null)
      throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");
    FeatureVectorGenerator.makeFeatures(filenames, true);
    endStage("future vector generation", time);
  }

  int numFolds = cfg.getNumFolds();
  System.out.println("Running cross validation with " + numFolds + " folds.");

  filenames = Randomizer.shuffleArray(filenames, 10);
  CrossValidator cv = CrossValidator.createCrossValidator();
  for (int i = 1; i <= numFolds; i++) {

    Iterable<Document>[] foldFiles = splitFoldValidation(filenames, numFolds, i);
    Iterable<Document> foldTrain = foldFiles[0], foldValid = foldFiles[1], foldTest = foldFiles[2];
    System.out.println("----------------- Fold #" + i + " -------------------");
    long time = startStage("train", "Training on " + Iterables.size(foldTrain) + " files; Validating on "
        + Iterables.size(foldValid) + " files.");
    File trainFeatures = formTrainFeatFilenameCV(DATASET, featSetName, i);

    try {
      FeatureMerger.combine(new FileOutputStream(trainFeatures), foldTrain);
    }
    catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    cv.trainAndValidateFold(foldValid, trainFeatures, i);
    endStage("train", time);
    time = startStage("classify", "Classifying " + Iterables.size(foldTest) + " files");
    cv.classifyFold(foldTest, i);
    endStage("classify", time);
  }

  System.out.println(cv.outputInformation());

  boolean test = cfg.getTest();
  if (test) {
    long time = startStage("test", "Scoring " + length + " files");
    Scorer.score(false/*cfg.getBoolean("OUTPUT_DOC_SCORES", false)*/, filenames);

    /*	
    if (DATASET.contains("ace04") || DATASET.contains("ace05")) {
    	String[] testNames = getFiles(testDirectory, testFiles);
    	for (String t : testNames) {
    		try {
    			Scorers.ACEScorer.readInClusters(t, fStructure);	
    			Scorers.ACEScorer.writeACEFile(t, fStructure);	
    		}
    		catch(IOException ex) {
    			ex.printStackTrace();
    		}
    	}
    }
     */

    endStage("test", time);
  }

  long totalTime = System.currentTimeMillis() - systemTime;
  System.out.println("The system ran in " + Long.toString(totalTime / 1000) + " seconds");

}

@SuppressWarnings("unchecked")
public Iterable<Document>[] splitFold(Iterable<Document> filenames, int numFolds, int foldNum)
{
  List<Document> trainFiles = Lists.newArrayList();
  List<Document> testFiles = Lists.newArrayList();
  int numFiles = Iterables.size(filenames);
  int start = getBagStart(foldNum, numFolds, numFiles), end = getBagStart(foldNum + 1, numFolds, numFiles);
  System.out.println("Test docs from " + start + " to " + end);
  int i = 0;
  for (Document doc : filenames) {
    if (i >= start && i < end) {
      testFiles.add(doc);
    }
    else {
      trainFiles.add(doc);
    }
    i++;
  }
  List[] result = { trainFiles, testFiles };
  return result;
}

@SuppressWarnings("unchecked")
public Iterable<Document>[] splitFoldValidation(Iterable<Document> filenames, int numFolds, int foldNum)
{
  List<Document> trainFiles = Lists.newArrayList();
  List<Document> testFiles = Lists.newArrayList();
  List<Document> validFiles = Lists.newArrayList();
  int numFiles = Iterables.size(filenames);
  int start = getBagStart(foldNum, numFolds, numFiles), end = getBagStart(foldNum + 1, numFolds, numFiles);
  int validFoldNum = foldNum == 1 ? numFolds : foldNum - 1;
  int valStart = getBagStart(validFoldNum, numFolds, numFiles), valEnd = getBagStart(validFoldNum + 1, numFolds,
      numFiles);
  System.out.println("Test docs from " + start + " to " + end);
  System.out.println("Validation docs from " + valStart + " to " + valEnd);
  int i = 0;
  for (Document doc : filenames) {
    if (i >= start && i < end) {
      testFiles.add(doc);
    }
    else if (i >= valStart && i < valEnd) {
      validFiles.add(doc);
    }
    else {
      trainFiles.add(doc);
    }
    i++;
  }
  List[] result = { trainFiles, validFiles, testFiles };
  return result;
}

public static int getBagStart(int bagNum, int numBags, int numDocs)
{
  bagNum--;
  if (bagNum <= 0) return 0;
  if (bagNum > numBags) return numDocs;
  float docsPerBag = numDocs / (float) numBags;
  return Math.round(docsPerBag * bagNum);
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

public CrossValidator validateData(Iterable<Document> validFiles, String modelFN)
{

  String validator = cfg.getValidator();
  CrossValidator cv = Constructor.createCrossValidator(validator);
  long time = startStage("valid", "Total of " + Iterables.size(validFiles) + " files");
  cv.validateFold(validFiles, modelFN);
  endStage("valid", time);

  System.out.println(cv.outputInformation());
  return cv;
}

public void validateResultsOnTestData(Iterable<Document> testFiles, String modelFN)
{

  int numFolds = cfg.getNumFolds();
  System.out.println("Running test validation with " + numFolds + " folds.");

  testFiles = Randomizer.shuffleArray(testFiles, 10);
  String validator = cfg.getValidator();
  CrossValidator cv = Constructor.createCrossValidator(validator);
  for (int i = 1; i <= numFolds; i++) {

    Iterable<Document>[] foldFiles = splitFold(testFiles, numFolds, i);
    Iterable<Document> foldValid = foldFiles[0], foldTest = foldFiles[1];
    System.out.println("----------------- Fold #" + i + " -------------------");
    long time = startStage("test", "Total of " + Iterables.size(foldValid) + " files");
    cv.validateFold(foldValid, modelFN);

    endStage("train", time);

    time = startStage("classify", "Classifying " + Iterables.size(foldTest) + " files");

    cv.classifyFold(foldTest, i, modelFN);

    endStage("classify", time);
  }
  System.out.println(cv.outputInformation());
}

public static File formTrainFeatFilename(String dataset, String featSetName)
{
  String trainFeatures = Utils.getWorkDirectory() + Utils.SEPARATOR + dataset + "." + featSetName + ".train.arff";
  return new File(trainFeatures);
}

public static File formTestFeatFilename(String dataset, String featSetName)
{
  String trainFeatures = Utils.getWorkDirectory() + Utils.SEPARATOR + dataset + "." + featSetName + ".test.arff";
  return new File(trainFeatures);
}

public static File formTrainFeatFilenameCV(String dataset, String featSetName, int foldNum)
{
  String trainFeatures = Utils.getWorkDirectory() + Utils.SEPARATOR + dataset + "." + featSetName + ".fold" + foldNum
      + ".train.arff";
  return new File(trainFeatures);
}
}
