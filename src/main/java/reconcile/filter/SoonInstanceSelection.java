package reconcile.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.individualFeature.instClass;

public class SoonInstanceSelection extends PairGenerator {

// A list to store all the pairs
ArrayList<Annotation[]> pairs;
Iterator<Annotation[]> pairIter;

@Override
public void initialize(Annotation[] nps, Document doc, boolean training)
{
  super.initialize(nps, doc, training);

  pairs = new ArrayList<Annotation[]>();
  // Generate pairs: all possible pairs during testing and only up to the
  // first positive instance during training
  for (int i = nps.length - 2; i >= 0; i--) {
    Annotation np1 = nps[i];
    ArrayList<Annotation[]> pairsI = new ArrayList<Annotation[]>();
    boolean anaphoric = false;
    
    for (int j = i + 1; j < nps.length; j++) {
      Annotation np2 = nps[j];
		if (training) {
	        if (!anaphoric) {
	          pairsI.add(0, new Annotation[] { np1, np2 });
	          anaphoric = (new instClass()).getValue(np1, np2, doc, new HashMap<Feature, String>()).equals(instClass.POSITIVE);
	        }
      }
      else {
        pairsI.add(0, new Annotation[] { np1, np2 });
      }
    }

    if (!training || anaphoric) {
      pairs.addAll(pairsI);
    }
  }
  pairIter = pairs.iterator();
}

@Override
public boolean hasNext()
{
  return pairIter.hasNext();
}

@Override
public Annotation[] nextPair()
{
  return pairIter.next();
}

}
