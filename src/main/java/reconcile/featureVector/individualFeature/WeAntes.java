package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.PersonPronounTypeEnum;
import reconcile.features.properties.Number;
import reconcile.features.properties.Property;


/*
 * This feature is: Y if the first NP is part of quoted string N otherwise
 * 
 * Not the most efficient implementation;
 */

public class WeAntes
    extends NominalFeature {

public WeAntes() {
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
  if (FeatureUtils.isPronoun(np1, doc) && FeatureUtils.getPronounPerson(doc.getAnnotText(np1)) == PersonPronounTypeEnum.FIRST
      && FeatureUtils.NumberEnum.PLURAL.equals(Number.getValue(np1, doc))) {
    Annotation author = (Annotation) Property.COMP_AUTHOR.getValueProp(np1, doc);
    if (author != null && np2.equals(author)) return COMPATIBLE;
  }
  if (FeatureUtils.isPronoun(np2, doc) && FeatureUtils.getPronounPerson(doc.getAnnotText(np2)) == PersonPronounTypeEnum.FIRST
      && FeatureUtils.NumberEnum.PLURAL.equals(Number.getValue(np2, doc))) {
    Annotation author = (Annotation) Property.COMP_AUTHOR.getValueProp(np2, doc);
    if (author != null && np1.equals(author)) return COMPATIBLE;
  }
  return INCOMPATIBLE;

}

}
