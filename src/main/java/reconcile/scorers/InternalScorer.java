package reconcile.scorers;

import java.util.List;

import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;
import reconcile.scorers.Matcher.MatchStyleEnum;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;


public abstract class InternalScorer
    extends Scorer {

// protected boolean outputDocScores = false;

public abstract double[] score(Iterable<DocumentPair> docs, boolean printIndividualFiles);

public abstract double[] score(DocumentPair doc, boolean printIndividualScores);

public abstract double[][] scoreRaw(DocumentPair doc, boolean printIndividualFiles);

@Override
public double[] score(boolean printIndividualFiles, Iterable<Document> files)
{
  return score(printIndividualFiles, files, Constants.CLUSTER_FILE_NAME);
}

@Override
public double[] score(boolean printIndividualFiles, Iterable<Document> files, String clusterName)
{

  setPrintIndividualScores(printIndividualFiles);
  try {
    List<DocumentPair> docs = Lists.newArrayList();// DocumentPair[files.length];

    for (Document doc : files) {
      AnnotationSet key = doc.getAnnotationSet(Constants.GS_NP);
      key.setName("nps");
      // String responseName = FileStructure.getPath(file, fs.getClusterSubdir(), clusterName);
      AnnotationSet response = doc.getAnnotationSet(clusterName);
      if (doc.getCannonicalAnnotationSetName(Constants.GS_NP).equals(doc.getCannonicalAnnotationSetName(Constants.NP))) {
        // Case 1: key and response are the same set of CEs
        Matcher.exactMatchAnnotationSets(key, response);
      }
      else {
        // Need to read in all annotations and the text since some of them are used for matching

        MatchStyleEnum matchStyle;
        // Match automatic to gs nps
        if (Utils.getConfig().getDataset().toLowerCase().startsWith("ace")) {
          matchStyle = MatchStyleEnum.ACE;
        }
        else {
          matchStyle = MatchStyleEnum.MUC;
        }
        // System.out.println(key);
        Matcher.matchAnnotationSets(key, response, matchStyle, doc, false);
      }

      docs.add(DocumentPair.makeFromMatchedAnnots(key, response));
    }

    return score(docs, printIndividualFiles);
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

public double[] scoreInternal(DocumentPair doc, boolean printIndividualFiles)
{
  double[][] sc = scoreRaw(doc, printIndividualFiles);
  double[] result = new double[RESULT_SIZE];
  // System.out.println(Arrays.toString(sc[0]));
  // System.out.println(Arrays.toString(sc[1]));
  for (int i = 0; i < RESULT_SIZE; i++) {
    if (i != F) {
      result[i] = sc[0][i] / sc[1][i];
    }
    else {
      result[i] = sc[0][i];
    }
  }
  return result;
}

public double[] microAverage(Iterable<DocumentPair> docs, boolean printIndividualFiles)
{
  double totalPrecision = 0, totalRecall = 0;
  double totalDenumPrecsion = 0, totalDenumRecall = 0;
  int i = 0;
  for (DocumentPair doc : docs) {
    // System.out.println("Document "+docs[i].getFilename());
    double[][] score = scoreRaw(doc, printIndividualFiles);
    totalPrecision += score[0][PRECISION];
    totalRecall += score[0][RECALL];
    totalDenumPrecsion += score[1][PRECISION];
    totalDenumRecall += score[1][RECALL];
    if (printIndividualFiles) {
      double[] docScore = new double[RESULT_SIZE];
      double pNum = score[0][PRECISION], pDenum = score[1][PRECISION];
      double rNum = score[0][RECALL], rDenum = score[1][RECALL];
      docScore[PRECISION] = pNum / pDenum;
      docScore[RECALL] = rNum / rDenum;
      docScore[F] = f1(docScore[PRECISION], docScore[RECALL]);
      System.out.print("Document\t" + i + " " + (int) rNum + "/" + (int) rDenum + "\t" + (int) pNum + "/"
          + (int) pDenum);
      printScore("", docScore);
    }
    i++;
  }
  // System.out.println("Precision: "+(int)totalPrecision+"/"+(int)totalDenumPrecsion+" recall "+(int)totalRecall+"/"+(int)totalDenumRecall);
  double precision = totalPrecision / totalDenumPrecsion;
  double recall = totalRecall / totalDenumRecall;
  double[] result = new double[RESULT_SIZE];
  result[PRECISION] = precision;
  result[RECALL] = recall;
  result[F] = f1(precision, recall);

  return result;
}

public double[] macroAverage(Iterable<DocumentPair> docs, boolean printIndividualFiles)
{
  double totalPrecision = 0, totalRecall = 0, totalF = 0;
  int i = 0;
  int length = Iterables.size(docs);
  for (DocumentPair doc : docs) {
    double[] score = scoreInternal(doc, printIndividualFiles);
    if (printIndividualFiles) {
      System.out.println("Document " + i++);
      printScore(getClass().getSimpleName(), score);
    }
    totalPrecision += score[PRECISION];
    totalRecall += score[RECALL];
    totalF += score[F];
  }
  double precision = totalPrecision / length;
  double recall = totalRecall / length;
  double[] result = new double[RESULT_SIZE];
  result[PRECISION] = precision;
  result[RECALL] = recall;
  result[F] = totalF / length;

  return result;
}

/*
 * Average a single-number score (i.e., no precision and recall)
 */
public double[] macroAverageSingleScore(Iterable<DocumentPair> docs, boolean printIndividualFiles)
{
  double total = 0;
  int length = Iterables.size(docs);
  int i = 0;
  for (DocumentPair doc : docs) {
    double[][] score = scoreRaw(doc, printIndividualFiles);
    total += score[0][0];
    if (PRINT_SCORES) {
      System.out.println("Document " + i++);
      printScoreSingle(getClass().getSimpleName(), score[0]);
    }
  }
  double[] result = new double[RESULT_SIZE];
  result[0] = total / length;

  result[F] = result[0];
  return result;
}

}
