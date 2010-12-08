package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;


public class Stopword
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new Stopword(false, true);
  }
  return ref;
}

public static Boolean getValue(Annotation np, Document doc)
{
  return (Boolean) getInstance().getValueProp(np, doc);
}

private Stopword(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  String str = doc.getAnnotString(np);
  return FeatureUtils.isStopword(str);
}

}
