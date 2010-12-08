package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;


public class Conjunction
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new Conjunction(true, true);
  }
  return ref;
}

public static Boolean getValue(Annotation np, Document doc)
{
  return (Boolean) getInstance().getValueProp(np, doc);
}

private Conjunction(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  Boolean value = false;
  Annotation head = HeadNoun.getValue(np, doc);
  AnnotationSet nps = doc.getAnnotationSet(Constants.NP);

  AnnotationSet contained = nps.getContained(np.getStartOffset(), head.getEndOffset());
  if (contained.size() > 0 && np.getStartOffset() < head.getStartOffset()) {
    String words[] = FeatureUtils.getWords(doc.getAnnotText(np.getStartOffset(), head.getStartOffset()));
    value = FeatureUtils.memberArray("and", words);
  }
  return value;
}

}
