package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.Property;
import reconcile.general.RuleResolvers;


/*
 * This feature is: C if the two NPs are coreferent according to a rule-based algorithm I otherwise
 */

public class RuleResolve
    extends NominalFeature {

public RuleResolve() {
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
  Integer corId1 = (Integer) np1.getProperty(Property.RULE_COREF_ID);
  if (corId1 == null) {
    RuleResolvers.ruleResolve(doc);
    corId1 = (Integer) np1.getProperty(Property.RULE_COREF_ID);
  }
  Integer corId2 = (Integer) np2.getProperty(Property.RULE_COREF_ID);
  if (corId1.equals(corId2)) return COMPATIBLE;
  return INCOMPATIBLE;
}

}
