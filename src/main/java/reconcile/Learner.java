package reconcile;

import java.io.File;

import reconcile.classifiers.Classifier;
import reconcile.general.Utils;


public class Learner {

public static void learn(File trainFeaturesFile, String modelName)
{
  SystemConfig cfg = Utils.getConfig();
  String classifier = cfg.getClassifier();
  if (classifier == null) throw new RuntimeException("Classifier not specified");

  String modelFilename = Utils.getWorkDirectory() + Utils.SEPARATOR + modelName;
  File modelFile = new File(modelFilename);
  Classifier learner = Constructor.createClassifier(classifier, modelFilename);
  String[] options = cfg.getStringArray("ClOptions." + classifier);
  learner.train(trainFeaturesFile, modelFile, options);
}
}
