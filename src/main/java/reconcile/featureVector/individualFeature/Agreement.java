package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;


/*
 * This feature is: C the 2 np's agree in both gender and number I if they disagree on either gender or number NA if
 * number or gender information for either np cannot be determined
 */

public class Agreement
    extends NominalFeature {

public Agreement() {
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
  String gender = Constructor.createFeature("Gender").getValue(np1, np2, doc, featVector);
  String number = Constructor.createFeature("Number").getValue(np1, np2, doc, featVector);
  String animacy = Constructor.createFeature("Animacy").getValue(np1, np2, doc, featVector);
  if ((gender.equals(COMPATIBLE) || gender.equals(SAME)) && number.equals(COMPATIBLE) && animacy.equals(COMPATIBLE))
    return COMPATIBLE;
  else if (gender.equals(NA) || number.equals(NA) || animacy.equals(NA)) return NA;
  return INCOMPATIBLE;
}

}
