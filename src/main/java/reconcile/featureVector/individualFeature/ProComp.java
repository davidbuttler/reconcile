package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;


/*
 * This feature is: C if both NPs are comparable pronouns -- i.e., he and his I otherwise
 */

public class ProComp
    extends NominalFeature {

public ProComp() {
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

  if (!FeatureUtils.isPronoun(np1, doc) || !FeatureUtils.isPronoun(np2, doc)) return COMPATIBLE;
  if (Constructor.createFeature(BothInQuotes.class.getName()).getValue(np1, np2, doc, featVector).equals(NA)) return COMPATIBLE;

  if (Constructor.createFeature(Gender.class.getName()).getValue(np1, np2, doc, featVector).equals(INCOMPATIBLE))
    return INCOMPATIBLE;
  if (!Constructor.createFeature(Number.class.getName()).getValue(np1, np2, doc, featVector).equals(COMPATIBLE)) return INCOMPATIBLE;
  if (FeatureUtils.getPronounPerson(doc.getAnnotText(np1)) == FeatureUtils.getPronounPerson(doc.getAnnotText(np2)))
    return COMPATIBLE;
  return INCOMPATIBLE;
}

}
