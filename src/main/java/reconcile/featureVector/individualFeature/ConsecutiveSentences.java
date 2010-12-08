package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;


/*
 * This feature is: C if the NPs are in consecutive sentences I otherwise
 */

public class ConsecutiveSentences
    extends NominalFeature {

public ConsecutiveSentences() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return IC;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  if (Constructor.createFeature("SentNum").getValue(np1, np2, doc, featVector).equals("1"))
    return COMPATIBLE;
  else
    return INCOMPATIBLE;
}

}
