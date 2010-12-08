package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.features.FeatureUtils.NPSemTypeEnum;


public class ProperName
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new ProperName(false, true);
  }
  return ref;
}

public static Boolean getValue(Annotation np, Document doc)
{
  return (Boolean) getInstance().getValueProp(np, doc);
}

private ProperName(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  NPSemTypeEnum propNameType = ProperNameType.getValue(np, doc);
  boolean isPN = (propNameType != null)
      && (propNameType.equals(NPSemTypeEnum.PERSON) || propNameType.equals(NPSemTypeEnum.ORGANIZATION) || propNameType
          .equals(NPSemTypeEnum.LOCATION));
  return isPN;
}

}
