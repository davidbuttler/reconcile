/*
 * @author ves
 */

package reconcile.scorers;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import com.google.common.collect.Lists;

/*
 * BCubed Metric for measuring noun-phrase coreference resolution.
 */
public class MUCScore
    extends InternalScorer {

public MUCScore() {
  super();
  // outputDocScores = false;
}

/**
 * Calculates the MUC score for the given range of documents. This is a recall score for the response, compared to the
 * key. To calculate precision simply pass the response as the key, and vice versa. For more information see
 * <i>Algorithms for Scoring Coreference Chains </i> by Bagga and Baldwin.
 * 
 * @param key
 *          The gold standard document for the scoring.
 * @param response
 *          The LeanDocument containing the predicted clustering
 * @return The MUC score for the response.
 */
public static int[] mucScore(LeanDocument key, LeanDocument response)
{
  // System.out.println("==========================================================");
  // System.out.println("Key:\n"+key.toStringNoSing()+"\n*************************\nResponse:\n"+response.toStringNoSing());

  Iterator<TreeMap<Integer, Integer>> goldChains = key.chainIterator();
  // double mucRecall = 0.0;
  int mucRecallNom = 0;
  int mucRecallDenom = 0;
  while (goldChains.hasNext()) {
    TreeMap<Integer, Integer> keyChain = goldChains.next();
    if (keyChain.size() > 1) {
      int numInt = numIntersect(key, keyChain, response);

      // int numMatched = getNumMatched(key, keyChain);
      // if(numMatched>0){
      // mucRecallNom += numMatched-numInt;
      mucRecallNom += (keyChain.size() - numInt);
      // mucRecallDenom += numMatched-1;
      mucRecallDenom += keyChain.size() - 1;

      // System.out.println(keyChain+"\n"+(keyChain.size() - numInt)+"/"+(keyChain.size()-1));
      // }
    }
  }
  int[] result = { mucRecallNom, mucRecallDenom };

  return result;
}

@Override
public double[][] scoreRaw(DocumentPair doc, boolean printIndividualFiles)
{
  int[] recall = mucScore(doc.getKey(), doc.getResponse());
  int[] precision = mucScore(doc.getResponse(), doc.getKey());

  double[][] result = newRawScoreArray();
  result[0][PRECISION] = precision[0];
  result[1][PRECISION] = precision[1];
  result[0][RECALL] = recall[0];
  result[1][RECALL] = recall[1];

  return result;
}

@Override
public double[] score(Iterable<DocumentPair> docs, boolean printIndividualFiles)
{
  return (new MUCScore()).microAverage(docs, printIndividualFiles);
}

@Override
public double[] score(DocumentPair doc, boolean printIndividualFiles)
{
  List<DocumentPair> docs = Lists.newArrayList(doc);
  return score(docs, printIndividualFiles);
}

public static int getNumMatched(LeanDocument doc, TreeMap<Integer, Integer> chain)
{
  Iterator<Integer> chIter = chain.keySet().iterator();
  int matched = 0;
  while (chIter.hasNext()) {
    Integer cur = chIter.next();
    if (doc.getMatch(cur) != null) {
      matched++;
    }
  }
  return matched;
}
}
