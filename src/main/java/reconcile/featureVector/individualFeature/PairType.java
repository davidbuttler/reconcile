package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.Definite;
import reconcile.features.properties.ProperName;


/*
 * Encodes the type of the pair
 */

public class PairType
    extends NominalFeature {

public PairType() {
  name = this.getClass().getSimpleName();
}

static String[] npTypes = { "n", "p", "d", "i" };

@Override
public String[] getValues()
{
  String[] result = new String[npTypes.length * npTypes.length];
  int count = 0;
  for (String t1 : npTypes) {
    for (String t2 : npTypes) {
      result[count++] = t1 + t2;
    }
  }

  return result;
}

String getType(Annotation np, Document doc)
{
  if (ProperName.getValue(np, doc)) return "n";
  if (FeatureUtils.isPronoun(np, doc)) return "p";
  if (Definite.getValue(np, doc)) return "d";
  return "i";
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{

  return getType(np1, doc) + getType(np2, doc);
}

}
