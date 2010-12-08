package reconcile.features.properties;

import java.util.ArrayList;
import java.util.List;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;


public class AllModifiers
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new AllModifiers(true, true);
  }
  return ref;
}

public static String[] getValue(Annotation np, Document doc)
{
  return (String[]) getInstance().getValueProp(np, doc);
}

private AllModifiers(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  String[] value = null;
  if (Conjunction.getValue(np, doc)) return new String[0];
  AnnotationSet posAnnots = doc.getAnnotationSet(Constants.POS);
  // AnnotationSet ne = annotations.get(Constants.NE);
  Annotation head = HeadNoun.getValue(np, doc);
  // AnnotationSet overlap = ne.getContained(np);
  // Annotation namedEntity;
  // if (overlap == null || overlap.isEmpty())
  // namedEntity = null;
  // else
  // namedEntity = overlap.getLast();
  Annotation namedEntity = (Annotation) Property.LINKED_PROPER_NAME.getValueProp(np, doc);
  ArrayList<String> result = new ArrayList<String>();

  // Loop over all tokens
  // int start = np.getStartOffset();
  // int end = head.getStartOffset();
  // if (namedEntity != null && namedEntity.getStartOffset() < end)
  // end = namedEntity.getStartOffset();
  // System.err.println(head);
  // System.err.println(np);
  // System.err.println("st"+start+" end"+end);

  List<Annotation> contained = posAnnots.getOverlapping(np).getOrderedAnnots();
  for (Annotation a : contained) {
    if (!a.overlaps(head) && !a.overlaps(namedEntity)) {
      String word = doc.getAnnotText(a);
      if (FeatureUtils.isNumeral(word) || FeatureUtils.isCardinalNumber(a) || FeatureUtils.isAdjective(a)
          || FeatureUtils.isAdverb(a) || (!FeatureUtils.isStopword(word) && !FeatureUtils.isUninfWord(word))) {
        result.add(word);
      }
    }
  }

  value = result.toArray(new String[0]);
  if (value == null) {
    value = new String[0];
  }
  return value;

}

}
