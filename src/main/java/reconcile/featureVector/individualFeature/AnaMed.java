package reconcile.featureVector.individualFeature;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NumericFeature;
import reconcile.features.FeatureUtils;


/*
 * This feature is: (m - med(np1,np2))/m, where m = len(np1), med is the minimum edit distance this feature is due to
 * Strube et al. 2002
 */

public class AnaMed
    extends NumericFeature {

private NumberFormat nf;

public AnaMed() {
  name = this.getClass().getSimpleName();
  nf = new DecimalFormat("#.0000");
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  String s1 = doc.getAnnotString(np1);
  String s2 = doc.getAnnotString(np2);
  return nf.format(FeatureUtils.medMeasure(s1, s2));
}

}
