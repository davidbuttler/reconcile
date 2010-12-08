package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;


/*
 * This feature is: C if both NPs are pronouns and the strings match I otherwise
 */

public class ProStr
    extends NominalFeature {

public ProStr() {
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

  if (!FeatureUtils.isPronoun(np1, doc) || !FeatureUtils.isPronoun(np2, doc)) return INCOMPATIBLE;

  return doc.getAnnotText(np1).equalsIgnoreCase(doc.getAnnotText(np2)) ? COMPATIBLE : INCOMPATIBLE;
}

}
