package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;


public class Embedded
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new Embedded(true, true);
  }
  return ref;
}

public static Boolean getValue(Annotation np, Document doc)
{
  return (Boolean) getInstance().getValueProp(np, doc);
}

private Embedded(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  // According to Vincent's definition:
  // A noun is an embedded noun if it is a single-word non-named-entity that
  // is NOT aligned with the right boundary of any basenp
  boolean emb = false;
  if (!ProperName.getValue(np, doc)) {
    // String[] words = getWords(np, text);
    // if (words.length == 1) {
    // AnnotationSet bnps = annotations.get(Constants.NP);
    // AnnotationSet overlap = bnps.getOverlapping(np);

    // if(overlap.size()>1)
    // emb=true;
    // // An np is embedded if another np contains it and has a larger
    // // span
    // for (Annotation cur : overlap.getOrderedAnnots()) {
    // if(!cur.equals(np)&&cur.getEndOffset()==np.getEndOffset()){
    // emb=false;
    // break;
    // }
    // }

    // }
    // Try a different way
    String type = np.getType();
    if (type.startsWith("NN") || type.equals("PRP$")) {
      String gramRole = AllGramRole.getValue(np, doc);
      if (gramRole != null && gramRole.equalsIgnoreCase("conj")) {
        emb = false;
      }
      else {
        Annotation cont = MaximalNP.getValue(np, doc);
        emb = true;
        if (cont != null) {
          AnnotationSet dep = doc.getAnnotationSet(Constants.DEP).getContained(cont);
          if (dep != null) {
            for (Annotation d : dep) {
              if (d.getType() != null && d.getType().equalsIgnoreCase("conj")) {
                Annotation gov = SyntaxUtils.getGov(d, dep);
                if (gov != null && gov.overlaps(np)) {

                  emb = false;
                  break;
                }
              }
            }
          }
          if (emb) {
            AnnotationSet contained = doc.getAnnotationSet(Constants.PARSE).getContained(cont);
            if (contained != null) {
              for (Annotation p : contained) {
                if ((doc.getAnnotString(p).matches("\\,")) && p.getEndOffset() == np.getEndOffset()) {
                  emb = true;
                  break;
                }
              }
            }
          }
        }
      }
    }
    else {
      emb = false;
    }
    // Annotation max = (Annotation)FeatureUtils.MAX_NP.getValue(np, annotations, text);
    // if(!max.properCovers(np)||np.getEndOffset()==max.getEndOffset())
    // emb =false;
    // else
    // emb =true;
  }
  return emb;

}

}
