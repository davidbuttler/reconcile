package reconcile.general;

/*
 * A simple class used to save some attributes of properties of single nps
 */
public class Property {

String name;
boolean whole;

public boolean annotateWholeDocument()
{
  /*
   * For some properties is just much more efficient for the document
   * to be annotated as a whole
   */
  return whole;
}

public Property(String name, boolean annotateWhole) {
  this.name = name;
  whole = annotateWhole;
}

@Override
public String toString()
{
  return name;
}
}
