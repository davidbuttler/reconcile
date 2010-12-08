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
 * Created on Oct 8, 2009
 */
package reconcile.drivers;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.Reconcile;
import reconcile.SystemConfig;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Corpus;
import reconcile.data.Document;
import reconcile.general.Constants;

/**
 * Given a response, clean the data so that only chains that contain an NP are left
 * 
 * @author David Buttler
 * 
 */
public class Cleaner {

public static void usage()
{
  System.out.println("Usage:");
  String use = Cleaner.class.getName() + ": <corpus>" + " <name of ouptput annotations>" + " ["
      + DriverUtils.CONFIG_ARG + "<name>]* " + "[" + DriverUtils.HELP_ARG + "]";

  System.out.println(use);
  System.exit(0);
}

/**
 * @param args
 */
public static void main(String[] args)
{

  try {
    Cleaner d = new Cleaner(args);
    if (args.length < 2) {
      usage();
    }

    String corpusFile = args[0];

    // note that we need to initialize the driver so that the configuration can be done before we load files as that
    // uses a config option
    Corpus testCorpus = DriverUtils.loadFiles(corpusFile);

    d.run(testCorpus);
  }
  catch (IOException e) {
    e.printStackTrace();
  }
  catch (ConfigurationException e) {
    e.printStackTrace();
  }
}

public Cleaner(String[] args)
    throws ConfigurationException {
  SystemConfig cfg = DriverUtils.configure(args);
  cfg.setAnnotationSetName(Constants.RESPONSE_NPS, args[1]);
}

public void run(Corpus testCorpus)
{
  for (Document d : testCorpus) {
    AnnotationSet chains = d.getAnnotationSet(Constants.RESPONSE_NPS);
    System.out.println("chains size: " + chains.size());

    AnnotationSet origChains = new AnnotationSet(Constants.RESPONSE_NPS + ".orig", chains);
    d.writeAnnotationSet(origChains);
    Map<String, Set<Annotation>> map = Reconcile.computeChains(chains);
    System.out.println("map size: " + map.size());
    // remove annotations not in an NE chain
    for (String key : map.keySet()) {
      Set<Annotation> chain = map.get(key);
      // System.out.println("key size: " + key + ": " + chain.size());
      boolean hasNE = hasNE(d, chain);
      if (!hasNE || chain.size() < 2) {
        for (Annotation a : chain) {
          chains.remove(a);
        }
      }
    }
    d.writeAnnotationSet(chains);
  }
}

private boolean hasNE(Document d, Set<Annotation> chain)
{
  AnnotationSet ne = d.getAnnotationSet(Constants.NE);
  boolean overlapsNE = false;
  for (Annotation a : chain) {
    if (ne.getOverlapping(a).size() > 0) {
      overlapsNE = true;
      break;
    }
  }
  return overlapsNE;
}
}
