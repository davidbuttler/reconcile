package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.InfWords;
import reconcile.features.properties.Stopword;
import reconcile.features.properties.SubsumesNumber;
import reconcile.general.Utils;


/*
 * This feature is: C if both NPs are non-pronominal and one np is proper substrings of the other with respect to
 * content words I otherwise
 * 
 * This feature agrees with the description, but differs slightly from Vincent's implementation.
 */

public class WordsSubstr
    extends NominalFeature {

public WordsSubstr() {
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
  if (np1.overlaps(np2)) return INCOMPATIBLE;
  if (FeatureUtils.isPronoun(np1, doc) || FeatureUtils.isPronoun(np2, doc)) return INCOMPATIBLE;
  if (Stopword.getValue(np1, doc) || Stopword.getValue(np2, doc)) return INCOMPATIBLE;
  if (SubsumesNumber.getValue(np1, doc) || SubsumesNumber.getValue(np2, doc)) return INCOMPATIBLE;
  String str1 = doc.getAnnotText(np1);
  String str2 = doc.getAnnotText(np2);
  if (FeatureUtils.isAlphabetStr(str1) || FeatureUtils.isAlphabetStr(str2)) return INCOMPATIBLE;
  if (FeatureUtils.isNumeral(str1) || FeatureUtils.isNumeral(str2)) return INCOMPATIBLE;
  String[] infW1 = InfWords.getValue(np1, doc);
  String[] infW2 = InfWords.getValue(np2, doc);

  if (infW1 == null || infW1.length == 0 || infW2 == null || infW2.length == 0) return INCOMPATIBLE;
  return Utils.isAnySubset(infW1, infW2) ? COMPATIBLE : INCOMPATIBLE;
}

}
