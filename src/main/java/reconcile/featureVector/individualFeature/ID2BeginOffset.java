package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NumericFeature;


/*
 * This feature is: the beginning offset of the second np
 */

public class ID2BeginOffset
    extends NumericFeature {

public ID2BeginOffset() {
  name = this.getClass().getSimpleName();
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  return Integer.toString(np2.getStartOffset());
}

}
