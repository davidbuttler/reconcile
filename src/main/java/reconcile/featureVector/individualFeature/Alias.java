package reconcile.featureVector.individualFeature;

import java.util.ArrayList;
import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.NPSemTypeEnum;
import reconcile.features.properties.ContainsAcronym;
import reconcile.features.properties.InfWords;
import reconcile.features.properties.NPSemanticType;
import reconcile.features.properties.ProperNameType;
import reconcile.features.properties.Property;
import reconcile.general.CustomDate;


/*
 * This feature is: C if one NP is an alias of the other I otherwise
 */

public class Alias
    extends NominalFeature {

public Alias() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return IC;
}

public static boolean isAcronym(String s1, String s2)
{
  boolean caps1 = FeatureUtils.isAllCaps(s1), caps2 = FeatureUtils.isAllCaps(s2);
  if ((!caps1 && !caps2) || (caps1 && caps2)) return false;
  String name, acr;
  if (caps1) {
    name = s2;
    acr = s1;
  }
  else {
    name = s1;
    acr = s2;
  }

  acr = acr.replaceAll("\\.|\\s|\\&", "");
  String[] words = FeatureUtils.getWords(name);
  // Three posible versions of the name string
  ArrayList<String> name1 = new ArrayList<String>(), name2 = new ArrayList<String>(), name3 = new ArrayList<String>();
  // System.err.println(acr+" "+name);
  for (String w : words) {
    if (w.length() > 1) {
      boolean inf = !FeatureUtils.isUninfWord(w);
      // System.err.println(w);
      boolean cap = FeatureUtils.isAllCaps(w.substring(0, 1));
      boolean corpDes = FeatureUtils.isCorpDesign(w);
      if (!corpDes && cap) {
        name1.add(w);
      }
      if (cap) {
        name2.add(w);
      }
      if (inf && cap && !corpDes) {
        name3.add(w);
      }
    }
  }
  if (isAcronym(acr, name1) || isAcronym(acr, name2) || isAcronym(acr, name3)) return true;
  return false;
}

public static boolean isAcronym(String acr, ArrayList<String> name)
{
  if (acr == null || acr.length() <= 1) return false;
  StringBuilder acr2 = new StringBuilder();
  for (String s : name) {
    if (s != null && s.length() > 0) {
      acr2.append(s.charAt(0));
    }
  }
  return acr.equalsIgnoreCase(acr2.toString());
}

