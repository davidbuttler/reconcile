package reconcile.features.properties;

import java.util.HashMap;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.NPSemTypeEnum;
import reconcile.general.Constants;
import reconcile.general.CustomDate;


public class NPSemanticType
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new NPSemanticType(false, true);
  }
  return ref;
}

public static NPSemTypeEnum getValue(Annotation np, Document doc)
{
  return (NPSemTypeEnum) getInstance().getValueProp(np, doc);
}

private NPSemanticType(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  NPSemTypeEnum value = null;
  String annotText = doc.getAnnotText(np);

  if (FeatureUtils.isPronoun(np, doc)) {
    value = NPSemTypeEnum.UNKNOWN;
  }
  else if (Conjunction.getValue(np, doc)) {
    value = NPSemTypeEnum.UNKNOWN;
    // get the conjuncts
    HashMap<Annotation, Boolean> isContained = new HashMap<Annotation, Boolean>();
    AnnotationSet interm = doc.getAnnotationSet(Constants.NP).getContained(np);

    for (Annotation n : interm) {
      if (np.properCovers(n)) {
        for (Annotation p : interm.getContained(n)) {
          if (n.properCovers(p)) {
            isContained.put(p, true);
          }
        }
      }
    }
    boolean first = true;
    for (Annotation a : interm) {
      if (np.properCovers(a) && !isContained.containsKey(a)) {
        NPSemTypeEnum type = NPSemanticType.getValue(a, doc);
        if (first || !value.equals(type)) {
          value = NPSemTypeEnum.UNKNOWN;
          break;
        }
        first = false;
      }
    }

  }
  else {
    Annotation ne = null;
    if (ProperName.getValue(np, doc)) {
      ne = (Annotation) Property.LINKED_PROPER_NAME.getValueProp(np, doc);
    }
    else {
      Annotation hn = HeadNoun.getValue(np, doc);
      AnnotationSet nes = doc.getAnnotationSet(Constants.NE).getOverlapping(hn);
      if (nes != null) {
        for (Annotation n : nes) {
          if (!n.getType().equalsIgnoreCase("Person") && !n.getType().equalsIgnoreCase("Organization")) {
            ne = n;
            break;
          }
        }
      }
    }
    if (FeatureUtils.isDate(annotText)) {
      value = NPSemTypeEnum.DATE;
      np.setProperty(Property.DATE, CustomDate.getDate(annotText));
    }
    else {
      if (ne == null) {
        String[] wnType = WNSemClass.getValue(np, doc);
        if (wnType == null || wnType.length < 1) {
          value = NPSemTypeEnum.UNKNOWN;
        }
        else {
          value = NPSemTypeEnum.UNKNOWN;
          String entType;
          for (int i = 0; i < wnType.length && i < 2 && NPSemTypeEnum.UNKNOWN.equals(value); i++) {
            entType = wnType[i];
            if (entType.equalsIgnoreCase("PERSON")) {
              value = NPSemTypeEnum.PERSON;
            }
            else if (entType.equalsIgnoreCase("ORGANIZATION")) {
              value = NPSemTypeEnum.ORGANIZATION;
            }
            else if (entType.equalsIgnoreCase("TIME")) {
              value = NPSemTypeEnum.TIME;
            }
            else if (entType.equalsIgnoreCase("DATE")) {
              value = NPSemTypeEnum.DATE;
            }
            else if (entType.equalsIgnoreCase("LOCATION")) {
              value = NPSemTypeEnum.LOCATION;
            }
            else if (entType.equalsIgnoreCase("MONEY")) {
              value = NPSemTypeEnum.MONEY;
            }
            else if (entType.equalsIgnoreCase("NUMBER")) {
              value = NPSemTypeEnum.NUMBER;
            }
            else if (entType.equalsIgnoreCase("GPE"))
              return NPSemTypeEnum.GPE;
            else if (entType.equalsIgnoreCase("FAC"))
              return NPSemTypeEnum.FAC;
            else if (entType.equalsIgnoreCase("VEHICLE"))
              return NPSemTypeEnum.VEHICLE;
            else if (entType.equalsIgnoreCase("PERCENTAGE") || entType.equalsIgnoreCase("PERCENT")) {
              value = NPSemTypeEnum.PERCENTAGE;
            }
            else {
              value = NPSemTypeEnum.UNKNOWN;
            }
          }
        }

      }
      else {
        if (FeatureUtils.memberArray(doc.getWords(np)[0], FeatureUtils.PERSON_PREFIXES)) {
          value = NPSemTypeEnum.PERSON;
        }

        Annotation entity = ne;// .getFirst();
        String entType = entity.getType();
        if (entType.equalsIgnoreCase("PERSON")) {
          value = NPSemTypeEnum.PERSON;
        }
        else if (entType.equalsIgnoreCase("ORGANIZATION")) {
          value = NPSemTypeEnum.ORGANIZATION;
        }
        else if (entType.equalsIgnoreCase("TIME")) {
          value = NPSemTypeEnum.TIME;
        }
        else if (entType.equalsIgnoreCase("DATE")) {
          value = NPSemTypeEnum.DATE;
        }
        else if (entType.equalsIgnoreCase("LOCATION")) {
          value = NPSemTypeEnum.LOCATION;
        }
        else if (entType.equalsIgnoreCase("MONEY")) {
          value = NPSemTypeEnum.MONEY;
        }
        else if (entType.equalsIgnoreCase("NUMBER")) {
          value = NPSemTypeEnum.NUMBER;
        }
        else if (entType.equalsIgnoreCase("GPE"))
          return NPSemTypeEnum.GPE;
        else if (entType.equalsIgnoreCase("FAC"))
          return NPSemTypeEnum.FAC;
        else if (entType.equalsIgnoreCase("VEHICLE"))
          return NPSemTypeEnum.VEHICLE;
        else if (entType.equalsIgnoreCase("PERCENTAGE") || entType.equalsIgnoreCase("PERCENT")) {
          value = NPSemTypeEnum.PERCENTAGE;
        }
        else {
          value = NPSemTypeEnum.UNKNOWN;
        }
      }
    }
  }

  return value;

}

}
