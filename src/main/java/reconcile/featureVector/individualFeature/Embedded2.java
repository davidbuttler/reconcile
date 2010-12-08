package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.Embedded;


/*
 * This feature is: Y if the second NP is embedded/nested N otherwise
 */

public class Embedded2
    extends NominalFeature {

public Embedded2() {
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
  if (Embedded.getValue(np2, doc))
    return "Y";
  else
    return "N";
}

}
