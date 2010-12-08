/*
 * stripSGML.java nathan; May 23, 2007
 */

package reconcile.featureExtractor;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.UnionFind;


public abstract class SGMLStripper
    extends InternalAnnotator {

private int MAX_ID;
protected Handler handler = new Handler();

public class Handler
    extends DefaultHandler {

@Override
public void startElement(String uri, String name, String qName, Attributes atts)
{
  SGMLStripper.this.startElement(uri, name, qName, atts);
}

@Override
public void endElement(String uri, String name, String qName)
{
  SGMLStripper.this.endElement(uri, name, qName);
}

@Override
public void characters(char ch[], int start, int length)
{
  SGMLStripper.this.characters(ch, start, length);
}
}

/*
 * Call the parent's constructor. 
 */
public SGMLStripper() {
  super();
}

public AnnotationSet labelClusterIDs(AnnotationSet key)
{
  // Form the clusters of coreferent nps by performing transitive closure
  int maxID = -1;

  // First, we find the largest key
  for (Annotation k : key) {
    //System.err.println("Working on "+k +" - "+k.getAttribute("ID"));
    int id = Integer.parseInt(k.getAttribute("ID"));
    maxID = maxID > id ? maxID : id;
  }

  // Create an array of pointers
  UnionFind uf = new UnionFind(maxID);

  // Intialize the next id
  setMaxID(maxID);

  for (Annotation k : key) {
    int id = Integer.parseInt(k.getAttribute("ID"));
    String refString = k.getAttribute("REF");
    int ref = refString == null ? -1 : Integer.parseInt(refString);
    if (ref > -1) {
      uf.merge(id, ref);
      // FeatureUtils.union(id, ref, ptrs);
    }
  }
  AnnotationSet gsNps = new AnnotationSet("gsNPs");
  for (Annotation k : key) {
    k.setAttribute(Constants.CLUSTER_ID, Integer.toString(uf.find(Integer.parseInt(k.getAttribute("ID")))));
    k.setAttribute(Constants.CE_ID, k.getAttribute("ID"));
    gsNps.add(k);
  }

  return gsNps;
}

private void setMaxID(int id)
{
  MAX_ID = id + 2;
}

protected int getNextID()
{
  return MAX_ID++;
}

public abstract void format(BufferedReader br, FileWriter out)
    throws IOException;

@Override
public abstract void run(Document doc, String[] annSetNames);

public abstract void startElement(String uri, String name, String qName, Attributes atts);

public abstract void endElement(String uri, String name, String qName);

public abstract void characters(char ch[], int start, int length);
}
