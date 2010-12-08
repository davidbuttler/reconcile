package reconcile.features.properties;

import java.util.HashMap;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;
import reconcile.scorers.Matcher;
import reconcile.scorers.Matcher.MatchStyleEnum;


public class CorefID
    extends Property {

private int MAX_ID;
private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new CorefID(true, true);
  }
  return ref;
}

public static Integer getValue(Annotation np, Document doc)
{
  return (Integer) getInstance().getValueProp(np, doc);
}

private CorefID(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  setCorefIDs(doc);
  return np.getProperty(this);
}

private void setCorefIDs(Document doc)
{
  // Get the key CEs
  AnnotationSet key = doc.getAnnotationSet(Constants.GS_NP);
  // Gt the response CEs
  AnnotationSet nps = doc.getAnnotationSet(Constants.NP);

  MatchStyleEnum matchStyle;
  // Match automatic to gs nps
  if (Utils.getConfig().getDataset().toLowerCase().startsWith("ace")) {
    matchStyle = MatchStyleEnum.ACE;
  }
  else {
	if (Utils.getConfig().getDataset().toLowerCase().startsWith("uw"))
	    matchStyle = MatchStyleEnum.UW;	
	else
		matchStyle = MatchStyleEnum.MUC;
  }
  Matcher.matchAnnotationSets(key, nps, matchStyle, doc);

  HashMap<Integer, Integer> clustIDs = new HashMap<Integer, Integer>();
  setMaxID(0);
  if (key != null) {
    for (Annotation a : key) {
		//System.out.println(a);
		//System.out.println(a.getAttribute(Constants.CE_ID));
		//System.out.println(a.getAttribute(Constants.CLUSTER_ID));
      String ceId = a.getAttribute(Constants.CE_ID);
      String clusterId = a.getAttribute(Constants.CLUSTER_ID);
      if (ceId != null && clusterId != null) {
        clustIDs.put(Integer.parseInt(ceId), Integer.parseInt(clusterId));
      }
      else {
        System.out.println("error in extracting cluster id: " + a.toString());
      }
    }
  }
  HashMap<Integer, Integer> clustMap = new HashMap<Integer, Integer>();
  for (Annotation a : nps) {
    Integer match = (Integer) a.getProperty(Property.MATCHED_CE);
    int id = (match == null) ? -1 : match;
    int coref = (id < 0) ? getNextID() : getTranslatedClustId(clustMap, clustIDs.get(id)); // FeatureUtils.find(id,
                                                                                           // ptrs);
    a.setProperty(this, coref);
    // System.out.println(a+"CI "+a.getProperty(this));
  }

}

private Integer getTranslatedClustId(HashMap<Integer, Integer> clustMap, Integer clust)
{
  if (!clustMap.containsKey(clust)) {
    clustMap.put(clust, getNextID());
  }
  return clustMap.get(clust);
}

private void setMaxID(int id)
{
  MAX_ID = id;
}

private int getNextID()
{
  return MAX_ID++;
}

}
