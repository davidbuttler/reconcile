/*
 * @author ves
 */

package reconcile.scorers;

import gov.llnl.text.util.LineIterable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reconcile.SystemConfig;
import reconcile.data.Annotation;
import reconcile.data.AnnotationReaderBytespan;
import reconcile.data.AnnotationSet;
import reconcile.data.AnnotationWriterEmbedded;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;
import reconcile.general.Utils;

import com.google.common.collect.Maps;


/*
 * BCubed Metric for measuring noun-phrase coreference resolution.
 */
public class OfficialMUCScore
    extends ExternalScorer {

private static String[] COPY_TAGS = { "DOC", "TXT", "DOCID", "DOCNO" };
private static String[] SLOT_TAGS = { "MIN", "TEXT", "TYPE", "ID", "REF", "STATUS" };
public static final boolean USE_NES = true;

@Override
public String getName()
{
  return "OMUCScore";
}

/**
 * Runs the official MUC scorer and reads in the results. Expects to find the script for the official MUC score in the
 * scripts directory. For more information see <i>Algorithms for Scoring Coreference Chains </i> by Bagga and Baldwin.
 * 
 * @param key
 *          The gold standard document for the scoring.
 * @param response
 *          The LeanDocument containing the predicted clustering
 * @return The MUC score for the response.
 */

@Override
public double[] score(boolean printIndividualFiles, Iterable<Document> files)
{
  return score(printIndividualFiles, files, Constants.CLUSTER_FILE_NAME);
}

@Override
public double[] score(boolean printIndividualFiles, Iterable<Document> files, String clusterName)
{
  boolean includeSingletons = true;
  return produceScore(printIndividualFiles, files, clusterName, includeSingletons);
}

public static double[] produceScore(boolean printIndividualFiles, Iterable<Document> files, String clusterName,boolean includeSingletons)
{
  SystemConfig cfg = Utils.getConfig();
  String muc_scorer_path = cfg.getMUCScorerPath();
  String workDir = Utils.getWorkDirectory();
  // Make key and response files
  PrintWriter outKey, outResp;
  try {
    outKey = new PrintWriter(workDir + Utils.SEPARATOR + "keys");
    outResp = new PrintWriter(workDir + Utils.SEPARATOR + "responses");
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
  for (Document doc : files) {
    // System.out.println("Scoring "+file);
    AnnotationSet key = doc.getAnnotationSet(Constants.ORIG);
    String text = Utils.getTextFromFile(doc.readFile("orig.raw.txt"));

    AnnotationSet newKey = new AnnotationSet("newKey");
    // remove some of the tags
    for (Annotation a : key) {
      if (FeatureUtils.memberArray(a.getType(), COPY_TAGS) || a.getType().equalsIgnoreCase("COREF")) {
        // a.setAttribute("STATUS", "REQ");
        Map<String, String> attrs = a.getFeatures();
        Map<String, String> newAttrs = Maps.newTreeMap();
        for (String k : attrs.keySet()) {
          if (FeatureUtils.memberArray(k, SLOT_TAGS)) {
            newAttrs.put(k, attrs.get(k));
          }
        }
        a.setFeatures(newAttrs);
        newKey.add(a);
      }
    }
    (new AnnotationWriterEmbedded()).write(newKey, outKey, text);
    outKey.println();

    //AnnotationSet responseAS = doc.getAnnotationSet(clusterName);
    AnnotationSet responseAS = new AnnotationReaderBytespan().read(doc.readClusterFile(), clusterName);
    LeanDocument response = DocumentPair.readDocument(responseAS);


    AnnotationSet nps = translateNPs(doc.getAnnotationSet(Constants.PROPERTIES_FILE_NAME), response, includeSingletons);
    // AnnotationSet nps = translateNPs((new AnnotationReaderBytespan()).read(fs.getAnnSetFilenameByKey(file,
    // Constants.NP), "nps"),response);

    HashMap<Integer, String> representatives = new HashMap<Integer, String>();

    for (Annotation anot : nps) {
      int num = Integer.parseInt(anot.getAttribute("ID"));
      int cluster = response.getClusterNum(num);
      if (representatives.containsKey(cluster)) {
        anot.setAttribute("REF", representatives.get(cluster));
      }
      else {
        representatives.put(cluster, Integer.toString(num));
      }
    }
    // Add the non-coref document annotations
    for (Annotation a : key) {
      if (FeatureUtils.memberArray(a.getType(), COPY_TAGS)) {

        nps.add(a);
      }
    }
    // Need to do some bookkeeping on the key
    // AnnotationSet doc = nps.get("DOC");
    // if(doc==null||doc.size()<1)
    // nps.add(0, text.length(), "DOC");

    try {
      (new AnnotationWriterEmbedded()).write(nps, outResp, text);
      outResp.println();
    }
    catch (Exception e) {
      // System.out.println(nps);
      System.out.println("File was: " + doc.getAbsolutePath());
      throw new RuntimeException(e);
    }
  }
  outKey.flush();
  outKey.close();
  outResp.flush();
  outResp.close();
  File runDir = new File(workDir);
  String command = muc_scorer_path;
  if (cfg.getDataset().contains("muc7")) {
    command += " " + Utils.getScriptDirectory() + Utils.SEPARATOR + "config_muc7";
  }
  else if (cfg.getDataset().toLowerCase().startsWith("ace")) {
    command += " " + Utils.getScriptDirectory() + Utils.SEPARATOR + "config_" + cfg.getDataset().toLowerCase();
  }
  else {
    command += " " + Utils.getScriptDirectory() + Utils.SEPARATOR + "config";
    // System.out.println("Running: "+command);
  }

  try {
    Utils.runExternal(command, runDir, false);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
  catch (InterruptedException ie) {
    throw new RuntimeException(ie);
  }
  // System.out.println("done");
  String outFile = workDir + "/scores";
  Pattern p = Pattern
      .compile("TOTALS\\:\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\/\\s+\\d+\\s+(\\d+\\.\\d)\\%\\s+\\d+\\s+\\/\\s+\\d+\\s+(\\d+.\\d)\\%\\s+(\\d+\\.\\d)\\%");
  // Parse the outfile
  double[] result = new double[Scorer.RESULT_SIZE];
  try {
    for (String line : LineIterable.iterate(new File(outFile))) {
      Matcher m = p.matcher(line);
      if (m.matches()) {
        double recall = Double.parseDouble(m.group(1));
        double prec = Double.parseDouble(m.group(2));
        double f = Double.parseDouble(m.group(3));
        result[Scorer.PRECISION] = prec / 100;
        result[Scorer.RECALL] = recall / 100;
        result[Scorer.F] = f / 100;
        break;
      }
    }
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }

  return result;
}

static AnnotationSet translateNPs(AnnotationSet nps, LeanDocument cl, boolean includeSingletons)
{
  AnnotationSet result = new AnnotationSet("new-nps");
  // First add the non-nes
  for (Annotation a : nps) {

    int id = Integer.parseInt(a.getAttribute(Constants.CE_ID));
    // System.out.println(a);
    // System.out.println("ID="+a.getAttribute(Constants.CE_ID));
    // System.out.println(cl.getElChain(id));
    // System.out.println("Document"+cl);
    if (includeSingletons || cl.getElChain(id).size() > 1) {
      String pn = a.getAttribute("ProperName");
      boolean properName = (pn != null && pn.equals("true"));
      if (!USE_NES || !properName) {
        Map<String, String> attrs = Maps.newTreeMap();
        attrs.put("ID", Integer.toString(id));
        int start = a.getStartOffset(), end = a.getEndOffset();
        Annotation cur = new Annotation(a.getId(), start, end, "COREF", attrs);

        result.add(cur);
        // result.add(a.getId(),start,end,"COREF",attrs);
      }
    }
  }
  if (USE_NES) {
    // Now the nes
    for (Annotation a : nps) {
      int id = Integer.parseInt(a.getAttribute(Constants.CE_ID));
      if (includeSingletons || cl.getElChain(id).size() > 1) {
        String pn = a.getAttribute("ProperName");
        boolean properName = (pn != null && pn.equals("true"));
        if (properName) {
          Map<String, String> attrs = Maps.newTreeMap();
          attrs.put("ID", Integer.toString(id));

          int start = a.getStartOffset(), end = a.getEndOffset();
          String linked = a.getAttribute("LinkedPN");
          // attrs.put("NE", linked);
          String[] offs = linked.trim().split("\\,");
          int newstart = Integer.parseInt(offs[0]);
          int newend = Integer.parseInt(offs[1]);
          if (newstart >= start && newend <= end) {
            start = newstart;
            end = newend;
          }
          Annotation cur = new Annotation(a.getId(), start, end, "COREF", attrs);
          if (result.getCrossing(cur) != null) {
            // System.out.println("CONFLICT "+cur);
            cur.setStartOffset(a.getStartOffset());
            cur.setEndOffset(a.getEndOffset());
            // if(result.getCrossing(cur)!=null){
            // System.out.println("STILL CONFLICT\n"+cur);
            // }else{
            // System.out.println("RESOLVED\n"+cur);
            // }
          }
          result.add(cur);
          // result.add(a.getId(),start,end,"COREF",attrs);
        }
      }
    }
  }

  return result;
}

}
