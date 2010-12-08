/*
 * Copyright (c) 2008, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National
 * Laboratory. Written by David Buttler, buttler1@llnl.gov CODE-400187 All rights reserved. This file is part of
 * RECONCILE
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License (as published by the Free Software Foundation) version 2, dated June 1991. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the IMPLIED WARRANTY OF MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the terms and conditions of the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA For full text see license.txt
 * 
 * Created on Sep 18, 2009
 * 
 */
package reconcile.drivers;

import static reconcile.Driver.endStage;
import static reconcile.Driver.startStage;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.FeatureVectorGenerator;
import reconcile.Preprocessor;
import reconcile.SystemConfig;
import reconcile.data.Corpus;
import reconcile.data.Document;

/**
 * @author David Buttler
 * 
 */
public class FeatureGenerator {

private SystemConfig cfg;

public static void main(String[] args)
{
  try {
    String corpusFile = args[0];

    SystemConfig systemConfig = DriverUtils.configure(args);
    
    FeatureGenerator trainer = new FeatureGenerator(systemConfig);

    // get corpus
    Corpus c = DriverUtils.loadFiles(corpusFile);

    Preprocessor preprocessor = new Preprocessor(systemConfig);
    preprocessor.preprocess(c, false);

    // generate features
    String featureSetName = trainer.generateFeatures(c, false);
    System.out.println("generated features in feature set: " + featureSetName);

  }
  catch (IOException e) {
    e.printStackTrace();
  }
  catch (ConfigurationException e) {
    e.printStackTrace();
  }

}

public FeatureGenerator(SystemConfig cfg) {
  this.cfg = cfg;
}

public String generateFeatures(Corpus corpus, boolean training)
    throws IOException
{
  return generateFeatures(corpus, training, -1);
}

public String generateFeatures(Document doc)
throws IOException
{
  FeatureVectorGenerator.makeFeatures(doc);
  return cfg.getFeatSetName();
}

public String generateFeatures(Corpus corpus, boolean training, int corpusSize)
    throws IOException
{
  /* make feature vector */
  // generate feature files
  boolean generateFeatures = cfg.getGenerateFeatures();
  // String DATASET = cfg.getDataset();
  String featSetName = cfg.getFeatSetName();

  if (generateFeatures) {
    long time = startStage("feature vector generation", "Total of " + corpusSize + " files");
    // FeatureVectorGenerator.makeFeatures(allFilenames);
    if (featSetName == null)
      throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");

    FeatureVectorGenerator.makeFeatures(corpus, training);
    endStage("feature vector generation", time);
  }

  return featSetName;
}

}
