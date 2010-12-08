package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;


/*
 * The class of the instance: + if the NPs are coreferent - otherwise
 */
public class unknwnClass
    extends NominalFeature {

private static String[] values = { "+", "-" };

public unknwnClass() {
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
  return "-";
}

}
