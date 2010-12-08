/*
 * @author ves
 */

package reconcile.scorers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

/*
 * BCubed Metric for measuring noun-phrase coreference resolution.
 */
public class KrippAlpha
    extends InternalScorer {

// Constants used in alpha computation
public static final double SAME = 0.0;
public static final double DISJOINT = 1.0;
public static final double SUBSUME = .25;
public static final double INTERSECT = .25;

/**
 * Calculates Krippendorff's alpha
 * 
 * @param response
 *          The coreference chains in the response. Should be organized by document such that response contains a
 *          mapping from document ID (as a java.lang.Long) to a {@link LeanDocument}.
 * @param key
 *          The gold standard for the scoring. Use the same organization as response.
 * @return Krippendorff's alpha for the response.
 */
@SuppressWarnings("unchecked")
@Override
public double[] score(DocumentPair doc, boolean printIndividualFiles)
{
  LeanDocument key = doc.getKey(), response = doc.getResponse();
  if (DEBUG) {
    System.err.println("Scoring document " + key.getID());
  }
  // Get the set of unique clusters
  HashMap<TreeMap<Integer, Integer>, Integer> clNumbers = new HashMap<TreeMap<Integer, Integer>, Integer>();
  ArrayList<TreeMap<Integer, Integer>> sets = new ArrayList<TreeMap<Integer, Integer>>();
  Iterator<TreeMap<Integer, Integer>> keyChains = key.chainIterator();
  while (keyChains.hasNext()) {
    TreeMap<Integer, Integer> keyChain = keyChains.next();
    sets.add(keyChain);
    clNumbers.put(keyChain, sets.size() - 1);
  }
  Iterator<TreeMap<Integer, Integer>> resChains = response.chainIterator();
  while (resChains.hasNext()) {
    TreeMap<Integer, Integer> resChain = resChains.next();
    keyChains = key.chainIterator();
    int index = -1;
    while (keyChains.hasNext()) {
      TreeMap<Integer, Integer> keyChain = keyChains.next();
      if (same(response, resChain, keyChain)) {
        index = sets.indexOf(keyChain);
        break;
      }
    }

    if (index < 0) {
      sets.add(resChain);
      clNumbers.put(resChain, sets.size() - 1);
    }
    else {
      clNumbers.put(resChain, index);
    }
  }

  TreeMap<Integer, Integer>[] clusters = new TreeMap[sets.size()];
  clusters = sets.toArray(clusters);

  double[][] scores = new double[clusters.length][clusters.length];
  int[][] num = new int[clusters.length][clusters.length];
  keyChains = key.chainIterator();
  while (keyChains.hasNext()) {
    TreeMap<Integer, Integer> keyChain = keyChains.next();
    Iterator<Integer> itemIter = keyChain.keySet().iterator();
    while (itemIter.hasNext()) {
      Integer item = itemIter.next();
      // System.out.println("Item "+item+" "+key.getElChain(item)+"-"+response.getElChain(item));
      int i = clNumbers.get(key.getElChain(item));
      Integer match = key.getMatch(item);
      if (match != null) {
        int j = clNumbers.get(response.getElChain(match));
        num[i][j]++;
        num[j][i]++;
      }
    }
  }

  double numerator = 0;
  for (int i = 0; i < scores.length; i++) {
    for (int j = i; j < scores.length; j++) {
      scores[i][j] = alphaDistance(key, clusters[i], response, clusters[j]);// Math.pow(alphaDistance(clusters[i],
                                                                            // clusters[j]),2);
      // num[i][j]=numIntersect(clusters[i], clusters[j]);
      // num[j][i]+=num[i][j];
      numerator += scores[i][j] * num[i][j];
    }
  }

  int total = 0;
  int[] sum = new int[clusters.length];
  for (int i = 0; i < scores.length; i++) {
    for (int j = 0; j < scores.length; j++) {
      sum[i] += num[i][j];
      total += num[i][j];
    }
  }
  int denum = 0;
  for (int i = 0; i < scores.length; i++) {
    for (int j = i + 1; j < scores.length; j++) {
      denum += sum[i] * sum[j] * scores[i][j];
    }
  }

  if (DEBUG) {
    System.out.println("Clusters");
    printArray(clusters);
    System.out.println("Numbers");
    printArray(num);
    System.out.println("-----------");
    printArray(sum);
    System.out.println("Scores");
    printArray(scores);
    System.out.println("Numerator " + numerator + " Denum " + denum + "=" + ((numerator) / denum));
  }
  double[] result = new double[1];
  if (numerator == 0 && denum == 0) {
    result[0] = 1;
    return result;
  }
  double score = 1 - ((total - 1) * ((numerator) / denum));
  if (Double.isNaN(score)) throw new RuntimeException("Result is " + score);
  result[0] = score;
  if (printIndividualFiles) {
    printScoreSingle(this.getName(), result);
  }
  return result;
}

private static double alphaDistance(LeanDocument key, TreeMap<Integer, Integer> keyChain, LeanDocument response,
    TreeMap<Integer, Integer> resChain)
{
  int numIntersect = numIntersect(key, keyChain, response, resChain);
  // Several cases
  // 1) the two clusters are the same
  if (keyChain.size() == numIntersect && resChain.size() == numIntersect)
    return SAME;
  else if (keyChain.size() == numIntersect || resChain.size() == numIntersect)
    // 2) one cluster subsumes the other
    return 1 - 2 * numIntersect / (double) (keyChain.size() + resChain.size());
  else if (numIntersect > 1) // 3) intersection
    return 1 - 2 * numIntersect / (double) (keyChain.size() + resChain.size());
  else
    // disjunction
    return DISJOINT;
}

@Override
public double[] score(Iterable<DocumentPair> docs, boolean printIndividualFiles)
{
  return (new KrippAlpha()).macroAverageSingleScore(docs, printIndividualFiles);
}

@Override
public double[][] scoreRaw(DocumentPair doc, boolean printIndividualFiles)
{
  double result[][] = new double[1][1];
  result[0] = score(doc, printIndividualFiles);
  return result;
}

}
