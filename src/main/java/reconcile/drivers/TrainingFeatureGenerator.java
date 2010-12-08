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

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.Preprocessor;
import reconcile.SystemConfig;
import reconcile.data.Corpus;
import reconcile.general.Constants;

/**
 * Generate features for training
 * 
 * @author David Buttler
 * 
 */
public class TrainingFeatureGenerator {


public static void main(String[] args)
{
  try {
    String corpusFile = args[0];
    String goldSet = args[1];

    SystemConfig systemConfig = DriverUtils.configure(args);
    systemConfig.setAnnotationSetName(Constants.GS_NP, goldSet);
    systemConfig.setProperty("STOP_AT_FIRST_POSITIVE", true);
    
    FeatureGenerator trainer = new FeatureGenerator(systemConfig);

    // get corpus
    Corpus c = DriverUtils.loadFiles(corpusFile);

    Preprocessor preprocessor = new Preprocessor(systemConfig);
    preprocessor.preprocess(c, false);

    // generate features
    String featureSetName = trainer.generateFeatures(c, true);
    System.out.println("generated features in feature set: " + featureSetName);

  }
  catch (IOException e) {
    e.printStackTrace();
  }
  catch (ConfigurationException e) {
    e.printStackTrace();
  }

}
}
