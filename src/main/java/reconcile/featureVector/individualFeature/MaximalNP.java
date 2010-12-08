package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.general.Constants;


/*
 * This feature is: I if the two NP's have the same maximal NP projection C otherwise
 */

public class MaximalNP
    extends NominalFeature {

public MaximalNP() {
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
  Annotation maxNP1 = reconcile.features.properties.MaximalNP.getValue(np1, doc);
  Annotation maxNP2 = reconcile.features.properties.MaximalNP.getValue(np2, doc);

  // System.err.println(Utils.getAnnotText(np2, text)+" -> "+Utils.getAnnotText(maxNP2, text));

  if (maxNP1.compareSpan(maxNP2) == 0) {
    if (Constructor.createFeature("Prednom").getValue(np1, np2, doc, featVector).equals(COMPATIBLE)) return COMPATIBLE;
    if (Constructor.createFeature(Constants.APPOSITIVE).getValue(np1, np2, doc, featVector).equals(COMPATIBLE))
      return COMPATIBLE;
    if (Constructor.createFeature("Quantity").getValue(np1, np2, doc, featVector).equals(COMPATIBLE))
      return COMPATIBLE;
    return INCOMPATIBLE;
  }
  return COMPATIBLE;
}

}
