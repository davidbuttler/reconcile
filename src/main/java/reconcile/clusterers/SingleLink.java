package reconcile.clusterers;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;

import reconcile.data.AnnotationSet;
import reconcile.general.Constants;
import reconcile.general.UnionFind;


public class SingleLink
    extends ThresholdClusterer {


/**
 * Does clustering using a single link, threshold approach.
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
  if (ces.size() == 1) {
    ces.getFirst().setAttribute(Constants.CLUSTER_ID, "1");
    return ces;
  }
  if (ces.size() < 1) return ces;
  try {
    /** load in the edges file and construct an internal data structure **/
    edges = new HashMap<Integer, HashMap<Integer, Double>>();
    HashSet<Integer> npIDs = new HashSet<Integer>();

    int maxNpID = readClusterFile(in, npIDs, edges);

    if (maxNpID > Integer.MIN_VALUE) {
      /** perform the clustering by checking all edges against the threshold **/

      UnionFind uf = new UnionFind(maxNpID);

      for (int i : npIDs) {
        for (int j : npIDs) {
          if (hasEdge(i, j)) {
            if (getWeight(i, j) > threshold) {
              uf.merge(i, j);
            }
          }
        }
      }

      /** return resulting clusters to file **/
      // System.out.println(ces);
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
