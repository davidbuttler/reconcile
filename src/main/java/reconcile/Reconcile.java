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
 * Created on Mar 26, 2009
 *
 */
package reconcile;

import gov.llnl.text.util.FileUtils;
import gov.llnl.text.util.Timer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import reconcile.classifiers.Classifier;
import reconcile.clusterers.Clusterer;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Corpus;
import reconcile.data.CorpusFile;
import reconcile.data.Document;
import reconcile.drivers.CorefAnnotator;
import reconcile.general.Constants;
import reconcile.general.Utils;

/**
 * @author David Buttler
 *
 */
public class Reconcile {



// classifier
Classifier classifier;

// clusterer
Clusterer clusterer;

// annotator
CorefAnnotator annotator;

/**
 * whether or not to overwrite existing annotations
 */
public boolean overwrite;

private SystemConfig config;

private Preprocessor preprocessor;

public static final String CONFIG_ARG = "--config=";
public static final String MODEL_ARG = "--model=";
public static final String DEBUG_MODE_ARG = "--debug";
public static String HELP_ARG = "--help";
private static boolean debug = false;


static public void usage()
{
  String use = "Reconcile: <input corpus>"
      + "[" + MODEL_ARG + "<name>] "
      + "[" + CONFIG_ARG + "<name>] "
      + "[" + HELP_ARG + "] ["+DEBUG_MODE_ARG+"]";

  System.out.println(use);
  System.exit(0);
}



public static void main(String[] args)
{
  try {
    String configFile = null;
    String modelFile = null;
    for (int i = 1; i < args.length; ++i) {
      if (args[i].startsWith(CONFIG_ARG)) {
        configFile = args[i].substring(CONFIG_ARG.length());
      }
      else if (args[i].startsWith(MODEL_ARG)) {
        modelFile = args[i].substring(MODEL_ARG.length());
      }
      else if (args[i].startsWith(DEBUG_MODE_ARG)) {
        debug  = true;;
      }
      else {
        usage();
      }
    }

    File corpusFile = new File(args[0]);
    Corpus testCorpus = null;
    if (configFile != null) {
      System.out.println("config file: "+configFile);
      Utils.setConfig(configFile);
    }
    if (corpusFile.isDirectory()) {
      System.out.println("corpus dir: "+corpusFile);
      testCorpus = new CorpusFile(corpusFile);
    }
    else {
      throw new RuntimeException("input corpus must be a directory");
    }

    SystemConfig cfg = Utils.getConfig();
    Reconcile reconcile = null;
    // either use the default model or the one passed on the command line
    if (modelFile == null) {
      reconcile = new Reconcile(cfg);
    }
    else {
      System.out.println("model file: "+modelFile);
      reconcile = new Reconcile(cfg, new File(modelFile));
    }

    Timer t = new Timer();
    for (Document d : testCorpus) {
      t.increment();
      // get the coreference annotations
      AnnotationSet corefAnnots = null;
      if (!reconcile.overwrite && d.existsAnnotationSetFile(Constants.RESPONSE_NPS)) {
        corefAnnots = d.getAnnotationSet(Constants.RESPONSE_NPS);
      }
      else {
        corefAnnots = reconcile.process(d);
      }
      System.out.println("corefernt annotations for "+d.getDocumentId());
      Map<String, Set<Annotation>> chains = computeChains(corefAnnots);
      Set<Set<Annotation>> doneSet = new HashSet<Set<Annotation>>();
      for (String id: chains.keySet()) {
        Set<Annotation> chain = chains.get(id);
        if (doneSet.contains(chain)) {
          continue;
        }
        else {
          doneSet.add(chain);
        }
        System.out.println("id:"+id);
        for (Annotation a:chain) {
          System.out.println("\t" + a.getStartOffset() + ", " + a.getEndOffset() + ":" + d.getAnnotText(a));
        }
        System.out.println("----------------------");
      }
    }
    if (debug) {
      FeatureVectorGenerator.printFeatTiming();
    }
    t.end();
  }
  catch (IOException e) {
    e.printStackTrace();
  }
}

/**
 * Construct a reconcile pipeline using the default model name in the default working directory
 */
public Reconcile(SystemConfig systemConfig) {
  config = systemConfig;
  String modelName = config.getModelName();
  File model = new File(Utils.getWorkDirectory(), modelName);
  init(model.toString());
}

/**
 * Construct a reconcile pipeline using the given model file
 */
public Reconcile(SystemConfig systemConfig, File model) {
  config = systemConfig;
  init(model.toString());
}


private void init(String model) {

  preprocessor = new Preprocessor(config);

  // classifier reqs
  classifier = Constructor.createClassifier(model);

  // clusterer reqs
  String clustererName = config.getClusterer();
  clusterer = Constructor.createClusterer(clustererName);

  // annotator
  annotator = new CorefAnnotator();

  overwrite = config.getBoolean("OVERWRITE_FILES");
}
/**
 * @param d
 * @return
 * @throws IOException
 */
public AnnotationSet process(Document d) throws IOException
{
  return process(d, 0);
}
/**
 * @param d
 * @return
 * @throws IOException
 */
public AnnotationSet process(Document d, int docNum) throws IOException
{
  try {
    // preprocessing steps (parsing and ner)
    preprocessor.preprocess(d, overwrite);
    AnnotationSet nps = d.getAnnotationSet(Constants.NP) ;
    System.out.println("there are "+nps.size()+" nps");
//    if (nps.size()>1000) {
//      System.out.println("TOO MANY NPs.  Will not process");
//      return new AnnotationSet(Constants.RESPONSE_NPS);
//    }
    // generate feature vectors
    FeatureVectorGenerator.makeFeatures(d);
    // classify and cluster mentions
    double[] result = classifier.test(d);
    if (result != null) {
      clusterer.cluster(d);
      // annotate corpus with coreference
      annotator.annotate(d);
      return d.getAnnotationSet(Constants.RESPONSE_NPS);
    }
    else {  // failed to classify results, so we can't do the coreference
      return new AnnotationSet(Constants.RESPONSE_NPS);
    }
  }
  finally {
    // clean up the feature file as this may be too big to keep around
    d.deleteFeatureFile();
  }
}

/**
 * Given a string, return the annotation set that defines the coreference annotations
 * @param documentText
 * @return  the annotation set that defines the coreference annotations
 * @throws IOException
 */
public AnnotationSet process(String documentText) throws IOException
{
  File tmpFile = File.createTempFile("reconcile_", ".dir");
  FileUtils.delete(tmpFile);
  Utils.createDirectory(tmpFile.getAbsolutePath());
  Document doc = new Document(tmpFile);
  AnnotationSet coref = process(doc);
  doc.clean();
  doc = null;
  FileUtils.recursivelyDelete(tmpFile);

  return coref;
}
/**
 *
 */
public static Map<String, Set<Annotation>> computeChains(Iterable<Annotation> model)
{
  Map<String, Set<Annotation>> chains = new HashMap<String, Set<Annotation>>();

  // first go through and make every annotation match a chain
  for (Annotation a : model) {
    String id = a.getAttribute(Constants.CE_ID);
    if (id != null) {
      try {
        Set<Annotation> chain = chains.get(id);
        if (chain == null) {
          chain = new TreeSet<Annotation>();
          chains.put(id, chain);
        }
        chain.add(a);
      }
      catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
  }

  // now merge chains when one refs the other
  for (Annotation a : model) {
    String id = a.getAttribute(Constants.CE_ID);
    String ref = a.getAttribute(Constants.CLUSTER_ID);
    if (id != null) {
      Set<Annotation> idChain = chains.get(id);
      if (ref != null) {
        Set<Annotation> refChain = chains.get(ref);
        if (refChain == null) {
          refChain = new TreeSet<Annotation>();
          chains.put(ref, refChain);
        }
        refChain.addAll(idChain);
        for (Annotation refA : refChain) {
          String oId = refA.getAttribute(Constants.CE_ID);
          chains.put(oId, refChain);
        }
      }
    } // else we don't need to merge with anybody else so nothing left to do
  }
  return chains;
}




public boolean isOverwrite()
{
  return overwrite;
}




public void setOverwrite(boolean overwrite)
{
  this.overwrite = overwrite;
}

}
