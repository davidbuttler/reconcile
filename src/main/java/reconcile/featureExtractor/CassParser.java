package reconcile.featureExtractor;

import gov.llnl.text.util.FileUtils;
import gov.llnl.text.util.LineIterable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Pattern;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;


/**
 * @author ves A wrapper for the Cass parser by Steven Abney.
 */
public class CassParser
    extends ExternalAnnotator {

public static final Pattern pBeginSentenceTag = Pattern.compile("<s>");
public static final Pattern pEndSentenceTag = Pattern.compile("<\\/s>");
public static final Pattern pSpace = Pattern.compile("\\s+");
public static final Pattern pAssignment = Pattern.compile("\\w=\\[.+");
public static final Pattern pBeginBracket = Pattern.compile("\\[.+");

private String options;

private String inFile;
private String tempFilename;

private Document doc;

private String[] annSetName;

private String applicationDirectory = reconcile.general.Utils.getExtToolsDirectory() + Utils.SEPARATOR + "bin";;

private String applicationName = "tagfixes";

@Override
public void postprocess()
    throws IOException
{
  System.err.println("Processing the results");
  // Read in the text from the raw file
  AnnotationSet sentSet = doc.getAnnotationSet(Constants.SENT);

  // get the pos from precomputed annotation set on disk
  AnnotationSet posSet = doc.getAnnotationSet(Constants.POS);

  // Read in the text from the raw file
  String text = doc.getText();

  Iterator<Annotation> sentIter = sentSet.iterator();
  // loop through sentences

  AnnotationSet result = new AnnotationSet(annSetName[0]);

  // Read in sentences
  boolean inSentence = false;
  Stack<Annotation> compStack = new Stack<Annotation>();
  Annotation tok = null;
  Iterator<Annotation> tokIter = null;
  for (String line : LineIterable.iterate(inFile)) {
    line = line.trim();
    // System.out.println(line);

    if (line.length() > 0) {
      if (!inSentence) {
        if (pBeginSentenceTag.matcher(line).matches()) {
          inSentence = true;
          Annotation sentence = sentIter.next();
          List<Annotation> posAnnots = posSet.getContained(sentence).getOrderedAnnots();
          tokIter = posAnnots.iterator();
          tok = tokIter.next();
          int sent = result.add(sentence.getStartOffset(), sentence.getEndOffset(), "s");
          Annotation cur = result.get(sent);
          compStack.push(cur);
        }
      }
      else if (pEndSentenceTag.matcher(line).matches()) {
        inSentence = false;
      }
      else {
        // tok=tokIter.next();
        String[] words = pSpace.split(line);

        for (int w = 0; w < words.length; w++) {
          if (pAssignment.matcher(words[w]).matches() && w < 1) {
            Map<String, String> features = new TreeMap<String, String>();
            features.put("role", words[w].substring(0, 1));
            int id = result.add(tok.getStartOffset(), Integer.MAX_VALUE, words[w].substring(3).toUpperCase(), features);
            Annotation cur = result.get(id);
            compStack.push(cur);
            // System.out.println("Starting "+words[w]);
          }
          else if (pBeginBracket.matcher(words[w]).matches() && w < 1) {
            Map<String, String> features = new TreeMap<String, String>();
            features.put("role", "null");
            int id = result.add(tok.getStartOffset(), Integer.MAX_VALUE, words[w].substring(1).toUpperCase(), features);
            Annotation cur = result.get(id);
            compStack.push(cur);
            // System.out.println("Starting "+words[w].substring(1)+" role is null");
          }
          else {
            String word = words[w];
            int count = 0;
            while (word.endsWith("]")) {
              count++;
              Annotation an = compStack.pop();
              an.setEndOffset(tok.getEndOffset());
              if (!compStack.isEmpty()) {
                Annotation parent = compStack.peek();
                an.setAttribute("parent", Integer.toString(parent.getId()));
                String child = parent.getAttribute("CHILD_IDS");
                child = child == null ? Integer.toString(an.getId()) : child + "," + an.getId();
                parent.setAttribute("CHILD_IDS", child);
              }
              word = word.substring(0, word.length() - 1);
              // System.out.println("Ending "+an+" ");
            }
            String tokText = Utils.getAnnotText(tok, text);
            if (!tokText.equalsIgnoreCase(word) && !tokText.equals("]"))
              throw new RuntimeException(Utils.getAnnotText(tok, text) + " vs. " + word);
            // System.out.println("ending "+word+" ("+count+")");
            if (tokIter.hasNext()) {
              tok = tokIter.next();
            }
            else {
              tok = null;
            }
          }
          // System.out.print(words[w]+" -- ");

          // System.out.println();
        }
      }
    }
  }
  addResultSet(doc,result);

}

@Override
public String runOptions()
{
  return options;
}

public void setOptions(String opt)
{
  options = opt;
}

@Override
public void preprocess()
    throws IOException
{
  // Create an input file for cass based on pos tags
  // get the sentences from precomputed annotation set on disk
  AnnotationSet sentSet = doc.getAnnotationSet(Constants.SENT);

  // get the pos from precomputed annotation set on disk
  AnnotationSet posSet = doc.getAnnotationSet(Constants.POS);

  // Read in the text from the raw file
  String text = doc.getText();

  // Output in a temp file for cass
  BufferedWriter tempFile = new BufferedWriter(new FileWriter(tempFilename));

  // loop through sentences

  int numWords = 0;
  int numSents = 0;
  int num = 0;

  for (Annotation sentence : sentSet) {
    num++;
    numSents++;
    AnnotationSet toks = posSet.getContained(sentence);
    int len = toks.size();
    numWords += len;
    tempFile.write("<s>\n");
    for (Annotation tok : toks) {
      tempFile.write(Utils.getAnnotText(tok, text).replaceAll("]", ")") + "\t" + tok.getType() + "\n");
    }
    tempFile.write("</s>\n");
  }
  tempFile.flush();
  tempFile.close();
}

@Override
public void run(Document doc, String[] annSetNames)
{
  try {
    this.doc = doc;
    this.annSetName = annSetNames;
    String tempDirname = doc.getRootDir().getAbsolutePath() + Utils.SEPARATOR + "temp";
    FileUtils.mkdir(tempDirname);
    tempFilename = tempDirname + "/cassIn";
    preprocess();
    applicationDirectory = reconcile.general.Utils.getExtToolsDirectory() + Utils.SEPARATOR + "bin";
    ;
    // +" | "+applicationDirectory+Utils.SEPARATOR+"cass";
    inFile = doc + Utils.SEPARATOR + "temp" + Utils.SEPARATOR + "cassOut";
    String command[] = {
        "bash ",
        "-c",
        (applicationDirectory + Utils.SEPARATOR + applicationName + " " + tempFilename + " | " + applicationDirectory
            + Utils.SEPARATOR + "cass -r").replaceAll("\\\\", "/") };
    // String intermFilename = tempDirname+Utils.SEPARATOR+"cassIntermediate";
    PrintStream outStream = new PrintStream(inFile);
    System.err.println("Running " + Arrays.toString(command));
    Utils.runExternal(command, true, outStream);
    postprocess();
  }
  catch (Exception e) {
    e.printStackTrace();
    throw new RuntimeException(e);
  }
}

@Override
public String getApplicationName()
{
  return applicationDirectory + Utils.SEPARATOR + applicationName;
}

}
