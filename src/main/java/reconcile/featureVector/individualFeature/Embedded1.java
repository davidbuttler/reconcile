package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.Embedded;


/*
 * This feature is: Y if the first NP is embedded/nested N otherwise
 */

public class Embedded1
    extends NominalFeature {

public Embedded1() {
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
  if (Embedded.getValue(np1, doc))
    return "Y";
  else
    return "N";
}

}
