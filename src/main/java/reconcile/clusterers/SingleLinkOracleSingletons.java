package reconcile.clusterers;

import java.io.File;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.UnionFind;


public class SingleLinkOracleSingletons
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

// This is just a hack class
@Override
public AnnotationSet cluster(AnnotationSet ces, Reader in, String[] options){
	throw new RuntimeException("Not implemented");
}
public AnnotationSet cluster(Document odoc, String[] options){
  File in = odoc.getPredictionFile();
  AnnotationSet ces = odoc.getAnnotationSet(Constants.NP);
  File dirname = in.getParentFile();
  dirname = in.getParentFile();
  dirname = in.getParentFile();
  Document doc = new Document(dirname);
  // System.out.println("SL "+dirname);
  AnnotationSet key = doc.getAnnotationSet(Constants.GS_OUTPUT_FILE);
  key.setName("nps");
  AnnotationSet response = doc.getAnnotationSet(Constants.PROPERTIES_FILE_NAME);
  response.setName("auto_nps");
  // LeanDocument keyDoc = DocumentPair.readDocument(key);
  // System.out.println(keyDoc);
  HashSet<Integer> singletons = new HashSet<Integer>();
  for (Annotation a : response) {
    int num = Integer.parseInt(a.getAttribute(Constants.CE_ID));
    String matchStr = a.getAttribute(Constants.MATCHED_GS_NP);
    if (matchStr == null) {
      singletons.add(num);
    }
    else {
      int match = Integer.parseInt(a.getAttribute(Constants.MATCHED_GS_NP));
      // System.out.println("Match "+match);
      if (match < 0) {// ||keyDoc.getElChain(match).size()<2){
        singletons.add(num);
      }
    }
  }
  try {
    /** load in the edges file and construct an internal data structure **/
    edges = new HashMap<Integer, HashMap<Integer, Double>>();
    HashSet<Integer> npIDs = new HashSet<Integer>();

    int maxNpID = Clusterer.readClusterFile(odoc.getPredictionReader(), npIDs, edges);

    if (maxNpID > Integer.MIN_VALUE) {
      /** perform the clustering by checking all edges against the threshold **/

      UnionFind uf = new UnionFind(maxNpID);

      for (int i : npIDs) {
        for (int j : npIDs) {
          if (!singletons.contains(i) && !singletons.contains(j) && hasEdge(i, j)) {
            if (getWeight(i, j) > threshold) {
              // System.out.println("Merging ("+i+","+j+") w: "+getWeight(i, j));
              uf.merge(i, j);
            }
          }
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
