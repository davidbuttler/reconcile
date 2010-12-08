/*
 * This class contains different methods for matching automatically extracted CE's to gold-standard CEs
 */
package reconcile.scorers;

import java.util.HashMap;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.HeadNoun;
import reconcile.features.properties.Property;
import reconcile.general.Constants;
import reconcile.general.Utils;

public class Matcher {

public enum MatchStyleEnum {
  MUC, ACE, UW
}

public static int numMatchedKey = 0;
public static int totalKey = 0;
public static int totalNPsMatched = 0;
public static int doubleMatches = 0;

public static void exactMatchAnnotationSets(AnnotationSet gsNps, AnnotationSet nps)
{
  for (Annotation a : gsNps) {
    a.setProperty(Property.MATCHED_CE, Integer.parseInt(a.getAttribute("ID")));
  }
  for (Annotation a : nps) {
    a.setProperty(Property.MATCHED_CE, Integer.parseInt(a.getAttribute("ID")));
  }
}

public static void matchAnnotationSets(AnnotationSet gsNps, AnnotationSet nps, MatchStyleEnum matchStyle, Document doc)
{
  matchAnnotationSets(gsNps, nps, matchStyle, doc, true);
}

public static void matchAnnotationSets(AnnotationSet gsNps, AnnotationSet nps, MatchStyleEnum matchStyle, Document doc, boolean outputStats)
{
  int numMatched = 0;

  /*
  for (Annotation a : gsNps) {
	System.out.println(a);
  }
  */

  HashMap<Annotation, Annotation> matched = new HashMap<Annotation, Annotation>();

  // System.out.println("Matching "+gsNps.getName()+" and "+nps.getName());
  for (Annotation a : nps.getOrderedAnnots()) {
    Annotation match;
    switch (matchStyle) {
      case MUC:
        match = matchAnnotationMUCStyle(a, gsNps, doc);
        break;
      case ACE:
        match = matchAnnotationACEStyle(a, gsNps, doc);
        break;
      case UW:
    	match = matchAnnotationUWStyle(a, gsNps, doc);
        break;
      default:
        match = matchAnnotationACEStyle(a, gsNps, doc);
        break;
    }

    if (match != null) {
      numMatched++;
      if (matched.containsKey(match)) {
        doubleMatches++;
        Annotation oldMatch = matched.get(match);

        // Annotation newMatch = null;
        // Double match -- match the key to the longer auto annotation
        if (outputStats) {
          System.out.println("Double match " + doc.getAnnotText(match));
          System.out.println("Matches: [" + doc.getAnnotText(matched.get(match)) + "]" + "] [" + doc.getAnnotText(a)
              + "]");
        }

        boolean conjOldMatch = FeatureUtils.memberArray("and", doc.getWords(oldMatch));
        boolean conjNewMatch = FeatureUtils.memberArray("and", doc.getWords(a));
        if ((oldMatch.getLength() >= a.getLength() && (!conjOldMatch || conjNewMatch)) || (conjNewMatch && !conjOldMatch)) {
          a.setProperty(Property.MATCHED_CE, -1);
          // a.setProperty(FeatureUtils.MATCHED_GS_NP, match.getAttribute("ID"));
          // newMatch = oldMatch;
        }
        else {
          oldMatch.setProperty(Property.MATCHED_CE, -1);
          match.setProperty(Property.MATCHED_CE, Integer.parseInt(a.getAttribute(Constants.CE_ID)));
          a.setProperty(Property.MATCHED_CE, Integer.parseInt(match.getAttribute(Constants.CE_ID)));
          matched.put(match, a);
          // newMatch = match;
        }
        if (outputStats) {
          System.out.println("Resolved to: [" + doc.getAnnotText(matched.get(match)) + "]");
        }
      }
      else {
        matched.put(match, a);
        match.setProperty(Property.MATCHED_CE, Integer.parseInt(a.getAttribute(Constants.CE_ID)));
        a.setProperty(Property.MATCHED_CE, Integer.parseInt(match.getAttribute(Constants.CE_ID)));
      }
    }
    else {
      a.setProperty(Property.MATCHED_CE, -1);
    }
  }



  numMatchedKey += matched.size();
  int gsNpsSize = gsNps == null ? 0 : gsNps.size();

  totalKey += gsNpsSize;
  totalNPsMatched += numMatched;
  if (outputStats) {
    System.out.println("Matched KEY: " + matched.size() + "/" + gsNpsSize + " CEs. RESPONSE: "
    		+numMatched + "/" + nps.size()+" CEs");
  }

  if (Constants.DEBUG && gsNps != null) {
    for (Annotation a : gsNps) {
      if (!matched.containsKey(a)) {
        System.err.println("Not matched key: " + doc.getAnnotText(a) + " -- " + a.getAttribute("ID"));
      }
    }
  }
}

public static Annotation matchAnnotation(Annotation a, Document doc, AnnotationSet key)
{
  Annotation match = null;
  AnnotationSet posannotations = doc.getAnnotationSet(Constants.POS);
  AnnotationSet overlapCoref = key.getOverlapping(a);
  if (overlapCoref != null) {
    // need to determine if any of the coref annotations match the np
    // annotations
    for (Annotation cur : overlapCoref) {
      if (cur.compareSpan(a) == 0) {
        // the two spans match exactly
        match = cur;
        break;
      }

      String min = cur.getAttribute("MIN");
      String[] mins;
      if (min == null) {
        mins = new String[] { doc.getAnnotString(cur) };
      }
      else {
        mins = min.split("\\|");
      }

      // The following criteria must hold
      if (cur.covers(a)) {
        // The np is covered completely by the key annotation
        // or the key's string either matches the head of
        // the np
        Annotation headNoun = HeadNoun.getValue(a, doc);
        String head = doc.getAnnotString(headNoun);

        if (FeatureUtils.memberArray(head, mins)) {
          match = cur;
          break;
        }

        // or the np ends with the min string
        String npString = doc.getAnnotString(a).toLowerCase().replaceAll("\n", " ").replaceAll("\\A\\W", "");
        for (String m : mins) {
          if (npString.endsWith(m.toLowerCase())) {
            match = cur;
            break;
          }
        }

        // or the np's head ends with the min string
        String headString = doc.getAnnotString(a.getStartOffset(), headNoun.getEndOffset()).toLowerCase().replaceAll(
            "\n", " ").replaceAll("\\A\\W", "");
        for (String m : mins) {

          if (headString.endsWith(m.toLowerCase())) {
            match = cur;
            break;
          }

          if (headNoun.compareSpan(HeadNoun.getValue(cur, doc)) == 0 && npString.contains(m.toLowerCase())) {
            match = cur;
            break;
          }
        }
      }

      if (match == null && a.getEndOffset() == cur.getEndOffset()
          || HeadNoun.getValue(a, doc).getEndOffset() == cur.getEndOffset()) {
        // Another case -- the two differ just by a determiner or an adjective
        AnnotationSet diff;
        if (a.covers(cur)) {
          diff = posannotations.getContained(a.getStartOffset(), cur.getStartOffset());
        }
        else {
          diff = posannotations.getContained(cur.getStartOffset(), a.getStartOffset());
        }
        boolean same = true;
        for (Annotation d : diff) {
          if (!(d.getType().startsWith("JJ") || d.getType().startsWith("RB") || d.getType().startsWith("CD")
              || d.getType().equals("DT") || d.getType().matches("\\W+"))) {
            same = false;
          }
        }
        if (same) {
          match = cur;
          break;
        }
      }
      else {
        // Neither the annotation covers the string
        // nor the ends of the two match
        // the two only correspond if the np ends with the min string
        String npString = doc.getAnnotText(a).toLowerCase().replaceAll("\n", " ").replaceAll("\\A\\W", "");

        for (String m : mins) {
          if (npString.endsWith(m.toLowerCase())) {
            match = cur;
            break;
          }
        }

        if (match == null) {
          npString = doc.getAnnotString(a.getStartOffset(), HeadNoun.getValue(a, doc).getEndOffset()).toLowerCase()
              .replaceAll("\n", " ").replaceAll("\\A\\W", "");
          for (String m : mins) {
            if (npString.endsWith(m.toLowerCase())) {
              match = cur;
              break;
            }
          }
        }
      }
    }
  }
  return match;
}

public static Annotation matchAnnotationMUCStyle(Annotation a, AnnotationSet gsNps, Document doc)
{
  Annotation match = null;
  if (gsNps != null) {
    AnnotationSet overlapCoref = gsNps.getOverlapping(a);
    if (overlapCoref != null) {
      // need to determine if any of the coref annotations match the np
      // annotations
      for (Annotation cur : overlapCoref) {
        if (cur.compareSpan(a) == 0) {
          // the two spans match exactly
          match = cur;
          break;
        }

        String min = cur.getAttribute("MIN");
        String[] mins;
        if (min == null || min.length() < 1) {
          mins = new String[] { doc.getAnnotString(cur) };
        }
        else {
          mins = min.split("\\|");
        }

        // The following criteria must hold
        if (coversAnnot(cur, a, doc.getText())) {
          /*
           * EQ -- two coref instances are equal if either
           *       (1) their text string offsets match perfectly
           *       (2) If the response has no MIN strings, then
           *              if the key MIN is a substring of the response TEXT and
           *                 the response TEXT is a substring of the key TEXT
           *                  ====> True
           *       (3) If the response has MIN strings, then use them in place of the
           *           response TEXT string in case (2) above.
           *
           */
          // Case (1)
          if (a.covers(cur)) {
            match = cur;
            break;
          }

          // case (2)
          String respString = doc.getAnnotString(a).toLowerCase().replaceAll("\n", " ").replaceAll("\\A", "");
          // FeatureUtils.getText(a, text).toLowerCase().replaceAll("\n", " ").replaceAll("\\A", "");
          // String keyString = FeatureUtils.getText(cur, text).toLowerCase().replaceAll("\n", " ").replaceAll("\\A",
          // "");
          for (String m : mins) {
            if (respString.contains(m.toLowerCase())) {
              match = cur;
              break;
            }
          }

        }

        if (match == null) {
          // Another case -- the two differ just by a pre or post modifier or a corporate designation
          String diff;
          if (a.getStartOffset() < cur.getStartOffset()) {
            diff = doc.getAnnotString(a.getStartOffset(), cur.getStartOffset());
          }
          else {
            diff = doc.getAnnotString(cur.getStartOffset(), a.getStartOffset());
          }
          boolean same = true;
          String[] words = FeatureUtils.getWords(diff);
          if (diff != null && diff.length() > 0) {
            for (String s : words) {
              if (!FeatureUtils.memberArray(s, PREMODIFIERS)) {
                same = false;
              }
            }
          }
          if (same) {
            if (a.getEndOffset() < cur.getEndOffset()) {
              diff = doc.getAnnotString(a.getEndOffset(), cur.getEndOffset());
            }
            else {
              diff = doc.getAnnotString(cur.getEndOffset(), a.getEndOffset());
            }
            words = FeatureUtils.getWords(diff);
            if (diff != null && diff.length() > 0) {
              for (String s : words) {
                if (!FeatureUtils.memberArray(s, POSTMODIFIERS)) {
                  same = false;
                }
              }
            }
          }
          if (same) {
            match = cur;
            break;
          }
        }
      }
    }
  }
  return match;
}

public static Annotation matchAnnotationACEStyle(Annotation a, AnnotationSet gsNps, Document doc)
{
  Annotation match = null;
  AnnotationSet overlapCoref = gsNps.getOverlapping(a);
  if (overlapCoref != null) {
    // need to determine if any of the coref annotations match the np
    // annotations
    for (Annotation cur : overlapCoref) {
      if (cur.compareSpan(a) == 0) {
        // the two spans match exactly
        match = cur;
        break;
      }
      // Check if two annotations differ only by non-word characters
      String preDifference, postDifference;
      if (a.getStartOffset() < cur.getStartOffset()) {
        preDifference = doc.getAnnotString(a.getStartOffset(), cur.getStartOffset());
      }
      else {
        preDifference = doc.getAnnotString(cur.getStartOffset(), a.getStartOffset());
      }
      if (a.getEndOffset() < cur.getEndOffset()) {
        postDifference = doc.getAnnotString(a.getEndOffset(), cur.getEndOffset());
      }
      else {
        postDifference = doc.getAnnotString(cur.getEndOffset(), a.getEndOffset());
      }
      boolean preSame, postSame;
      if (preDifference.matches("\\W*")) {
        preSame = true;
      }
      else {
        preSame = true;
        String[] words = FeatureUtils.getWords(preDifference);
        if (preDifference.length() > 0) {
          for (String s : words) {
            if (!FeatureUtils.memberArray(s, PREMODIFIERS)) {
              preSame = false;
            }
          }
        }
      }
      if (postDifference.matches("\\W*")) {
        postSame = true;
      }
      else {
        postSame = true;
        String[] words = FeatureUtils.getWords(postDifference);
        if (postDifference.length() > 0) {
          for (String s : words) {
            if (!FeatureUtils.memberArray(s, PREMODIFIERS)) {
              postSame = false;
            }
          }
        }
      }

      if (preSame && postSame) {
        match = cur;
        break;
      }

      int headStart = Integer.parseInt(cur.getAttribute(Constants.HEAD_START));
      int headEnd = Integer.parseInt(cur.getAttribute(Constants.HEAD_END));

      Annotation head = HeadNoun.getValue(a, doc);
      boolean headMatch = false;
      if (head.getEndOffset() == headEnd) {
        if (head.getStartOffset() == headStart) {
          headMatch = true;
        }
        else if (head.getStartOffset() >= headStart) {
          headMatch = true;
          Annotation gsHead = HeadNoun.getValue(cur, doc);
          if (gsHead.compareSpan(head) == 0) {
            headMatch = true;
          }
        }
      }
      // if(FeatureUtils.isProperName(np, annotations, text)){
      // Annotation pn = (Annotation)FeatureUtils.LINKED_PROPER_NAME.getValue(np, annotations, text);
      // if(pn!=null&&pn.getStartOffset()==headStart&&pn.getEndOffset()==headEnd)
      // pnMatch = true;
      // }

      if (headMatch) {
        // Check for conjunctions
        String[] words = FeatureUtils.getWords(preDifference);
        if (preDifference.length() < 1 || !FeatureUtils.memberArray("and", words)) {
          if (headEnd == cur.getEndOffset()) {
            match = cur;
            break;
          }
          else {
            // System.out.println("TEXT: "+Utils.getAnnotText(headEnd, cur.getEndOffset(),text));
            if (!doc.getAnnotString(headEnd, cur.getEndOffset()).trim().startsWith("and")) {
              match = cur;
              break;
            }
            else {
              // System.out.println("STARTS WITH AND");
            }
          }
        }
      }
    }
  }
  return match;

}

public static Annotation matchAnnotationUWStyle(Annotation a, AnnotationSet gsNps, Document doc)
{
  Annotation match = null;
  AnnotationSet overlapCoref = gsNps.getOverlapping(a);
  
  if (overlapCoref != null) {
    // need to determine if any of the coref annotations match the np
    // annotations
    for (Annotation cur : overlapCoref) {
      if (cur.compareSpan(a) == 0) {
        // the two spans match exactly
        match = cur;
        break;
      }
      // Check if two annotations differ only by non-word characters
      String preDifference, postDifference;
      if (a.getStartOffset() < cur.getStartOffset()) {
        preDifference = doc.getAnnotString(a.getStartOffset(), cur.getStartOffset());
      }
      else {
        preDifference = doc.getAnnotString(cur.getStartOffset(), a.getStartOffset());
      }
      if (a.getEndOffset() < cur.getEndOffset()) {
        postDifference = doc.getAnnotString(a.getEndOffset(), cur.getEndOffset());
      }
      else {
        postDifference = doc.getAnnotString(cur.getEndOffset(), a.getEndOffset());
      }
      boolean preSame, postSame;
      if (preDifference.matches("\\W*")) {
        preSame = true;
      }
      else {
        preSame = true;
        String[] words = FeatureUtils.getWords(preDifference);
        if (preDifference.length() > 0) {
          for (String s : words) {
            if (!FeatureUtils.memberArray(s, PREMODIFIERS)) {
              preSame = false;
            }
          }
        }
      }
      if (postDifference.matches("\\W*")) {
        postSame = true;
      }
      else {
        postSame = true;
        String[] words = FeatureUtils.getWords(postDifference);
        if (postDifference.length() > 0) {
          for (String s : words) {
            if (!FeatureUtils.memberArray(s, PREMODIFIERS)) {
              postSame = false;
            }
          }
        }
      }

      if (preSame && postSame) {
        match = cur;
        break;
      }

      //Do the same for the min span of the coreference element
      String min = cur.getAttribute("min");
      //System.out.println(cur);
      if (min != null && min.length() > 1) {
        String[] minSpan = min.split(",");
        int minStart = Integer.parseInt(minSpan[0]);
        int minEnd = Integer.parseInt(minSpan[1]);
      if (a.compareSpan(minStart,minEnd) == 0) {
          // the two spans match exactly
          match = cur;
          break;
        }
        // Check if two annotations differ only by non-word characters
        if (a.getStartOffset() < minStart) {
          preDifference = doc.getAnnotString(a.getStartOffset(), minStart);
        }
        else {
          preDifference = doc.getAnnotString(minStart, a.getStartOffset());
        }
        if (a.getEndOffset() < minEnd) {
          postDifference = doc.getAnnotString(a.getEndOffset(), minEnd);
        }
        else {
          postDifference = doc.getAnnotString(minEnd, a.getEndOffset());
        }
        preSame=false;
        postSame=false;
        if (preDifference.matches("\\W*")) {
          preSame = true;
        }
        else {
          preSame = true;
          String[] words = FeatureUtils.getWords(preDifference);
          if (preDifference.length() > 0) {
            for (String s : words) {
              if (!FeatureUtils.memberArray(s, PREMODIFIERS)) {
                preSame = false;
              }
            }
          }
        }
        if (postDifference.matches("\\W*")) {
          postSame = true;
        }
        else {
          postSame = true;
          String[] words = FeatureUtils.getWords(postDifference);
          if (postDifference.length() > 0) {
            for (String s : words) {
              if (!FeatureUtils.memberArray(s, PREMODIFIERS)) {
                postSame = false;
              }
            }
          }
        }

        if (preSame && postSame) {
          //System.out.println("MIN match ");
          match = cur;
          break;
        }
      }
      
      //Now try to find a head match
      String headTxt = cur.getAttribute("head");
      //System.out.println(cur);
      if (headTxt != null && headTxt.length() > 1) {
        String[] headSpan = headTxt.split(",");
        int headStart = Integer.parseInt(headSpan[0]);
        int headEnd = Integer.parseInt(headSpan[1]);

        Annotation head = HeadNoun.getValue(a, doc);
        boolean headMatch = false;
        if (head.getEndOffset() == headEnd) {
          if (head.getStartOffset() == headStart) {
        	headMatch = true;
          }
          else if (head.getStartOffset() >= headStart) {
            headMatch = true;
            Annotation gsHead = HeadNoun.getValue(cur, doc);
            if (gsHead.compareSpan(head) == 0) {
              headMatch = true;
            }
          }
        }	
        // if(FeatureUtils.isProperName(np, annotations, text)){
        // Annotation pn = (Annotation)FeatureUtils.LINKED_PROPER_NAME.getValue(np, annotations, text);
        // if(pn!=null&&pn.getStartOffset()==headStart&&pn.getEndOffset()==headEnd)
        // pnMatch = true;
        // }

        if (headMatch) {
          // Check for conjunctions
          String[] words = FeatureUtils.getWords(preDifference);
          if (preDifference.length() < 1 || !FeatureUtils.memberArray("and", words)) {
            if (headEnd == cur.getEndOffset()) {
              //System.out.println("HEAD MATCH");
              match = cur;
              break;
            }
            else {
              // System.out.println("TEXT: "+Utils.getAnnotText(headEnd, cur.getEndOffset(),text));
              if (!doc.getAnnotString(headEnd, cur.getEndOffset()).trim().startsWith("and")) {
                //System.out.println("HEAD MATCH");
                match = cur;
                break;
              }
              else {
                // System.out.println("STARTS WITH AND");
              }
            }
          }
        }
      }
    }
  }
  return match;

}
// public Object matchACEStyleOld(Annotation np,Map<String, AnnotationSet> annotations, String text) {
//
// // Get the key annotations
// AnnotationSet key = annotations.get(Constants.ORIG);
// AnnotationSet nps = annotations.get(Constants.NP);
//
// // for(String k:annotations.keySet())
// // System.out.println(k+"  ---  "+annotations.get(k).getName());
//
// key = key.get("COREF");
//
// int numMatched = 0;
//
// // Form the clusters of coreferent nps by performing transitive closure
// int maxID = -1;
//
// // First, we find the largest key
// for (Annotation k : key) {
// //System.err.println("Working on "+k +" - "+k.getAttribute("ID"));
// int id = Integer.parseInt((String) k.getAttribute("ID"));
// maxID = maxID > id ? maxID : id;
// }
//
// // Create an array of pointers
// UnionFind uf = new UnionFind(maxID);
//
// //Intialize the next id
// setMaxID(maxID);
//
// for (Annotation k : key) {
// int id = Integer.parseInt((String) k.getAttribute("ID"));
// String refString = (String) k.getAttribute("REF");
// int ref = refString == null ? -1 : Integer.parseInt(refString);
// if (ref > -1){
// uf.merge(id, ref);
// //FeatureUtils.union(id, ref, ptrs);
// }
// }
//
// AnnotationSet gsNps = new AnnotationSet("gsNPs");
// for (Annotation k:key){
// k.setProperty(FeatureUtils.COREF_ID, uf.find(Integer.parseInt(k.getAttribute("ID"))));
// if(k.getAttribute("STATUS")!=null){
// k.setProperty(FeatureUtils.STATUS, k.getAttribute("STATUS"));
// }
// gsNps.add(k);
// }
//
// HashMap<Annotation, Annotation> matched = new HashMap<Annotation, Annotation>();
//
// for (Annotation a : nps.getOrderedAnnots()) {
// Annotation match = null;
// AnnotationSet overlapCoref = gsNps.getOverlapping(a);
// if (overlapCoref != null) {
// // need to determine if any of the coref annotations match the np
// // annotations
// for (Annotation cur : overlapCoref) {
// if (cur.compareSpan(a) == 0) {
// //the two spans match exactly
// match = cur;
// break;
// }
// //Check if two annotations differ only by non-word characters
// String difference = "";
// if(a.getStartOffset()<cur.getStartOffset())
// difference = Utils.getAnnotText(a.getStartOffset(), cur.getStartOffset(),text);
// else
// difference = Utils.getAnnotText(cur.getStartOffset(), a.getStartOffset(),text);
// if(a.getEndOffset()<cur.getEndOffset())
// difference += Utils.getAnnotText(a.getEndOffset(), cur.getEndOffset(),text);
// else
// difference += Utils.getAnnotText(cur.getEndOffset(), a.getEndOffset(),text);
// if(difference.matches("\\W*")){
// match = cur;
// break;
// }
//
// String min = (String) cur.getAttribute("MIN");
// String[] mins;
// if (min == null || min.length()<1)
// mins = new String[]{Utils.getAnnotText(cur, text)};
// else
// mins = min.split("\\|");
//
// //The following criteria must hold
// if (coversAnnot(cur, a, text)) {
// /*
// * EQ -- two coref instances are equal if either
// * (1) their text string offsets match perfectly
// * (2) If the response has no MIN strings, then
// * if the key MIN is a substring of the response TEXT and
// * the response TEXT is a substring of the key TEXT
// * ====> True
// * (3) If the response has MIN strings, then use them in place of the
// * response TEXT string in case (2) above.
// *
// */
// //Case (1)
// if(a.covers(cur)) {
// match = cur;
// break;
// }
//
//
// // case (2)
// String respString = Utils.getAnnotText(a, text).toLowerCase().replaceAll("\n", " ").replaceAll("\\A",
// "");//FeatureUtils.getText(a, text).toLowerCase().replaceAll("\n", " ").replaceAll("\\A", "");
// //String keyString = FeatureUtils.getText(cur, text).toLowerCase().replaceAll("\n", " ").replaceAll("\\A", "");
// for (String m : mins) {
// if (respString.contains(m.toLowerCase())) {
// match = cur;
// break;
// }
// }
//
// }
//
//
// if(match==null ){
// //Another case -- the two differ just by a pre or post modifier or a corporate designation
// String diff;
// if(a.getStartOffset()<cur.getStartOffset())
// diff = Utils.getAnnotText(a.getStartOffset(), cur.getStartOffset(),text);
// else
// diff = Utils.getAnnotText(cur.getStartOffset(), a.getStartOffset(),text);
// boolean same = true;
// String[] words = FeatureUtils.getWords(diff);
// if(diff!=null&&diff.length()>0){
// for(String s:words){
// if(!FeatureUtils.memberArray(s, PREMODIFIERS))//
// same=false;
// }
// }
// if(same){
// if(a.getEndOffset()<cur.getEndOffset())
// diff = Utils.getAnnotText(a.getEndOffset(), cur.getEndOffset(),text);
// else
// diff = Utils.getAnnotText(cur.getEndOffset(), a.getEndOffset(),text);
// words = FeatureUtils.getWords(diff);
// if(diff!=null&&diff.length()>0){
// for(String s:words){
// if(!FeatureUtils.memberArray(s, POSTMODIFIERS))//
// same=false;
// }
// }
// }
// if(same){
// match = cur;
// break;
// }
// }
// }
// }
//
// int id = (match == null) ? -1 : Integer.parseInt((String) match.getAttribute("ID"));
//
// if(match!=null){
// numMatched++;
// if(matched.containsKey(match)){
// doubleMatches++;
// Annotation oldMatch = matched.get(match);
// Annotation newMatch = null;
// //Double match -- match the key to the longer auto annotation
// System.out.println("Double match "+FeatureUtils.getText(match, text));
// System.out.println("Matches: ["+FeatureUtils.getText(matched.get(match), text)+"]"+"] ["+FeatureUtils.getText(a,
// text)+"]");
// if(oldMatch.getLength()>=a.getLength()){
// a.setProperty(FeatureUtils.MATCHED_CE, -1);
// a.setProperty(FeatureUtils.MATCHED_CE, match.getAttribute("ID"));
// id=-1;
// newMatch=oldMatch;
// }else{
// oldMatch.setProperty(FeatureUtils.MATCHED_CE, -1);
// oldMatch.setProperty(this, new Integer(-1));
// match.setProperty(FeatureUtils.MATCHED_CE, a.getAttribute(Constants.CE_ID));
// a.setProperty(FeatureUtils.MATCHED_CE, match.getAttribute("ID"));
// matched.put(match, a);
// newMatch=match;
// }
// System.out.println("Resolved to: ["+FeatureUtils.getText(matched.get(match), text)+"]");
// }else{
// matched.put(match, a);
// match.setProperty(FeatureUtils.MATCHED_CE, a.getAttribute(Constants.CE_ID));
// a.setProperty(FeatureUtils.MATCHED_CE, match.getAttribute("ID"));
// }
// }else{
// a.setProperty(FeatureUtils.MATCHED_CE, -1);
// }
//
// int coref = (id < 0) ? getNextID() : uf.find(id); //FeatureUtils.find(id, ptrs);
// a.setProperty(this, new Integer(coref));
// }
//
// System.out.println("Matched "+numMatched+"/"+nps.size());
// numMatchedKey+=matched.size();
// totalKey += gsNps.size();
// totalNPsMatched += numMatched;
// System.out.println("MUC nps matched "+matched.size()+"/"+gsNps.size());
//
// annotations.put(Constants.GS_NP, gsNps);
//
// if(Constants.DEBUG){
// for(Annotation a:gsNps){
// if(!matched.containsKey(a)){
// System.err.println("Not matched key: "+FeatureUtils.getText(a,text)+" -- "+a.getAttribute("ID"));
// }
// }
// }
//
// return np.getProperty(this);
// }

private static boolean coversAnnot(Annotation bigger, Annotation smaller, String text)
{
  String diff;
  boolean coversStart = true;
  if (bigger.getStartOffset() <= smaller.getStartOffset()) {
    coversStart = true;
  }
  else {
    diff = Utils.getAnnotText(smaller.getStartOffset(), bigger.getStartOffset(), text);
    String[] words = FeatureUtils.getWords(diff);
    // System.out.println("diff: "+diff);
    if (diff != null && diff.length() > 0) {
      for (String s : words) {
        if (!FeatureUtils.memberArray(s, PREMODIFIERS)) {
          coversStart = false;
        }
      }
    }
  }

  if (!coversStart) return false;
  boolean coversEnd = true;
  if (smaller.getEndOffset() <= bigger.getEndOffset()) {
    coversEnd = true;
  }
  else {
    diff = Utils.getAnnotText(bigger.getEndOffset(), smaller.getEndOffset(), text);
    String[] words = FeatureUtils.getWords(diff);
    if (diff != null && diff.length() > 0) {
      for (String s : words) {
        if (!FeatureUtils.memberArray(s, POSTMODIFIERS)) {
          coversEnd = false;
        }
      }
    }
  }

  if (!coversEnd) return false;
  // else{
  // System.out.println("["+Utils.getAnnotText(bigger, text)+"] covers ["+Utils.getAnnotText(smaller, text)+"]");
  // }
  return true;
}

public static void nullifyCounters()
{
  numMatchedKey = 0;
  totalKey = 0;
  totalNPsMatched = 0;
  doubleMatches = 0;
}
private static String[] PREMODIFIERS = { "A", "AN", "THE" };
private static String[] POSTMODIFIERS = { ";", "'", ",", "." };
// private static String[] CORPORATE_DESIGN = { "CO", "COS.", "COMPANY", "CORP", "CORPORATION", "GP", "G.P.",
// "GENERAL PARTNERSHIP", "INC", "INCORPORATED", "LTD", "LIMITED", "LP", "LIMITED PARTNERSHIP", "NL", "NO LIABILITY",
// "NPL", "NO PERSONAL LIABILITY", "PLC", "PUBLIC LIMITED COMPANY", "PTE LTD", "PRIVATE LIMITED COMPANY", "PTY LTD",
// "PROPRIETARY LIMITED", "WLL", "WITH LIMITED RESPONSIBILITY", "ANONIM SIRKETI", "A.O.", "A/S", "BERHAD", "BHD",
// "B.M.", "BSC", "C.A.", "C.V.", "GMBH", "H.F.", "I/S", "KG", "KK", "KY", "N.V.", "OHG", "O.E.", "OY", "S.A.",
// "S.A. DE C.V.", "S.N.C.", "S.L.", "S.P.A.", "SV", "T.A.S.", "VN" };// , "LLC", "LLC.", "INC."};

}
