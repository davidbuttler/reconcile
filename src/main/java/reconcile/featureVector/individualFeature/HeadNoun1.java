package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.StringFeature;
import reconcile.features.properties.HeadNoun;


/*
 * The head noun of the first np
 */

public class HeadNoun1
    extends StringFeature {

public HeadNoun1() {
  name = this.getClass().getSimpleName();
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  Annotation head = HeadNoun.getValue(np1, doc);
  return doc.getAnnotText(head);
}

}
