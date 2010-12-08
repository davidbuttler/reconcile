/*
 * Nathan Gilbert
 * 7/26/10
 */

package reconcile.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Random;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.general.Utils;
import reconcile.featureVector.individualFeature.instClass;
import reconcile.SystemConfig;

/*
 * RatioInstanceGenerator - Create feature vectors with a desired positive/negative instance ratio
 * 
 */

public class RatioInstanceGenerator extends PairGenerator {

ArrayList<Annotation[]> pairs;
Iterator<Annotation[]> pairIter;
int ratio;

@Override
public void initialize(Annotation[] nps, Document doc, boolean training)
{	
	super.initialize(nps, doc, training);
	SystemConfig cfg = Utils.getConfig();
	pairs = new ArrayList<Annotation[]>();
	ratio = cfg.getRatio();
	
	// Generate pairs: all possible pairs during testing and only the desired ratio during training.
	int coref = 0;
	if(training) {
		//first get all coref annotations
		for (int i = nps.length - 2; i >= 0; i--) {
			Annotation np1 = nps[i];
		    ArrayList<Annotation[]> pairsI = new ArrayList<Annotation[]>();
		    boolean anaphoric = false;
		    
		    for (int j = i + 1; j < nps.length; j++) {
		      Annotation np2 = nps[j];
		      anaphoric = (new instClass()).getValue(np1, np2, doc, new HashMap<Feature, String>()).equals(instClass.POSITIVE);
		      if (anaphoric) {
		    	  pairsI.add(0, new Annotation[] { np1, np2 });
		    	  coref++;
		      }	     
		   }
		   pairs.addAll(pairsI);
		}
		
		System.out.println("RATIOSELECTION:>>>>>Total Coreferent Pairs: " + pairs.size());		
	    ArrayList<Annotation[]> pairsAll = new ArrayList<Annotation[]>();
	    Random generator = new Random();
		
		//now get all instances
		for (int i = nps.length - 2; i >= 0; i--) {
			Annotation np1 = nps[i];
		    for (int j = i + 1; j < nps.length; j++) {
		    	Annotation np2 = nps[j];
		        pairsAll.add(0, new Annotation[] { np1, np2 });
		        //System.out.println(doc.getAnnotString(np1) + " ~ " + doc.getAnnotString(np2));
		      }
		}
		
		int n = (pairs.size())*ratio;
		if (n > pairsAll.size()) {
			//this will cause all negative instances to be generated in this case, though a bit slower...probably
			//occured because you set the ratio incredibly high.
			n = (pairsAll.size() - pairs.size());
		}
		
		boolean anaphoric = false;
		ArrayList<Integer> used = new ArrayList<Integer>();
		ArrayList<Annotation[]> pairsNegative = new ArrayList<Annotation[]>();
		
		int next;
		//randomly select negative instances to ensure the proper ratio
		
		while (n > 0) {
			next = generator.nextInt(pairsAll.size());
			Annotation[] pair = pairsAll.get(next);
			anaphoric = (new instClass()).getValue(pair[0], pair[1], doc, new HashMap<Feature, String>()).equals(instClass.POSITIVE);
			
			if (anaphoric) {
				continue;
			}
			else if (used.contains(next)) {
				//System.out.println("Found double: " + next);
				continue;				
			}
			else {
				//we haven't used this negative instance before, so let's add it in.
				pairsNegative.add(0, new Annotation[] { pair[0],pair[1] });
				//System.out.println(doc.getAnnotString(pair[0]) + " ~ " + doc.getAnnotString(pair[1]));
				used.add(next);
				n--;
			}			
		}		
		pairs.addAll(pairsNegative);
		System.out.println("RATIOSELECTION:>>>>>Total Negative Pairs: " + pairsNegative.size());	
	} 
	else {
		//if test, then add all pairs
		for (int i = nps.length - 2; i >= 0; i--) {
			Annotation np1 = nps[i];
			ArrayList<Annotation[]> pairsI = new ArrayList<Annotation[]>();
			for (int j = i + 1; j < nps.length; j++) {
				Annotation np2 = nps[j];
				pairsI.add(0, new Annotation[] { np1, np2 });
			}
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
