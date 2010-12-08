/*
 * @author Veselin Stoyanov
 * 
 * @version 1.0 History: 2004/09/20 Art Munson created. 2005/10/18 ves changed for different document format
 */

package reconcile.scorers;

// import coref.*;
import gov.llnl.text.util.FileUtils;
import gov.llnl.text.util.LineIterator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import com.google.common.collect.Maps;

/**
 * Abstraction for a text document, for use with coreference scoring.
 */
public class LeanDocument {

// All we need to store for the NPs is the cluster number for each NP
HashMap<Integer, Integer> NPchains;
// The clusters are stored as a hashMap (indexed by clusterNumber) of TreeMaps
HashMap<Integer, TreeMap<Integer, Integer>> clusters;
HashMap<Integer, Integer> matches;
private static long nextId = 0;
private boolean isKey;

private int numChains = 0;

private long id = -1, uniqueId;

public LeanDocument() {
  NPchains = new HashMap<Integer, Integer>();
  clusters = new HashMap<Integer, TreeMap<Integer, Integer>>();
  matches = new HashMap<Integer, Integer>();
  uniqueId = getNextId();
}

public LeanDocument(long id) {
  NPchains = new HashMap<Integer, Integer>();
  clusters = new HashMap<Integer, TreeMap<Integer, Integer>>();
  matches = new HashMap<Integer, Integer>();
  this.id = id;
  uniqueId = getNextId();
}

@Override
public boolean equals(Object o)
{
  if (o != null && o instanceof LeanDocument)
    return ((LeanDocument) o).uniqueId == uniqueId;
  else
    return false;
}

@Override
public int hashCode()
{
  return (int) id;
}

public boolean isMatched(Integer np)
{
  return matches.containsKey(np);
}

public Integer setMatch(Integer np, Integer match)
{
  return matches.put(np, match);
}

public Integer getMatch(Integer np)
{
  return matches.get(np);
}

/**
 * Prints a representation of the coref chain to 'out' in the format: each chain is printed on a single line, and the
 * entries on that line are the space-delimited IDs of the mentions (NPs) in the chain.
 * 
 * If 'printSingletons = false,' only chains that contain at least two mentions will be printed.
 * 
 * - dah Oct 16 07
 */
public void printCorefChains(PrintWriter out, boolean printSingletons)
{
  for (Integer cur : clusters.keySet()) {
    TreeMap<Integer, Integer> cluster = clusters.get(cur);
    if (cluster.keySet().size() > 1 || printSingletons) {
      for (Integer k : cluster.keySet()) {
        out.print(k + " ");
      }
      out.print("\n");
    }
  }
}

@Override
public String toString()
{
  StringBuilder desc = new StringBuilder();// "Doc " + id + ": ";
  for (Integer cur : clusters.keySet()) {
    desc.append("Cl ").append(cur).append(" [ ");
    TreeMap<Integer, Integer> cluster = clusters.get(cur);
    for (Integer item : cluster.keySet()) {
      desc.append(item).append("(").append(getMatch(item)).append(") ");
    }
    desc.append("]");
  }
  return desc.toString();
}

public String toStringNoSing()
{
  StringBuilder desc = new StringBuilder();// "Doc " + id + ": ";
  for (Integer cur : clusters.keySet()) {
    TreeMap<Integer, Integer> cluster = clusters.get(cur);
    if (cluster.size() > 1) {
      desc.append("Cl ").append(cur).append(" [ ");
      for (Integer item : cluster.keySet()) {
        desc.append(item).append("(").append(getMatch(item)).append(") ");
      }
      desc.append("]");
    }
  }
  return desc.toString();
}

public long getID()
{
  return id;
}

public int numChains()
{
  return clusters.size();
}

public int numNounPhrases()
{
  return NPchains.size();
}

/**
 * Reads the next annotated document from the specified directory and builds a document structure containing the
 * coreference chains.
 * 
 * @param directory
 *          Name of the directory containing the document.
 * @return LeanDocument containing coreference chains, or null if no document could be read from the data source.
 * @throws IOException
 *           if an error occurs while reading.
 */
public static LeanDocument readDocumentOld(String directory, int docID)
    throws IOException
{
  // Open the document
  String corefFilename = directory + "/" + docID + "/answer.key.corrected";
  LeanDocument doc = new LeanDocument(docID);
  // This will keep track of the number of chains

  LineIterator fi = new LineIterator(new File(corefFilename));
  String line;
  while (fi.hasNext()) {
    line = fi.next();

    String[] parse = line.split(",");
    int currentID = Integer.parseInt(parse[0]);

    // int start = Integer.parseInt(parse[1]);
    // int end = Integer.parseInt(parse[2]);
    int corefID = Integer.parseInt(parse[3]);

    // The rest of the line belongs to the text of the NP
    StringBuilder npText1 = new StringBuilder(parse[4]);
    for (int i = 5; i < parse.length; i++) {
      npText1.append(",").append(parse[i]);
    }
    String npText = npText1.toString();
    // Make sure that we read the np until the end (across newlines)
    while (!npText.endsWith("<END>#") && !npText.endsWith("<END>*") && fi.hasNext()) {
      // add back the endl
      line = fi.next();
      npText1.append("\n").append(line);
      npText = npText1.toString();
    }
    if (npText.charAt(npText.length() - 1) == '#') {
      // we have a source
      npText = npText.substring(0, npText.length() - 6);

      // Now we have all the info, create a new np
      if (doc.NPchains.containsKey(currentID)) throw new RuntimeException("Id " + currentID + " read twice.");
      doc.NPchains.put(currentID, corefID);
      TreeMap<Integer, Integer> cluster;
      if (corefID < 0 || !doc.clusters.containsKey(corefID)) {
        doc.numChains++;
      }
      if (doc.clusters.containsKey(corefID)) {
        cluster = doc.clusters.get(corefID);
      }
      else {
        cluster = new TreeMap<Integer, Integer>();
        doc.clusters.put(corefID, cluster);
      }
      cluster.put(currentID, currentID);

    }
  }
  // Take care of singleton clusters (marked -1)
  TreeMap<Integer, Integer> singl = doc.clusters.get(-1);
  if (singl != null) {
    int index = doc.numChains;
    for (Integer item : singl.keySet()) {
      TreeMap<Integer, Integer> cl = Maps.newTreeMap();
      cl.put(item, item);
      Integer itemInd = index--;
      doc.clusters.put(itemInd, cl);
      doc.NPchains.put(item, itemInd);
    }
    doc.clusters.remove(-1);
  }
  return doc;
}

/**
 * This function filters all the np's in a document, leaving only those np's that are present in goldSt
 */
public void filterDoc(LeanDocument goldSt)
{
  HashMap<Integer, Integer> newNPchains = new HashMap<Integer, Integer>();
  clusters = new HashMap<Integer, TreeMap<Integer, Integer>>();
  Iterator<Integer> keyIterator = goldSt.NPchains.keySet().iterator();
  while (keyIterator.hasNext()) {
    Integer current = keyIterator.next();
    Integer corefID = NPchains.get(current);
    if (!NPchains.containsKey(current))
      throw new RuntimeException("Gold Standard np " + current + " not found in doc " + id);
    newNPchains.put(current, corefID);
    TreeMap<Integer, Integer> cluster;
    if (clusters.containsKey(corefID)) {
      cluster = clusters.get(corefID);
    }
    else {
      cluster = new TreeMap<Integer, Integer>();
      clusters.put(corefID, cluster);
    }
    cluster.put(current, current);
  }
  NPchains = newNPchains;
}

public Iterator<TreeMap<Integer, Integer>> chainIterator()
{
  return clusters.values().iterator();
}

public Iterator<Integer> npIterator()
{
  return NPchains.keySet().iterator();
}

public Integer[] getNPArray()
{
  return NPchains.keySet().toArray(new Integer[0]);
}

/*
 * Return the cluster number given an NP id
 */
public Integer getClusterNum(Integer key)
{
  return NPchains.get(key);
}

/*
 * Return the chain given a cluster number
 */
public TreeMap<Integer, Integer> getChain(Integer key)
{
  return clusters.get(key);
}

/*
 * Return the chain for a given NP
 */
public TreeMap<Integer, Integer> getElChain(Integer key)
{
  return clusters.get(NPchains.get(key));
}

public boolean sameCluster(Integer el1, Integer el2)
{
  return getClusterNum(el1).equals(getClusterNum(el2));
}

public boolean contains(Integer item)
{
  return NPchains.containsKey(item);
}

public void add(Integer NP, Integer chain)
{
  if (contains(NP)) return;
  TreeMap<Integer, Integer> cluster;
  if (clusters.containsKey(chain)) {
    cluster = clusters.get(chain);
    cluster.put(NP, NP);
  }
  else {
    cluster = new TreeMap<Integer, Integer>();
    cluster.put(NP, NP);
    clusters.put(chain, cluster);
  }
  NPchains.put(NP, chain);
}

/**
 * returns a LeanDocument
 */
public static LeanDocument readDocument(String fn, boolean delete_singletons)
    throws IOException
{
  HashMap<Integer, Integer> singleton_test = new HashMap<Integer, Integer>();
  LeanDocument result = new LeanDocument();
  List<String> list = FileUtils.readFileLines(fn);
  for (String line : list) {
    String tmp[] = line.split(" ");
    int coref_id = Integer.parseInt(tmp[1]);
    if (singleton_test.containsKey(coref_id)) {
      Integer ct = singleton_test.get(coref_id);
      ++ct;
      singleton_test.put(coref_id, ct);
    }
    else {
      singleton_test.put(coref_id, 1);
    }
  }

  for (String line : list) {
    String tmp[] = line.split(" ");
    int coref_id = Integer.parseInt(tmp[1]);
    int np_id = Integer.parseInt(tmp[0]);
    if (delete_singletons) {
      if (singleton_test.get(coref_id) != 1) {
        result.add(np_id, coref_id);
      }
    }
    else {
      result.add(np_id, coref_id);
    }
  }

  return result;
}

public static long getNextId()
{
  return nextId++;
}

/**
 * returns a LeanDocument
 */
public static LeanDocument glb(LeanDocument d1, LeanDocument d2)
{
  LeanDocument result = new LeanDocument();
  Iterator<TreeMap<Integer, Integer>> clusters = d1.chainIterator();
  int nextId = 1;
  HashMap<Integer, HashMap<Integer, Integer>> clusterCorresp = Maps.newHashMap();
  while (clusters.hasNext()) {
    TreeMap<Integer, Integer> cluster = clusters.next();
    for (Integer item : cluster.values()) {
      Integer cl1 = d1.getClusterNum(item);
      Integer cl2 = d2.getClusterNum(item);
      int newCluster = getClusterCorrespondance(cl1, cl2, clusterCorresp, nextId);
      nextId = nextId <= newCluster ? nextId + 1 : nextId;
      result.add(item, newCluster);
    }
  }

  return result;
}

private static int getClusterCorrespondance(Integer cl1, Integer cl2,
    HashMap<Integer, HashMap<Integer, Integer>> clusterCorresp, int nextId)
{
  // System.out.println("Clusters "+cl1+" and "+cl2+" nextId "+nextId);
  HashMap<Integer, Integer> map = clusterCorresp.get(cl1);
  if (map == null) {
    map = new HashMap<Integer, Integer>();
    map.put(cl2, nextId);
    clusterCorresp.put(cl1, map);
    return nextId;
  }
  Integer corresp = map.get(cl2);
  if (corresp == null) {
    map.put(cl2, nextId);
    return nextId;
  }
  return corresp.intValue();
}

public boolean isKey()
{
  return isKey;
}

public void setKey(boolean isKey)
{
  this.isKey = isKey;
}

}
