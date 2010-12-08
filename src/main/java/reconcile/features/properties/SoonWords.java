package reconcile.features.properties;

import java.util.ArrayList;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;


public class SoonWords
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new SoonWords(false, true);
  }
  return ref;
}

public static String[] getValue(Annotation np, Document doc)
{
  return (String[]) getInstance().getValueProp(np, doc);
}

private SoonWords(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  String[] w = FeatureUtils.removeDeterminers(doc.getWords(np));
  ArrayList<String> result = new ArrayList<String>();

  for (String s : w) {
    if (!FeatureUtils.isAlphabetStr(s) || !s.equalsIgnoreCase("I")) {
      result.add(s);
    }
  }

  return result.toArray(new String[0]);
}

}
