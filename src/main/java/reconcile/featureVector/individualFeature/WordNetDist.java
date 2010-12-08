package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NumericFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.HeadNoun;


/*
 * @author nathan
 */
public class WordNetDist
    extends NumericFeature {

public WordNetDist() {
  name = this.getClass().getSimpleName();
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  int value = WN_MAX;
  if (FeatureUtils.isPronoun(np1, doc) || FeatureUtils.isPronoun(np2, doc)) {
    value = WN_MAX;
  }
  else {
    Annotation head1 = HeadNoun.getValue(np1, doc);
    Annotation head2 = HeadNoun.getValue(np2, doc);

    String h1 = doc.getAnnotText(head1).toLowerCase();
    String h2 = doc.getAnnotText(head2).toLowerCase();

    // Heads must differ.
    if (h1.equalsIgnoreCase(h2)) {
      value = 0;
    }
    else {
      try {
        value = FeatureUtils.wnDist(np1, np2, doc);
      }
      catch (Exception ex) {
        System.err.println(ex);
        ex.printStackTrace();
      }
    }
  }
  return new Double(value / (double) WN_MAX).toString();
}
}
