package reconcile.featureVector;

public abstract class NominalFeature extends Feature {

@Override
public boolean isNominal()
{
  return true;
}

public abstract String[] getValues();

/*
 * A few basic feature values used throughout
 */
public static final String COMPATIBLE = "C";
public static final String INCOMPATIBLE = "I";
public static final String NA = "D";
public static final String SAME = "S";
protected static final String[] ICN = { INCOMPATIBLE, COMPATIBLE, NA };
protected static final String[] ICNS = { INCOMPATIBLE, COMPATIBLE, SAME, NA };
protected static final String[] IC = { COMPATIBLE, INCOMPATIBLE };
protected static final String[] YN = { "Y", "N" };
protected static final String[] PAIRS = { "officier:gentlement", "NA"};

}
