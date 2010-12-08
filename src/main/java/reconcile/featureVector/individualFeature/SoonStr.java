package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.SoonWords;
import reconcile.features.properties.Stopword;
import reconcile.features.properties.SubsumesNumber;


/*
 * This feature is: C if, after discarding determiners the strings of the two NPs match I otherwise
 */

public class SoonStr
    extends NominalFeature {

public SoonStr() {
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
  // if(AllFeatures.makeFeature("WordNetClass").getValue(np1, np2, annotations, text, featVector).equals(INCOMPATIBLE))
  // return INCOMPATIBLE;
  if (FeatureUtils.isPronoun(np1, doc) || FeatureUtils.isPronoun(np2, doc)) return INCOMPATIBLE;
  if (Stopword.getValue(np1, doc) || Stopword.getValue(np2, doc)) return INCOMPATIBLE;
  if (SubsumesNumber.getValue(np1, doc) || SubsumesNumber.getValue(np2, doc)) return INCOMPATIBLE;
  String[] infWords1 = SoonWords.getValue(np1, doc);
  String[] infWords2 = SoonWords.getValue(np2, doc);
  if (FeatureUtils.equalsIgnoreCase(infWords1, infWords2))
    return COMPATIBLE;
  else
    return INCOMPATIBLE;
}
}
