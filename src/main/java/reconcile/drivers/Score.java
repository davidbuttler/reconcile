package reconcile.drivers;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.ExperimentRecord;
import reconcile.SystemConfig;
import reconcile.data.Corpus;
import reconcile.general.Constants;
import reconcile.scorers.MUCScore;
import reconcile.scorers.Scorer;

/**
 * This driver is to test a list of files (instead of root directories) using a generated model, and to specify the name
 * of the gold annotations to score against
 */
public class Score {

public static void usage()
{
  System.out.println("Usage:");
  String use = Score.class.getName() + ": <corpus>" + " <name of gold annotations>"
      + " <name of test annotations>" + " [" + DriverUtils.CONFIG_ARG + "<name>]* " + "[" + DriverUtils.HELP_ARG + "]";

  System.out.println(use);
  System.exit(0);
}

/**
 * @param args
 */
public static void main(String[] args)
{

  try {
    Score d = new Score(args);

    String corpusFile = args[0];

    // note that we need to initialize the driver so that the configuration can be done before we load files as that
    // uses a config option
    Corpus testCorpus = DriverUtils.loadFiles(corpusFile);

    d.runScorer(testCorpus);
  }
  catch (IOException e) {
    e.printStackTrace();
  }
  catch (ConfigurationException e) {
    e.printStackTrace();
  }
}

private SystemConfig cfg;



public Score(String[] args)
    throws ConfigurationException {
  SystemConfig systemConfig = DriverUtils.configure(args);
  cfg = systemConfig;
  String goldAnnots = args[1];
  String testAnnots = args[2];
  cfg.setAnnotationSetName(Constants.GS_NP, goldAnnots);
  cfg.setAnnotationSetName(Constants.RESPONSE_NPS, testAnnots);
  cfg.setProperty("RUN_FEATURE_GENERATION", true);
}
public Score(SystemConfig config) {
  super();
  cfg = config;
}

protected void runScorer(Corpus testCorpus)
    throws IOException
{
  MUCScore muc = new MUCScore();
  double[] result = muc.score(true, testCorpus);
  System.out.println("final MUC score: ");
  Scorer.printScore("MUC", result);

//  BCubed bc = new BCubed();
//  bc.score(testCorpus, outList, Constants.ORIG, Constants.RESPONSE_NPS, true);
}


protected void runScorer(Corpus testCorpus, ExperimentRecord rec)
    throws IOException
{

  MUCScore muc = new MUCScore();
  double[] result = muc.score(true, testCorpus);
  System.out.println("final MUC score: ");
  Scorer.printScore("MUC", result);
  Scorer.printFileScore("MUC", result, rec.getOutput());

//  BCubed bc = new BCubed();
//  bc.score(testCorpus, outList, Constants.ORIG, Constants.RESPONSE_NPS, true);
}
}
