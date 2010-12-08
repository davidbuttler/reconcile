package reconcile.featureExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;

import com.google.common.collect.Sets;

public class LongSentenceSplitter
    extends InternalAnnotator {

private static final int TOKEN_LIMIT = 100;

private static final Set<String> sBreakSet = Sets.newHashSet( ";", ":", ",", "." );

/**
 * This feature extractor takes sentences and a tokenization and splits up sentences that are "too long".  "Too long" is defined by 
 * the constant TOKEN_LIMIT.  It should be set to a value that makes it tractable to run a dependency parse on the sentence using a 
 * statistical model (like the Stanford parser or the Berkeley parser) 
 */
public LongSentenceSplitter() {
}

@Override
public void run(Document doc, String[] annSetNames)
{

    // get the tokens and long sentences from annotation set on disk
    AnnotationSet sentSet = doc.getAnnotationSet(Constants.SENT);
    AnnotationSet tokenSet = doc.getAnnotationSet(Constants.TOKEN);

    AnnotationSet shortSent = parse(doc, sentSet, tokenSet, annSetNames[0]);
    
    // write out long sentences as a new annotation set name
    // and short sentences as the original sentences
    sentSet.setName(annSetNames[1]);
    addResultSet(doc,sentSet);
    addResultSet(doc,shortSent);
}

public AnnotationSet parse(Document doc, AnnotationSet sentSet, AnnotationSet tokenSet, String annotationSetName)
{
  AnnotationSet sent = new AnnotationSet(annotationSetName);
  int counter = 0;

  for (Annotation longSentence : sentSet) {
    if (doc.getAnnotText(longSentence).trim().equals("")) {
      System.out.println("skipping empty sentence");
      continue;
    }
    List<Annotation> shortSentences = splitSentenceText(tokenSet.getContained(longSentence), longSentence, doc);
    sent.addAll(shortSentences);
    for (Annotation ss: shortSentences) {
      Map<String, String> feat = ss.getFeatures();
      feat.put("sentNum", String.valueOf(counter++));
      sent.add(ss);
    }

  }
  return sent;
}

/**
 * Given a sentence and the set of tokens in that sentence, check to see if the sentence is too long (defined by
 * TOKEN_LIMIT). If so, try to split the sentence on a token that matches a break token (e.g. ':'). The split is
 * recursive until all sentences are less then TOKEN_LIMIT tokens. If there is no break token in the sentence, then it
 * is ignored.
 * 
 * <p>
 * Note: the split starts from the center of the sentence and simultaneously works its way towards both ends until it
 * finds a break or the sentence ends.
 * 
 * @param toks
 * @param text
 * @return
 */
public List<Annotation> splitSentenceText(AnnotationSet toks, Annotation sentence, Document doc) {
  return splitSentenceText(toks, sentence, doc, false);
}
public List<Annotation> splitSentenceText(AnnotationSet toks, Annotation sentence, Document doc, boolean forceSplit)
{
  List<Annotation> result = new ArrayList<Annotation>();
  if (!forceSplit && toks.size() <= TOKEN_LIMIT) {
    result.add(sentence);
    return result;
  }
  System.out.println("LSS Long sentence: " + doc.getAnnotString(sentence));
  List<Annotation> tokens = toks.getOrderedAnnots();
  int mid = tokens.size() / 2;

  boolean split = false;
  for (int indexLow = mid, indexHigh = mid; indexLow >= 0; indexLow--) {
    indexHigh++;
    Annotation tokLow = tokens.get(indexLow);
    String lowText = doc.getAnnotString(tokLow);
    if (sBreakSet.contains(lowText)) {
      Annotation newSen = new Annotation(sentence.getId(), sentence.getStartOffset(), tokLow.getEndOffset(),
 "sentence");
      result = splitSentenceText(toks.getContained(newSen), newSen, doc);
      Annotation newSen1 = new Annotation(sentence.getId(), tokLow.getEndOffset() + 1, sentence.getEndOffset(),
          "sentence");
      result.addAll(splitSentenceText(toks.getContained(newSen1), newSen1, doc));
      System.out.println("Split into:" + doc.getAnnotString(newSen) + "\nand\n" + doc.getAnnotString(newSen1));
      split = true;
      break;
    }
    else {
      if (indexHigh < tokens.size() - 1) { // remember: java is 0-offset, so if indexHigh == tokens.size()-1, that is
        // the last token
        Annotation tokHigh = tokens.get(indexHigh);
        String hiText = doc.getAnnotString(tokHigh);
        if (sBreakSet.contains(hiText)) {
          Annotation newSen = new Annotation(sentence.getId(), sentence.getStartOffset(), tokHigh.getEndOffset(),
              "sentence");
          result = splitSentenceText(toks.getContained(newSen), newSen, doc);
          Annotation newSen1 = new Annotation(sentence.getId(), tokHigh.getEndOffset() + 1, sentence.getEndOffset(),
              "sentence");
          result.addAll(splitSentenceText(toks.getContained(newSen1), newSen1, doc));
          System.out.println("Split into:" + doc.getAnnotString(newSen) + "\nand\n" + doc.getAnnotString(newSen1));
          split = true;
          break;
        }
      }
    }
    if (split) {
      break;
    }
  }

  return result;
}

}
