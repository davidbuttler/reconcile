package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.PRTypeEnum;


public class Pronoun
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new Pronoun(false, true);
  }
  return ref;
}

public static PRTypeEnum getValue(Annotation np, Document doc)
{
  return (PRTypeEnum) getInstance().getValueProp(np, doc);
}

private Pronoun(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  PRTypeEnum value;
  String[] words = doc.getWords(np);
  if (words.length == 1) {
    value = FeatureUtils.getPronounType(words[0]);
  }
  else {
    value = PRTypeEnum.NONE;
  }
  return value;
}

}
