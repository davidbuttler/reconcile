/*
 * An interface to the OpenNLP Named entity finder
 * 
 * For training look at: \OpenNLP\src\java\opennlp\tools\namefindNameFinderME.java It has a method called "train", which
 * I assume is used for creating new models.
 * 
 * For some tools to aid in training, look in the Testing directory: trainner.sh and AnnotateNE.java
 * 
 * To see how named entity finding is currently done (with the default white-space tokenizer), look at:
 * \OpenNLP\src\java\opennlp\tools\lang\english\NameFinder.java
 */

package reconcile.featureExtractor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import opennlp.maxent.MaxentModel;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.Span;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;

public class NamedEntityOpenNLP
    extends InternalAnnotator {

NameFinderME[] finders;
String[] tagTypes = { "date", "location", "money", "organization", "percentage", "person", "time" };

private static MaxentModel getModel(File name)
{
  try {
    return new SuffixSensitiveGISModelReader(name).getModel();
  }
  catch (IOException e) {
    e.printStackTrace();
    return null;
  }
}

public NamedEntityOpenNLP() {
  /* Establish a name finder for each tag type */
  finders = new NameFinderME[tagTypes.length];

  for (int i = 0; i < tagTypes.length; ++i) {

    try {

      URL res = this.getClass().getClassLoader().getResource(Utils.lowercaseIfNec("OpenNLP")+"/models/" + tagTypes[i] + ".bin.gz");
      System.out.println("load openNLP named entity model: " + res.toString());
      File modelFileName = new File(res.toURI());
      // load the model
      MaxentModel model = getModel(modelFileName);

      // create the finder
      finders[i] = new NameFinderME(model);
    }
    catch (URISyntaxException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}

@Override
public void run(Document doc, String[] annSetNames)
{

  AnnotationSet namedEntities = new AnnotationSet(annSetNames[0]);

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

    // array containing text segments that make up each token
    String[] tokenList = new String[sentToks.size()];

    // build the array
    Iterator<Annotation> sentToksItr = sentToks.iterator();
    for (int i = 0; sentToksItr.hasNext(); ++i) {
      Annotation tok = (sentToksItr.next());
      tokenList[i] = (text.substring(tok.getStartOffset(), tok.getEndOffset()));
    }

    for (int fndIndx = 0; fndIndx < finders.length; ++fndIndx) {
      NameFinderME findr = finders[fndIndx];

      // Tag the sentence
      Span[] sentTags = findr.find(tokenList); // findr.find(tokenList, new HashMap());

      for (Span span : sentTags) {
        namedEntities.add(span.getStart(), span.getEnd(), tagTypes[fndIndx]);
      }
    }
  }

  addResultSet(doc,namedEntities);
}
}
