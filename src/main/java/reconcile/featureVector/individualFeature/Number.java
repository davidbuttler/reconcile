package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.NPSemTypeEnum;
import reconcile.features.properties.NPSemanticType;
import reconcile.features.properties.WNSemClass;


/*
 * This feature is: C the 2 np's agree in number I if they disagree NA if number information for either np cannot be
 * determined
 */

public class Number
    extends NominalFeature {

public Number() {
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
  FeatureUtils.NumberEnum num1 = reconcile.features.properties.Number.getValue(np1, doc);
  FeatureUtils.NumberEnum num2 = reconcile.features.properties.Number.getValue(np2, doc);
  // Special case -- singular organizations can take plural pronouns
  if (FeatureUtils.isPronoun(np1, doc)
      && (NPSemanticType.getValue(np2, doc).equals(NPSemTypeEnum.ORGANIZATION) || FeatureUtils.memberArray(
          "organization", WNSemClass.getValue(np2, doc))))
    if (num1.equals(FeatureUtils.NumberEnum.PLURAL)) return COMPATIBLE;
  if (FeatureUtils.isPronoun(np2, doc)
      && (NPSemanticType.getValue(np1, doc).equals(NPSemTypeEnum.ORGANIZATION) || FeatureUtils.memberArray(
          "organization", WNSemClass.getValue(np1, doc))))
    if (num2.equals(FeatureUtils.NumberEnum.PLURAL)) return COMPATIBLE;
  if (num1.equals(FeatureUtils.NumberEnum.UNKNOWN) || num2.equals(FeatureUtils.NumberEnum.UNKNOWN)) return NA;
  if (num1.equals(num2)) return COMPATIBLE;

  return INCOMPATIBLE;
}

}
