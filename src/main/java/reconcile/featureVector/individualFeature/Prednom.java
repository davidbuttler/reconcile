package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.HeadNoun;
import reconcile.features.properties.Property;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;


/*
 * This feature is: C if the NPs for predicate nominal construction I otherwise
 */

public class Prednom
    extends NominalFeature {

public Prednom() {
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

  if (!FeatureUtils.sameSentence(np1, np2, doc)
      || Constructor.createFeature("Number").getValue(np1, np2, doc, featVector).equals(NominalFeature.INCOMPATIBLE))
    return INCOMPATIBLE;
  AnnotationSet sents = doc.getAnnotationSet(Constants.SENT);
  Annotation sent = sents.getOverlapping(np1).getFirst();
  AnnotationSet dep = doc.getAnnotationSet(Constants.DEP);
  dep = dep.getContained(sent);
  AnnotationSet parse = doc.getAnnotationSet(Constants.PARSE);
  parse = parse.getContained(sent);
  Annotation hn1 = HeadNoun.getValue(np1, doc);
  Annotation hn2 = HeadNoun.getValue(np2, doc);
  if (SyntaxUtils.isPrednom(hn1, hn2, dep, parse, doc.getText())) {
    np1.setProperty(Property.PREDNOM, np2);
    np2.setProperty(Property.PREDNOM, np1);
    // System.err.println("Prednom1: "+doc.getAnnotString(np1)+" - "+doc.getAnnotText(np2)+": "+doc.getAnnotText(sent));
    return COMPATIBLE;
  }
  if (SyntaxUtils.isPrednom(hn2, hn1, dep, parse, doc.getText())) {
    np1.setProperty(Property.PREDNOM, np2);
    np2.setProperty(Property.PREDNOM, np1);
    // System.err.println("Prednom2: "+doc.getAnnotString(np1)+" - "+doc.getAnnotText(np2)+": "+doc.getAnnotText(sent));
    return COMPATIBLE;
  }
  // if(AllFeatures.makeFeature("Quantity").getValue(np1, np2, annotations, text,
  // featVector).equals(NominalFeature.COMPATIBLE))
  // return COMPATIBLE;
  return INCOMPATIBLE;
}

}
