package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.NPSemTypeEnum;
import reconcile.features.properties.Gender;
import reconcile.features.properties.NPSemanticType;
import reconcile.features.properties.Number;
import reconcile.features.properties.WNSemClass;


/*
 * This feature is: C the two np's match in animacy I if they don't NA if animacy information for either cannot be
 * determined
 */

public class Animacy
    extends NominalFeature {

public Animacy() {
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
  FeatureUtils.AnimacyEnum an1 = reconcile.features.properties.Animacy.getValue(np1, doc);
  FeatureUtils.AnimacyEnum an2 = reconcile.features.properties.Animacy.getValue(np2, doc);
  // Special case -- unanimate organizations can take animate pronouns
  // Special case -- an organization can be refered in different ways
  if (NPSemanticType.getValue(np2, doc).equals(NPSemTypeEnum.ORGANIZATION)
      || FeatureUtils.memberArray("organization", WNSemClass.getValue(np2, doc))) {
    if (FeatureUtils.isPronoun(np1, doc))
      if (Number.getValue(np1, doc).equals(FeatureUtils.NumberEnum.SINGLE)
          || Gender.getValue(np1, doc).equals(FeatureUtils.GenderEnum.NEUTER))
        return COMPATIBLE;
      else
        return INCOMPATIBLE;
    else
    // Not pronoun --can be coreferent with other both animate and unanimate np's
    if (an1.equals(FeatureUtils.AnimacyEnum.UNKNOWN))
      return NA;
    else
      return COMPATIBLE;
  }
  if (NPSemanticType.getValue(np1, doc).equals(NPSemTypeEnum.ORGANIZATION)
      || FeatureUtils.memberArray("organization", WNSemClass.getValue(np1, doc))) {
    if (FeatureUtils.isPronoun(np2, doc))
      if (!Number.getValue(np2, doc).equals(FeatureUtils.NumberEnum.SINGLE)
          || Gender.getValue(np2, doc).equals(FeatureUtils.GenderEnum.NEUTER))
        return COMPATIBLE;
      else
        return INCOMPATIBLE;
    else
    // Not pronoun --can be coreferent with other both animate and unanimate np's
    if (an2.equals(FeatureUtils.AnimacyEnum.UNKNOWN))
      return NA;
    else
      return COMPATIBLE;
  }

  if (an1.equals(FeatureUtils.AnimacyEnum.UNKNOWN) || an2.equals(FeatureUtils.AnimacyEnum.UNKNOWN)) return NA;
  if (an1.equals(an2)) return COMPATIBLE;
  return INCOMPATIBLE;
}

}
