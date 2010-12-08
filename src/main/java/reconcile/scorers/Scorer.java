package reconcile.scorers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reconcile.data.Document;



public abstract class Scorer {

public static final int PRECISION = 0, RECALL = 1, F = 2, RESULT_SIZE = 3;
protected static boolean VERBOSE = false;
protected static boolean DEBUG = false;
protected static boolean PRINT_SCORES = false;
public static boolean RUN_MATCHED = true;

public abstract double[] score(boolean printIndividualFiles, Iterable<Document> files);

public abstract double[] score(boolean printIndividualFiles, Iterable<Document> files, String clustName);

public static double f1(double precision, double recall)
{
  if (precision == recall && precision == 0) return 0;
  return (2 * precision * recall) / (precision + recall);
}

public static double[][] newRawScoreArray()
{
  double[][] result = new double[2][RESULT_SIZE];
  Arrays.fill(result[0], 0.0);
  Arrays.fill(result[1], 0.0);
  return result;
}

public static <K, V> TreeMap<K, V> intersect(TreeMap<K, V> c1, TreeMap<K, V> c2)
{
  TreeMap<K, V> intersection = new TreeMap<K, V>();
  Iterator<K> it = c1.keySet().iterator();
  while (it.hasNext()) {
    K np = it.next();
    if (c2.containsKey(np)) {
      intersection.put(np, c2.get(np));
    }
  }

  return intersection;
}

public static boolean same(LeanDocument doc1, TreeMap<Integer, Integer> c1, TreeMap<Integer, Integer> c2)
{
  if (c1.size() != c2.size()) return false;
  Iterator<Integer> i1 = c1.keySet().iterator();
  while (i1.hasNext()) {
    Integer cur = i1.next();
    Integer match = doc1.getMatch(cur);
    if (match == null) return false;
    if (!c2.containsKey(match)) return false;
  }
  return true;
}

// Some utility functions used by the scorers
public static int numIntersect(LeanDocument d1, TreeMap<Integer, Integer> c1, LeanDocument d2,
    TreeMap<Integer, Integer> c2)
{
  int res = 0;
  for (Integer np : c1.keySet()) {
    Integer match = d1.getMatch(np);
    if (match != null && c2.containsKey(match)) {
      res++;
    }
  }

  return res;
}

public static int numIntersect(LeanDocument d1, TreeMap<Integer, Integer> c, LeanDocument d)
{
  // return the number of chains in d that intersect with c
  HashSet<Integer> intersects = new HashSet<Integer>();
  Iterator<Integer> chIter = c.keySet().iterator();
  int unMatched = 0;
  while (chIter.hasNext()) {
    Integer current = chIter.next();
    Integer match = d1.getMatch(current);
    if (match != null) {
      Integer cluster = d.getClusterNum(match);
      intersects.add(cluster);
    }
    else {
      unMatched++;
    }
  }
  // System.out.println("inter: "+intersects.size());
  return intersects.size() + unMatched;
}

protected static void printArray(int[][] ar)
{
  for (int[] ar1 : ar) {
    for (int el : ar1) {
      System.out.print(el + "\t");
    }
    System.out.println();
  }
}

protected static void printArray(int[] ar)
{
  for (int el : ar) {
    System.out.print(el + "\t");
  }
  System.out.println();
}

protected static void printArray(double[][] ar)
{
  for (double[] ar1 : ar) {
    for (double el : ar1) {
      System.out.print(el + "\t");
    }
    System.out.println();
  }
}

protected static void printArray(TreeMap<Integer, Integer>[] clusters)
{
  int count = 0;
  for (TreeMap<Integer, Integer> cl : clusters) {
    System.out.print(count++ + "[");
    for (Integer el : cl.keySet()) {
      System.out.print(el + " ");
    }
    System.out.print("]");
  }
  System.out.println();
}

public static void printScore(String scoreName, double[] score)
{
  Formatter format = new Formatter(System.out);
  format.format("%-15s %.2f|%.2f=%.2f %n", scoreName, score[PRECISION] * 100, score[RECALL] * 100, score[F] * 100);
}

public static void printFileScore(String scoreName, double[] score, PrintWriter pw)
{
  Formatter format = new Formatter(pw);
  format.format("%-15s %.2f|%.2f=%.2f %n", scoreName, score[PRECISION] * 100, score[RECALL] * 100, score[F] * 100);
}

public static void printScoreSingle(String scoreName, double[] score)
{
  Formatter format = new Formatter(System.out);
  format.format("%-15s               %.2f %n", scoreName, score[0] * 100);
}

public static void printFileScoreSingle(String scoreName, double[] score, PrintWriter pw)
{
  Formatter format = new Formatter(pw);
  format.format("%-15s               %.2f %n", scoreName, score[0] * 100);
}

public static void setPrintIndividualScores(boolean flag)
{
  PRINT_SCORES = flag;
}

public static LeanDocument getDocumentMatched(LeanDocument doc)
{
  LeanDocument result = new LeanDocument();
  Iterator<Integer> nps = doc.npIterator();
  while (nps.hasNext()) {
    Integer id = nps.next();
    int clId = doc.getClusterNum(id);
    boolean match = doc.isMatched(id);
    if (match) {
      result.add(id, clId);
    }
  }
  return result;
}

public static LeanDocument getDocumentMatched(LeanDocument doc, LeanDocument key)
{
  LeanDocument result = new LeanDocument();
  Iterator<Integer> nps = doc.npIterator();
  while (nps.hasNext()) {
    Integer id = nps.next();
    int clId = doc.getClusterNum(id);
    boolean match = key.isMatched(id);
    if (match) {
      result.add(id, clId);
    }
  }
  return result;
}

public static LeanDocument readResponse(String filename)
{

  LeanDocument result = new LeanDocument();
  Pattern p = Pattern.compile("(\\d+)\\s+(\\d+)\\s*");

  try {
    BufferedReader bf = new BufferedReader(new FileReader(filename));
    String line;
    while ((line = bf.readLine()) != null) {
      Matcher m = p.matcher(line);
      if (m.matches()) {
        int id = Integer.parseInt(m.group(1));
        int clId = Integer.parseInt(m.group(2));
        if (!result.contains(id)) {
          // System.err.println("Add "+curId1+"-"+find(curId1,ptrs));
          result.add(id, clId);
        }

      }
    }
    bf.close();
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
  return result;
}

public static LeanDocument readResponseMatched(String filename, LeanDocument key)
{

  LeanDocument result = new LeanDocument();
  Pattern p = Pattern.compile("(\\d+)\\s+(\\d+)\\s*");

  try {
    BufferedReader bf = new BufferedReader(new FileReader(filename));
    String line;
    while ((line = bf.readLine()) != null) {
      Matcher m = p.matcher(line);
      if (m.matches()) {
        int id = Integer.parseInt(m.group(1));
        int clId = Integer.parseInt(m.group(2));
        if (key.isMatched(id)) if (!result.contains(id)) {
          // System.err.println("Add "+curId1+"-"+find(curId1,ptrs));
          result.add(id, clId);
        }

      }
    }
    bf.close();
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
  return result;
}

public String getName()
{
  return this.getClass().getSimpleName();
}

}
