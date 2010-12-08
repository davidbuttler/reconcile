package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;


public class AllGramRole
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new AllGramRole(true, true);
  }
  return ref;
}

public static String getValue(Annotation np, Document doc)
{
  return (String) getInstance().getValueProp(np, doc);
}

private AllGramRole(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  String value;
  Annotation hn = HeadNoun.getValue(np, doc);
  AnnotationSet dep = doc.getAnnotationSet(Constants.DEP).getContained(hn);
  value = "NONE";
  if (dep != null && dep.size() > 0) {
    value = dep.getFirst().getType();
  }
  return value;

}

}
