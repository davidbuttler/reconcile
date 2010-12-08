package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NumericFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.HeadNoun;


public class WordNetSense
    extends NumericFeature {

public WordNetSense() {
  name = this.getClass().getSimpleName();
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{

  Annotation head1 = HeadNoun.getValue(np1, doc);
  Annotation head2 = HeadNoun.getValue(np2, doc);

  String h1 = doc.getAnnotString(head1).toLowerCase();
  String h2 = doc.getAnnotString(head2).toLowerCase();
  int value = WN_SENSE_MAX;
  /* Checking to ensure the head nouns differ. */
  if (h1.equalsIgnoreCase(h2)) {
    value = 1;
  }
  else if (FeatureUtils.isPronoun(np1, doc) || FeatureUtils.isPronoun(np2, doc)) {
    value = WN_SENSE_MAX;
  }
  else {
    try {
      int sense = FeatureUtils.getWNSense(np1, np2, doc);
      int sense1 = FeatureUtils.getWNSense(np2, np1, doc);
      // System.err.println(np1.getAttribute(Constants.CE_ID)+","+np2.getAttribute(Constants.CE_ID)+":"+sense+"-"+sense1);
      value = sense < sense1 ? sense1 : sense;
    }
    catch (Exception ex) {
      System.err.println(ex);
      ex.printStackTrace();
    }
  }

  // To qwell compiler warnings.
  return Double.toString(value / (double) WN_SENSE_MAX);
}
}
