package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NumericFeature;


/*
 * This feature is: the number of the document from which the nps came.
 */

public class DocNo
    extends NumericFeature {

public static final String ID = "docNo";

public DocNo() {
  name = this.getClass().getSimpleName();
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  AnnotationSet docNo = doc.getAnnotationSet(ID);
  for (Annotation num : docNo) {
  Map<String, String> f = num.getFeatures();
  if (f == null) throw new NullPointerException("no features for docNo annotation");
  return num.getAttribute(ID);
  }
  throw new NullPointerException("no elements in docNo annotation set");

}

}
