package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.NumberEnum;
import reconcile.features.properties.HeadNoun;
import reconcile.features.properties.Number;
import reconcile.features.properties.WNSemClass;


/*
 * This feature is: C if the two NPs form the pattern <sum> of <money> (e.g. loss of 1 million) I otherwise
 */

public class Quantity
    extends NominalFeature {

public Quantity() {
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
  if (!FeatureUtils.sameSentence(np1, np2, doc)) return INCOMPATIBLE;
  if (FeatureUtils.isPronoun(np1, doc) || FeatureUtils.isPronoun(np2, doc)) return INCOMPATIBLE;
  Annotation first, second;
  if (np1.getStartOffset() <= np2.getStartOffset()) {
    first = np1;
    second = np2;
  }
  else {
    first = np2;
    second = np1;
  }

  if (!inOfRel(first, second, doc, featVector)) return INCOMPATIBLE;
  // System.out.println("Of rel:"+FeatureUtils.getText(np1, text)+" -- "+FeatureUtils.getText(np2, text));
  String[] wnClass = WNSemClass.getValue(first, doc);
  // for(String s:wnClass)
  // System.out.print(s+", ");
  // System.out.println();
  if (wnClass != null
      && (FeatureUtils.memberArray("number", wnClass) || FeatureUtils.memberArray("statistic", wnClass) || FeatureUtils
          .memberArray("measure", wnClass)) && Number.getValue(second, doc).equals(NumberEnum.PLURAL))
    return COMPATIBLE;
  if (wnClass == null
      || !(FeatureUtils.memberArray("sum", wnClass) || FeatureUtils.memberArray("statistic", wnClass) || FeatureUtils
          .memberArray("transferred_property", wnClass))) return INCOMPATIBLE;
  String[] wnClass2 = WNSemClass.getValue(second, doc);
  // for(String s:wnClass2)
  // System.out.print(s+", ");
  // System.out.println();
  if (wnClass2 == null
      || !(FeatureUtils.memberArray("money", wnClass2) || FeatureUtils.memberArray("sum", wnClass2) || FeatureUtils
          .memberArray("percent", wnClass2))) return INCOMPATIBLE;
  // System.out.println("Quantity: "+FeatureUtils.getText(np1, text)+" "+FeatureUtils.getText(np2, text));
  return COMPATIBLE;
}

private boolean inOfRel(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  if (!FeatureUtils.sameSentence(np1, np2, doc)) return false;
  Annotation head = HeadNoun.getValue(np1, doc);
  if (head.getEndOffset() >= np2.getStartOffset()) return false;
  String inBetween = doc.getAnnotString(head.getEndOffset(), np2.getStartOffset());
  if (inBetween.matches("\\W*of\\W*")) return true;
  return false;
}
}
