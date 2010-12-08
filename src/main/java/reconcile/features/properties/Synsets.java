package reconcile.features.properties;


import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.dictionary.Dictionary;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;

public class Synsets
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new Synsets(false, true);
  }
  return ref;
}

public static Synset[] getValue(Annotation np, Document doc)
{
  return (Synset[]) getInstance().getValueProp(np, doc);
}

private Synsets(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  Synset[] value;
  // Starting up Wordnet
  Dictionary wordnet = FeatureUtils.initializeWordNet();
  IndexWord w1 = null;
  String word1 = doc.getAnnotText(HeadNoun.getValue(np, doc)).toLowerCase();
  if (FeatureUtils.isPronoun(np, doc)) {
    value = new Synset[0];
  }
  else {
    if (ProperName.getValue(np, doc)) {
      word1 = ProperNameType.getValue(np, doc).toString().toLowerCase();
    }
    else {
      Annotation ne = (Annotation) Property.LINKED_PROPER_NAME.getValueProp(np, doc);
      if (ne != null && !ne.equals(Annotation.getNullAnnot())) {
        word1 = ne.getType();
      }
    }
    try {
      // System.err.println("Working on "+word1+".");
      String[] embWords = word1.split("\\W");
      if (embWords.length < 1) {
        value = new Synset[0];
      }
      else {
        word1 = embWords[embWords.length - 1];

        AnnotationSet posAnnots = doc.getAnnotationSet(Constants.POS);
        AnnotationSet parse = doc.getAnnotationSet(Constants.PARSE);
        Annotation hn = HeadNoun.getValue(np, doc);
        Annotation headPOS = SyntaxUtils.getNode(hn, parse);
        if (headPOS == null) {
          AnnotationSet contPOS = posAnnots.getContained(hn);
          headPOS = contPOS == null ? null : contPOS.getLast();
        }
        String type = headPOS == null ? null : headPOS.getType();
        boolean plural = type != null && (type.equalsIgnoreCase("NNS") || type.equalsIgnoreCase("NNPS"));
        if (plural) {
          w1 = wordnet.getIndexWord(POS.NOUN, word1);
          if (w1 == null) {
            w1 = wordnet.getMorphologicalProcessor().lookupBaseForm(POS.NOUN, word1);
          }
        }
        if (w1 == null) {
          w1 = wordnet.getIndexWord(POS.NOUN, word1);
        }
        if (w1 == null) {
          w1 = wordnet.lookupIndexWord(POS.NOUN, word1);
        }
        if (w1 == null) {
          value = new Synset[0];
        }
        else {
          value = w1.getSenses();
        }
      }
    }
    catch (JWNLException e) {
      throw new RuntimeException(e);
    }
  }

  return value;
}

}
