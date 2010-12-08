/*
 * A common ancestor for all feature types
 */
package reconcile.featureVector;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;


public abstract class Feature {

protected String name;
protected boolean ignore = false;

public Feature() {
  name = getClass().getSimpleName();
}

public boolean isNominal()
{
  return false;
}

public boolean isNumeric()
{
  return false;
}

public boolean isString()
{
  return false;
}

public abstract String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector);

// A cached version of the produce value function
public String getValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  String val = featVector.get(this);
  if (val == null) {
    val = produceValue(np1, np2, doc, featVector);
  }
  featVector.put(this, val);
  return val;
}

public String getName()
{
  return name;
}

public boolean ignoreFeature()
{
  return ignore;
}
}
