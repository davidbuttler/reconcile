package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.Document;

public class SubsumesNumber
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new SubsumesNumber(false, true);
  }
  return ref;
}

public static Boolean getValue(Annotation np, Document doc)
{
  return (Boolean) getInstance().getValueProp(np, doc);
}

private SubsumesNumber(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  String str = doc.getAnnotString(np);
  boolean subs = false;
  String[] words = str.split("\\s");

  for (String cur : words) {
    if (cur.matches("\\d+")) {
      subs = true;
      break;
    }
  }
  return subs;
}

}
