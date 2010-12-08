package reconcile.featureExtractor;

import gov.llnl.text.util.LineIterable;

import java.io.IOException;

import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Utils;


/**
 * @author ves Sentence splitter using the perl script from UIUC (available at
 *         http://l2r.cs.uiuc.edu/~cogcomp/software/sentencereg.html)
 */
public class SentenceSplitterUIUC
    extends ExternalAnnotator {

private String options;

private String inFile;

private Document doc;

private String[] annSetName;

private String applicationDirectory;

private String applicationName = "sentence-boundary.pl";

@Override
public void postprocess()
    throws IOException
{
  System.err.println("Processing the results");
  // Read in the text from the raw file
  String text = doc.getText();
  AnnotationSet sent = new AnnotationSet(annSetName[0]);
  int currentOffset = 0;
  for (String line : LineIterable.iterate(inFile)) {
    line = line.trim();
    if (line.length() > 0) {
      String[] words = line.split("\\s");
      int start = -1;
      for (String word : words) {
        int offset = text.indexOf(word, currentOffset);
        if (offset < 0) {

          System.out.println(text);

          throw new RuntimeException("Unmatched sentence " + line);
        }
        if (start == -1) {
          start = offset;
        }
        currentOffset = offset + word.length();
      }
      int end = currentOffset;
      sent.add(start, end, "sentence_split");
    }
  }
  addResultSet(doc,sent);

}

@Override
public String runOptions()
{
  return options;
}

@Override
public void preprocess()
    throws IOException
{
  // do nothing
}

@Override
public void run(Document doc, String[] annSetNames)
{
  this.doc = doc;
  this.annSetName = annSetNames;
  applicationDirectory = reconcile.general.Utils.getExtToolsDirectory() + Utils.SEPARATOR + "sentenceSplitterUIUC";
  ;
  options = "-d " + applicationDirectory + Utils.SEPARATOR + "HONORIFICS" + " -i " + doc + "raw.txt -o " + doc
      + "temp" + Utils.SEPARATOR + "sentSplit";
  inFile = doc + "temp" + Utils.SEPARATOR + "sentSplit";
  runApplication();
}

@Override
public String getApplicationName()
{
  return "perl " + applicationDirectory + Utils.SEPARATOR + applicationName;
}

}
