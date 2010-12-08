package reconcile.featureVector.individualFeature;

import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.Property;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;


/*
 * This feature is: C if the NPs for predicate nominal construction I otherwise
 */

public class CassPrednom
    extends NominalFeature {

// private String[] COPULARS = { "BE", "BED", "BEDZ", "BEM", "BER", "BEZ" };

public CassPrednom() {
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
  AnnotationSet parParse = doc.getAnnotationSet(Constants.PARTIAL_PARSE);
  parParse = parParse.getContained(sent);

  if (isPrednom(np1, np2, parParse, doc)) {
    np1.setProperty(Property.PREDNOM, np2);
    np2.setProperty(Property.PREDNOM, np1);
    System.err.println("NNNPrednom1: " + doc.getAnnotString(np1) + " - " + doc.getAnnotText(np2) + ": "
        + doc.getAnnotText(sent));
    return COMPATIBLE;
  }
  if (isPrednom(np2, np1, parParse, doc)) {
    np1.setProperty(Property.PREDNOM, np2);
    np2.setProperty(Property.PREDNOM, np1);
    System.err.println("NNNPrednom2: " + doc.getAnnotString(np1) + " - " + doc.getAnnotText(np2) + ": "
        + doc.getAnnotText(sent));
    return COMPATIBLE;
  }
  // if(AllFeatures.makeFeature("Quantity").getValue(np1, np2, annotations, text,
  // featVector).equals(NominalFeature.COMPATIBLE))
  // return COMPATIBLE;
  return INCOMPATIBLE;
}

private boolean isPrednom(Annotation np1, Annotation np2, AnnotationSet parParse, Document doc)
{
  return isPrednomHelper(np2, np1, parParse, doc);
}

private boolean isPrednomHelper(Annotation np1, Annotation np2, AnnotationSet parParse, Document doc)
{
  Annotation node1 = SyntaxUtils.getHighestNode(np1, parParse);
  if (node1 == null) return false;
  if (doc.getAnnotText(node1).equalsIgnoreCase("there")) return false;
  boolean isSubject = "s".equals(node1.getAttribute("role"));
  boolean isHead1 = "h".equals(node1.getAttribute("role"));
  while ((node1.getType().startsWith("N") || node1.getType().startsWith("PRP")) && !isSubject && isHead1) {
    node1 = SyntaxUtils.getParent(node1, parParse);
    isSubject = "s".equals(node1.getAttribute("role"));
    isHead1 = "h".equals(node1.getAttribute("role"));
  }
  if (!isSubject) return false;
  // get the C0
  node1 = SyntaxUtils.getParent(node1, parParse);
  // find the VX child

  boolean isCop = false;
  AnnotationSet children = SyntaxUtils.getChildren(node1, parParse);
  for (Annotation a : children) {
    if (a.getType().equals("VX") && SyntaxUtils.isCopular(doc.getAnnotText(a))) {
      isCop = true;
    }
  }
  if (!isCop) return false;

  Annotation node2 = SyntaxUtils.getHighestNode(np2, parParse);
  if (node2 == null) return false;
  boolean isObject = "o".equals(node2.getAttribute("role"));
  boolean isHead2 = "h".equals(node2.getAttribute("role"));
  while ((node2.getType().startsWith("N") || node1.getType().startsWith("PRP")) && !isObject && isHead2) {
    node2 = SyntaxUtils.getParent(node2, parParse);
    isObject = "o".equals(node2.getAttribute("role"));
    isHead2 = "h".equals(node2.getAttribute("role"));
  }
  if (!isObject) return false;
  // System.out.println(doc.getAnnotText(node1)+":"+node1);
  // System.out.println(doc.getAnnotText(node1,parParse));
  // System.out.println(doc.getAnnotText(node2)+":"+node2);
  // System.out.println(doc.getAnnotText(SyntaxUtils.getParent(node2,parParse)));
  // System.out.println(doc.getAnnotText(np1)+" ---- "+doc.getAnnotString(np2));

  return SyntaxUtils.getParent(node2, parParse).equals(SyntaxUtils.getParent(node1, parParse));
  // return true;

}

}
