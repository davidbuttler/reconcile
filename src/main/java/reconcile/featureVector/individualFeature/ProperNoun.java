package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.InfWords;


/*
 * This feature is: I if both NPs are proper nouns and contain no words in common C otherwise
 */

public class ProperNoun
    extends NominalFeature {

public ProperNoun() {
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
  if (featVector.get(Constructor.createFeature("Alias")).equals(COMPATIBLE)) return COMPATIBLE;
  if (reconcile.features.properties.ProperNoun.getValue(np1, doc) && reconcile.features.properties.ProperNoun.getValue(np2, doc)) {
    String[] words1 = InfWords.getValue(np1, doc);
    String[] words2 = InfWords.getValue(np2, doc);
    int inter = FeatureUtils.intersection(words1, words2);
    if (inter < Math.min(words1.length, words2.length)) return INCOMPATIBLE;
  }
  return COMPATIBLE;
}

}
