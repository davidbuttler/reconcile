package reconcile.featureExtractor;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import opennlp.maxent.io.BinaryGISModelReader;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;

public class SentenceSplitterOpenNLP
    extends InternalAnnotator {

private SentenceDetector sdetector;

public SentenceSplitterOpenNLP() {
  // set up the sentence splitter
  try {
    //InputStream resStream = this.getClass().getClassLoader().getResourceAsStream(Utils.lowercaseIfNec("OpenNLP")+"/models/EnglishSD.bin.gz");
    InputStream resStream = this.getClass().getClassLoader().getResourceAsStream("OpenNLP/models/EnglishSD.bin.gz");
    DataInputStream dis = new DataInputStream(new GZIPInputStream(resStream));
    sdetector = new SentenceDetectorME((new BinaryGISModelReader(dis)).getModel());

  }
  catch (IOException e) {
    e.printStackTrace();
  }
}

@Override
public void run(Document doc, String[] annSetNames)
{

  // Read in the text from the raw file
  String text = doc.getText();

  // get the paragraphs from precomputed annotation set on disk
  AnnotationSet parSet = doc.getAnnotationSet(Constants.PAR);

  AnnotationSet sent = parse(text, parSet, annSetNames[0]);
  addResultSet(doc,sent);
}

public AnnotationSet parse(String text, AnnotationSet parSet, String annotationSetName)
{
  AnnotationSet sent = new AnnotationSet(annotationSetName);

  if (parSet == null) {
    parSet = new AnnotationSet(Constants.PAR);
  }
  
  if (parSet.size() == 0) {
    System.out.println("no paragraphs detected.  Adding a covering paragraph");
    parSet.add(0, text.length() - 1, "paragraph");
  }
  
  int counter = 0;

  for (Annotation par : parSet) {
    String parText = Utils.getAnnotText(par, text);
    int start = par.getStartOffset();
    int prevEndPos = start;
    boolean sentenceAdded = false;
    
    for (int e : sdetector.sentPosDetect(parText)) {
      int endPos = start + e;      
      Map<String, String> feat = new TreeMap<String, String>();
      feat.put("sentNum", String.valueOf(counter++));
      // add each sentence to the annotation set
      sent.add(prevEndPos, endPos, "sentence_split", feat);
      sentenceAdded = true;
      prevEndPos = endPos;
    }
    
    if (!sentenceAdded) {
      Map<String, String> feat = new TreeMap<String, String>();
      feat.put("sentNum", String.valueOf(counter++));
      // add each sentence to the annotation set
      if (start != par.getEndOffset())      
    	  sent.add(start, par.getEndOffset(), "sentence_split", feat);
    }
    else if (prevEndPos < par.getEndOffset() - 1) {
      Map<String, String> feat = new TreeMap<String, String>();
      feat.put("sentNum", String.valueOf(counter++));
      // add each sentence to the annotation set
      if(prevEndPos != par.getEndOffset())
    	  sent.add(prevEndPos, par.getEndOffset(), "sentence_split", feat);
    }

  }
  return sent;
}
}
