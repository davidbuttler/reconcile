package reconcile.features.properties;

import java.util.ArrayList;

import net.didion.jwnl.data.Synset;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;

public class WNSemClass
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new WNSemClass(false, true);
  }
  return ref;
}

public static String[] getValue(Annotation np, Document doc)
{
  return (String[]) getInstance().getValueProp(np, doc);
}

private WNSemClass(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  String[] value;
  ArrayList<String> vals = new ArrayList<String>();
  // Starting up Wordnet

  if (FeatureUtils.isPronoun(np, doc)) {
    String txt = doc.getAnnotText(np);
    if ((FeatureUtils.memberArray(txt, FeatureUtils.PLURAL_PRONOUNS) && !FeatureUtils.memberArray(txt,
        FeatureUtils.UN_ANIMATE_PRONOUN_LIST))
        || FeatureUtils.memberArray(txt, FeatureUtils.UNKNWNS)) {
      value = new String[] { "person", "organization", "group" };
    }
    else if (FeatureUtils.memberArray(txt, FeatureUtils.UN_ANIMATE_PRONOUN_LIST)) {
      value = new String[] { "group", "organization", "time", "date", "day", "money", "measure", "relation",
          "abstraction", "phenomenon", "state", "act", "psychological_feature", "event", "object", "quantity",
          "statistic", "artifact" };
    }
    else {
      value = new String[] { "person" };
    }
  }
  else {
    String annotText = doc.getAnnotText(np);
    ProperNameType.getValue(np, doc);
    Annotation ne = (Annotation) Property.LINKED_PROPER_NAME.getValueProp(np, doc);
    // annotations.get(Constants.NE).getOverlapping(hn);
    if (FeatureUtils.isDate(annotText)) {
      vals.add("date");
      vals.add("time");
      vals.add("time_period");
    }
    else {
      if (ne != null && !ne.equals(Annotation.getNullAnnot())) {
        if (FeatureUtils.memberArray(doc.getWords(np)[0], FeatureUtils.PERSON_PREFIXES)) {
          vals.add("person");
        }
        // Annotation entity = ne.getFirst();
        String entType = ne.getType().toLowerCase();
        vals.add(entType);
        if (entType.equals("date")) {
          vals.add("time");
          vals.add("time_period");
        }
        if (entType.equals("money")) {
          vals.add("quantity");
          vals.add("sum");
        }
        if (entType.equals("percentage")) {
          vals.add("statistic");
        }
      }
      else {
        Synset[] synset = Synsets.getValue(np, doc);
        if (synset == null || synset.length < 1) return new String[0];
        try {
          for (int i = 0; i < synset.length && i < 5; i++) {
            // if (isWNHypernym(synset[0],SUPERTYPE_SYNSETS[count])
            // > 0)
            // Only use the first 5 senses
            for (String type : FeatureUtils.SUPERTYPES) {
              if (FeatureUtils.isWNHypernym(synset[i], type) >= 0) {
                if (!vals.contains(type)) {
                  vals.add(type);
                }
              }
            }
          }
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
    /*
    if(vals.contains("person")&&FeatureUtils.getNumber(np, annotations, text).equals(NumberEnum.PLURAL)){
    	if(!vals.contains("group"))
    		vals.add("group");
    	if(!vals.contains("organization"))
    		vals.add("organization");
    }
    */
    value = vals.toArray(new String[0]);
  }

  if (value == null) {
    value = new String[0];
  }
  return value;
}

}
