package reconcile.filter;

import reconcile.data.Annotation;
import reconcile.data.Document;

public class AllPairs
    extends PairGenerator {

private int i, j;

public AllPairs() {

}

@Override
public void initialize(Annotation[] nps, Document doc, boolean training)
{
  super.initialize(nps, doc, training);
  j = nps.length - 1;
  i = j - 1;
}

@Override
public boolean hasNext()
{
  if (j > 0) return true;
  return false;
}

@Override
public Annotation[] nextPair()
{
  if (hasNext()) {
    Annotation np1 = nps[i];
    Annotation np2 = nps[j];
    if (i == 0) {
      j--;
      i = j - 1;
    }
    else {
      i--;
    }
    // System.out.println("Pair "+i+" "+j);
    return new Annotation[] { np1, np2 };
  }
  else
    return null;
}

}
