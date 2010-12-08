package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;


/*
 * This feature is: I if one or both NPs is a title C otherwise
 */

public class Title
    extends NominalFeature {

public Title() {
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
  if (reconcile.features.properties.Title.getValue(np1, doc) || reconcile.features.properties.Title.getValue(np2, doc))
    return INCOMPATIBLE;
  return COMPATIBLE;
}

}
