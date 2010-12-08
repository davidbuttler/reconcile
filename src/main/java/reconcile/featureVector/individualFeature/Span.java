package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;


/*
 * This feature is: I if one NP spans the other C otherwise
 */

public class Span
    extends NominalFeature {

public Span() {
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
  // AnnotationSet pos = annotations.get(Constants.POS);
  if (!FeatureUtils.sameSentence(np1, np2, doc)) return COMPATIBLE;
  if (Constructor.createFeature("Prednom").getValue(np1, np2, doc, featVector).equals(COMPATIBLE)) return COMPATIBLE;
  if (Constructor.createFeature(Constants.APPOSITIVE).getValue(np1, np2, doc, featVector).equals(COMPATIBLE))
    return COMPATIBLE;
  if (Constructor.createFeature("Quantity").getValue(np1, np2, doc, featVector).equals(COMPATIBLE)) return COMPATIBLE;
  if (np1.covers(np2) || np2.covers(np1)) return INCOMPATIBLE;
  return COMPATIBLE;
}

}
