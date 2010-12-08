package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;


/*
 * This feature is: I if the second NP is an indefinite and is not an appositive C otherwise
 */

public class Indefinite1
    extends NominalFeature {

public Indefinite1() {
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
  if (featVector.get(Constructor.createFeature(Constants.APPOSITIVE)).equals(COMPATIBLE)) return COMPATIBLE;
  if (FeatureUtils.articleType(np2, doc).equals(FeatureUtils.ArticleTypeEnum.INDEFINITE)) return INCOMPATIBLE;
  return COMPATIBLE;
}

}
