package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NumericFeature;


/*
 * This feature is: the distance between the two NPs in terms of number of sentences
 */

public class SentNum
    extends NumericFeature {

private static int MAX_SENT_DISTANCE = 10;

public SentNum() {
  name = this.getClass().getSimpleName();
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  int sent1 = reconcile.features.properties.SentNum.getValue(np1, doc);
  int sent2 = reconcile.features.properties.SentNum.getValue(np2, doc);
  int distance = Math.abs(sent1 - sent2);
  distance = distance > MAX_SENT_DISTANCE ? MAX_SENT_DISTANCE : distance;
  return Integer.toString(distance);
}

}
