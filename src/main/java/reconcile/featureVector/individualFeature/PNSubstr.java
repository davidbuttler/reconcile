package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.InfWords;
import reconcile.features.properties.ProperName;
import reconcile.features.properties.Property;
import reconcile.general.Utils;


/*
 * This feature is: C if both NPs are proper names and one NP is a proper substring with respect of content words of the
 * other I otherwise
 * 
 * Slightly differs from Vincent's implementation
 */

public class PNSubstr
    extends NominalFeature {

public PNSubstr() {
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

  if (!ProperName.getValue(np1, doc) || !ProperName.getValue(np2, doc)) return NA;
  Annotation ne1 = (Annotation) np1.getProperty(Property.LINKED_PROPER_NAME);
  Annotation ne2 = (Annotation) np2.getProperty(Property.LINKED_PROPER_NAME);
  String[] infW1 = InfWords.getValue(ne1, doc);
  String[] infW2 = InfWords.getValue(ne2, doc);
  if (infW1 == null || infW2 == null || infW1.length < 1 || infW2.length < 1) return INCOMPATIBLE;
  if (Utils.isAnySubset(infW1, infW2))
    return COMPATIBLE;
  else
    return INCOMPATIBLE;
}

}
