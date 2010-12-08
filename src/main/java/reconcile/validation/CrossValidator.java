package reconcile.validation;

import java.io.File;

import reconcile.Constructor;
import reconcile.SystemConfig;
import reconcile.classifiers.Classifier;
import reconcile.clusterers.Clusterer;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Utils;
import reconcile.scorers.Scorer;


public abstract class CrossValidator {

SystemConfig cfg;
Scorer scorer;
Classifier learner;
String[] learnerOptions;
String[] testerOptions;
String[] clustOptions;
String modelName;
Clusterer clusterer;

public CrossValidator() {
  cfg = Utils.getConfig();
  String scorerName = cfg.getOptimizeScorer();
  if (scorerName == null || scorerName.length() <= 0)
    throw new RuntimeException("Score for which to cross-validate not specified");
  scorer = Constructor.createScorer(scorerName);

  modelName = cfg.getModelName();
  String modelFile = Utils.getWorkDirectory() + "/" + modelName;
  String classifierName = cfg.getClassifier();
  String clustererName = cfg.getClusterer();
  learner = Constructor.createClassifier(classifierName, modelFile);
  learnerOptions = cfg.getStringArray("ClOptions." + classifierName);
  testerOptions = cfg.getStringArray("TesterOptions." + classifierName);

  clustOptions = cfg.getStringArray("ClustOptions." + clustererName);
  clusterer = Constructor.createClusterer(clustererName);
  configure(cfg);
}

protected abstract void trainAndValidateFoldInternal(Iterable<Document> validFilenames, File trainVectorsFile,
    int foldNum);

protected abstract void classifyFoldInternal(Iterable<Document> testFilenames, int foldNum);

protected abstract void classifyFoldInternal(Iterable<Document> testFilenames, int foldNum, String modelFN);

protected abstract void classifyInternal(Iterable<Document> testFilenames, String modelFN);

public abstract void validateFold(Iterable<Document> validFilenames, String modelFN);

public abstract String outputInformation();

public abstract void configure(SystemConfig cfg);

public void trainAndValidateFold(Iterable<Document> validFilenames, File trainVectorsFile, int foldNum)
{
  // do any preliminary work that repeats for all cross validators
  trainAndValidateFoldInternal(validFilenames, trainVectorsFile, foldNum);
}

public void classifyFold(Iterable<Document> testFilenames, int foldNum)
{
  // do any preliminary work that repeats for all cross validators
  classifyFoldInternal(testFilenames, foldNum);
}

public void classifyFold(Iterable<Document> testFilenames, int foldNum, String modelFN)
{
  // do any preliminary work that repeats for all cross validators
  classifyFoldInternal(testFilenames, foldNum, modelFN);
}

public void classify(Iterable<Document> testFilenames, String modelFN)
{
  // do any preliminary work that repeats for all cross validators
  classifyInternal(testFilenames, modelFN);
}

public static CrossValidator createCrossValidator()
{
  String cv = Utils.getConfig().getCrossValidator();
  if (cv == null) throw new RuntimeException("CrossValidator not specified");
  return Constructor.createCrossValidator(cv);
}

public double score(Iterable<Document> files)
{
  try {
    double[] score = scorer.score(true, files);
    return score[Scorer.F];
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

public double score(Iterable<Document> files, String clusterName)
{
  try {
    // LeanDocument[] keys = new LeanDocument[files.length];
    // LeanDocument[] responses = new LeanDocument[files.length];
    //			
    // for(int i=0; i<files.length; i++){
    // String file = files[i];
    // AnnotationSet key = (new AnnotationReaderBytespan()).read(file+Utils.SEPARATOR+"processedNPs", "nps");
    //				
    // String responseName = FileStructure.getPath(file, fs.getClusterSubdir(), clusterName);
    // responses[i] = Clusterer.readDocument(responseName);
    // keys[i] = InternalScorer.readDocument(key);
    // }

    double[] score = scorer.score(true, files, clusterName);
    return score[Scorer.F];
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

public void learn(File trainFeaturesFile, File modelName)
{
  learner.train(trainFeaturesFile, modelName, learnerOptions);
}

public double[] runClassifier(Iterable<Document> testFilenames, String modelFN)
{
  System.out.println("Classifying...");
  double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
  for (Document doc : testFilenames) {
    File featFN = doc.getFeatureFile();
    File predictionFN = doc.getPredictionFile();
    // System.out.println("Running on "+featFN+" output is "+predictionFN+" modelFN is "+modelFN);
    double[] res = learner.test(featFN, predictionFN, modelFN, testerOptions);
    if (res != null) {
      min = min < res[0] ? min : res[0];
      max = max > res[1] ? max : res[1];
    }
  }

  System.out.println("Values in range [" + min + "," + max + "]");
  double[] result = new double[] { min, max };
  return result;
}

public void cluster(Iterable<Document> testFilenames)
{
  System.out.println("Clustering: " + clusterer.getInfo(clustOptions));
  for (Document doc : testFilenames) {
    AnnotationSet ces = clusterer.cluster(doc, clustOptions);
    ces.setName(doc.getClusterName());
    doc.writeAnnotationSet(ces);

  }

}

public void cluster(Iterable<Document> testFilenames, String clusterName)
{
  // System.out.println("Clustering: "+clusterer.getInfo(clustOptions));
  for (Document doc : testFilenames) {
    AnnotationSet ces = clusterer.cluster(doc, clustOptions);
    ces.setName(clusterName);
    doc.writeAnnotationSet(ces);
  }

}

}
