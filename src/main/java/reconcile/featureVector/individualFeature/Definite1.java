package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.Definite;
import reconcile.features.properties.ProperName;


/*
 * This feature is: Y if the first NP starts with "the" N otherwise
 */

public class Definite1
    extends NominalFeature {

public Definite1() {
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
  if (Definite.getValue(np1, doc) && !ProperName.getValue(np1, doc))
    return "Y";
  else
    return "N";
}

}
