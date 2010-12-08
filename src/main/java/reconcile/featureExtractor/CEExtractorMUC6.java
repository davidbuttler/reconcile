package reconcile.featureExtractor;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.SyntaxUtils;


public class CEExtractorMUC6
    extends CEExtractor {

public boolean isNoun(Annotation a, String text)
{
  if (a == null) return false;
  String type = a.getType();
  if (type.startsWith("NN") || type.startsWith("PRP")) return true;
  return false;
}

public boolean isNP(Annotation an, String text)
{
  try {
    String type = an.getType();

    if (FeatureUtils.memberArray(type, SyntaxUtils.NPType)) return true;
  }
  catch (NullPointerException npe) {
    return false;
  }

  return false;
}
public boolean addNE(Annotation a, AnnotationSet includedCEs, AnnotationSet baseCEs, Document doc){
	return true;
}

}
