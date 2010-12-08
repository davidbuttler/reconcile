/*
 * Copyright (c) 2008, Lawrence Livermore National Security, LLC. 
 * Produced at the Lawrence Livermore National Laboratory. 
 * Written by David Buttler, buttler1@llnl.gov
 * CODE-400187 All rights reserved.
 * This file is part of RECONCILE
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License (as published by the Free Software Foundation) version 2, dated June 1991.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the IMPLIED WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the terms and conditions of the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * For full text see license.txt
 * 
 * Created on Sep 18, 2009
 *
 *
 */
package reconcile.drivers;

import gov.llnl.text.util.LineIterable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;

import reconcile.SystemConfig;
import reconcile.data.CorpusFile;
import reconcile.general.Utils;

import com.google.common.collect.Lists;


/**
 * @author David Buttler
 *
 */
public class DriverUtils {

  public static SystemConfig configure(String[] args)
      throws ConfigurationException
  {
    List<String> configFiles = Lists.newArrayList();
  
  
    for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith(CONFIG_ARG)) {
        configFiles.add(args[i].substring(CONFIG_ARG.length()));
      }
      else if (args[i].startsWith(HELP_ARG)) {
        DriverBase.usage();
      }
    }
  
    SystemConfig cfg = null;
    if (configFiles.size() >= 1) {
      for (String cFile :configFiles) {
        System.out.println("Loading config file: " + cFile);
        if (cfg == null) {
          cfg = new SystemConfig(cFile);
        }
        else {
          cfg.addConfig(cFile);
        }
      }
    }
    System.out.println("loading default config");
    SystemConfig defaultConfig = Utils.getDefaultConfig();
    if (cfg == null) {
      cfg = defaultConfig;
    }
    else {
      cfg.addConfig(defaultConfig);
    }
    Utils.setConfig(cfg);
    return cfg;
  }

  /**
   * @param corpusFile
   * @param corpusName
   *          name of the corpus so we can store metadata if needed in the work dir
   * @param half
   *          which half of the corpus to return
   * @return list of files constructed from the names in the corpusFile
   * @throws IOException
   */
  public static CorpusFile loadFiles(String corpusFile)
      throws IOException
  {
    return loadFiles(corpusFile, "test");
  }

  /**
   * @param corpusFile
   * @param corpusName
   *          name of the corpus so we can store metadata if needed in the work dir
   * @param half
   *          which half of the corpus to return
   * @return list of files constructed from the names in the corpusFile
   * @throws IOException
   */
  public static CorpusFile loadFiles(String corpusFile, String corpusName)
      throws IOException
  {
    List<File> files = new ArrayList<File>();
    for (String fileName : LineIterable.iterate(corpusFile)) {
      files.add(new File(fileName));
    }
    File workDir = new File(".");
    System.out.println("corpus size: " + files.size());
    CorpusFile corpus = new CorpusFile(new File(workDir, corpusName), files);
    return corpus;
  }

  public static final String CONFIG_ARG = "--config=";
  public static String HELP_ARG = "--help";

}
