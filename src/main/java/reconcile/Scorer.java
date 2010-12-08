package reconcile;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import reconcile.data.AnnotationReaderBytespan;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;
import reconcile.scorers.DocumentPair;
import reconcile.scorers.InternalScorer;
import reconcile.scorers.Matcher;
import reconcile.scorers.Matcher.MatchStyleEnum;

import com.google.common.collect.Lists;


public class Scorer {

// private static boolean ace = false;

/**
 * @param args
 */
public static void score(Iterable<Document> files)
{
  score(true, files, null);
}

/**
 * @param args
 */
public static void score(boolean printIndividualScores, Iterable<Document> files)
{
  score(printIndividualScores, files, null);
}

public static void score(Iterable<Document> files, PrintWriter pw)
{
  score(true, files, pw);
}

public static void score(boolean printIndividualScores, Iterable<Document> files, PrintWriter pw)
{
  String[] scNames = Utils.getConfig().getScorers();
  score(printIndividualScores, files, pw, scNames);
}

public static void score(Iterable<Document> files, PrintWriter pw, String scName)
{
  String[] scNames = new String[] { scName };
  score(true, files, pw, scNames);
}
public static void score(boolean printIndividualScores, Iterable<Document> files, PrintWriter pw, String scName)
{
  String[] scNames = new String[] { scName };
  score(printIndividualScores, files, pw, scNames);
}

/**
 * @param args
 */
public static void score(boolean printIndividualScores, Iterable<Document> files, PrintWriter pw, String[] scNames)
{
  // Initialize the scorers
  ArrayList<reconcile.scorers.Scorer> scorers = intitializeScorers(scNames);
  SystemConfig cfg = Utils.getConfig();
  try {
    List<DocumentPair> docs = Lists.newArrayList(); // DocumentPair[files.length];

    for (Document doc : files) {
      AnnotationSet keyAnnots = doc.getAnnotationSet(Constants.GS_NP);

      File responseName = doc.getClusterFile();
      AnnotationSet responseAnnots = (new AnnotationReaderBytespan()).read(new FileInputStream(responseName),
          "resp_ces");
      // We have to match key and response CEs.
      if (cfg.getAnnotationSetName(Constants.GS_NP).equals(cfg.getAnnotationSetName(Constants.NP))) {
        // Case 1: key and response are the same set of CEs
        Matcher.exactMatchAnnotationSets(keyAnnots, responseAnnots);
      }
      else {
        // Need to read in all annotations and the text since some of them are used for matching
        MatchStyleEnum matchStyle;
        // Match automatic to gs nps
        if (Utils.getConfig().getDataset().toLowerCase().startsWith("ace")) {
          matchStyle = MatchStyleEnum.ACE;
        }
        else if (Utils.getConfig().getDataset().toLowerCase().startsWith("uw")) {
          matchStyle = MatchStyleEnum.UW;
        }
        else {
          matchStyle = MatchStyleEnum.MUC;
        }
        Matcher.matchAnnotationSets(keyAnnots, responseAnnots, matchStyle, doc, false);
      }
      docs.add(DocumentPair.makeFromMatchedAnnots(keyAnnots, responseAnnots));
    }

    for (reconcile.scorers.Scorer sc : scorers) {
      double[] score;
      if (sc instanceof InternalScorer) {
        score = ((InternalScorer) sc).score(docs, printIndividualScores);
      }
      else {
        score = sc.score(printIndividualScores, files);
      }
      reconcile.scorers.Scorer.printScore(sc.getName(), score);
      if (pw != null) {
        reconcile.scorers.Scorer.printFileScore(sc.getName(), score, pw);
      }
    }
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

private static ArrayList<reconcile.scorers.Scorer> intitializeScorers(String[] ElNames)
{
  ArrayList<reconcile.scorers.Scorer> result = new ArrayList<reconcile.scorers.Scorer>();
  for (String el : ElNames) {
    result.add(Constructor.createScorer(el));
  }
  return result;
}

}
