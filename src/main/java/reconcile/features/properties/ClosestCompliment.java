package reconcile.features.properties;

import java.util.HashMap;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;


public class ClosestCompliment
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new ClosestCompliment(true, true);
  }
  return ref;
}

public static Annotation getValue(Annotation np, Document doc)
{
  return (Annotation) getInstance().getValueProp(np, doc);
}

private ClosestCompliment(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  AnnotationSet nps = doc.getAnnotationSet(Constants.NP);
  // Keep track of the closest comp for each semantic class
  Annotation zero = new Annotation(0, -1, -1, "nill");
  HashMap<String, Annotation> last = new HashMap<String, Annotation>();
  for (Annotation an : nps) {
    String[] types = WNSemClass.getValue(an, doc);
    String type = "unknwn";
    if (types.length > 0) {
      type = types[0];
    }
    if (last.get(type) != null) {
      an.setProperty(this, last.get(type));
    }
    else {
      an.setProperty(this, zero);
    }
    last.put(type, an);
  }
  return np.getProperty(this);
}

}
