package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.InQuote;


/*
 * This feature is: C if both NPs are part of a quoted string NA if exactly one is I otherwise
 */

public class BothInQuotes
    extends NominalFeature {

public BothInQuotes() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return ICN;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  boolean inq1 = InQuote.getValue(np1, doc) > 0;
  boolean inq2 = InQuote.getValue(np2, doc) > 0;

  if (inq1 && inq2) return COMPATIBLE;
  if (inq1 || inq2) return NA;
  return INCOMPATIBLE;
}

}
