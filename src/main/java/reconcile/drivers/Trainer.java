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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.Constructor;
import reconcile.FeatureMerger;
import reconcile.Preprocessor;
import reconcile.SystemConfig;
import reconcile.classifiers.Classifier;
import reconcile.data.Corpus;
import reconcile.general.Constants;
import reconcile.general.Utils;

/**
 * @author David Buttler
 * 
 */
public class Trainer {

private SystemConfig cfg;

public static void main(String[] args)
{
  try {
    String corpusFile = args[0];
    String goldSet = args[1];
    File outputDir = new File(args[2]);
        
    SystemConfig systemConfig = DriverUtils.configure(args);
    systemConfig.setAnnotationSetName(Constants.GS_NP, goldSet);

    Trainer trainer = new Trainer(systemConfig);
    FeatureGenerator featureGenerator = new FeatureGenerator(systemConfig);
    
    // get corpus
    Corpus c = DriverUtils.loadFiles(corpusFile);

    Preprocessor preprocessor = new Preprocessor(systemConfig);
    preprocessor.preprocess(c, false);
    
    // generate features
    String featureSetName = featureGenerator.generateFeatures(c, true);
    
    // train classifier
    Classifier classifier = trainer.runLearner(c, outputDir, featureSetName);
    System.out.println("classifier trained: "+classifier.getName());

  }
  catch (IOException e) {
    e.printStackTrace();
  }
  catch (ConfigurationException e) {
    e.printStackTrace();
  }
  
}
public Trainer(SystemConfig cfg) {
  this.cfg = cfg;
}


public Classifier runLearner(Corpus trainCorpus, File workDir, String featSetName)
    throws IOException, FileNotFoundException
{
  String modelName = cfg.getModelName();
  String model = Utils.getWorkDirectory()+"/"+ modelName;
  if (modelName == null) throw new RuntimeException("Model name needs to be specified (parameter MODEL_NAME)");
  // SimpleDateFormat nameFormat = new SimpleDateFormat("yyyyMMdd");
  // Date date = new Date();
  // modelName = nameFormat.format(date) + "-" + modelName;
  Classifier classifier = Constructor.createClassifier(model);

  if (featSetName == null)
    throw new RuntimeException("Feature set name needs to be specified (parameter FEAT_SET_NAME)");
  if (modelName == null) throw new RuntimeException("Model name needs to be specified (parameter MODEL_NAME)");

  // merge feature files together
  File mergedFeatureVector = File.createTempFile("mergedFeatureVector_", ".csv.gz", workDir);

  OutputStream trainFeatures = new FileOutputStream(mergedFeatureVector);
  FeatureMerger.combine(trainFeatures, trainCorpus);
  System.out.println("start training");
  classifier.train(mergedFeatureVector, new File(workDir, classifier.getName() + ".model"));
  return classifier;
}

}
