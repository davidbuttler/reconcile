package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.general.Constants;


/*
 * This feature is: I if the two NP's have incompatible values for BINDING, CONTRAINDICES, SPAN, or MAXIMALNP C
 * otherwise
 */

public class Syntax
    extends NominalFeature {

public Syntax() {
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
  if (Constructor.createFeature(Constants.APPOSITIVE).getValue(np1, np2, doc, featVector).equals(COMPATIBLE)
      || Constructor.createFeature("Prednom").getValue(np1, np2, doc, featVector).equals(COMPATIBLE))
    return COMPATIBLE;
  if (Constructor.createFeature("Binding").getValue(np1, np2, doc, featVector).equals(INCOMPATIBLE))
    return INCOMPATIBLE;
  if (Constructor.createFeature("Contraindices").getValue(np1, np2, doc, featVector).equals(INCOMPATIBLE))
    return INCOMPATIBLE;
  // if(AllFeatures.makeFeature("Span").getValue(np1, np2, annotations, text, featVector).equals(INCOMPATIBLE))
  // return INCOMPATIBLE;
  // if(AllFeatures.makeFeature("MaximalNP").getValue(np1, np2, annotations, text, featVector).equals(INCOMPATIBLE))
  // return INCOMPATIBLE;

  return COMPATIBLE;
}

}
