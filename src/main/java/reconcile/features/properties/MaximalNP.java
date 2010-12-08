package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;


public class MaximalNP
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new MaximalNP(true, true);
  }
  return ref;
}

public static Annotation getValue(Annotation np, Document doc)
{
  return (Annotation) getInstance().getValueProp(np, doc);
}

private MaximalNP(boolean whole, boolean cached) {
  super(whole, cached);
}

public void maxNPAnnotate(AnnotationSet nps, String text)
{
  // Find all maximal NPs
  for (Annotation np : nps) {
    // Check if this NP has not yet been annotated. If it hasn't, then
    // it is either maximal or its parent is
    // still to be processed
    if (np.getProperty(this) == null) {
      np.setProperty(this, np);
      AnnotationSet contained = nps.getContained(np);
      for (Annotation c : contained) {
        c.setProperty(this, np);
      }
    }
  }
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  AnnotationSet nps = doc.getAnnotationSet(Constants.NP);
  maxNPAnnotate(nps, doc.getText());
  return np.getProperty(this);
}

}
