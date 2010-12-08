package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.HeadNoun;
import reconcile.features.properties.ProperName;


/*
 * @author nathan 6/8/2007
 * 
 * Is one NP's WordNet class a subclass of the other NP?
 */
public class Subclass
    extends NominalFeature {

public Subclass() {
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
  if (FeatureUtils.isPronoun(np1, doc) || FeatureUtils.isPronoun(np2, doc)) return INCOMPATIBLE;
  Annotation head1 = HeadNoun.getValue(np1, doc);
  Annotation head2 = HeadNoun.getValue(np2, doc);

  String h1 = doc.getAnnotString(head1).toLowerCase();
  String h2 = doc.getAnnotString(head2).toLowerCase();

  /* Checking to ensure the head nouns differ. */
  if (h1.equals(h2)) return COMPATIBLE;
  if (FeatureUtils.isPronoun(np1, doc) || FeatureUtils.isPronoun(np2, doc)) return INCOMPATIBLE;
  if (ProperName.getValue(np2, doc) || ProperName.getValue(np1, doc)) return INCOMPATIBLE;
  if (FeatureUtils.isSubclass(np1, np2, doc) || FeatureUtils.isSubclass(np2, np2, doc)) return COMPATIBLE;
  return INCOMPATIBLE;
}
}
