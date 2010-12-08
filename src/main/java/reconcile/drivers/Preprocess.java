/*
 * Copyright (c) 2008, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National
 * Laboratory. Written by David Buttler, buttler1@llnl.gov
 * 
 * Created on Feb 22, 2008
 * 
 */
package reconcile.drivers;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.Preprocessor;
import reconcile.SystemConfig;
import reconcile.data.Corpus;
import reconcile.data.CorpusFile;

/**
 * Given a corpus, preprocess the files
 * 
 * @author <a href="mailto:buttler1@llnl.gov">David Buttler</a>
 * 
 */
public class Preprocess {

public static void usage()
{
  System.out.println("Usage:");
  String use = Score.class.getName() + ": <corpus>" + " [" + DriverUtils.CONFIG_ARG + "<name>]* " + "["
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
    Preprocess p = new Preprocess(args);
    p.run();
  }
  catch (ConfigurationException e) {
    e.printStackTrace();
  }

}

private SystemConfig cfg;
private Corpus corpus;
private Preprocessor preprocessor;

public Preprocess(String[] args)
    throws ConfigurationException {
  SystemConfig systemConfig = DriverUtils.configure(args);
  cfg = systemConfig;
  preprocessor = new Preprocessor(cfg);

  File tempDir = new File(args[0]);
  corpus = new CorpusFile(tempDir, cfg);

}

public Preprocess(SystemConfig config) {
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
    preprocessor.preprocess(corpus, overwrite);
  }
  catch (Exception e) {
    e.printStackTrace();
  }

}

}
