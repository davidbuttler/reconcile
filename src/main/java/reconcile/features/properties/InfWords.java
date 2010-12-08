package reconcile.features.properties;

import java.util.ArrayList;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;


public class InfWords
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new InfWords(false, true);
  }
  return ref;
}

public static String[] getValue(Annotation np, Document doc)
{
  return (String[]) getInstance().getValueProp(np, doc);
}

private InfWords(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  ArrayList<String> result = new ArrayList<String>();
  String[] words = doc.getWords(np);
  for (String w : words) {

    if (!FeatureUtils.isUninfWord(w.toLowerCase()) && !(FeatureUtils.isAlphabetStr(w) || "I".endsWith(w))
        && !FeatureUtils.isCorpDesign(w)) {
      result.add(w);
    }
  }

  return result.toArray(new String[0]);
}

}
