package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.GramRole;


/*
 * This feature is: The grammmatical role of the first NP.
 */

public class GramRole2
    extends NominalFeature {

public GramRole2() {
  name = this.getClass().getSimpleName();
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  return GramRole.getValue(np2, doc);
}

@Override
public String[] getValues()
{
  return FeatureUtils.KNOWN_GRAM_RELATIONS;
}

}
