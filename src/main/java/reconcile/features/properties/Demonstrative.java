package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;


public class Demonstrative
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new Demonstrative(true, true);
  }
  return ref;
}

public static Boolean getValue(Annotation np, Document doc)
{
  return (Boolean) getInstance().getValueProp(np, doc);
}

private Demonstrative(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  Boolean value;
  String[] w = doc.getWords(np);
  if (w == null || w.length <= 0) {
    value = false;
  }
  else {
    value = FeatureUtils.memberArray(w[0], FeatureUtils.DEMONSTRATIVES);
  }
  return value;
}

}
