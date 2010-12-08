package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;


public class ContainsProperName
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new ContainsProperName(true, true);
  }
  return ref;
}

public static Annotation getValue(Annotation np, Document doc)
{
  return (Annotation) getInstance().getValueProp(np, doc);
}

private ContainsProperName(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  AnnotationSet NE = doc.getAnnotationSet(Constants.NE);
  AnnotationSet overlap = NE.getOverlapping(np);

  Annotation contains = new Annotation(-1, -1, -1, "nil");
  if (overlap != null && !overlap.isEmpty()) {
    Annotation an = overlap.getLast();
    if (an.getEndOffset() <= np.getEndOffset()) {
      contains = an;
    }
  }
  return contains;
}

}
