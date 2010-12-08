package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;


public class ProperNoun
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new ProperNoun(false, true);
  }
  return ref;
}

public static Boolean getValue(Annotation np, Document doc)
{
  return (Boolean) getInstance().getValueProp(np, doc);
}

private ProperNoun(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  boolean pn = false;
  if (ProperName.getValue(np, doc)) {
    pn = true;
  }
  else {
    AnnotationSet pos = doc.getAnnotationSet(Constants.POS);
    AnnotationSet cont = pos.getContained(HeadNoun.getValue(np, doc));
    for (Annotation a : cont)
      if (a.getType().startsWith("NNP")) {
        pn = true;
      }
  }
  return pn;
}

}
