/**
 * 
 */
package reconcile.general;

import java.util.ArrayList;
import java.util.List;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.features.FeatureUtils;


/**
 * @author ves General utility methods that are used for generation of features
 */
public class SyntaxUtils {

private static String PARENT = "parent";
private static String CHILD_IDS = "CHILD_IDS";
public static String[] NPType = { "NP", "NX" };

public static Annotation getParent(Annotation parseAnnot, AnnotationSet parse)
{
  // if(parseAnnot==null)
  // throw new RuntimeException("NULUUUUU");
  // System.err.print("Annot: "+parseAnnot);
  String parentStr = parseAnnot.getAttribute(PARENT);
  if (parentStr == null) return null;
  int par = Integer.parseInt(parentStr);
  if (par < 0)
    return null;
  else
    return parse.get(par);
}

public static AnnotationSet getChildren(Annotation parseAnnot, AnnotationSet parse)
{
  AnnotationSet result = new AnnotationSet("children");
  String childString = parseAnnot.getAttribute(CHILD_IDS);
  if (childString != null) {
    String[] childIds = childString.split("\\,");
    for (String childId : childIds) {
      int id = Integer.parseInt(childId);
      result.add(parse.get(id));
    }
  }
  return result;
}

public static List<Annotation> getOrderedChildren(Annotation parseAnnot, AnnotationSet parse)
{
  AnnotationSet contained = parse.getContained(parseAnnot);
  AnnotationSet children = new AnnotationSet("children");

  for (Annotation a : contained) {
    int par = Integer.parseInt(a.getAttribute(PARENT));
    if (par == parseAnnot.getId()) {
      children.add(a);
    }
  }
  return children.getOrderedAnnots();
}

public static Annotation getGov(Annotation depAnnot, AnnotationSet dep)
{
  // System.err.print("Annot: "+parseAnnot);
  // String[] span = ((String)depAnnot.getAttribute("GOV")).split("\\,");
  String govId = depAnnot.getAttribute("GOV_ID");
  if (govId == null) return null;
  int id = Integer.parseInt(govId);
  if (id < 0) return null;
  // int stSpan = Integer.parseInt(span[0]);
  // int endSpan = Integer.parseInt(span[1]);
  Annotation res = dep.get(id);
  // Annotation res = dep.getContained(stSpan, endSpan).getFirst();
  return res;
}

public static Annotation getGovPosNode(Annotation depAnnot, AnnotationSet parse)
{
  // System.err.print("Annot: "+parseAnnot);
  String spanStr = depAnnot.getAttribute("GOV");
  if (spanStr == null) return null;
  String[] span = spanStr.split("\\,");
  int stSpan = Integer.parseInt(span[0]);
  int endSpan = Integer.parseInt(span[1]);
  Annotation res = getNode(stSpan, endSpan, parse);
  return res;
}

public static Annotation getPosNode(int start, int end, AnnotationSet pos)
{
  // System.err.print("Annot: "+parseAnnot);
  Annotation res = pos.getContained(start, end).getFirst();
  return res;
}

public static boolean areSiblings(Annotation an1, Annotation an2, AnnotationSet parse)
{
  Annotation par1 = getParent(an1, parse), par2 = getParent(an2, parse);
  if (par1 == null || par2 == null) return false;
  return par1.compareSpan(par2) == 0;
}

public static boolean dominates(Annotation an1, Annotation an2, AnnotationSet parse)
{
  if (an1 == null || an2 == null) return false;
  return an1.covers(an2);
}

public static boolean cCommands(Annotation an1, Annotation an2, AnnotationSet parse)
{
  if (an1 == null || an2 == null) return false;
  return !dominates(an1, an2, parse) && !dominates(an2, an1, parse) && dominates(getParent(an1, parse), an2, parse);
}

public static boolean isRExpression(String s)
{
  return !FeatureUtils.isPronoun(s) && !FeatureUtils.isReflexive(s) && !FeatureUtils.isDemonstrative(s);
}

public static Annotation localDomain(Annotation parseAnnot, AnnotationSet parse)
{
  if (parseAnnot == null) return null;
  Annotation parent = getParent(parseAnnot, parse);
  String[] types = { "NP", "S" };
  while (parent != null) {
    String type = parent.getType();
    if (FeatureUtils.memberArray(type, types)) return parent;
    parent = getParent(parent, parse);
  }
  return null;
}

public static Annotation getVP(Annotation an, AnnotationSet parse)
{
  if (an == null) return null;
  Annotation node = getNode(an.getStartOffset(), an.getEndOffset(), parse);
  while (node != null && !node.getType().equals("VP")) {
    node = getParent(node, parse);
  }
  return node;
}

public static Annotation getNode(Annotation an, AnnotationSet parse)
{
  if (an == null) return null;
  return getNode(an.getStartOffset() - 1, an.getEndOffset() + 1, parse);
}

public static Annotation getNode(int start, int end, AnnotationSet parse)
{
  AnnotationSet p = parse.getContained(start, end);
  int largest = 0;
  Annotation max = null;
  for (Annotation r : p) {
    if ((r.getEndOffset() - r.getStartOffset() >= largest)) {
      largest = r.getEndOffset() - r.getStartOffset();
      max = r;
    }
  }

  return max;
}

public static Annotation getHighestNode(Annotation an, AnnotationSet parse)
{
  if (an == null) return null;
  return getHighestNode(an.getStartOffset() - 1, an.getEndOffset() + 1, parse);
}

public static Annotation getHighestNode(int start, int end, AnnotationSet parse)
{
  AnnotationSet p = parse.getContained(start, end);
  int largest = 0;
  Annotation max = null;
  for (Annotation r : p) {
    if ((r.getEndOffset() - r.getStartOffset() >= largest)) {
      largest = r.getEndOffset() - r.getStartOffset();
      max = r;
    }
  }
  if (max == null) return null;
  Annotation parent = getParent(max, parse);
  while (parent != null && parent.covered(start, end)) {
    max = parent;
    parent = getParent(max, parse);
  }

  return max;
}

/*
 * Get the smallest node fully containing an annotation
 */
public static Annotation getSmallestContainingNode(int start, int end, AnnotationSet parse)
{
  AnnotationSet p = parse.getOverlapping(start, end);
  int smallest = Integer.MAX_VALUE;
  Annotation min = null;
  for (Annotation r : p) {
    if (r.covers(start, end) && (r.getEndOffset() - r.getStartOffset() <= smallest)) {
      smallest = r.getEndOffset() - r.getStartOffset();
      min = r;
    }
  }

  return min;
}

public static Annotation getSmallestContainingNode(Annotation a, AnnotationSet parse)
{
  return getSmallestContainingNode(a.getStartOffset(), a.getEndOffset(), parse);
}

// Get the largest NP containing an annotation
public static Annotation getNP(Annotation an, AnnotationSet parse)
{
  AnnotationSet p = parse.get("NP");
  if (an == null || p == null) return null;
  p = p.getOverlapping(an);
  if (p == null) return null;
  int largest = Integer.MIN_VALUE;
  Annotation max = null;
  for (Annotation r : p) {
    if ((r.getEndOffset() - r.getStartOffset() > largest)) {
      largest = r.getEndOffset() - r.getStartOffset();
      max = r;
    }
  }

  return max;
}

// Get the largest np contained in the annotation
public static Annotation getContainedNP(Annotation an, AnnotationSet parse)
{
  if (an == null) return null;
  AnnotationSet p = parse.getContained(an.getStartOffset() - 1, an.getEndOffset() + 1).get("NP");
  if (p == null) return null;
  p = p.getOverlapping(an);
  if (p == null) return null;
  int largest = Integer.MIN_VALUE;
  Annotation max = null;
  for (Annotation r : p) {
    if ((r.getEndOffset() - r.getStartOffset() > largest)) {
      largest = r.getEndOffset() - r.getStartOffset();
      max = r;
    }
  }

  return max;
}

// Get the smallest np containing an annotation
public static Annotation getContainingNP(Annotation an, AnnotationSet parse)
{
  AnnotationSet p = parse.getOverlapping(an).get("NP");
  if (an == null || p == null) return null;
  p = p.getOverlapping(an);
  if (p == null) return null;
  int smallest = Integer.MAX_VALUE;
  Annotation min = null;
  for (Annotation r : p) {
    if ((r.getEndOffset() - r.getStartOffset() < smallest)) {
      smallest = r.getEndOffset() - r.getStartOffset();
      min = r;
    }
  }

  return min;
}

// Get the smallest clause containing an annotation
public static Annotation getClause(Annotation an, AnnotationSet parse)
{
  return getClause(an.getStartOffset(), an.getEndOffset(), parse);
}

public static Annotation getClause(int startOffset, int endOffset, AnnotationSet parse)
{
  AnnotationSet p = parse.get("S");
  if (p == null) return null;
  p = p.getOverlapping(startOffset, endOffset);
  if (p == null) return null;
  int smallest = Integer.MAX_VALUE;
  Annotation min = null;
  for (Annotation r : p) {
    if (r.covers(startOffset, endOffset) && (r.getEndOffset() - r.getStartOffset() <= smallest)) {
      smallest = r.getEndOffset() - r.getStartOffset();
      min = r;
    }
  }

  return min;
}

public static Annotation getRealClause(Annotation an, AnnotationSet parse)
{
  return getRealClause(an.getStartOffset(), an.getEndOffset(), parse);
}

public static Annotation getRealClause(int startOffset, int endOffset, AnnotationSet parse)
{
  AnnotationSet p = parse.get("S");
  if (p == null) return null;
  p = p.getOverlapping(startOffset, endOffset);
  if (p == null) return null;
  int smallest = Integer.MAX_VALUE;
  Annotation min = null;
  for (Annotation r : p) {
    if (r.covers(startOffset, endOffset) && (r.getEndOffset() - r.getStartOffset() <= smallest)) {
      smallest = r.getEndOffset() - r.getStartOffset();
      min = r;
    }
  }

  if (min == null) return min;
  // make sure this is not a prepositional clause
  String[] preps = { "IN", "TO" };
  AnnotationSet prep = parse.get(preps);
  if (prep != null) {
    boolean pc = false;
    for (Annotation pr : prep) {
      if (min.getStartOffset() == pr.getStartOffset()) {
        pc = true;
      }
    }

    if (pc) {
      Annotation result = getParent(min, parse);
      min = result == null ? min : result;
    }
    else {
      Annotation parent = getParent(min, parse);
      if (parent != null && parent.getType().equals("SBAR")) {
        System.out.println("Getting the dad");
        Annotation result = getParent(parent, parse);
        min = result == null ? min : result;
      }
    }
  }
  return min;
}

public static Annotation getClauseOrPP(Annotation an, AnnotationSet parse)
{
  AnnotationSet p = parse.get(new String[] { "S", "SBAR", "PP", "X" });
  if (an == null || p == null) return null;
  p = p.getOverlapping(an);
  if (p == null) return null;
  int smallest = Integer.MAX_VALUE;
  Annotation min = null;
  for (Annotation r : p) {
    if ((r.getEndOffset() - r.getStartOffset() <= smallest)) {
      smallest = r.getEndOffset() - r.getStartOffset();
      min = r;
    }
  }

  return min;
}

// Get the smallest phrase containing an annotation
public static Annotation getPhrase(Annotation an, AnnotationSet parse)
{

  if (an == null || parse == null) return null;
  parse = parse.getOverlapping(an);
  if (parse == null) return null;
  int smallest = Integer.MAX_VALUE;
  Annotation min = null;
  for (Annotation r : parse) {
    if (r.covers(an) && (r.getType().startsWith("S") || r.getType().equals("VP"))
        && (r.getEndOffset() - r.getStartOffset() <= smallest)) {
      smallest = r.getEndOffset() - r.getStartOffset();
      min = r;
    }
  }

  return min;
}

public static Annotation getParentClause(Annotation clause, AnnotationSet parse)
{
  if (clause == null) return null;
  Annotation cur = getParent(clause, parse);
  while (cur != null && !cur.getType().equals("S")) {
    cur = getParent(cur, parse);
  }
  return cur;
}

public static boolean isMainClause(Annotation an, AnnotationSet parse)
{
  Annotation clause = getClause(an, parse);

  if (clause == null) return false;
  Annotation parent = getParent(clause, parse);
  boolean mainClause = parent == null || parent.getType().equalsIgnoreCase("ROOT");
  // System.err.println(mainClause + " Clause "+clause+parent);
  return mainClause;

}

public static Annotation getDepNode(Annotation an, AnnotationSet dep)
{
  if (an == null || dep == null) return null;
  AnnotationSet p = dep.getContained(an);

  return p.getLast();
}

public static ArrayList<Annotation> getDepPath(Annotation head1, Annotation head2, AnnotationSet dep,
    AnnotationSet parse)
{
  // get the dependency path between the two annotations
  ArrayList<Annotation> result = new ArrayList<Annotation>();
  Annotation node1 = getDepNode(head1, dep);
  Annotation node2 = getDepNode(head2, dep);
  if (node1 == null || node2 == null) // System.err.println("No parse node found for "+an1);
    return result;
  // go to the top level on the second annotation
  ArrayList<Annotation> dep2 = new ArrayList<Annotation>();
  Annotation cur = node2;
  dep2.add(getNode(cur.getStartOffset(), cur.getEndOffset(), parse));
  while (cur != null) {
    Annotation p = getGovPosNode(cur, parse);
    if (dep2.contains(p) || p == null) {
      break;
    }
    dep2.add(p);
    // System.err.println("Adding "+p.getType()+" GOV "+cur.getAttribute("GOV"));
    cur = getGov(cur, dep);
  }

  // now, go through the first annotation's path
  ArrayList<Annotation> dep1 = new ArrayList<Annotation>();
  // ArrayList<String> dep1Types = new ArrayList<String>();
  Annotation cur1 = node1;
  Annotation p = getNode(cur1.getStartOffset(), cur1.getEndOffset(), parse);
  int count = 0;
  boolean match = false;
  while (cur1 != null && !match) {
    // System.err.println("Woorking on "+p.getType()+" GOV "+cur1.getAttribute("GOV"));
    if ((count = dep2.indexOf(p)) >= 0) {
      match = true;
      // System.err.println("Matching on "+p.getType());
    }
    else {
      if (dep1.contains(p)) return result;
      dep1.add(p);
      p = getGovPosNode(cur1, parse);
      cur1 = getGov(cur1, dep);
    }
  }
  // System.err.println("Last  "+p.getType());
  if (match || (count = dep2.indexOf(p)) >= 0) {
    result = dep1;
    // System.err.println("Matching on "+p.getType());
    for (int i = count; i >= 0; i--) {

      result.add(dep2.get(i));
    }
  }

  return result;
}

public static String getDepType(Annotation node1, Annotation node2, AnnotationSet dep)
{
  if (node1 == null || node2 == null) return null;
  Annotation dep1 = getDepNode(node1, dep);
  Annotation dep2 = getDepNode(node2, dep);
  if (dep1 == null || dep2 == null) return null;
  Annotation gov1 = getGov(dep1, dep);
  Annotation gov2 = getGov(dep2, dep);
  if (gov1 != null && gov1.equals(dep2)) return dep1.getType();
  if (gov2 != null && gov2.equals(dep1)) return dep2.getType();
  return null;
}

public static boolean isCopular(String verb)
{
  String[] additionals = { "become", "becomes", "became", "remain", "remains", "remained" };// , "keep", "keeps",
                                                                                            // "kept", "call", "calls",
                                                                                            // "called", "name",
                                                                                            // "names", "named"};
  return FeatureUtils.memberArray(verb, TO_BE) || FeatureUtils.memberArray(verb, additionals);
}

public static boolean isPreposition(String type)
{
  return type.equals("IN") || type.equals("TO");
}

public static boolean isPrednom(Annotation head1, Annotation head2, AnnotationSet dep, AnnotationSet parse, String t)
{
  // System.err.println("isPrednom");
  Annotation node1 = getDepNode(head1, dep);
  if (node1 == null) // System.err.println("No parse node found for "+an1);
    return false;
  boolean prednom = false;
  if (!node1.getType().equalsIgnoreCase("SUBJECT")) return false;
  if (isGovernor(head2, node1)) {
    for (Annotation a : dep) {
      if (a.getType().equalsIgnoreCase("cop") && isGovernor(head2, a)) {
        prednom = true;
      }
      if (a.getType().equalsIgnoreCase("neg") && isGovernor(head2, a)) return false;
    }
  }
  if (!prednom) {
    // Try a conjuction (i.e., head2 is a conjuct with an np that is in prednom relation
    Annotation node2 = getDepNode(head2, dep);
    if (node2 == null) // System.err.println("No parse node found for "+an1);
      return false;
    if (node2.getType().equalsIgnoreCase("conj")) {
      Annotation cur = node2;
      while (node2 != null && node2.getType().equalsIgnoreCase("conj")) {
        cur = node2;
        // System.out.print(node2.toString()+": "+Utils.getAnnotText(node2, t)+" ----conj--- ");
        String[] span = (node2.getAttribute("GOV")).split("\\,");
        int stSpan = Integer.parseInt(span[0]);
        int endSpan = Integer.parseInt(span[1]);
        node2 = dep.getContained(stSpan, endSpan).getFirst();
        // if(node2!=null)
        // System.out.println(Utils.getAnnotText(node2, t));
      }
      String[] span = (cur.getAttribute("GOV")).split("\\,");
      int stSpan = Integer.parseInt(span[0]);
      int endSpan = Integer.parseInt(span[1]);
      head2 = parse.getContained(stSpan, endSpan).getFirst();

      if (isGovernor(head2, node1)) {
        for (Annotation a : dep) {
          if (a.getType().equalsIgnoreCase("cop") && isGovernor(head2, a)) {
            // System.out.println(Utils.getAnnotText(head1, t)+" prednom "+Utils.getAnnotText(head2, t));
            prednom = true;
          }
          if (a.getType().equalsIgnoreCase("neg") && isGovernor(head2, a)) return false;
        }
      }
    }
  }
  // if(prednom)
  // System.out.println(Utils.getAnnotText(head1, t)+" prednom "+Utils.getAnnotText(head2, t));
  return prednom;
}

public static Annotation findPrednom(Annotation an, AnnotationSet dep, String t)
{
  boolean isPrednom = false;
  for (Annotation a : dep) {
    if (a.getType().equalsIgnoreCase("cop") && isGovernor(an, a)) {
      isPrednom = true;
    }
  }
  if (!isPrednom) return null;
  for (Annotation d : dep) {
    if (d.getType().equalsIgnoreCase("SUBJECT")) {
      if (isGovernorOverlap(an, d)) return d;
    }
  }
  return null;
}

public static boolean isMaximalNP(Annotation an, AnnotationSet parse)
{

  Annotation par = getParent(an, parse);

  if (isNP(par))
    return false;
  else if (par == null)
    return true;
  else
    return isMaximalNP(par, parse);
}

public static boolean isNP(Annotation an)
{
  try {
    String type = an.getType();

    if (FeatureUtils.memberArray(type, NPType)) return true;
  }
  catch (NullPointerException npe) {
    return false;
  }

  return false;
}

public static Annotation getSubject(Annotation verb, Annotation sent, AnnotationSet dep, AnnotationSet parse)
{
  AnnotationSet sDep = dep.getOverlapping(sent);
  // System.err.println("Verb is "+verb);
  if (sDep == null || sDep.size() < 1) return null;
  for (Annotation a : sDep) {
    // System.err.println("Trying annotation "+a);
    if (a.getType().equalsIgnoreCase("SUBJECT")) {
      Annotation gov = getGovPosNode(a, parse);
      // System.err.println("----- gov "+gov);
      if (gov != null && verb.overlaps(gov)) return a;
    }
  }
  return null;
}

public static Annotation getSubjectClause(Annotation verb, Annotation sent, AnnotationSet dep, AnnotationSet parse)
{
  Annotation subj = getSubject(verb, sent, dep, parse);
  if (subj == null) return getClause(verb.getStartOffset(), verb.getEndOffset(), parse);
  return getNP(subj, parse);
}

// Is an the governor of the dependency annotation dep
public static boolean isGovernor(Annotation an, Annotation dep)
{
  if (dep == null || an == null) return false;
  String span = dep.getAttribute("GOV");
  String[] offsets = span.split(",");
  int start = Integer.parseInt(offsets[0]), end = Integer.parseInt(offsets[1]);
  // System.err.println("covered "+start+"-"+end+"an "+an);
  return an.covered(start, end);
}

// Is an the governor of the dependency annotation dep
public static boolean isGovernorOverlap(Annotation an, Annotation dep)
{
  if (dep == null || an == null) return false;
  String span = dep.getAttribute("GOV");
  String[] offsets = span.split(",");
  int start = Integer.parseInt(offsets[0]), end = Integer.parseInt(offsets[1]);
  // System.err.println("covered "+start+"-"+end+"an "+an);
  return an.overlaps(start, end);
}
private static String[] TO_BE = { "am", "is", "are", "was", "were", "be", "been", "'s", "'re" };
// private static String[] TO_DO = { "does", "do", "did" };
// private static String[] TO_HAVE = { "have", "had", "has" };
// private static String[] MODALS = { "would", "could", "should", "can", "cannot", "must" };
// private static String[] AUXILIARIES = { "am", "is", "are", "was", "were", "be", "been", "does", "do", "did", "have",
// "had", "has", "would", "could", "should", "can", "cannot", "must" };

}
