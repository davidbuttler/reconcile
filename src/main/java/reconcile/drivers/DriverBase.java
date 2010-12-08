package reconcile.drivers;

import static reconcile.Driver.endStage;
import static reconcile.Driver.startStage;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.ExperimentRecord;
import reconcile.Preprocessor;
import reconcile.SystemConfig;
import reconcile.classifiers.Classifier;
import reconcile.data.Corpus;
import reconcile.data.CorpusFile;
import reconcile.general.Utils;

import com.google.common.collect.Iterables;

/**
 * This driver runs over two root directories for training and testing.  It has an optional configuration file to customize results
 * to a particular data set, operating parameters
 * @author David Buttler
 *
 */
public class DriverBase {

protected SystemConfig cfg;

protected Trainer trainer;
protected Score score;
protected FeatureGenerator featureGenerator;
protected Annotator annotator;
protected Preprocessor preprocessor;

public static void usage()
{
  System.out.println("Usage:");
  String use = DriverBase.class.getName() + ": <train corpus> <test corpus>" + "[" + DriverUtils.CONFIG_ARG + "<name>]* "
  + "[" + DriverUtils.HELP_ARG + "]";

  System.out.println(use);
  System.exit(0);
}

/**
 * @param args
 */
public static void main(String[] args)
{

  try {
    String trainCorpus = args[0];
    String testCorpus = args[1];


    DriverBase d = new DriverBase(args);
    d.runSystem(new CorpusFile(new File(trainCorpus)), new CorpusFile(new File(testCorpus)));
  }
  catch (IOException e) {
    e.printStackTrace();
  }
  catch (ConfigurationException e) {
    e.printStackTrace();
  }
}

protected DriverBase() {
  // empty constructor for children to override
}

public DriverBase(String[] args) throws ConfigurationException {
  SystemConfig systemConfig = DriverUtils.configure(args);
  cfg = systemConfig;
  preprocessor = new Preprocessor(cfg);
  featureGenerator = new FeatureGenerator(cfg);
  trainer = new Trainer(cfg);
  annotator = new Annotator();
  score = new Score(cfg);
}

public void runSystem(Corpus trainCorpus, Corpus testCorpus)
    throws IOException
{
  long systemTime = System.currentTimeMillis();
  ExperimentRecord rec = new ExperimentRecord();
  File workDir = new File(Utils.getWorkDirectory());

  boolean clean = cfg.getBoolean("clean", false);
  if (clean) {
    trainCorpus.cleanTest();
    testCorpus.cleanTrain();
  }

  Corpus combinedCorpus = new CorpusFile(Iterables.concat(trainCorpus, testCorpus), new File(workDir, "combined"));

  // Should the system overwrite existing files
  boolean overwrite = cfg.getBoolean("OVERWRITE_FILES", true);

  int corpusSize = -1;
  
  /* preprocess */
  corpusSize = runPreprocessor(combinedCorpus, overwrite, corpusSize);

  String featSetName = featureGenerator.generateFeatures(combinedCorpus, true, corpusSize);

  /* run learners */
  long time = startStage("learn", "Training on " + Iterables.size(trainCorpus) + " files");
  Classifier classifier = trainer.runLearner(trainCorpus, workDir, featSetName);
  endStage("learn", time);

  time = startStage("test", "Testing on " + Iterables.size(testCorpus) + " files");
  if (featSetName == null)
      throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");


  annotator.runAnnotator(testCorpus, classifier);
  endStage("test", time);

  time = startStage("score", "Score " + Iterables.size(testCorpus) + " files");
  score.runScorer(testCorpus, rec);
  endStage("score", time);

    
  rec.commitRecord();

  long totalTime = System.currentTimeMillis() - systemTime;
  System.out.println("The system ran in " + Long.toString(totalTime / 1000) + " seconds");

}

protected int runPreprocessor(Corpus combinedCorpus, boolean overwrite, int corpusSize)
{
  boolean prepocess = cfg.getPreProcess();
  if (prepocess) {
    long time = startStage("preprocess", "Total of " + corpusSize + " files");
    preprocessor.preprocess(combinedCorpus, overwrite);
    endStage("preprocess", time);
  }
  return corpusSize;
}









}
