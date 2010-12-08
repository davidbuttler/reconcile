/*
 * An interface to the Hal Daume's TagChunk NER system
 */
package reconcile.featureExtractor;

import gov.llnl.text.util.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import reconcile.SystemConfig;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;


public class NamedEntityTagChunk
    extends InternalAnnotator {

private String[] models;
private String modelDir;
private SystemConfig currentConfig;

public NamedEntityTagChunk() {
  /* defaults */
  currentConfig = Utils.getConfig();
  modelDir = Utils.getDataDirectory();
  models = currentConfig.getNERModels("tagchunkmodels");
}

@Override
public void run(Document doc, String annSetNames[])
{

  String tagChunk = currentConfig.getTagChunk();
  String listDir = currentConfig.getTagChunkLists();

  AnnotationSet namedEntities = new AnnotationSet(annSetNames[0]);

  // get the sentences from the input
  AnnotationSet sentSet = doc.getAnnotationSet(Constants.SENT);

  // get the tokens from each sentence
  AnnotationSet tokenSet = doc.getAnnotationSet(Constants.TOKEN);

  // Read in the text from the raw file
  String text = doc.getText();

  Iterator<Annotation> sents = sentSet.iterator();
  ArrayList<String> lines = new ArrayList<String>();
  ArrayList<Vector<Annotation>> tokenList = new ArrayList<Vector<Annotation>>();

  while (sents.hasNext()) {
    Vector<Annotation> annVector = new Vector<Annotation>();
    Annotation sent = sents.next();
    int sentStart = sent.getStartOffset();
    int sentEnd = sent.getEndOffset();
    String sentText = Utils.getAnnotText(sent, text);
    AnnotationSet sentTokens = tokenSet.getContained(sentStart, sentEnd);

    // gather all sentences to tag
    if (!sentText.matches("\\W+")) {
      StringBuilder tmp = new StringBuilder();
      for (Annotation a : sentTokens) {
        tmp.append(Utils.getAnnotTextClean(a, text)).append(" ");
        annVector.add(a);
      }

      lines.add(tmp.toString());
      tokenList.add(annVector);
    }
  }

  // write out a tmp file that contains the words to be tagged
  File tmpFile = new File(doc.getRootDir(), "tmp.ner");
  try {
    tmpFile.deleteOnExit();
    FileWriter fw = new FileWriter(tmpFile);
    BufferedWriter bw = new BufferedWriter(fw);
    for (String l : lines) {
      // System.out.println(l);
      bw.write(l + "\n");
    }

    bw.close();
    fw.close();
  }
  catch (IOException ioe) {
    ioe.printStackTrace();
  }

  // run the tagger
  String command = tagChunk + " -predict . " + modelDir + Utils.SEPARATOR + models[0] + " " + tmpFile.getAbsolutePath()
      + " " + listDir;

  // collect the results
  ArrayList<String> results;
  int i = 0;
  try {
    results = Utils.runExternalCaptureOutput(command);
    Annotation current = null;
    for (String l : results) {
      Vector<Annotation> annVector = tokenList.get(i);

      // get rid of these extraneous tags
      l = l.replace("_O-O", "");
      String[] tokens = l.split(" ");
      // System.out.println(l);

      int j = 0;
      int underscore;
      int nes = 1;
      String tag;
      for (String t : tokens) {
        underscore = t.lastIndexOf('_');
        tag = t.substring(underscore + 1, t.length());
        Annotation a = annVector.get(j);
        // System.out.print(Utils.getAnnotTextClean(a, text) + "_" + tag + " ");

        if (tag.equals("B-O")) {
          j++;
          if (current != null) {
            namedEntities.add(current);
            // System.out.println("NE Found: " + Utils.getAnnotTextClean(current, text) + ":" + current.getType());
            nes++;
            current = null;
          }

          continue;
        }

        String entityType = tag.substring(tag.indexOf("-") + 1, tag.length());

        if (entityType.equals("ORG")) {
          entityType = "ORGANIZATION";
        }
        else if (entityType.equals("LOC")) {
          entityType = "LOCATION";
        }
        else if (entityType.equals("PER")) {
          entityType = "PERSON";
        }
        else if (entityType.equals("VEH")) {
          entityType = "VEHICLE";
        }

        if (tag.startsWith("B-")) {
          if (current != null) {
            namedEntities.add(current);
            nes++;
            current = null;
            // System.out.println("NE Found: " + Utils.getAnnotTextClean(current, text));
          }

          current = new Annotation(nes, a.getStartOffset(), a.getEndOffset(), entityType);
        }
        else if (tag.startsWith("I-")) {
          if (current != null) {
            current.setEndOffset(a.getEndOffset());
          }
          else {
            current = new Annotation(nes, a.getStartOffset(), a.getEndOffset(), entityType);
          }
        }

        j++;
      }

      // System.out.println();
      i++;
    }
    FileUtils.delete(tmpFile);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
  catch (InterruptedException e) {
    throw new RuntimeException(e);
  }

  addResultSet(doc,namedEntities);
}
}
