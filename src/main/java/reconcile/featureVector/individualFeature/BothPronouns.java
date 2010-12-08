package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;


/*
 * This feature is: C if both NPs are pronouns NA if exactly one is I otherwise
 */

public class BothPronouns
    extends NominalFeature {

public BothPronouns() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return ICN;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  boolean pro1 = FeatureUtils.isPronoun(np1, doc);
  boolean pro2 = FeatureUtils.isPronoun(np2, doc);

  if (pro1 && pro2) return COMPATIBLE;
  if (pro1 || pro2) return NA;
  return INCOMPATIBLE;
}

}
