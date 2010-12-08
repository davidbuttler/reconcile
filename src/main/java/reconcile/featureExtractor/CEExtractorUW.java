package reconcile.featureExtractor;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;
import reconcile.general.Utils;


public class CEExtractorUW extends CEExtractor {
String[] HERE = {"here","there"};
public boolean isNoun(Annotation a, String text)
{
	if (a == null) return false;
	String type = a.getType();
	// if(type.startsWith("NNP"))
	// return true;
	//if (type.startsWith("NN") && FeatureUtils.isCapitalized(Utils.getAnnotText(a, text))) return true;
	//if (type.equals("JJ") && FeatureUtils.isCapitalized(Utils.getAnnotText(a, text))) return true;
	if (type.startsWith("PRP")) return true;
	return false;
}

public boolean isNP(Annotation an, String text)
{
	try {
		String type = an.getType();
		
		if (FeatureUtils.memberArray(type, SyntaxUtils.NPType)&&!FeatureUtils.memberArray(Utils.getAnnotText(an, text),HERE)) return true;
		if (type.equalsIgnoreCase("WHNP") && 
				FeatureUtils.memberArray(Utils.getAnnotText(an, text), REL_POS_PRONOUNS))
			return true;
		
	}
	catch (NullPointerException npe) {
		return false;
	}
	
	return false;
}

public boolean addNE(Annotation a, AnnotationSet includedCEs, AnnotationSet baseCEs, Document doc){
	AnnotationSet parse = doc.getAnnotationSet(Constants.PARSE);
	String text = doc.getText();
	AnnotationSet olapCEs = baseCEs.getOverlapping(a);
	if(!includedCEs.coversSpan(a))
		return true;
	for(Annotation o:olapCEs){
		Annotation oAn = trimNP(o, parse, text);
		if(oAn.compareSpan(a)==0)
			return true;
		if(containsConj(oAn, parse, text))
			return true;
	}
	Annotation par = SyntaxUtils.getHighestNode(a,parse);
	if(par!=null&&par.getType().equalsIgnoreCase("NP"))
		return true;
	if(a.getEndOffset()+2<text.length()){
		String post = doc.getAnnotString(a.getEndOffset(), a.getEndOffset()+2);
		if (post.equalsIgnoreCase("'s"))
			return true;
	}
	//System.out.println("Excluded: "+doc.getAnnotText(a.getStartOffset()-10<0?0:a.getStartOffset()-10,a.getStartOffset())
	//		+"["+doc.getAnnotText(a)+"]"+doc.getAnnotText(a.getEndOffset(),a.getEndOffset()+10));
	return false;
}

}
