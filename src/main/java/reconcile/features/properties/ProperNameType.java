package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.NPSemTypeEnum;
import reconcile.general.Constants;


public class ProperNameType
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new ProperNameType(false, true);
  }
  return ref;
}

public static NPSemTypeEnum getValue(Annotation np, Document doc)
{
  return (NPSemTypeEnum) getInstance().getValueProp(np, doc);
}

private ProperNameType(boolean whole, boolean cached) {
  super(whole, cached);
}

public static NPSemTypeEnum getPropNameType(Annotation np, Document doc)
{
  AnnotationSet NE = doc.getAnnotationSet(Constants.NE);
  Annotation hn = HeadNoun.getValue(np, doc);

  AnnotationSet overlapAll = NE.getOverlapping(np.getStartOffset(), hn.getEndOffset());

  if (overlapAll == null || overlapAll.isEmpty()) return null;

  if (Conjunction.getValue(np, doc)) return null;

  if (FeatureUtils.isDate(doc.getAnnotText(np))) return NPSemTypeEnum.DATE;

  AnnotationSet overlap = overlapAll;

  /*
  = new AnnotationSet(Constants.NE);
  for(Annotation a:overlapAll){
  	String enamex = a.getAttribute("ENAMEX");
  	if(enamex==null || !enamex.equals("false"))
  		overlap.add(a);
  }*/

  if (overlap == null || overlap.isEmpty()) return null;

  Annotation neAnnot = overlap.getLast();

  // System.err.println("NP "+Utils.getAnnotText(np, text)+" ne "+Utils.getAnnotText(neAnnot, text));
  // make sure the two have the same head
  Annotation neHn = HeadNoun.getValue(neAnnot, doc);

  if (hn.compareSpan(neHn) != 0) return null;

  String[] wds = doc.getWords(np.getStartOffset(), hn.getEndOffset());

  if (wds == null || wds.length < 1) return null;

  if (overlap.size() > 1 && FeatureUtils.memberArray("and", wds)) return null;

  if (overlap.size() == 1) {
    Annotation ne = overlap.getFirst();
    if (ne.getStartOffset() < np.getStartOffset())
      if (FeatureUtils.memberArray("and", doc.getWords(ne.getStartOffset(), np.getStartOffset()))) return null;
  }

  np.setProperty(Property.LINKED_PROPER_NAME, neAnnot);
  neAnnot.setProperty(Property.LINKED_PROPER_NAME, np);

  if (FeatureUtils.memberArray(wds[0], FeatureUtils.PERSON_PREFIXES)) return NPSemTypeEnum.PERSON;

  String entType = neAnnot.getType();
  if (entType.equalsIgnoreCase("PERSON"))
    return NPSemTypeEnum.PERSON;
  else if (entType.equalsIgnoreCase("ORGANIZATION") || entType.equalsIgnoreCase("COMPANY"))
    return NPSemTypeEnum.ORGANIZATION;
  else if (entType.equalsIgnoreCase("TIME"))
    return NPSemTypeEnum.TIME;
  else if (entType.equalsIgnoreCase("DATE"))
    return NPSemTypeEnum.DATE;
  else if (entType.equalsIgnoreCase("LOCATION"))
    return NPSemTypeEnum.LOCATION;
  else if (entType.equalsIgnoreCase("MONEY"))
    return NPSemTypeEnum.MONEY;
  else if (entType.equalsIgnoreCase("NUMBER"))
    return NPSemTypeEnum.NUMBER;
  else if (entType.equalsIgnoreCase("GPE"))
    return NPSemTypeEnum.GPE;
  else if (entType.equalsIgnoreCase("FAC"))
    return NPSemTypeEnum.FAC;
  else if (entType.equalsIgnoreCase("VEHICLE"))
    return NPSemTypeEnum.VEHICLE;
  else if (entType.equalsIgnoreCase("PERCENTAGE") || entType.equalsIgnoreCase("PERCENT"))
    return NPSemTypeEnum.PERCENTAGE;

  return null;
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  NPSemTypeEnum propNameType = getPropNameType(np, doc);
  return propNameType;
}

}
