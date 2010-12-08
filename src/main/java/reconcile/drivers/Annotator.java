package reconcile.drivers;

import gov.llnl.text.util.Timer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.Constructor;
import reconcile.SystemConfig;
import reconcile.classifiers.Classifier;
import reconcile.clusterers.Clusterer;
import reconcile.data.AnnotationSet;
import reconcile.data.Corpus;
import reconcile.data.Document;
import reconcile.general.Constants;

/**
 * Assuming that a corpus has feature files generated, this class generates coreference annotations.
 * This involves: <ol>
 * <li> classification: predicting whether each potentially coreferent pair of NPs, is
 * <li> clustering: grouping the pairs
 * <li> computing the chains and generating annotations for nps that show which chain each is in 
 */
public class Annotator {

public static void usage()
{
  System.out.println("Usage:");
  String use = Annotator.class.getName() + ": <corpus>" + " <model file>" + " <name of ouptput annotations>"
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
    Annotator d = new Annotator(args);
    if (args.length < 3) {
      usage();
    }
    
    String corpusFile = args[0];
    

    // note that we need to initialize the driver so that the configuration can be done before we load files as that
    // uses a config option
    Corpus testCorpus = DriverUtils.loadFiles(corpusFile);

    d.runAnnotator(testCorpus);
  }
  catch (IOException e) {
    e.printStackTrace();
  }
  catch (ConfigurationException e) {
    e.printStackTrace();
  }
}

private File modelFile;
private SystemConfig cfg;



public Annotator(String[] args)
    throws ConfigurationException {
  cfg = DriverUtils.configure(args);
  modelFile = new File(args[1]);
  cfg.setAnnotationSetName(Constants.RESPONSE_NPS, args[2]);
}


/**
 * Assumes that the system has already been configured
 */
public Annotator() {
  super();
}


public void setModel(File model) {
  modelFile = model;
}

protected void runAnnotator(Corpus testCorpus)
    throws IOException
{
  if (modelFile == null) {
    throw new NullPointerException("model file has not been set");
  }
  Classifier classifier = Constructor.createClassifier(modelFile.getAbsolutePath());

  runAnnotator(testCorpus, classifier);
}


protected void runAnnotator(Corpus testCorpus, File model)
    throws IOException
{
  Classifier classifier = Constructor.createClassifier(model.getAbsolutePath());

  runAnnotator(testCorpus, classifier);
}


protected void runAnnotator(Corpus testCorpus, Classifier classifier)
    throws IOException
{
  Clusterer clusterer = Constructor.createClusterer();

  runAnnotator(testCorpus, classifier, clusterer);
}

protected void runAnnotator(Corpus testCorpus, Classifier classifier, Clusterer clusterer)
    throws IOException
{
  
  Timer t = new Timer();
  for (Document d : testCorpus) {
    t.increment();
    double[] result = classifier.test(d);
    if (result != null) {
      AnnotationSet cluster = clusterer.cluster(d);
      cluster.setName(cfg.getAnnotationSetName(Constants.RESPONSE_NPS));
      d.writeAnnotationSet(cluster);
    }
  }
  t.end();
}

}
