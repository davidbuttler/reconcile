package reconcile.features.properties;


import net.didion.jwnl.data.Synset;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.AnimacyEnum;
import reconcile.features.FeatureUtils.NPSemTypeEnum;

public class Animacy
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new Animacy(true, true);
  }
  return ref;
}

public static AnimacyEnum getValue(Annotation np, Document doc)
{
  return (AnimacyEnum) getInstance().getValueProp(np, doc);
}

private Animacy(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  AnimacyEnum value;
  if (FeatureUtils.isPronoun(np, doc)) {
    if (FeatureUtils.memberArray(doc.getAnnotText(np), FeatureUtils.UN_ANIMATE_PRONOUN_LIST)) {
      value = AnimacyEnum.UNANIMATE;
    }
    else if (FeatureUtils.memberArray(doc.getAnnotText(np), FeatureUtils.UNKNOWN_PRONOUN_LIST)) {
      value = AnimacyEnum.UNKNOWN;
    }
    else {
      value = AnimacyEnum.ANIMATE;
    }
  }
  else {
    NPSemTypeEnum type = NPSemanticType.getValue(np, doc);
    if (type == NPSemTypeEnum.PERSON) {
      value = AnimacyEnum.ANIMATE;
    }
    else if (type == NPSemTypeEnum.ORGANIZATION) {
      value = AnimacyEnum.UNANIMATE;
    }
    else if (type == NPSemTypeEnum.UNKNOWN) {
      if (FeatureUtils.getTitles().contains(doc.getAnnotText(HeadNoun.getValue(np, doc)).toLowerCase())) {
        value = AnimacyEnum.ANIMATE;
      }
      else {
        value = AnimacyEnum.UNKNOWN;

        String[] wnTypes = WNSemClass.getValue(np, doc);
        if (wnTypes != null && wnTypes.length > 0) {
          String wnType = wnTypes[0];
          if (wnType.equalsIgnoreCase("group") || wnType.equalsIgnoreCase("organization")) {
            value = AnimacyEnum.UNANIMATE;
          }
          else if (wnType.equalsIgnoreCase("person") || wnType.equalsIgnoreCase("animal")
              || wnType.equalsIgnoreCase("male") || wnType.equalsIgnoreCase("female")) {
            value = AnimacyEnum.ANIMATE;
          }
          else {
            Synset[] syns = Synsets.getValue(np, doc);
            if (syns != null && syns.length > 0) {
              if (FeatureUtils.isWNHypernym(syns[0], "life_form") > 0) {
                value = AnimacyEnum.ANIMATE;
              }
            }
            else {
              // System.out.println("No synsets for "+FeatureUtils.getText(np, text));
            }
            value = AnimacyEnum.UNANIMATE;
          }
        }
        else if (FeatureUtils.isNumeral(doc.getAnnotText(np))) {
          value = AnimacyEnum.UNANIMATE;
        }
        else {
          Synset[] syns = Synsets.getValue(np, doc);
          if (syns != null && syns.length > 0) {
            if (FeatureUtils.isWNHypernym(syns[0], "life_form") > 0) {
              value = AnimacyEnum.ANIMATE;
            }
            else if (FeatureUtils.isWNHypernym(syns[0], "object") > 0) {
              value = AnimacyEnum.UNANIMATE;
            }
          }
        }
      }
    }
    else {
      // All other noun types such as Location, Date, Percent, etc.
      value = AnimacyEnum.UNANIMATE;
    }
  }
  return value;
}

}
