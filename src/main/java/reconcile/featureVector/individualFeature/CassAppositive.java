package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.NPSemTypeEnum;
import reconcile.features.FeatureUtils.NumberEnum;
import reconcile.features.properties.Conjunction;
import reconcile.features.properties.HeadNoun;
import reconcile.features.properties.MaximalNP;
import reconcile.features.properties.NPSemanticType;
import reconcile.features.properties.Number;
import reconcile.features.properties.ProperNameType;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;


/*
 * The value of this feature is: C if the two NP's are in appositive construction I otherwise
 */
public class CassAppositive
    extends NominalFeature {

public CassAppositive() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return IC;
}

private boolean isBracketed(Annotation np, Document doc)
{
  if (!doc.getAnnotString(np.getStartOffset() - 1, np.getStartOffset() + 1).contains("(")) return false;
  if (!doc.getAnnotString(np.getEndOffset() - 1, np.getEndOffset() + 1).contains(")")) return false;
  // System.out.println("bracketed: "+Utils.getAnnotText(np.getStartOffset()-1, np.getEndOffset()+1,text));
  return true;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featMap)
{
  if (!FeatureUtils.sameSentence(np1, np2, doc)) return INCOMPATIBLE;
  AnnotationSet parParse = doc.getAnnotationSet(Constants.PARTIAL_PARSE);
  // if both are locations, then false -- avoid construct such as New York, NY
  FeatureUtils.NPSemTypeEnum type1 = ProperNameType.getValue(np1, doc);
  FeatureUtils.NPSemTypeEnum type2 = ProperNameType.getValue(np2, doc);
  if (type1 != null && type2 != null && type1.equals(FeatureUtils.NPSemTypeEnum.LOCATION)
      && type2.equals(FeatureUtils.NPSemTypeEnum.LOCATION)) return INCOMPATIBLE;
  FeatureUtils.NPSemTypeEnum nptype1 = NPSemanticType.getValue(np1, doc);
  FeatureUtils.NPSemTypeEnum nptype2 = NPSemanticType.getValue(np2, doc);

  if ((FeatureUtils.NPSemTypeEnum.DATE.equals(nptype1) && !FeatureUtils.NPSemTypeEnum.DATE.equals(nptype2))
      || (!FeatureUtils.NPSemTypeEnum.DATE.equals(nptype1) && FeatureUtils.NPSemTypeEnum.DATE.equals(nptype2)))
    return INCOMPATIBLE;

  if (Conjunction.getValue(np1, doc) && !Number.getValue(np2, doc).equals(NumberEnum.PLURAL)) return INCOMPATIBLE;
  // if((Boolean)FeatureUtils.CONJUNCTION.getValue(np2, annotations, text)&&!FeatureUtils.getNumber(np1, annotations,
  // text).equals(NumberEnum.PLURAL))
  // return INCOMPATIBLE;

  if (reconcile.features.properties.MaximalNP.getValue(np1, doc).properCovers(np1)
      || MaximalNP.getValue(np2, doc).properCovers(np2)) return INCOMPATIBLE;
  Annotation first = np1.getEndOffset() < np2.getStartOffset() ? np1 : np2;
  Annotation second = np1.getEndOffset() < np2.getStartOffset() ? np2 : np1;
  AnnotationSet between = parParse.getContained(first.getEndOffset(), second.getStartOffset());
  if (between != null && between.size() == 1) {
    Annotation mid = between.getFirst();
    Annotation parent1 = SyntaxUtils.getParent(SyntaxUtils.getNode(first, parParse), parParse);
    while (parent1.getType().startsWith("N")) {
      parent1 = SyntaxUtils.getParent(parent1, parParse);
    }
    Annotation parent2 = SyntaxUtils.getParent(SyntaxUtils.getNode(second, parParse), parParse);
    while (parent2.getType().startsWith("N")) {
      parent2 = SyntaxUtils.getParent(parent2, parParse);
    }
    if ((mid.getType().equals("CMA") || doc.getAnnotText(mid).equals("_") || doc.getAnnotText(mid).equals("-"))
        && (parent1.equals(parent2) || !parent2.getType().startsWith("C"))) // System.out.println(parent1);
      // System.out.println(parent2);
      // System.out.println("NAppositive: ["+doc.getAnnotString(np1)+"] and ["+doc.getAnnotString(np2)+"]");
      return COMPATIBLE;
  }
  // if(gr1.equals("APPOS")||gr1.equals("conj")){
  // Annotation hn2 = HeadNoun.getValue(np2, doc);
  // Annotation hn1 = HeadNoun.getValue(np1, doc);
  // AnnotationSet dep1 = dep.getContained(hn1);
  // for(Annotation d: dep1){
  // String type = d.getType();
  // while(type.equalsIgnoreCase("conj")){
  // String[] span = ((String)d.getAttribute("GOV")).split("\\,");
  // int stSpan = Integer.parseInt(span[0]);
  // int endSpan = Integer.parseInt(span[1]);
  // d = dep.getContained(stSpan, endSpan).getFirst();
  // if(d==null)
  // return INCOMPATIBLE;
  // type = d.getType();
  // }
  // if(type.equalsIgnoreCase("APPOS")){
  // String[] span = ((String)d.getAttribute("GOV")).split("\\,");
  //					
  // int stSpan = Integer.parseInt(span[0]);
  // int endSpan = Integer.parseInt(span[1]);
  // if(hn2.getStartOffset()<=stSpan && endSpan <= hn2.getEndOffset()){
  // np1.setProperty(Property.APPOSITIVE, np2);
  // np2.setProperty(Property.APPOSITIVE, np1);
  // System.out.println("Appositive: ["+doc.getAnnotString(np1)+"] and ["+doc.getAnnotString(np2)+"]");
  // return COMPATIBLE;
  // }
  // }
  // }
  // }
  // if(gr2.equals("APPOS")||gr2.equals("conj")){
  // Annotation hn1 = HeadNoun.getValue(np1, doc);
  // Annotation hn2 = HeadNoun.getValue(np2, doc);
  // AnnotationSet dep2 = dep.getContained(hn2);
  // for(Annotation d: dep2){
  // String type = d.getType();
  // while(type.equalsIgnoreCase("conj")){
  // String[] span = ((String)d.getAttribute("GOV")).split("\\,");
  // int stSpan = Integer.parseInt(span[0]);
  // int endSpan = Integer.parseInt(span[1]);
  // d = dep.getContained(stSpan, endSpan).getFirst();
  // if(d==null)
  // return INCOMPATIBLE;
  // type = d.getType();
  // }
  // if(type.equalsIgnoreCase("APPOS")){
  // String[] span = ((String)d.getAttribute("GOV")).split("\\,");
  //					
  // int stSpan = Integer.parseInt(span[0]);
  // int endSpan = Integer.parseInt(span[1]);
  // if(hn1.getStartOffset()<=stSpan && endSpan <= hn1.getEndOffset()){
  // np1.setProperty(Property.APPOSITIVE, np2);
  // np2.setProperty(Property.APPOSITIVE, np1);
  // System.out.println("Appositive: ["+doc.getAnnotString(np1)+"] and ["+doc.getAnnotString(np2)+"]");
  // return COMPATIBLE;
  // }
  // }
  // }
  // }

  // Check for np1(np2) condition
  if (!(NPSemTypeEnum.LOCATION.equals(type1) || NPSemTypeEnum.LOCATION.equals(type2)) && isBracketed(np2, doc)) {
    if (np1.getEndOffset() < np2.getStartOffset() && HeadNoun.getValue(np1, doc).getEndOffset() == np1.getEndOffset()) {
      if (doc.getAnnotString(np1.getEndOffset(), np2.getStartOffset()).matches("\\W*")) {
        // System.out.println("NAppos: "+Utils.getAnnotText(np1, text)+" - "+Utils.getAnnotText(np2, text));
        System.out.println("Appositive: [" + doc.getAnnotString(np1) + "] and [" + doc.getAnnotString(np2) + "]");
        return COMPATIBLE;
      }
    }
    else {
      if (np1.getStartOffset() < np2.getStartOffset() && np1.getEndOffset() == np2.getEndOffset()) {
        // System.out.println("NAppos: "+Utils.getAnnotText(np1, text)+" - "+Utils.getAnnotText(np2, text));
        System.out.println("Appositive: [" + doc.getAnnotString(np1) + "] and [" + doc.getAnnotString(np2) + "]");
        return COMPATIBLE;
      }
    }
  }
  return INCOMPATIBLE;
}

}
