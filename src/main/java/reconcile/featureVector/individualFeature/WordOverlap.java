package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.InfWords;
import reconcile.features.properties.ProperName;


/*
 * This feature is: C if the intersection of the content words of the two nps is not empty I otherwise
 */

public class WordOverlap
    extends NominalFeature {

public WordOverlap() {
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
  // String[] infWords1 = FeatureUtils.removeUninfWords(FeatureUtils.getUniqueWords( np1, annotations, text));
  // String[] infWords2 = FeatureUtils.removeUninfWords(FeatureUtils.getUniqueWords(np2, annotations, text));
  // String[] infWords1 = FeatureUtils.getWords(np1, text);
  // String[] infWords2 = FeatureUtils.getWords(np2, text);
  if (FeatureUtils.isPronoun(np1, doc) || FeatureUtils.isPronoun(np2, doc)) return INCOMPATIBLE;
  if (ProperName.getValue(np1, doc) || ProperName.getValue(np2, doc)) return INCOMPATIBLE;
  if (np1.overlaps(np2)) return INCOMPATIBLE;

  String[] infWords1 = InfWords.getValue(np1, doc);
  String[] infWords2 = InfWords.getValue(np2, doc);
  if (infWords1.length > 0 && infWords2.length > 0 && FeatureUtils.overlaps(infWords1, infWords2)) return COMPATIBLE;
  return INCOMPATIBLE;
}

}
