package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;


/*
 * This feature is: I if the first NP is a pronoun and the second is not C otherwise
 */

public class Pronoun
    extends NominalFeature {

public Pronoun() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return IC;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  // if(!FeatureUtils.sameSentence(np1, np2, annotations, text))
  // return COMPATIBLE;
  Annotation first, second;
  if (np1.compareSpan(np2) <= 0) {
    first = np1;
    second = np2;
  }
  else {
    first = np2;
    second = np1;
  }
  if (FeatureUtils.isPronoun(first, doc) && !FeatureUtils.isPronoun(second, doc)
      && !Constructor.createFeature("BothInQuotes").getValue(np1, np2, doc, featVector).equals(NA))
    return INCOMPATIBLE;
  return COMPATIBLE;
}

}
