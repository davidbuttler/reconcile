package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.HeadNoun;
import reconcile.features.properties.NPSemanticType;


public class WordNetClass
    extends NominalFeature {

public WordNetClass() {
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
  Annotation head1 = HeadNoun.getValue(np1, doc);
  Annotation head2 = HeadNoun.getValue(np2, doc);

  // Converting everything to lower case if it hasn't been already.
  String h1 = doc.getAnnotText(head1).toLowerCase();
  String h2 = doc.getAnnotText(head2).toLowerCase();

  if (h1.equals(h2)) return COMPATIBLE;
  int sameSemClass = FeatureUtils.sameSemanticClass(np1, np2, doc);
  if (sameSemClass == 2)
    return COMPATIBLE;
  else if (sameSemClass == 1) {
    if ("COMPATIBLE".equals(Constructor.createFeature("Subclass").getValue(np1, np2, doc, featVector)))
      return COMPATIBLE;
    // Special case -- organizations can "be" persons
    FeatureUtils.NPSemTypeEnum type1 = NPSemanticType.getValue(np1, doc);
    FeatureUtils.NPSemTypeEnum type2 = NPSemanticType.getValue(np2, doc);
    if (type1.equals(FeatureUtils.NPSemTypeEnum.ORGANIZATION) && type2.equals(FeatureUtils.NPSemTypeEnum.PERSON))
      return NA;
    if (type2.equals(FeatureUtils.NPSemTypeEnum.ORGANIZATION) && type1.equals(FeatureUtils.NPSemTypeEnum.PERSON))
      return NA;
    // if(Double.parseDouble(AllFeatures.makeFeature("WordNetDist").getValue(np1, np2, annotations, text,
    // featVector))<.5)
    // return NA;
    return INCOMPATIBLE;
  }
  else
    return NA;
}
}
