package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NumericFeature;


/*
 * This feature is: the beginning offset of the first np
 */

public class ID1BeginOffset
    extends NumericFeature {

public ID1BeginOffset() {
  name = this.getClass().getSimpleName();
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  return Integer.toString(np1.getStartOffset());
}

}
