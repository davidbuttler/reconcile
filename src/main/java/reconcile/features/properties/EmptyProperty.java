package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.Document;

public class EmptyProperty
    extends Property {

public EmptyProperty(String name, boolean whole, boolean cached) {
  super(whole, cached);
  this.name = name;
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  return np.getProperty(this);
}

}
