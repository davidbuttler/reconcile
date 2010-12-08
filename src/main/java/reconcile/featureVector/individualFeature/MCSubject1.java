package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.properties.GramRole;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;


/*
 * This feature is: Y if the first NP is subject of the main clause N otherwise
 * 
 * Not the most efficient implementation;
 */

public class MCSubject1
    extends NominalFeature {

public MCSubject1() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return YN;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  boolean sub = GramRole.getValue(np1, doc).equals("SUBJECT");
  return sub && SyntaxUtils.isMainClause(np1, doc.getAnnotationSet(Constants.PARSE)) ? "Y" : "N";
}

}
