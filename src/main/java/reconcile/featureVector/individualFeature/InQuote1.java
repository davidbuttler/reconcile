package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.InQuote;


/*
 * This feature is: Y if the first NP is part of quoted string N otherwise
 * 
 * Not the most efficient implementation;
 */

public class InQuote1
    extends NominalFeature {

public InQuote1() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return YN;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  Integer inq = InQuote.getValue(np1, doc);
  if (inq.intValue() > 0)
    return "Y";
  else
    return "N";
}

}
