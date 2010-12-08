package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.InfWords;
import reconcile.features.properties.Property;


/*
 * This feature is: I if both NPs are proper names and contain no words in common C otherwise
 */

public class ProperName
    extends NominalFeature {

public ProperName() {
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
  if (Constructor.createFeature("Alias").getValue(np1, np2, doc, featVector).equals(COMPATIBLE)) return COMPATIBLE;

  // if(FeatureUtils.isProperName(np1, annotations, text)&&FeatureUtils.isProperName(np2, annotations, text)){
  // String[] words1 = FeatureUtils.removeUninfWords(FeatureUtils.getWords(np1, text));
  // String[] words2 = FeatureUtils.removeUninfWords(FeatureUtils.getWords(np2, text));
  // int inter = FeatureUtils.intersection(words1, words2);
  // if(inter<1)//Math.min(words1.length, words2.length))
  // return INCOMPATIBLE;
  // }
  if (reconcile.features.properties.ProperName.getValue(np1, doc) && reconcile.features.properties.ProperName.getValue(np2, doc)) {
    Annotation pn1 = (Annotation) Property.LINKED_PROPER_NAME.getValueProp(np1, doc);
    Annotation pn2 = (Annotation) Property.LINKED_PROPER_NAME.getValueProp(np2, doc);
    String[] words1 = InfWords.getValue(pn1, doc);
    String[] words2 = InfWords.getValue(pn2, doc);
    int inter = FeatureUtils.intersection(words1, words2);
    if (inter < 1) // Math.min(words1.length, words2.length))
      return INCOMPATIBLE;
  }
  return COMPATIBLE;
}

}
