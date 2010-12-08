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
 * This feature is: (n - med(np1,np2))/n, where n = len(np2), med is the minimum edit distance this feature is due to
 * Strube et al. 2002
 */

public class AnteMed
    extends NumericFeature {

private NumberFormat nf;

public AnteMed() {
  name = this.getClass().getSimpleName();
  nf = new DecimalFormat("#.0000");
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  String s1 = doc.getAnnotString(np1);
  String s2 = doc.getAnnotString(np2);
  return nf.format(FeatureUtils.medMeasure(s2, s1));
}

}
