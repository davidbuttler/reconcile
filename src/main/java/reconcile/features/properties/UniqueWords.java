package reconcile.features.properties;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;


public class UniqueWords
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new UniqueWords(false, true);
  }
  return ref;
}

public static String[] getValue(Annotation np, Document doc)
{
  return (String[]) getInstance().getValueProp(np, doc);
}

private UniqueWords(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  AnnotationSet nps = doc.getAnnotationSet(Constants.NP);
  // Get the unique (non-overlapped) words for each NP
  for (Annotation n : nps) {
    // if(isProperName(np, annotations, text))
    char[] anText = doc.getAnnotString(n).toCharArray();
    int start = n.getStartOffset();
    AnnotationSet contained = nps.getContained(n);
    for (Annotation c : contained) {
      if (!c.equals(n)) {
        int embStart = c.getStartOffset() - start;
        int embEnd = c.getEndOffset() - start;
        for (int i = embStart; i < embEnd; i++) {
          anText[i] = ' ';
        }
      }
    }

    String[] res = FeatureUtils.removeUninfWords(FeatureUtils.getWords(String.valueOf(anText)));
    res = res == null || res.length == 0 ? Document.getWords(doc.getAnnotText(n)) : res;
    n.setProperty(this, res);
  }
  return np.getProperty(this);
}

}
