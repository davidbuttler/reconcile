package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.CorefID;


/*
 * The class of the instance: + if the NPs are coreferent - otherwise
 */

public class instClass
    extends NominalFeature {

public static String POSITIVE = "+";
public static String NEGATIVE = "-";
private static String[] values = { POSITIVE, NEGATIVE };

public instClass() {
  name = "class";
}

@Override
public String[] getValues()
{
  return values;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  Integer corefID1 = CorefID.getValue(np1, doc);
  Integer corefID2 = CorefID.getValue(np2, doc);
  if (corefID1.intValue() < 0) return NEGATIVE;
  if (corefID1.equals(corefID2)) return POSITIVE;
  return NEGATIVE;
}

}
