package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;


/*
 * This feature is: C if both NPs are non-pronominal and after discarding determiners the strings of the two NPs match I
 * otherwise
 */

public class SoonStrNonPro
    extends NominalFeature {

public SoonStrNonPro() {
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
  String[] words1 = doc.getWords(np1);
  String[] words2 = doc.getWords(np2);
  if (FeatureUtils.isPronoun(words1) || FeatureUtils.isPronoun(words2)) return INCOMPATIBLE;
  return Constructor.createFeature("SoonStr").getValue(np1, np2, doc, featVector);
}
}
