package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NumericFeature;
import reconcile.general.Constants;


/*
 * This feature is: the number of the document from which the nps came.
 */

public class ID1
    extends NumericFeature {

public ID1() {
  name = this.getClass().getSimpleName();
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  String id1 = np1.getAttribute(Constants.CE_ID);
  if (id1 == null) throw new RuntimeException("No id for " + np1.toString());
  return id1;
}

}
