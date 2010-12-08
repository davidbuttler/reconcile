/*
 * Takes reconcile output and puts it into the ACE format to score.
 */
package reconcile.scorers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Vector;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;


public class ACEScorer {

private static String outFile = "ace.xml";
private static Writer fw;
private static BufferedWriter bw;
private static PrintWriter output;
private static Reader fr;
private static BufferedReader br;
private static HashMap<String, Vector<Integer>> clusters;

public ACEScorer() {
}

public static void readInClusters(Document doc)
    throws IOException
{
  fr = new InputStreamReader(doc.readClusterDirFile("clusters"));
  br = new BufferedReader(fr);
  String line;
  String[] tokens;
  clusters = new HashMap<String, Vector<Integer>>();

  while ((line = br.readLine()) != null) {
    line = line.trim();
    tokens = line.split("\\s+");

    if (!clusters.containsKey(tokens[1])) {
      Vector<Integer> v = new Vector<Integer>();
      v.add(Integer.parseInt(tokens[0]));
      clusters.put(tokens[1], v);
    }
    else {
      Vector<Integer> v = clusters.get(tokens[1]);
      v.add(Integer.parseInt(tokens[0]));
      clusters.put(tokens[1], v);
    }
  }
}

public static void writeACEFile(Document document)
    throws IOException
{
  fw = new OutputStreamWriter(document.writeFile(outFile));
  bw = new BufferedWriter(fw);
  output = new PrintWriter(bw);
  AnnotationSet nps = document.getAnnotationSet(Constants.PROPERTIES_FILE_NAME);
  nps.setName("nps");
  AnnotationSet answer = document.getAnnotationSet("ace_annots");
  AnnotationSet corefKeys = answer.get("COREF");
  String doc = nps.get(1).getAttribute("Text");

  // print header information
  output.write("<?xml version=\"1.0\"?>\n");
  output.write("<!DOCTYPE source_file SYSTEM \"apf.v5.1.1.dtd\">\n");
  output.write("<source_file URI=\"" + doc
      + ".sgm\" SOURCE=\"newswire\" TYPE=\"text\" AUTHOR=\"LDC\" ENCODING=\"UTF-8\">\n");
  output.write("<document DOCID=\"" + doc + "\">\n");

  /*
   * ProcessedNP id's start @ 1, whereas in the cluster files, the nps start 0. 
   */
  int j = 0;
  for (int i = 0; i < nps.size(); i++) {
    // find what cluster it is in
    for (String key : clusters.keySet()) {
      Vector<Integer> v = clusters.get(key);
      java.util.Collections.sort(v);

      // make sure it is the first reference of this mention.
      if (v.contains(i) && v.get(0) == i) {
        j++;

        Annotation currentNP = nps.get(i + 1);
        if (corefKeys.containsSpan(currentNP)) {
          Annotation correctNP = corefKeys.getContained(currentNP).getFirst();
          String[] type = correctNP.getAttribute("SEMANTIC").split(":");
          String c = correctNP.getAttribute("CLASS");

          output.write("\t<entity ID=\"" + doc + "-E" + j + "\" TYPE=\"" + type[0] + " SUBTYPE=\"" + type[1]
              + "\" CLASS=\"" + c + "\">\n");
        }
        else {
          // just output something default...
          output.write("\t<entity ID=\"" + doc + "-E" + j
              + "\" TYPE=\"FAC\" SUBTYPE=\"Building-Grounds\" CLASS=\"SPC\">\n");
        }

        for (int k = 0; k < v.size(); k++) {
          Annotation a = nps.get(v.get(k) + 1);
          String category = "PRO"; // default

          if (corefKeys.containsSpan(a)) {
            Annotation correctNP = corefKeys.getContained(a).getFirst();
            category = correctNP.getAttribute("CATEGORY");
          }

          // find out about the head
          String head = a.getAttribute("HeadNoun");
          String[] tokens = head.split(",");
          int headBegin = Integer.parseInt(tokens[0]);
          int headEnd = Integer.parseInt(tokens[1]);
          int diff = (headBegin - a.getStartOffset());

          // get the text of the np
          String text = a.getAttribute("Text");

          // System.out.println(a);
          // System.out.println(a.getStartOffset());
          // System.out.println(diff);

          output.write("\t\t<entity_mention ID=\"" + doc + "-E" + j + "-" + (k + 1) + "\" TYPE=\"" + category
              + "\" LDCTYPE=\"" + category + "\">\n");
          output.write("\t\t\t<extent>\n");
          output.write("\t\t\t\t<charseq START=\"" + a.getStartOffset() + "\" END=\"" + a.getEndOffset() + "\">" + text
              + "</charseq>\n");
          output.write("\t\t\t</extent>\n");
          output.write("\t\t\t<head>\n");

          try {
            output.write("\t\t\t\t<charseq START=\"" + headBegin + "\" END=\"" + headEnd + "\">" + text.substring(diff)
                + "</charseq>\n");
          }
          catch (StringIndexOutOfBoundsException se) {
            // this happens when the actual text is not the same length as what the bytespans indicate. I suppose this
            // is do to chopping off
            // whitespace.
            output.write("\t\t\t\t<charseq START=\"" + headBegin + "\" END=\"" + headEnd + "\">" + text
                + "</charseq>\n");
          }

          output.write("\t\t\t</head>\n");
          output.write("\t\t</entity_mention>\n");
        }

        output.write("\t</entity>\n");
      }
    }
  }

  output.write("</document>\n");
  output.write("</source_file>\n");
  output.close();
  bw.close();
  fw.close();
}
}
