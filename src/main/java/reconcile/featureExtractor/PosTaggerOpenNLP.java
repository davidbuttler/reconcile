package reconcile.featureExtractor;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import opennlp.maxent.MaxentModel;
import opennlp.maxent.io.BinaryGISModelReader;
import opennlp.tools.postag.DefaultPOSContextGenerator;
import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSTaggerME;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;

public class PosTaggerOpenNLP
    extends InternalAnnotator {

private POSTaggerME tagr;

public PosTaggerOpenNLP() {
  try {
    // Establish the models for the tagger
    //InputStream resStream = this.getClass().getClassLoader().getResourceAsStream(Utils.lowercaseIfNec("OpenNLP")+"/models/tag.bin.gz");
	  InputStream resStream = this.getClass().getClassLoader().getResourceAsStream("OpenNLP/models/tag.bin.gz");
    DataInputStream dis = new DataInputStream(new GZIPInputStream(resStream));
    

    //InputStream tagDictRes = this.getClass().getClassLoader().getResourceAsStream(Utils.lowercaseIfNec("OpenNLP")+"/models/tagdict");
    InputStream tagDictRes = this.getClass().getClassLoader().getResourceAsStream("OpenNLP/models/tagdict");
    boolean caseSensitive = true;

    tagr = null;

    // set up the POS tagger
    tagr = new POSTaggerME(getModel(dis), new DefaultPOSContextGenerator(null), new POSDictionary(new BufferedReader(
        new InputStreamReader(tagDictRes)), caseSensitive));
  }
  catch (IOException e) {
    e.printStackTrace();
    throw new RuntimeException(e);
  }

}

private static MaxentModel getModel(DataInputStream in)
{
  try {
    return new BinaryGISModelReader(in).getModel();
  }
  catch (IOException e) {
    e.printStackTrace();
    return null;
  }
}

@Override
public void run(Document doc, String[] annSetNames)
{
  AnnotationSet tags = new AnnotationSet(annSetNames[0]);

  // get the sentences from the input
  AnnotationSet sentSet = doc.getAnnotationSet(Constants.SENT);
  // get the tokens from the input
  AnnotationSet tokSet = doc.getAnnotationSet(Constants.TOKEN);

  // Read in the text from the raw file
  String text = doc.getText();

  Iterator<Annotation> sents = sentSet.iterator();
  while (sents.hasNext()) {
    Annotation sent = (sents.next());

    int sentStart = sent.getStartOffset();
    int sentEnd = sent.getEndOffset();

    // the tokens that make up this sentence
    AnnotationSet sentToks = tokSet.getContained(sentStart, sentEnd);

    // list containing text segments that make up each token
    ArrayList<String> tokenList = new ArrayList<String>(sentToks.size());

    // build the list
    Iterator<Annotation> sentToksItr = sentToks.iterator();
    while (sentToksItr.hasNext()) {
      Annotation tok = (sentToksItr.next());
      tokenList.add(text.substring(tok.getStartOffset(), tok.getEndOffset()));
    }

    // Tag the sentece
    @SuppressWarnings("unchecked")
    List sentTags = tagr.tag(tokenList);

    // reset the iterator
    sentToksItr = sentToks.iterator();

    // add each tag to the annotation set, looping in parallel with sentToksItr
    for (Object tag : sentTags) {
      Annotation tok = (sentToksItr.next());
      tags.add(tok.getStartOffset(), tok.getEndOffset(), (String) tag);
    }
  }

  addResultSet(doc,tags);

}

}
