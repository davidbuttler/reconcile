/*
 * @author ves
 */

package reconcile.scorers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import reconcile.assignment.AssignmentProblem;
import reconcile.assignment.HungarianAlgorithm;

import com.google.common.collect.Lists;


// import shotgun.Predictions;
// import shotgun.metrics.MetricBundle;
// import shotgun.metrics.PerfCache;

/*
 * Metric for measuring noun-phrase coreference resolution.
 */
public class CEAFScore
    extends InternalScorer {

/**
 * Calculates CEAF score
 * 
 * @param response
 *          The coreference chains in the response. Should be organized by document such that response contains a
 *          mapping from document ID (as a java.lang.Long) to a {@link LeanDocument}.
 * @param key
 *          The gold standard for the scoring. Use the same organization as response.
 * @return Krippendorff's alpha for the response.
 */
public static double scoreHelper(LeanDocument key, LeanDocument response)
{
  if (DEBUG) {
    System.out.println("Scoring document " + key.getID());
  }
  // Get the set of unique clusters
  // System.err.println(key.numChains()+" -- "+response.numChains());
  int size = key.numChains();
  size = size >= response.numChains() ? size : response.numChains();
  if (size == 0) return 0;
  double[][] scores = new double[size][size];
  double max = maxDistance(key.numNounPhrases());
  for (double[] score : scores) {
    Arrays.fill(score, max);
  }
  Iterator<TreeMap<Integer, Integer>> resChains = response.chainIterator();
  int i = 0, j = 0;
  while (resChains.hasNext()) {
    TreeMap<Integer, Integer> resChain = resChains.next();
    Iterator<TreeMap<Integer, Integer>> keyChains = key.chainIterator();
    j = 0;
    while (keyChains.hasNext()) {
      TreeMap<Integer, Integer> keyChain = keyChains.next();
      scores[j][i] = max - distance(key, keyChain, response, resChain);
      j++;
    }
    i++;
  }

  if (DEBUG) {
    System.out.println("Scores");
    printArray(scores);
  }
  AssignmentProblem ap = new AssignmentProblem(scores);
  int[][] solution = ap.solve(new HungarianAlgorithm());
  double cost = 0;
  for (i = 0; i < solution.length; i++) {
    if (solution[i][0] >= 0) {
      cost += max - scores[solution[i][0]][i];
    }
  }
  if (Double.isNaN(cost)) throw new RuntimeException(key.toString());
  return cost;
}

@Override
public double[][] scoreRaw(DocumentPair doc, boolean printIndividualFiles)
{
  LeanDocument key = doc.getKey(), response = doc.getResponse();
  double[][] result = newRawScoreArray();
  double score = scoreHelper(key, response);
  double keyScore = scoreHelper(key, key);
  double resScore = scoreHelper(response, response);
  result[0][PRECISION] = score;
  result[0][RECALL] = score;
  result[1][PRECISION] = resScore;
  result[1][RECALL] = keyScore;
  // System.out.println(score+"/("+keyScore+"+"+resScore+")");
  result[0][F] = 2 * score / (keyScore + resScore);

  return result;
}

private static double distance(LeanDocument key, TreeMap<Integer, Integer> keyChain, LeanDocument response,
    TreeMap<Integer, Integer> resChain)
{
  int numIntersect;
  if (!key.equals(response)) {
    numIntersect = numIntersect(key, keyChain, response, resChain);
  }
  else {
    numIntersect = keyChain.size();
  }
  return 2 * numIntersect / (double) (keyChain.size() + resChain.size());
  // return numIntersect;
}

private static double maxDistance(int numItems)
{
  return 1;
  // return numItems;
}

@Override
public double[] score(Iterable<DocumentPair> docs, boolean printIndividualFiles)
{
  return (new CEAFScore()).macroAverage(docs, printIndividualFiles);
}

@Override
public double[] score(DocumentPair doc, boolean printIndividualFiles)
{
  List<DocumentPair> docs = Lists.newArrayList(doc);
  return score(docs, printIndividualFiles);
}

}
