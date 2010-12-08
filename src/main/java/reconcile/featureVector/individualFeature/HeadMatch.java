package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.HeadNoun;


/*
 * This feature is: C if the two np's have the same head noun I if they don't \
 */

public class HeadMatch
    extends NominalFeature {

public HeadMatch() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return IC;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  String hn1 = doc.getAnnotText(HeadNoun.getValue(np1, doc));
  String hn2 = doc.getAnnotText(HeadNoun.getValue(np2, doc));

  if (hn1.equalsIgnoreCase(hn2)) return COMPATIBLE;
  return INCOMPATIBLE;
}

}
