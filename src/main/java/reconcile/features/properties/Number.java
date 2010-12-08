package reconcile.features.properties;

import java.util.Iterator;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.NPSemTypeEnum;
import reconcile.features.FeatureUtils.NumberEnum;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;


public class Number
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new Number(false, true);
  }
  return ref;
}

public static NumberEnum getValue(Annotation np, Document doc)
{
  return (NumberEnum) getInstance().getValueProp(np, doc);
}

private Number(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  NumberEnum value = NumberEnum.UNKNOWN;
  String annotText = doc.getAnnotText(np);

  if (FeatureUtils.isPronoun(np, doc)) {
    if (FeatureUtils.memberArray(annotText, FeatureUtils.PLURAL_PRONOUNS)) {
      value = NumberEnum.PLURAL;
    }
    else {

      if (FeatureUtils.memberArray(annotText, FeatureUtils.UNKNWNS)) {
        value = NumberEnum.UNKNOWN;
      }
      else {
        value = NumberEnum.SINGLE;
      }
    }
  }
  else {
    if (Embedded.getValue(np, doc)) {
      value = NumberEnum.UNKNOWN;
    }
    else {
      NPSemTypeEnum semType = NPSemanticType.getValue(np, doc);
      if ((semType.equals(NPSemTypeEnum.DATE) || semType.equals(NPSemTypeEnum.MONEY)
          || semType.equals(NPSemTypeEnum.PERCENTAGE) || semType.equals(NPSemTypeEnum.TIME))
          && !Conjunction.getValue(np, doc)) {
        value = NumberEnum.SINGLE;
      }
      else {
        if (Conjunction.getValue(np, doc)) {
          value = NumberEnum.PLURAL;
        }
        else {
          Annotation hn = HeadNoun.getValue(np, doc);
          AnnotationSet ne = doc.getAnnotationSet(Constants.NE).getOverlapping(np.getStartOffset(), hn.getEndOffset());
          AnnotationSet overlapPerson = ne.get("PERSON");
          AnnotationSet overlapOrg = ne.get("ORGANIZATION");

          AnnotationSet posAnnots = doc.getAnnotationSet(Constants.POS);
          AnnotationSet parse = doc.getAnnotationSet(Constants.PARSE);
          // System.err.println("Np "+Utils.getAnnotText(np, text)+" hn
          // "+Utils.getAnnotText(hn, text));

          Annotation headPOS = SyntaxUtils.getNode(hn, parse);
          if (headPOS == null) {
            AnnotationSet contPOS = posAnnots.getContained(hn);
            headPOS = contPOS == null ? null : contPOS.getLast();
          }
          np.setProperty(Property.HEAD_POS, headPOS == null ? "null" : headPOS.getType());
          AnnotationSet allNumNEs = overlapOrg;
          if (allNumNEs == null) {
            allNumNEs = new AnnotationSet(Constants.NE);
          }
          if (overlapPerson != null) {
            Iterator<Annotation> anIter = overlapPerson.iterator();
            while (anIter.hasNext()) {
              allNumNEs.add(anIter.next());
            }
          }
          Annotation namedEnt = allNumNEs == null ? null : allNumNEs.getFirst();
          String[] words = doc.getWords(np.getStartOffset(), hn.getEndOffset());
          if (ProperName.getValue(np, doc) && allNumNEs != null && allNumNEs.size() > 0) {
            if (allNumNEs.size() > 1
                && (FeatureUtils.memberArray("and", words) || FeatureUtils.memberArray(",", words))) {
              value = NumberEnum.PLURAL;
            }
            else if (allNumNEs.getFirst().getType().equals("person")) {
              if (FeatureUtils.memberArray("and", words)) {
                value = NumberEnum.PLURAL;
              }
              else {
                value = NumberEnum.SINGLE;
              }
            }
            else {
              value = NumberEnum.SINGLE;
            }
          }
          else if (FeatureUtils.memberArray("and", words)) {
            value = NumberEnum.PLURAL;
          }
          else if (FeatureUtils.isDate(annotText)) {
            value = NumberEnum.SINGLE;
          }
          else if (namedEnt != null
              && (namedEnt.getEndOffset() == np.getEndOffset() || namedEnt.getEndOffset() == hn.getEndOffset())) {
            value = NumberEnum.SINGLE;
          }
          else {
            boolean nns = false;
            boolean nn = false;
            boolean cd = false;
            String type = headPOS == null ? "" : headPOS.getType();
            nns = type.equalsIgnoreCase("NNS") || type.equalsIgnoreCase("NNPS");
            nn = type.equalsIgnoreCase("NN") || type.equalsIgnoreCase("NNP");
            cd = type.equalsIgnoreCase("CD");
            if (nns) {
              value = NumberEnum.PLURAL;
            }
            else if (nn) {
              value = NumberEnum.SINGLE;
            }
            else if (SubsumesNumber.getValue(np, doc)) {
              value = NumberEnum.SINGLE;
            }
            else if (words[0].contains("$")) {
              value = NumberEnum.SINGLE;
            }
            else if (cd && !words[0].equalsIgnoreCase("one")) {
              value = NumberEnum.PLURAL;
            }
            else {
              value = NumberEnum.UNKNOWN;
            }
          }
        }
      }
    }
  }
  return value;

}

}
