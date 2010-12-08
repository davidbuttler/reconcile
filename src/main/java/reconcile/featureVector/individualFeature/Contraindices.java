package reconcile.featureVector.individualFeature;

import java.util.ArrayList;
import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.Embedded;
import reconcile.features.properties.HeadNoun;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;
import reconcile.general.Utils;


/*
 * Implementation of the constraints that (1) two NP's separated by preposition cannot be coindexed and (2) two
 * non-prominal NP's seprated by a non-copular verb cannot be coindexed.
 * 
 * This feature is: I if the two NP's violate the above conditions C otherwise
 */

public class Contraindices
    extends NominalFeature {

// private static final String PREPOSITION = "IN";
private static final String VERB = "VB";

public Contraindices() {
  name = this.getClass().getSimpleName();
}

@Override
public String[] getValues()
{
  return IC;
}

private boolean nonCopularVerb(Annotation dep, String text)
{
  // System.out.println(dep);
  return dep.getType().startsWith(VERB) && !SyntaxUtils.isCopular(Utils.getAnnotText(dep, text));
}

private boolean containsCopular(AnnotationSet dep, Annotation head2, String text)
{

  for (Annotation a : dep) {
    if (a.getType().equalsIgnoreCase("cop") && SyntaxUtils.isGovernor(head2, a)) return true;
  }
  return false;
}

@Override
public String produceValue(Annotation np1, Annotation np2, Document doc, Map<Feature, String> featVector)
{
  if (!FeatureUtils.sameSentence(np1, np2, doc)) return COMPATIBLE;
  if (featVector.get(Constructor.createFeature(Constants.APPOSITIVE)).equals(COMPATIBLE)) return COMPATIBLE;
  if (featVector.get(Constructor.createFeature("Prednom")).equals(COMPATIBLE)) return COMPATIBLE;
  // boolean max1 = np1.compareSpan((Annotation)FeatureUtils.MAX_NP.getValue(np1, annotations, text))==0;
  // boolean max2 = np2.compareSpan((Annotation)FeatureUtils.MAX_NP.getValue(np2, annotations, text))==0;
  // if(!max1||!max2)
  // return COMPATIBLE;
  Annotation sent = doc.getAnnotationSet(Constants.SENT).getOverlapping(np1).getFirst();
  AnnotationSet dep = doc.getAnnotationSet(Constants.DEP).getContained(sent);
  AnnotationSet pos = doc.getAnnotationSet(Constants.POS);
  Annotation head1 = HeadNoun.getValue(np1, doc);
  Annotation head2 = HeadNoun.getValue(np2, doc);
  // The first condition, two np's in the same clause separated
  // by a non-copular verb
  ArrayList<Annotation> depPath = null;
  // Annotation clause1=SyntaxUtils.getClause(np1, parse);
  // Neither NP is a pronoun
  // boolean notPronouns = !FeatureUtils.isPronoun(np1, doc)&&!FeatureUtils.isPronoun(np2, doc);
  if (!FeatureUtils.isPronoun(np2, doc) && !Embedded.getValue(np2, doc)) {// && clause1!=null&&clause1.covers(np2)){
    // same clause
    depPath = SyntaxUtils.getDepPath(head1, head2, dep, pos);
    /*System.err.print(depPath.size()+"->"+FeatureUtils.getText(np1, text)+":");
    for(Annotation a:depPath)
    	System.err.print(FeatureUtils.getText(a, text)+"("+a.getType()+")-");
    System.err.println(":"+FeatureUtils.getText(np2, text));*/
    if (depPath.size() == 3) if (nonCopularVerb(depPath.get(1), doc.getText())) {
      if (!containsCopular(dep, head2, doc.getText())) return INCOMPATIBLE;
    }
    if (depPath.size() == 4) {
      Annotation a1 = depPath.get(1);
      Annotation a2 = depPath.get(2);
      if (nonCopularVerb(a1, doc.getText()) && nonCopularVerb(a2, doc.getText())
          && "xcomp".equalsIgnoreCase(SyntaxUtils.getDepType(a1, a2, dep))) return INCOMPATIBLE;
    }
  }

  if (!FeatureUtils.isPronoun(np2, doc)) {// notPronouns){
    depPath = depPath == null ? SyntaxUtils.getDepPath(head1, head2, dep, pos) : depPath;
    if (depPath.size() == 3) {
      if (SyntaxUtils.isPreposition(depPath.get(1).getType())) {
        // if (FeatureUtils.memberArray("percent", FeatureUtils
        // .getWNSemClass(np2, annotations, text))
        // || FeatureUtils.memberArray("money", FeatureUtils
        // .getWNSemClass(np2, annotations, text))
        // || FeatureUtils.memberArray("date", FeatureUtils
        // .getWNSemClass(np2, annotations, text))
        // || FeatureUtils.memberArray("measure", FeatureUtils
        // .getWNSemClass(np2, annotations, text))
        // || FeatureUtils.subsumesNumber(FeatureUtils.getText(np2,
        // text)))
        if (!Constructor.createFeature("Quantity").getValue(np1, np2, doc, featVector)
            .equals(NominalFeature.INCOMPATIBLE)) return COMPATIBLE;
        return INCOMPATIBLE;
      }
    }
    else if (depPath.size() == 5) {
      if (SyntaxUtils.isPreposition(depPath.get(1).getType()) && depPath.get(2).getType().startsWith(VERB)
          && SyntaxUtils.isPreposition(depPath.get(3).getType())) return INCOMPATIBLE;
    }
    else
      return COMPATIBLE;
    // for(Annotation a:depPath){
    // if(SyntaxUtils.isPreposition(a.getType()))
    // return INCOMPATIBLE;
    // }
  }
  // Third rule -- if the second np is not reflexive and a subject, then it can't be
  // coreferent with the first np
  if (!FeatureUtils.isReflexive(doc.getAnnotText(np2))) {
    depPath = depPath == null ? SyntaxUtils.getDepPath(head1, head2, dep, pos) : depPath;
    // for(Annotation a:depPath){
    // System.out.println("P: "+a);
    // }
    if (depPath.size() == 3) {
      Annotation an = depPath.get(1);
      if (nonCopularVerb(an, doc.getText())) {
        if ("nsubj".equalsIgnoreCase(SyntaxUtils.getDepType(depPath.get(0), an, dep))
            && "nobj".equalsIgnoreCase(SyntaxUtils.getDepType(depPath.get(2), an, dep))) return INCOMPATIBLE;
      }
    }
  }
  return COMPATIBLE;
  /*
  //go through all tokens between the two np's 		
  int start,end;
  if(np1.compareSpan(np2)<0){
  	start = np1.getEndOffset();
  	end = np2.getStartOffset();
  }else{
  	start = np2.getEndOffset();
  	end = np1.getStartOffset();
  }
  if(start<end){
  	boolean max1 = np1.compareSpan((Annotation)FeatureUtils.propertyValue(np1, FeatureUtils.MAX_NP, annotations, text))==0;
  	boolean max2 = np2.compareSpan((Annotation)FeatureUtils.propertyValue(np2, FeatureUtils.MAX_NP, annotations, text))==0;
  	boolean bothMax = max1&&max2;
  	if(bothMax){
  	AnnotationSet inBetween = pos.getContained(start, end);
  	boolean prep = false;
  	boolean nonCop = false;
  	if(inBetween!=null){
  		for(Annotation a:inBetween){
  			if(a.getType().equalsIgnoreCase(PREPOSITION))
  				prep=true;
  			if(notPronouns && a.getType().startsWith(VERB)&& !SyntaxUtils.isCopular(Utils.getAnnotText(a, text)))
  				nonCop = true;
  		}
  	}
  	if(notPronouns&&nonCop&&!prep)
  		return INCOMPATIBLE;
  	if(prep&&!FeatureUtils.isPronoun(np2, annotations, text))
  		return INCOMPATIBLE;
  }
  }
  return COMPATIBLE;
  */
}

}