public static boolean startsWith(String shortArray[], String longArray[])
{
  // True if the second string array starts with the first one
  if (shortArray == null || longArray == null) return false;
  int len1 = shortArray.length, len2 = longArray.length;
  if (len1 < 1 || len2 < 1 || len1 > len2) return false;
  for (int i = 0; i < len1; i++) {
    if (!shortArray[i].equalsIgnoreCase(longArray[i])) return false;
  }
  return true;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  if (np1.overlaps(np2)) return INCOMPATIBLE;
  // if(AllFeatures.makeFeature("WordNetClass").getValue(np1, np2, annotations, text, featVector).equals(INCOMPATIBLE))
  // return INCOMPATIBLE;
  if (FeatureUtils.isPronoun(np1, doc) || FeatureUtils.isPronoun(np2, doc)) return INCOMPATIBLE;
  NPSemTypeEnum type1 = ProperNameType.getValue(np1, doc);
  NPSemTypeEnum type2 = ProperNameType.getValue(np2, doc);
  NPSemTypeEnum semType1 = NPSemanticType.getValue(np1, doc);
  NPSemTypeEnum semType2 = NPSemanticType.getValue(np2, doc);

  String np1str = doc.getAnnotText(np1);
  String np2str = doc.getAnnotText(np2);
  Annotation linkedPN2 = (Annotation) Property.LINKED_PROPER_NAME.getValueProp(np2, doc);

  String[] words1 = doc.getWords(np1);
  Annotation linkedPN1 = (Annotation) Property.LINKED_PROPER_NAME.getValueProp(np1, doc);
  if (linkedPN1 != null) {
    words1 = doc.getWords(linkedPN1);
  }
  String[] words2 = doc.getWords(np2);
  if (linkedPN2 != null) {
    words2 = doc.getWords(linkedPN2);
  }

  // check for dates
  if (NPSemTypeEnum.DATE.equals(semType1) && NPSemTypeEnum.DATE.equals(semType2)) {
    CustomDate d1 = CustomDate.getDate(np1str);
    CustomDate d2 = CustomDate.getDate(np2str);
    if (d1 != null && d2 != null && d1.equals(d2)) return COMPATIBLE;
    return np1str.equalsIgnoreCase(np2str) ? COMPATIBLE : INCOMPATIBLE;
  }

  // Check money amounts
  if (NPSemTypeEnum.MONEY.equals(semType1) && NPSemTypeEnum.MONEY.equals(semType2))
    return np1str.equalsIgnoreCase(np2str) ? COMPATIBLE : INCOMPATIBLE;

  // check people
  if (NPSemTypeEnum.PERSON.equals(type1) || NPSemTypeEnum.PERSON.equals(type2)) {
    if (!FeatureUtils.memberArray("and", words1) && !FeatureUtils.memberArray("and", words2)
        && words1[words1.length - 1].equalsIgnoreCase(words2[words2.length - 1])
        && !Constructor.createFeature("Gender").getValue(np1, np2, doc, featVector).equals(NominalFeature.INCOMPATIBLE)) {
      String[] infWords1 = InfWords.getValue(np1, doc);
      String[] infWords2 = InfWords.getValue(np2, doc);
      if (infWords1.length > 1 && infWords2.length > 1)
        if (!infWords1[0].equalsIgnoreCase(infWords2[0])) return INCOMPATIBLE;
      return COMPATIBLE;
    }
  }

  // organizations
  if (NPSemTypeEnum.ORGANIZATION.equals(type1) && NPSemTypeEnum.ORGANIZATION.equals(type2)) {
    String[] inf1 = InfWords.getValue(np1, doc);
    String[] inf2 = InfWords.getValue(np2, doc);
    if (inf1 != null && inf1.length > 0 && inf2 != null && inf2.length > 0) {
      if (FeatureUtils.equalsIgnoreCase(inf1, inf2)) return COMPATIBLE;
    }
  }

  if (ContainsAcronym.getValue(np1, doc) || ContainsAcronym.getValue(np2, doc)) {
    // test for acronyms
    if (isAcronym(np1str, np2str)) return COMPATIBLE;
    if (!FeatureUtils.memberArray("and", words1) && isAcronym(words1[words1.length - 1], np2str)) return COMPATIBLE;
    if (!FeatureUtils.memberArray("and", words2) && isAcronym(np1str, words2[words2.length - 1])) return COMPATIBLE;
    if (isAcronym(words1[words1.length - 1], words2[words2.length - 1]) && !FeatureUtils.memberArray("and", words1)
        && !FeatureUtils.memberArray("and", words2)) return COMPATIBLE;
  }

  if (FeatureUtils.NPSemTypeEnum.ORGANIZATION.equals(type1) && type1.equals(type2)) {
    Annotation pn1 = (Annotation) np1.getProperty(Property.LINKED_PROPER_NAME);
    Annotation pn2 = (Annotation) np2.getProperty(Property.LINKED_PROPER_NAME);
    // if(pn1==null||pn2==null)
    // return INCOMPATIBLE;
    String[] wds1 = doc.getWords(pn1);
    String[] wds2 = doc.getWords(pn2);
    String[] inf1 = InfWords.getValue(np1, doc);
    String[] inf2 = InfWords.getValue(np2, doc);
    if (inf1 != null && inf1.length > 0 && FeatureUtils.equalsIgnoreCase(inf1, inf2)) return COMPATIBLE;
    np1str = doc.getAnnotText(pn1);
    np2str = doc.getAnnotText(pn2);
    if (ContainsAcronym.getValue(np1, doc) || ContainsAcronym.getValue(np2, doc)) {
      if (isAcronym(np1str, np2str)) return COMPATIBLE;
      if (isAcronym(words1[words1.length - 1], np2str) && !FeatureUtils.memberArray("and", words1)) return COMPATIBLE;
      if (isAcronym(np1str, words2[words2.length - 1]) && !FeatureUtils.memberArray("and", words2)) return COMPATIBLE;
      if (isAcronym(words1[words1.length - 1], words2[words2.length - 1]) && !FeatureUtils.memberArray("and", words1)
          && !FeatureUtils.memberArray("and", words2)) return COMPATIBLE;
      if ((startsWith(inf1, wds2) && !FeatureUtils.memberArray("and", wds2, inf1.length))
          || (startsWith(inf2, wds1) && !FeatureUtils.memberArray("and", wds1, inf2.length))) return COMPATIBLE;
    }
  }

  if (type1 != null && type2 != null && !type1.equals(NPSemTypeEnum.UNKNOWN) && !type2.equals(NPSemTypeEnum.UNKNOWN)
      && np1str.equalsIgnoreCase(np2str)) return COMPATIBLE;

  return INCOMPATIBLE;
}

}
