package reconcile.scorers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.features.properties.Property;
import reconcile.general.Constants;


public class DocumentPair {

private LeanDocument key;
private LeanDocument response;
private String filename;

public String getFilename()
{
  return filename;
}

public void setFilename(String filename)
{
  this.filename = filename;
}

public DocumentPair(LeanDocument key, LeanDocument response, String filename) {
  this.key = key;
  key.setKey(true);
  this.response = response;
  response.setKey(false);
  this.filename = filename;
}

public DocumentPair(LeanDocument key, LeanDocument response, boolean setLiteralMatch) {
  initialize(key, response, setLiteralMatch);
}

public DocumentPair(LeanDocument key, LeanDocument response) {
  initialize(key, response, false);
}

public void initialize(LeanDocument key, LeanDocument response, boolean setLiteralMatch)
{
  this.key = key;
  key.setKey(true);
  this.response = response;
  response.setKey(false);
  this.filename = null;
  if (setLiteralMatch) {
    Iterator<Integer> keyIterator = key.npIterator();
    while (keyIterator.hasNext()) {
      Integer i = keyIterator.next();
      if (response.contains(i)) {
        key.setMatch(i, i);
        response.setMatch(i, i);
      }
    }
  }
}

public LeanDocument getKey()
{
  return key;
}

public void setKey(LeanDocument key)
{
  this.key = key;
}

public LeanDocument getResponse()
{
  return response;
}

public void setResponse(LeanDocument response)
{
  this.response = response;
}

public static DocumentPair makeFromMatchedAnnots(AnnotationSet keyAS, AnnotationSet responseAS)
{
  LeanDocument k = readDocument(keyAS);
  LeanDocument r = readDocument(responseAS);
  return new DocumentPair(k, r);
}

public static DocumentPair makeFromClustering(AnnotationSet keyAS, String filename)
{
  LeanDocument r = readDocument(filename);
  LeanDocument k = readDocument(keyAS);

  Iterator<Integer> npIter = k.npIterator();
  while (npIter.hasNext()) {
    Integer np = npIter.next();
    if (k.isMatched(np)) {
      Integer match = k.getMatch(np);
      r.setMatch(match, np);
    }
  }
  return new DocumentPair(k, r, filename);
}
public static DocumentPair makeFromClustering(AnnotationSet keyAS, InputStream file)
{
  LeanDocument r = readDocument(file);
  LeanDocument k = readDocument(keyAS);

  Iterator<Integer> npIter = k.npIterator();
  while (npIter.hasNext()) {
    Integer np = npIter.next();
    if (k.isMatched(np)) {
      Integer match = k.getMatch(np);
      r.setMatch(match, np);
    }
  }
  return new DocumentPair(k, r);
}

public Integer getMatch(Integer key, LeanDocument fromDoc, LeanDocument intoDoc)
{
  if (fromDoc.equals(intoDoc)) return key;
  return fromDoc.getMatch(key);
}

public static LeanDocument readDocument(String filename)
{
  try {
    return readDocument(new FileInputStream(filename));
  }
  catch (FileNotFoundException e) {
    e.printStackTrace();
    throw new RuntimeException();
  }
}

public static LeanDocument readDocument(InputStream filename)
{

  LeanDocument result = new LeanDocument();
  Pattern p = Pattern.compile("(\\d+)\\s+(\\d+)\\s*");

  try {
    BufferedReader bf = new BufferedReader(new InputStreamReader(filename));
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

public static LeanDocument readDocument(AnnotationSet processedNPs)
{
  return readDocument(processedNPs, false);
}

public static LeanDocument readDocument(AnnotationSet processedNPs, boolean addOPTs)
{
  //System.out.println(processedNPs);
  LeanDocument result = new LeanDocument();
  Iterator<Annotation> nps = processedNPs.iterator();
  while (nps.hasNext()) {
    Annotation np = nps.next();
    int id = Integer.parseInt(np.getAttribute(Constants.CE_ID));
    int clId = Integer.parseInt(np.getAttribute(Constants.CLUSTER_ID));
    if (!result.contains(id)) {
      String status = np.getAttribute("STATUS");
      Integer match = (Integer) np.getProperty(Property.MATCHED_CE);
      if (addOPTs || status == null || !status.equals("OPT")) {
        result.add(id, clId);
        if (match != null && match >= 0) {
          result.setMatch(id, match);
        }
      }
      else {
        // System.out.println("OPT: "+np);
        if (match != null && match >= 0) {
          result.add(id, clId);
          result.setMatch(id, match);
        }
      }
    }
  }
  // System.out.println(result);
  return result;
}

}
