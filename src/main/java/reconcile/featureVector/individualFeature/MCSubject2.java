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
 * This feature is: Y if the second NP is the subject of the main clause N otherwise
 * 
 * Not the most efficient implementation;
 */

public class MCSubject2
    extends NominalFeature {

public MCSubject2() {
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
  boolean sub = GramRole.getValue(np2, doc).equals("SUBJECT");
  return sub && SyntaxUtils.isMainClause(np2, doc.getAnnotationSet(Constants.PARSE)) ? "Y" : "N";
}

}
