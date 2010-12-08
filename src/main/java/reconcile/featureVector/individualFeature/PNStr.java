package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.ProperName;
import reconcile.features.properties.Property;


/*
 * This feature is: C if both NPs are proper names and the same string I otherwise
 */

public class PNStr
    extends NominalFeature {

public PNStr() {
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
  if (!ProperName.getValue(np1, doc) || !ProperName.getValue(np2, doc)) return NA;
  Annotation ne1 = (Annotation) np1.getProperty(Property.LINKED_PROPER_NAME);
  Annotation ne2 = (Annotation) np2.getProperty(Property.LINKED_PROPER_NAME);
  // String[] words1 = FeatureUtils.getWords(np1, text);
  // String[] words2 = FeatureUtils.getWords(np2, text);
  String[] words1 = doc.getWords(ne1);
  String[] words2 = doc.getWords(ne2);
  // AnnotationSet NE = annotations.get(Constants.NE);
  // AnnotationSet pos = annotations.get(Constants.POS);
  // NPSemTypeEnum t1 = FeatureUtils.getPropNameType(np1, doc);
  // NPSemTypeEnum t2 = FeatureUtils.getPropNameType(np2, doc);

  if (FeatureUtils.equalsIgnoreCase(words1, words2)) return COMPATIBLE;
  String[] infWords1 = FeatureUtils.removeUninfWordsLeaveCorpDesign(words1);
  String[] infWords2 = FeatureUtils.removeUninfWordsLeaveCorpDesign(words2);
  if (FeatureUtils.equalsIgnoreCase(infWords1, infWords2)) return COMPATIBLE;

  // if(t1.equals(NPSemTypeEnum.ORGANIZATION)&&t2.equals(NPSemTypeEnum.ORGANIZATION)){
  // infWords1 = FeatureUtils.removeWords(words1, FeatureUtils.CORP_DESIGN);
  // infWords2 = FeatureUtils.removeWords(words2, FeatureUtils.CORP_DESIGN);
  // if(FeatureUtils.equalsIgnoreCase(infWords1, words2)||FeatureUtils.equalsIgnoreCase(words1, infWords2))
  // return COMPATIBLE;
  // }
  return INCOMPATIBLE;
}

}
