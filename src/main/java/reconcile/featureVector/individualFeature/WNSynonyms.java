package reconcile.featureVector.individualFeature;

import java.util.Map;

import net.didion.jwnl.data.Synset;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.HeadNoun;
import reconcile.features.properties.Synsets;

public class WNSynonyms
    extends NominalFeature {

public WNSynonyms() {
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
  Annotation head1 = HeadNoun.getValue(np1, doc);
  Annotation head2 = HeadNoun.getValue(np2, doc);

  // Converting everything to lower case if it hasn't been already.
  String h1 = doc.getAnnotString(head1).toLowerCase();
  String h2 = doc.getAnnotString(head2).toLowerCase();

  if (h1.equals(h2)) return COMPATIBLE;

  Synset[] syn1 = Synsets.getValue(np1, doc);
  Synset[] syn2 = Synsets.getValue(np2, doc);

  if (syn1 != null && syn2 != null) {
    for (Synset s1 : syn1) {
      for (Synset s2 : syn2) {
        if (s1.equals(s2)) return COMPATIBLE;
      }
    }
  }
  return INCOMPATIBLE;
}
}
