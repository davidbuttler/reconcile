package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.GenderEnum;
import reconcile.features.FeatureUtils.NPSemTypeEnum;


public class Gender
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new Gender(true, true);
  }
  return ref;
}

public static GenderEnum getValue(Annotation np, Document doc)
{
  return (GenderEnum) getInstance().getValueProp(np, doc);
}

private Gender(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  GenderEnum value;
  value = GenderEnum.UNKNOWN;
  GenderEnum orgGender = GenderEnum.NEUTER;
  String npStr = doc.getAnnotText(np);
  // First, handle pronouns
  if (FeatureUtils.isPronoun(np, doc)) {
    if (FeatureUtils.memberArray(npStr, FeatureUtils.FEMALE_PRONOUN_LIST)) {
      value = GenderEnum.FEMININE;
    }
    else if (FeatureUtils.memberArray(npStr, FeatureUtils.MALE_PRONOUN_LIST)) {
      value = GenderEnum.MASC;
    }
    else if (FeatureUtils.memberArray(npStr, FeatureUtils.NEITHER_PRONOUN_LIST)) {
      value = GenderEnum.NEUTER;
    }
    else if (FeatureUtils.memberArray(npStr, FeatureUtils.UNKNOWN_PRONOUN_LIST)) {
      value = GenderEnum.EITHER;
    }
    else {
      value = GenderEnum.EITHER;
    }
  }
  else {
    NPSemTypeEnum PNtype = ProperNameType.getValue(np, doc);
    NPSemTypeEnum type = NPSemanticType.getValue(np, doc);
    String[] wnTypes = WNSemClass.getValue(np, doc);
    String[] words = doc.getWords(np);
    // Now NPs that contain person ne's
    if (PNtype != null && PNtype.equals(NPSemTypeEnum.PERSON)) {
      // System.err.println(getText(np, text));
      // AnnotationSet nes = annotations.get(Constants.NE).getOverlapping(np);
      Annotation ne = null;
      if (ProperName.getValue(np, doc)) {
        ne = (Annotation) np.getProperty(Property.LINKED_PROPER_NAME);
      }
      // if(nes!=null)
      // ne = nes.getLast();
      if (ne != null) {
        words = doc.getWords(ne);
      }
      if (Conjunction.getValue(np, doc)) {
        value = GenderEnum.EITHER;
      }
      else if (FeatureUtils.memberArray(words[0], FeatureUtils.MALE_PERSON_PREFIXES)) {
        value = GenderEnum.MASC;
      }
      else if (FeatureUtils.memberArray(words[0], FeatureUtils.FEMALE_PERSON_PREFIXES)) {
        value = GenderEnum.FEMININE;
      }
      else {
        boolean male = FeatureUtils.isMaleName(words[0]);
        boolean female = FeatureUtils.isFemaleName(words[0]);
        if (words.length > 2) {
          male = male || FeatureUtils.isMaleName(words[words.length - 2]);
        }
        if (words.length > 2) {
          female = female || FeatureUtils.isFemaleName(words[words.length - 2]);
        }
        if (male && female) {
          value = GenderEnum.EITHER;
        }
        else if (male) {
          value = GenderEnum.MASC;
        }
        else if (female) {
          value = GenderEnum.FEMININE;
        }
        else {
          value = GenderEnum.EITHER;
        }
      }
      // System.err.println(value);
    }
    else if (type.equals(NPSemTypeEnum.PERSON)) {
      if (Conjunction.getValue(np, doc)) {
        value = GenderEnum.EITHER;
      }
      else if (FeatureUtils.memberArray(words[0], FeatureUtils.MALE_PERSON_PREFIXES)) {
        value = GenderEnum.MASC;
      }
      else if (FeatureUtils.memberArray(words[0], FeatureUtils.FEMALE_PERSON_PREFIXES)) {
        value = GenderEnum.FEMININE;
      }
      else if (FeatureUtils.memberArray("male", WNSemClass.getValue(np, doc))) {
        if (FeatureUtils.memberArray("female", WNSemClass.getValue(np, doc))) {
          value = GenderEnum.EITHER;
        }
        else {
          value = GenderEnum.MASC;
        }
      }
      else if (FeatureUtils.memberArray("female", WNSemClass.getValue(np, doc))) {
        value = GenderEnum.FEMININE;
      }
      else {
        Annotation head = HeadNoun.getValue(np, doc);
        String txt = doc.getAnnotText(head);
        if (txt.endsWith("women") || txt.endsWith("woman")) {
          System.out.println("Found " + txt);
          value = GenderEnum.FEMININE;
        }
        else if (txt.endsWith("men") || txt.endsWith("man")) {
          value = GenderEnum.EITHER;
        }
        else {
          value = GenderEnum.EITHER;
        }
      }
    }
    else if (type.equals(NPSemTypeEnum.ORGANIZATION)) {
      value = orgGender;
    }
    else if (FeatureUtils.memberArray("male", WNSemClass.getValue(np, doc))) {
      if (FeatureUtils.memberArray("female", WNSemClass.getValue(np, doc))) {
        value = GenderEnum.EITHER;
      }
      else {
        value = GenderEnum.MASC;
      }
    }
    else if (FeatureUtils.memberArray("female", WNSemClass.getValue(np, doc))) {
      value = GenderEnum.FEMININE;
    }
    else if (wnTypes.length > 0) {
      value = GenderEnum.NEUTER;
      String wnType = wnTypes[0];
      if (wnType.equalsIgnoreCase("person")) {
        value = GenderEnum.EITHER;
      }
      else if (wnType.equalsIgnoreCase("animal")) {
        value = GenderEnum.NEUTER;
      }
      else if (wnType.equalsIgnoreCase("organization")) {
        value = orgGender;
      }
      else if (wnType.equalsIgnoreCase("group")) {
        value = orgGender;
      }
    }
    /*if(memberArray("person", wnTypes)){
    					value = Gender.EITHER;
    		}else if(memberArray("animal", wnTypes))
    			value = Gender.NEUTER;
    		else if(memberArray("organization", wnTypes))
    			value = orgGender;
    		else if(memberArray("group", wnTypes))
    			value = orgGender;
    		else if(wnTypes.length>0)
    			value = Gender.NEUTER;*/
  }

  return value;
}

}
