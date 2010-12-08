package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.Document;

public class Definite
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new Definite(true, true);
  }
  return ref;
}

public static Boolean getValue(Annotation np, Document doc)
{
  return (Boolean) getInstance().getValueProp(np, doc);
}

private Definite(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  String[] words = doc.getWords(np);
  boolean def = false;

  if (words.length > 0 && "the".equalsIgnoreCase(words[0])) {
    def = true;
  }

  return def;
}

}
