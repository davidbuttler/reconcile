package reconcile.clusterers;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;

import reconcile.data.AnnotationSet;
import reconcile.general.UnionFind;


public class BestFirst
    extends ThresholdClusterer {

/**
 * Does clustering using a best-first, threshold approach.
 * 
 * @param in
 *          - filename of the "edges" file defining similarities between NPs.
 * @param out
 *          - filename where to write the resulting cluster
 * @param options
 *          - a String[] containing a single string representing the threshold
 */
@Override
public AnnotationSet cluster(AnnotationSet ces, Reader in, String[] options)
{
  try {
    /** load in the edges file and construct an internal data structure **/
    edges = new HashMap<Integer, HashMap<Integer, Double>>();
    HashSet<Integer> npIDs = new HashSet<Integer>();

    int maxNpID = readClusterFile(in, npIDs, edges);

    if (maxNpID > Integer.MIN_VALUE) {
      /** perform the clustering by checking all edges against the threshold **/
      // creates datastructure with capacity maxNpID
      UnionFind uf = new UnionFind(maxNpID);
      double mxWeight;
      int currentHigh = 0;
      double weight = 0.0;

      for (int i = 0; i <= maxNpID; i++) {
        mxWeight = Integer.MIN_VALUE;
        currentHigh = i;

        // find the highest scoring edge of np i
        for (int j = 0; j <= maxNpID; j++) {
          if (hasEdge(i, j)) {
            weight = getWeight(i, j);
            if ((weight > mxWeight) && (weight > threshold)) {
              currentHigh = j;
              mxWeight = getWeight(i, j);
            }
          }
        }

        // actually do the clustering
        if (hasEdge(i, currentHigh)) {
          uf.merge(i, currentHigh);
        }
      }

      /** return resulting clusters to file **/
      return joinClusteringIntoAnnotationSet(ces, uf);
    }
    else
      return null;
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}
}
