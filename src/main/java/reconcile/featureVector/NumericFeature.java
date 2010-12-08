package reconcile.featureVector;


public abstract class NumericFeature
    extends Feature {

@Override
public boolean isNumeric()
{
  return true;
}

// Infinity and beyond.
public static final int WN_MAX = 20;// INFINITY = Integer.MAX_VALUE;
public static final int WN_SENSE_MAX = 20;
}
