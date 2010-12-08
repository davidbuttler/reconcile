package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.Embedded;


/*
 * This feature is: C if both NPs are embedded NA if exactly one is I otherwise
 */

public class BothEmbedded
    extends NominalFeature {

public BothEmbedded() {
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
  boolean emb1 = Embedded.getValue(np1, doc);
  boolean emb2 = Embedded.getValue(np2, doc);

  if (emb1 && emb2) return COMPATIBLE;
  if (emb1 || emb2) return NA;
  return INCOMPATIBLE;
}

}
