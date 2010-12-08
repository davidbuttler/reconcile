package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.Embedded;
import reconcile.features.properties.Property;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;


/*
 * This feature is: I if the two NP's violate conditions B or C in Chomsky's binding theory C otherwise
 */

public class Binding
    extends NominalFeature {

public Binding() {
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
  String np1str = doc.getAnnotText(np1);
  String np2str = doc.getAnnotText(np2);

  if (!FeatureUtils.sameSentence(np1, np2, doc)) // if(FeatureUtils.isReflexive(np1str)||FeatureUtils.isReflexive(np2str))
    // return INCOMPATIBLE;
    return COMPATIBLE;
  AnnotationSet parse = doc.getAnnotationSet(Constants.PARSE);
  Annotation parse1 = SyntaxUtils.getNode(np1, parse);
  Annotation parse2 = SyntaxUtils.getNode(np2, parse);

  // Check for condition B violation
  // a pronoun is free in its local domain
  if (Constructor.createFeature(Constants.APPOSITIVE).getValue(np1, np2, doc, featVector).equals(COMPATIBLE))
    return COMPATIBLE;
  Annotation ap1 = (Annotation) Property.APPOSITIVE.getValueProp(np1, doc);
  Annotation ap2 = (Annotation) Property.APPOSITIVE.getValueProp(np2, doc);
  if (ap1 != null && !ap1.equals(Annotation.getNullAnnot()) && ap1.equals(ap2)) return COMPATIBLE;
  if (FeatureUtils.isPronoun(np1, doc) && !FeatureUtils.isReflexive(np1str)
      && SyntaxUtils.dominates(SyntaxUtils.localDomain(parse1, parse), parse2, parse)
      && SyntaxUtils.cCommands(parse2, parse1, parse) && !Embedded.getValue(np1, doc)) return INCOMPATIBLE;
  if (FeatureUtils.isPronoun(np2, doc) && !FeatureUtils.isReflexive(np2str)
      && SyntaxUtils.dominates(SyntaxUtils.localDomain(parse2, parse), parse1, parse)
      && SyntaxUtils.cCommands(parse1, parse2, parse) && !Embedded.getValue(np2, doc)) return INCOMPATIBLE;
  // Check for condition C violation:
  // an R-expression cannot be bound within its local domain
  if (Constructor.createFeature("Prednom").getValue(np1, np2, doc, featVector).equals(NominalFeature.INCOMPATIBLE)) {
    if (SyntaxUtils.isRExpression(np1str) /*&&!FeatureUtils.isDefinite(np1, annotations, text)*/
        && SyntaxUtils.cCommands(parse2, parse1, parse) && !Embedded.getValue(np1, doc)) return INCOMPATIBLE;
    if (SyntaxUtils.isRExpression(np2str) /*&&!FeatureUtils.isDefinite(np2, annotations, text)*/
        && SyntaxUtils.cCommands(parse1, parse2, parse) && !Embedded.getValue(np2, doc)) return INCOMPATIBLE;
  }
  return COMPATIBLE;
}

}
