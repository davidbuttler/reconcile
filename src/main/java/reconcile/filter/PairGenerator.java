package reconcile.filter;

import reconcile.data.Annotation;
import reconcile.data.Document;

public abstract class PairGenerator {

protected Annotation[] nps;
protected Document doc;
protected boolean training = false;

public PairGenerator() {
}

public void initialize(Annotation[] nps, Document doc, boolean training)
{
  this.nps = nps;
  this.doc = doc;
  this.training = training;  
}

// Returns a pair of noun phrases representing the next pair
// May differ for training and testing purposes
public abstract Annotation[] nextPair();

public abstract boolean hasNext();

}
