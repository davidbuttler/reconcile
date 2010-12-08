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
 * Created on Jan 26, 2009
 * 
 */
package reconcile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import reconcile.classifiers.Classifier;
import reconcile.clusterers.Clusterer;
import reconcile.clusterers.ThresholdClusterer;
import reconcile.featureExtractor.InternalAnnotator;
import reconcile.featureVector.AllFeatures;
import reconcile.featureVector.Feature;
import reconcile.filter.PairGenerator;
import reconcile.general.Utils;
import reconcile.scorers.Scorer;
import reconcile.validation.CrossValidator;

/**
 * This is a class to centralize all of the reflection inside of Reconcile
 * 
 * @author David Buttler
 * 
 */
public class Constructor {

@SuppressWarnings("unchecked")
public static InternalAnnotator createInternalAnnotator(String className)
{
  String origClassName = className;
  // Class names can be specified either by the full java name
  if (!className.contains(".")) {
    // otherwise, assume the class is in FeatureExtractor directory
    className = "reconcile.featureExtractor." + className;
  }
  try {
    // System.out.println(className);
    Class featClass = Class.forName(className);
    InternalAnnotator result = (InternalAnnotator) featClass.newInstance();
    result.setName(origClassName);
    return result;
  }
  catch (Exception e) {
	System.out.println("While creating "+className);
    throw new RuntimeException(e);
  }
}

public static Classifier createClassifier(String modelFile)
{
  SystemConfig cfg = Utils.getConfig();
  String name = cfg.getClassifier();
  return createClassifier(name, modelFile);

}

@SuppressWarnings("unchecked")
public static Classifier createClassifier(String name, String modelFile)
{
  // Class names can be specified either by the full java name
  if (!name.contains(".")) {
    // otherwise, assume the class is in FeatureExtractor directory
    name = "reconcile.classifiers." + name;
  }

  try {
    // System.out.println(className);
    Class featClass = Class.forName(name);
    Classifier result = (Classifier) featClass.newInstance();
    result.setModelFile(modelFile);
    return result;
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }

}

public static Clusterer createClusterer()
{
  SystemConfig cfg = Utils.getConfig();
  String name = cfg.getClusterer();
  return createClusterer(name);

}

@SuppressWarnings("unchecked")
public static Clusterer createClusterer(String name)
{
  // Class names can be specified either by the full java name
  if (!name.contains(".")) {
    // otherwise, assume the class is in FeatureExtractor directory
    name = "reconcile.clusterers." + name;
  }
  try {
    // System.out.println(className);
    Class featClass = Class.forName(name);
    Clusterer clusterer = (Clusterer) featClass.newInstance();
    if (clusterer instanceof ThresholdClusterer) {
      String thr = Utils.getConfig().getString("ClustOptions.THRESHOLD");
      if (thr != null && thr.length() > 0) {
        ((ThresholdClusterer) clusterer).setThreshold(Double.parseDouble(thr));
      }
    }
    return clusterer;
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }

}

@SuppressWarnings("unchecked")
public static Clusterer createClusterer(String name, double threshold)
{
  // Class names can be specified either by the full java name
  if (!name.contains(".")) {
    // otherwise, assume the class is in FeatureExtractor directory
    name = "reconcile.clusterers." + name;
  }
  try {
    // System.out.println(className);
    Class featClass = Class.forName(name);
    Clusterer clusterer = (Clusterer) featClass.newInstance();
    if (clusterer instanceof ThresholdClusterer) {
        ((ThresholdClusterer) clusterer).setThreshold(threshold);
    }
    return clusterer;
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }

}


@SuppressWarnings("unchecked")
public static Feature createFeature(String name)
{
  if (AllFeatures.featMap == null) {
    AllFeatures.featMap = new HashMap<String, Feature>();
  }

  Feature feat = AllFeatures.featMap.get(name);
  if (feat == null) {
    try {
      String className = name;
      if (!className.contains(".")) {
        className = "reconcile.featureVector.individualFeature." + name;
      }

      Class featClass = Class.forName(className);
      feat = (Feature) featClass.newInstance();
      AllFeatures.featMap.put(name, feat);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  return feat;
}

/*
 * Create the required features and return them in a list
 */
public static List<Feature> createFeatures(String[] features)
{
  ArrayList<Feature> result = new ArrayList<Feature>();

  for (String feat : features) {
    Feature newFeat = createFeature(feat);
    result.add(newFeat);
  }
  return result;
}

@SuppressWarnings("unchecked")
public static Scorer createScorer(String name)
{
  // Class names can be specified either by the full java name
  if (!name.contains(".")) {
    // otherwise, assume the class is in FeatureExtractor directory
    name = "reconcile.scorers." + name;
  }
  try {
    // System.out.println(className);
    Class featClass = Class.forName(name);
    Scorer result = (Scorer) featClass.newInstance();
    return result;
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }

}

@SuppressWarnings("unchecked")
public static CrossValidator createCrossValidator(String name)
{
  String cv = name;
  if (cv == null) throw new RuntimeException("Validator not specified");
  // Class names can be specified either by the full java name
  if (!cv.contains(".")) {
    // otherwise, assume the class is in FeatureExtractor directory
    cv = "reconcile.validation." + cv;
  }

  try {
    Class valid = Class.forName(cv);
    CrossValidator result = (CrossValidator) valid.newInstance();
    return result;
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

@SuppressWarnings("unchecked")
public static PairGenerator makePairGenClass(String className)
{
  // Class names can be specified either by the full java name
  if (!className.contains(".")) {
    // otherwise, assume the class is in FeatureExtractor directory
    className = "reconcile.filter." + className;
  }

  try {
    // System.out.println(className);
    Class featClass = Class.forName(className);
    PairGenerator result = (PairGenerator) featClass.newInstance();
    return result;
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}


}
