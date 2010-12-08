package reconcile.featureExtractor;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import opennlp.maxent.io.BinaryGISModelReader;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.util.Span;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;

public class TokenizerOpenNLP
    extends InternalAnnotator {

//private static final String OPEN_NLP_TOKENIZER_MODEL = Utils.lowercaseIfNec("OpenNLP")+"/models/EnglishTok.bin.gz";
private static final String OPEN_NLP_TOKENIZER_MODEL = "OpenNLP/models/EnglishTok.bin.gz";
private TokenizerME tknzr;

public TokenizerOpenNLP() {
  try {
    // set up the tokenizer
    InputStream resStream = this.getClass().getClassLoader().getResourceAsStream(OPEN_NLP_TOKENIZER_MODEL);
    DataInputStream dis = new DataInputStream(new GZIPInputStream(resStream));

    tknzr = null;
    tknzr = new TokenizerME((new BinaryGISModelReader(dis)).getModel());
  }
  catch (IOException e) {
    e.printStackTrace();
    throw new RuntimeException(e);
  }
}

@Override
public void run(Document doc, String[] annSetNames)
{
  AnnotationSet toks = new AnnotationSet(annSetNames[0]);

  // get the sentences from precomputed annotation set on disk
  AnnotationSet sentSet = doc.getAnnotationSet(Constants.SENT);

  // Read in the text from the raw file
  String text = doc.getText();

  Iterator<Annotation> sents = sentSet.iterator();
  while (sents.hasNext()) {
    Annotation sent = (sents.next());

    int sentStart = sent.getStartOffset();
    int sentEnd = sent.getEndOffset();

    String sentence = text.substring(sentStart, sentEnd);

    // tokenize the sentence
    Span[] spans = tknzr.tokenizePos(sentence);

    // add each token to the annotation set
    for (Span token : spans) {
      toks.add(sentStart + token.getStart(), sentStart + token.getEnd(), "token");
    }
  }

  
  addResultSet(doc,toks);
}
}
