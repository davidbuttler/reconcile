package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.Definite;
import reconcile.features.properties.ProperName;


/*
 * This feature is: Y if the second NP starts with "the" N otherwise
 */

public class Definite2
    extends NominalFeature {

public Definite2() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return YN;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  if (Definite.getValue(np2, doc) && !ProperName.getValue(np2, doc))
    return "Y";
  else
    return "N";
}

}
