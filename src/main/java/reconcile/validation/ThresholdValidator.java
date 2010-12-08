/**
 * A validator that attempts different clustering thresholds. The scores for the coreference classifier are assumed to
 * be in the range [-1,1]
 */
package reconcile.validation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import reconcile.Driver;
import reconcile.SystemConfig;
import reconcile.clusterers.ThresholdClusterer;
import reconcile.data.Document;
import reconcile.general.Utils;
import reconcile.scorers.Scorer;


/**
 * @author ves
 * 
 */
public class ThresholdValidator
    extends CrossValidator {

ThresholdClusterer thClusterer;
double threshold;
double totalThreshold;
int numFolds;
private static int NUM_POINTS = 60;
private static int NUM_INITIAL_POINTS = 25;

public ThresholdValidator() {
  super();
  if (clusterer instanceof ThresholdClusterer) {
    thClusterer = (ThresholdClusterer) clusterer;
  }
  else
    throw new RuntimeException("Attempting to run ThresholdValidatror with a non threshold-based clusterer");
  totalThreshold = 0;
  numFolds = 0;
}

/* (non-Javadoc)
 * @see Validation.CrossValidator#classifyFoldInternal()
 */
@Override
public void classifyFoldInternal(Iterable<Document> testFilenames, int foldNum)
{
  File modelFN = new File(Utils.getWorkDirectory() + Utils.SEPARATOR + modelName + ".fold" + foldNum);
  classifyFoldInternal(testFilenames, foldNum, modelFN.getAbsolutePath());
}

/* (non-Javadoc)
 * @see Validation.CrossValidator#classifyFoldInternal()
 */
@Override
public void classifyFoldInternal(Iterable<Document> testFilenames, int foldNum, String modelFN)
{
  runClassifier(testFilenames, modelFN);
  thClusterer.setThreshold(threshold);
  cluster(testFilenames);
}

@Override
public void classifyInternal(Iterable<Document> testFilenames, String modelFN)
{
  runClassifier(testFilenames, modelFN);
  thClusterer.setThreshold(threshold);
  cluster(testFilenames);
}

@Override
public void trainAndValidateFoldInternal(Iterable<Document> validFilenames, File trainVectorsFile, int foldNum)
{
  File modelFilename = new File(Utils.getWorkDirectory() + "/" + cfg.getModelName() + ".fold" + foldNum);
  long time = Driver.startStage("fold_learn", "Training fold " + foldNum);
  learn(trainVectorsFile, modelFilename);
  Driver.endStage("fold_learn", time);

  time = Driver.startStage("threshold_validate", "Validating fold " + foldNum);
  validateFold(validFilenames, modelFilename.getAbsolutePath());

  Driver.endStage("threshold_validate", time);

}

@Override
public void validateFold(Iterable<Document> validFilenames, String modelFN)
{
  double[] range = runClassifier(validFilenames, modelFN);
  double min = range[0], max = range[1];
  // We do a coarse first pass first to narow down the interval that we
  // will examine
  double intervalRange = max - min;
  double step = intervalRange / NUM_INITIAL_POINTS;
  double[] maxs = examineInterval(min, max, step, validFilenames, null);
  double maxThreshold = maxs[0];
  double maxScore = maxs[1];
  System.out.println("Max " + scorer.getName() + " of " + maxScore + " at threshold " + maxThreshold);

  // Now examine the interval arround the maximum threshold
  double newMin = maxThreshold - 1.0 * step;
  double newMax = maxThreshold + 1.0 * step;
  min = newMin < min ? min : newMin;
  max = newMax > max ? max : newMax;
  step = (max - min) / NUM_POINTS;
  File intermediateResultsFile = new File(cfg.getString("VALIDATE_OUTPUT_FILE", null));
  System.out.println("Examining interval [" + min + "," + max + "]");
  maxs = examineInterval(min, max, step, validFilenames, intermediateResultsFile);
  threshold = maxs[0];
  maxScore = maxs[1];
  System.out.println("Max " + scorer.getName() + " of " + maxScore + " at threshold " + threshold);
  totalThreshold += threshold;
  numFolds++;

}

@Override
public String outputInformation()
{
  String info = "Threshold validator: Average threshold =" + totalThreshold / numFolds;
  return info;
}

private double[] examineInterval(double min, double max, double step, Iterable<Document> validFilenames, File outFilename)
{
  double maxThreshold = 0;
  double maxScore = 0;
  boolean outputTrace = outFilename == null ? false : true;
  PrintStream outF = null;
  if (outputTrace) {
    try {
      outF = new PrintStream(outFilename);
    }
    catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
  // System.out.println("Examining range ["+min+","+max+"]. Step is "+step);
  for (double th = min; th <= max; th += step) {
    thClusterer.setThreshold(th);
    cluster(validFilenames, "valid");
    double[] score = scorer.score(true, validFilenames, "valid");

    double scor = score[Scorer.F];
    // Scorer.printScore(scorer.getName(), score);
    if (scor > maxScore) {
      maxScore = scor;
      maxThreshold = th;
    }
    if (outputTrace) {
      outF.println(th + "," + score[Scorer.PRECISION] + "," + score[Scorer.RECALL] + "," + scor);
    }
  }
  if (outF != null) {
    outF.flush();
    outF.close();
  }
  return new double[] { maxThreshold, maxScore };
}

public static void setNumInitialPoints(int num_initial_points)
{
  NUM_INITIAL_POINTS = num_initial_points;
}

public static void setNumPoints(int num_points)
{
  NUM_POINTS = num_points;
}

@Override
public void configure(SystemConfig cfg)
{
  NUM_INITIAL_POINTS = cfg.getInteger("NUM_INITIAL_POINTS", NUM_INITIAL_POINTS);
  NUM_POINTS = cfg.getInteger("NUM_POINTS", NUM_POINTS);
}

}
