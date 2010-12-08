package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;


/*
 * This feature is: Y if the second NP in the pair is a pronoun N otherwise
 */

public class Pronoun2
    extends NominalFeature {

public Pronoun2() {
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
  // PRTypeEnum type = FeatureUtils.getPronounType(np2,annotations, text);
  if (FeatureUtils.isPronoun(np2, doc))
    return "Y";
  else
    return "N";
}

}
