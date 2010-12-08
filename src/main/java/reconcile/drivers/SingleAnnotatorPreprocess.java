/*
 * Copyright (c) 2008, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National
 * Laboratory. Written by David Buttler, buttler1@llnl.gov
 * 
 * Created on Feb 22, 2008
 * 
 */
package reconcile.drivers;

import java.io.File;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.Preprocessor;
import reconcile.SystemConfig;
import reconcile.data.Corpus;
import reconcile.data.CorpusFile;

import com.google.common.collect.Lists;

/**
 * Given a corpus, preprocess the files with a single, given annotator
 * 
 * @author <a href="mailto:buttler1@llnl.gov">David Buttler</a>
 * 
 */
public class SingleAnnotatorPreprocess {

public static void usage()
{
  System.out.println("Usage:");
  String use = Score.class.getName() + ": <corpus> <annotator>+" + " [" + DriverUtils.CONFIG_ARG + "<name>]* " + "["
      + DriverUtils.HELP_ARG + "]";

  System.out.println(use);
  System.exit(0);
}

public static void main(String[] args)
{
  if (args.length < 1) {
    usage();
  }

  try {
    SingleAnnotatorPreprocess p = new SingleAnnotatorPreprocess(args);
    p.run();
  }
  catch (ConfigurationException e) {
    e.printStackTrace();
  }

}

private SystemConfig cfg;
private Corpus corpus;
private List<String> annotator;
private Preprocessor preprocessor;

public SingleAnnotatorPreprocess(String[] args)
    throws ConfigurationException {
  SystemConfig systemConfig = DriverUtils.configure(args);
  cfg = systemConfig;
  preprocessor = new Preprocessor(cfg);

  annotator = Lists.newArrayList();

  File tempDir = new File(args[0]);
  for (int i = 1; i < args.length; i++) {
    if (args[i].startsWith("-")) break;
    annotator.add(args[i]);
  }
  corpus = new CorpusFile(tempDir, cfg);

}

public SingleAnnotatorPreprocess(SystemConfig config) {
  super();
  cfg = config;
  preprocessor = new Preprocessor(cfg);
}

public void setCorpus(Corpus c)
{
  corpus = c;
}

/**
 * preprocess the current corpus
 */
private void run()
{
  try {
    boolean overwrite = cfg.getBoolean("OVERWRITE_FILES", false);
    preprocessor.preprocess(corpus, annotator, overwrite);
  }
  catch (Exception e) {
    e.printStackTrace();
  }

}

}
