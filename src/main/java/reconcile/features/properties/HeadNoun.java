package reconcile.features.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;


public class HeadNoun
    extends Property {

String[] nounPOS = { "NN", "NNS", "NNP", "NNPS" };
String[] npTypes = { "NN", "NNS", "NNP", "NNPS", "NP" };
private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new HeadNoun(true, true);
  }
  return ref;
}

public static Annotation getValue(Annotation np, Document doc)
{
  return (Annotation) getInstance().getValueProp(np, doc);
}

private HeadNoun(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  /*
   * given a list of p-o-s of an NP, return the head noun of the NP The head
   * noun is the last noun before the first prep with modifiers removed
   */
  AnnotationSet posAnnots = doc.getAnnotationSet(Constants.POS);
  AnnotationSet parse = doc.getAnnotationSet(Constants.PARSE);
  Annotation value;
  String[] prepPOS = { "IN", "TO" };
  String[] separator = { ",", ":", ";" };
  // System.out.println(posAnnots.getOrderedAnnots()+" np is"+np+posAnnots.getContained(np));

  AnnotationSet overlappingPos = posAnnots.getOverlapping(np);
  // System.out.println("Found "+overlappingPos.size()+" overlapping pos");
  List<Annotation> ordrdToks = overlappingPos.getOrderedAnnots();
  int endIndex = ordrdToks.size() - 1;
  boolean print = false;
  for (int i = 1; i < ordrdToks.size(); i++) {

    Annotation curAnnotation = ordrdToks.get(i);

    if (FeatureUtils.memberArray(curAnnotation.getType(), separator)) {
      // If the np contians a comma, we look at the parse
      // print = true;
      AnnotationSet contained = parse.getContained(curAnnotation.getEndOffset(), np.getEndOffset());
      boolean end = false;
      if (contained != null) {
        for (Annotation c : contained) {
          // System.out.println(c);
          // If the comma is followed by a phrase, we get the last noun before the comma
          if (c.getType().endsWith("P") && !c.getType().equals("NNP")
              && doc.getAnnotString(curAnnotation.getEndOffset(), c.getStartOffset()).matches("\\s*")) {
            // System.out.println(c);
            endIndex = i - 1;
            end = true;
            break;
          }
        }
      }
      if (end) {
        break;
      }
    }
    if ((FeatureUtils.memberArray(curAnnotation.getType(), prepPOS) && !doc.getAnnotText(curAnnotation)
        .equalsIgnoreCase("than"))) {
      // if((curAnnotation.getType().equals(",")&&i>0&& i+1<ordrdToks.size()&&
      // ordrdToks.get(i-1).getType().startsWith("J"))){
      // //Avoid cases like "bigger, better cars"
      // }else{
      endIndex = i - 1;
      break;

    }
    if (curAnnotation.getType().equals("DT")
        && doc.getAnnotText(curAnnotation).equalsIgnoreCase("a")
        && (FeatureUtils.memberArray(ordrdToks.get(i - 1).getType(), nounPOS) || ordrdToks.get(i - 1).getType().equals(
            "CD"))) {
      // System.out.println("Det: "+Utils.getAnnotText(curAnnotation,text));
      // System.out.println("Inside: "+Utils.getAnnotText(np,text));
      endIndex = i - 1;
      break;

    }
  }

  // Now the head is the last noun token (or the last token if no token
  // has
  // a noun pos)
  Annotation result = endIndex < 0 ? np : ordrdToks.get(endIndex);

  for (int i = endIndex; i >= 0; i--) {
    Annotation cur = ordrdToks.get(i);

    if (FeatureUtils.memberArray(cur.getType(), nounPOS)) {
      result = cur;
      break;
    }
    else {
      AnnotationSet conParse = parse.getContained(cur);
      boolean found = false;
      if (conParse != null) {
        for (Annotation p : conParse) {
          // System.out.println(p.getType());
          if (FeatureUtils.memberArray(p.getType(), nounPOS)) {
            result = cur;
            found = true;
            break;
          }
        }
      }
      if (found) {
        break;
      }
    }
  }

  if (result == null) throw new RuntimeException("Head noun not found for " + np);

  Annotation head = new Annotation(result.getId(), result.getStartOffset(), result.getEndOffset(), "HeadNoun",
      new HashMap<String, String>());
  if (print) {
    System.out.println(doc.getAnnotText(np) + " --head-- " + doc.getAnnotText(head));
  }
  value = head;

  // String head1=FeatureUtils.getText(value, text);
  // Annotation otherHead = getHead(np, annotations, text);
  //		
  // String head2=otherHead==null?head1:FeatureUtils.getText(getHead(np, annotations, text), text);
  // if(!head1.equals(head2)){
  // System.out.println("NP: "+FeatureUtils.getText(np, text));
  // System.out.println("Head1: "+head1+" Head2: "+head2);
  // }
  return value;
}

public Annotation getHead(Annotation np, Map<String, AnnotationSet> annotations, String text)
{
  AnnotationSet parse = annotations.get(Constants.PARSE);
  AnnotationSet posAnnots = annotations.get(Constants.POS);
  Annotation value;
  // if it contains a single token, then that's the head
  AnnotationSet toks = posAnnots.getOverlapping(np);
  if (toks.size() == 1) {
    Annotation result = toks.getFirst();
    return new Annotation(result.getId(), result.getStartOffset(), result.getEndOffset(), "HeadNoun",
        new HashMap<String, String>());
  }

  Annotation parseNp = SyntaxUtils.getContainingNP(np, parse);
  if (parseNp == null) return null;
  AnnotationSet contained = parse.getContained(parseNp);
  // Recursively find the noun that is the head of the noun phrase
  value = getRecursiveHead(parseNp, contained);

  return value;
}

private Annotation getRecursiveHead(Annotation np, AnnotationSet parse)
{
  // if(FeatureUtils.memberArray(np.getType(),nounPOS))
  // return np;
  List<Annotation> children = SyntaxUtils.getOrderedChildren(np, parse);
  if (children.size() < 1) return np;
  for (int i = children.size() - 1; i >= 0; i--) {
    Annotation cur = children.get(i);
    if (FeatureUtils.memberArray(cur.getType(), npTypes)) return getRecursiveHead(cur, parse);
  }
  return children.get(children.size() - 1);
}

}
