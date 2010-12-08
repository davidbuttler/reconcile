package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.Document;

/*
 * A simple class used to save some attributes of properties of single nps
 */
public abstract class Property {

String name;
boolean whole;
boolean cached;
public static final Property AUTHOR = new EmptyProperty("Author", true, true);

protected Property() {
  this.name = getClass().getSimpleName();
  whole = false;
  this.cached = true;
}

public Property(boolean annotateWhole, boolean cached) {
  this.name = getClass().getSimpleName();
  whole = annotateWhole;
  this.cached = cached;
}

public boolean annotateWholeDocument()
{
  /*
   * For some properties is just much more efficient for the document
   * to be annotated as a whole
   */
  return whole;
}

public Object getValueProp(Annotation np, Document doc)
{
  if (cached) {
    Object result = np.getProperty(this);
    if (result == null) {
      result = produceValue(np, doc);
      np.setProperty(this, result);
    }
    return result;
  }
  else
    return produceValue(np, doc);
}

abstract protected Object produceValue(Annotation np, Document doc);

@Override
public String toString()
{
  return name;
}

public static final Property MATCHED_CE = new EmptyProperty("matched_ce", true, true);
public static final Property HEAD_POS = new EmptyProperty("head_pos", true, true);
public static final Property PRO_ANTES = new EmptyProperty("pro_antes", true, true);
public static final Property RULE_COREF_ID = new EmptyProperty("rule_coref_id", true, true);
public static final Property PREDNOM = new EmptyProperty("prednom", false, true);
public static final Property APPOSITIVE = new EmptyProperty("appositive", false, true);
public static final Property DATE = new EmptyProperty("DATE", false, true);
public static final Property LINKED_PROPER_NAME = new EmptyProperty("LinkedPN", false, true);
public static final Property COMP_AUTHOR = new EmptyProperty("CAuthor", true, true);
}
