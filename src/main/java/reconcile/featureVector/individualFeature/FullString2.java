package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.StringFeature;


/*
 * The full string of the second np
 */

public class FullString2
    extends StringFeature {

public FullString2() {
  name = this.getClass().getSimpleName();
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  return doc.getAnnotText(np2).replaceAll("\\n", " ");
}

}
