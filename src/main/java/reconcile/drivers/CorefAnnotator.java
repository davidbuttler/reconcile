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
 * Created on Mar 16, 2009
 * 
 * 
 */
package reconcile.drivers;

import gov.llnl.text.util.MapUtil;
import gov.llnl.text.util.Timer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import reconcile.clusterers.Clusterer;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Corpus;
import reconcile.data.CorpusFile;
import reconcile.data.Document;
import reconcile.general.Constants;

import com.google.common.collect.Iterables;

/**
 * @author David Buttler
 * 
 */
public class CorefAnnotator {

public CorefAnnotator() {

}

public void run(Corpus corpus)
{
  try {
    annotate(corpus);
  }
  catch (IOException e) {
    e.printStackTrace();
    throw new RuntimeException(e);
  }
}

/**
 * Create the coref annotations for a document
 * 
 * @param corpus
 * @throws IOException
 */
public void annotate(Corpus corpus)
    throws IOException
{

  Timer t = new Timer();
  for (Document doc : corpus) {
    t.increment("a");
    annotate(doc);
  }
  t.end();
}

public void annotate(Document doc)
throws IOException
{
  annotate(doc, Constants.RESPONSE_NPS);
}
public void annotate(Document doc, String outputAnnotationSetName)
    throws IOException
{
  Clusterer.printClusteringAsAnnotationSet(doc, doc.readClusterFile(), outputAnnotationSetName);

  // remove singletons
  AnnotationSet coref = doc.getAnnotationSet(outputAnnotationSetName);
  Map<String, Set<String>> map = new HashMap<String, Set<String>>();
  Map<String, Annotation> aMap = new HashMap<String, Annotation>();
  for (Annotation a : coref) {
    String key = a.getAttribute(Constants.CLUSTER_ID);
    String val = a.getAttribute(Constants.CE_ID);
    MapUtil.addToMapSet(map, key, val);
    aMap.put(key, a);
  }
  for (String key : map.keySet()) {
    Set<String> vals = map.get(key);
    if (vals.size() == 1) {
      String val = Iterables.getOnlyElement(vals);
      Annotation a = aMap.get(val);
      boolean success = coref.remove(a);
      if (!success) {
        System.out.println("failed to remove: " + a);
      }
    }
  }
  doc.writeAnnotationSet(coref);
}

public static void main(String[] args)
{
  try {
    Corpus c = new CorpusFile(new File(args[0]), args[1]);
    CorefAnnotator annotator = new CorefAnnotator();
    annotator.annotate(c);
  }
  catch (IOException e) {
    e.printStackTrace();
  }
}
}
