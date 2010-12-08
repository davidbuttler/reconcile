/*
 * Copyright (c) 2008, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National
 * Laboratory. Written by David Buttler, buttler1@llnl.gov
 * 
 */
package reconcile.drivers;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.Preprocessor;
import reconcile.SystemConfig;
import reconcile.data.Corpus;

/**
 * Given a corpus, preprocess the files
 * 
 * @author <a href="mailto:buttler1@llnl.gov">David Buttler</a>
 * 
 */
public class PreprocessFileList {

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
    PreprocessFileList p = new PreprocessFileList(args);
    p.run();
  }
  catch (ConfigurationException e) {
    e.printStackTrace();
  }
  catch (IOException e) {
    e.printStackTrace();
  }

}

private SystemConfig cfg;
private Corpus corpus;
private Preprocessor preprocessor;

public PreprocessFileList(String[] args)
    throws ConfigurationException, IOException {
  SystemConfig systemConfig = DriverUtils.configure(args);
  cfg = systemConfig;
  preprocessor = new Preprocessor(cfg);

  corpus = DriverUtils.loadFiles(args[0]);

}

public PreprocessFileList(SystemConfig config) {
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
