package reconcile.clusterers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reconcile.SystemConfig;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.UnionFind;
import reconcile.general.Utils;
import reconcile.scorers.DocumentPair;
import reconcile.scorers.LeanDocument;

public abstract class Clusterer {

public static Pattern p = Pattern.compile("\\d*,(\\d*),(\\d*)\\s+(-?\\d*\\.?\\d*)\\s*");
public static AnnotationSet joinClusteringIntoAnnotationSet(AnnotationSet ces, UnionFind clust)
{
  for (Annotation r : ces) {
    int key = Integer.parseInt(r.getAttribute(Constants.CE_ID));
    r.setAttribute(Constants.CLUSTER_ID, Integer.toString(clust.find(key)));
  }
  return ces;
}

public static void printClusteringAsAnnotationSet(Document doc, InputStream clust, String outputAnnotationSetName)
{
  AnnotationSet responseAnnots = doc.getAnnotationSet(Constants.PROPERTIES_FILE_NAME);
  responseAnnots.setName("auto_nps");
  LeanDocument response = DocumentPair.readDocument(clust);
  AnnotationSet outAnnots = new AnnotationSet(outputAnnotationSetName);
  for (Annotation r : responseAnnots) {
    Map<String, String> features = new TreeMap<String, String>();
    int key = Integer.parseInt(r.getAttribute(Constants.CE_ID));
    features.put(Constants.CE_ID, Integer.toString(key));
    features.put(Constants.CLUSTER_ID, response.getClusterNum(key).toString());

    outAnnots.add(r.getId(), r.getStartOffset(), r.getEndOffset(), r.getType(), features);
  }
  doc.writeAnnotationSet(outAnnots);
}

/**
 * Read a cluster file into the give data structures
 * 
 * @param in
 *          the file to read
 * @param npIDs
 *          an empty set to put the np ids into
 * @param edges
 *          an empty map to put the edges into
 * @return the largest np id
 * @throws FileNotFoundException
 * @throws IOException
 */
public static int readClusterFile(Reader in, HashSet<Integer> npIDs, Map<Integer, HashMap<Integer, Double>> edges)
    throws FileNotFoundException, IOException
{
  int maxNpID = Integer.MIN_VALUE;


  BufferedReader bf = new BufferedReader(in);
  String line;
  while ((line = bf.readLine()) != null) {
    Matcher m = p.matcher(line);
    if (m.matches()) {
      int np1 = Integer.parseInt(m.group(1));
      int np2 = Integer.parseInt(m.group(2));
      double weight = Double.parseDouble(m.group(3));

      setWeight(np1, np2, weight, edges);
      npIDs.add(np1);
      npIDs.add(np2);

      if (np1 > maxNpID) {
        maxNpID = np1;
      }
      if (np2 > maxNpID) {
        maxNpID = np2;
      }
    }
  }

  bf.close();
  return maxNpID;
}

/**
 * Sets the weight of the edge between nodes i and j
 */
public static void setWeight(int i, int j, double d, Map<Integer, HashMap<Integer, Double>> edges)
{
  HashMap<Integer, Double> step1 = edges.get(i);
  if (null == step1) {
    step1 = new HashMap<Integer, Double>();
    edges.put(i, step1);
  }
  step1.put(j, d);
}

protected HashMap<Integer, HashMap<Integer, Double>> edges;

public abstract AnnotationSet cluster(AnnotationSet ces, Reader in, String[] options);

/**
 * @param d
 */
public AnnotationSet cluster(Document d)
{
  SystemConfig cfg = Utils.getConfig();
  String[] clustOptions = cfg.getStringArray("ClustOptions." + this.getClass().getName());
  return cluster(d, clustOptions);
}

/**
 * Cluster the nps in a document give a prediction file and options for the given clusterer
 * 
 * @param doc
 * @param options
 * @return
 */
public AnnotationSet cluster(Document doc, String[] options)
{
  Reader in = doc.getPredictionReader();
  AnnotationSet ces = doc.getAnnotationSet(Constants.NP);
  AnnotationSet result = cluster(ces, in, options);
  result.setName(Constants.RESPONSE_NPS);
  return result;
}


public String getInfo(String[] options)
{
  String result = "Clusterer " + getClass().getSimpleName() + ".";
  if (this instanceof ThresholdClusterer) {
    result += " Threshold = " + ((ThresholdClusterer) this).getThreshold() + ".";
  }
  result += " Options " + Arrays.toString(options);
  return result;
}

/**
 * Returns the weight of the edge between nodes i and j. If no edge exists, returns null.
 */
protected Double getWeight(int i, int j)
{
  HashMap<Integer, Double> step1 = edges.get(i);
  if (step1 != null) return step1.get(j);
  return null;
}

protected boolean hasEdge(int i, int j)
{
  HashMap<Integer, Double> step1 = edges.get(i);
  return (step1 != null && step1.get(j) != null);
}

/**
 * Sets the weight of the edge between nodes i and j
 */
protected void setWeight(int i, int j, double d)
{
  HashMap<Integer, Double> step1 = edges.get(i);
  if (null == step1) {
    step1 = new HashMap<Integer, Double>();
    edges.put(i, step1);
  }
  step1.put(j, d);
}

}
