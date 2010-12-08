package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.general.RuleResolvers;


/*
 * This feature is: C if one NP is a pronoun and the other NP is its antecedent according to a rule-based algorithm I
 * otherwise
 */

public class ProResolve
    extends NominalFeature {

public ProResolve() {
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
  if (FeatureUtils.isPronoun(np2, doc)) {
    Annotation ant = RuleResolvers.getPronounAntecedentDoNotResolve(np2, doc);
    if (ant != null && ant.equals(np1)) return COMPATIBLE;
  }
  if (FeatureUtils.isPronoun(np1, doc)) {
    Annotation ant = RuleResolvers.getPronounAntecedentDoNotResolve(np1, doc);
    if (ant != null && ant.equals(np2)) return COMPATIBLE;
  }
  return INCOMPATIBLE;
}

}
