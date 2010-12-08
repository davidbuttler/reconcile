package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;


/*
 * This feature is: C if the two NP's compatible values for GENDER, NUMBER, and do not have incompatible values for
 * CONTRAINDICES, ANIMACY, PRONOUN and CONTAINSPN I otherwise
 */

public class Constraints
    extends NominalFeature {

public Constraints() {
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
  if (!Constructor.createFeature("Gender").getValue(np1, np2, doc, featVector).equals(COMPATIBLE)
      && !featVector.get(Constructor.createFeature("Gender")).equals(SAME)) return INCOMPATIBLE;
  if (!Constructor.createFeature("Number").getValue(np1, np2, doc, featVector).equals(COMPATIBLE)) return INCOMPATIBLE;
  if (Constructor.createFeature("Contraindices").getValue(np1, np2, doc, featVector).equals(INCOMPATIBLE))
    return INCOMPATIBLE;
  if (Constructor.createFeature("Span").getValue(np1, np2, doc, featVector).equals(INCOMPATIBLE)) return INCOMPATIBLE;
  if (Constructor.createFeature("Animacy").getValue(np1, np2, doc, featVector).equals(INCOMPATIBLE))
    return INCOMPATIBLE;
  if (Constructor.createFeature("Pronoun").getValue(np1, np2, doc, featVector).equals(INCOMPATIBLE))
    return INCOMPATIBLE;
  if (Constructor.createFeature("ContainsPN").getValue(np1, np2, doc, featVector).equals(INCOMPATIBLE))
    return INCOMPATIBLE;

  return COMPATIBLE;
}

}
