/***
 * This class implements 2 rule resolvers that compute possible antecedents and a number of supporting methods.
 * 
 */
package reconcile.general;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import reconcile.Constructor;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.featureVector.Feature;
import reconcile.featureVector.NominalFeature;
import reconcile.featureVector.NumericFeature;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.NumberEnum;
import reconcile.features.FeatureUtils.PersonPronounTypeEnum;
import reconcile.features.properties.Embedded;
import reconcile.features.properties.GramRole;
import reconcile.features.properties.HeadNoun;
import reconcile.features.properties.InQuote;
import reconcile.features.properties.Modifier;
import reconcile.features.properties.NPSemanticType;
import reconcile.features.properties.Number;
import reconcile.features.properties.Pronoun;
import reconcile.features.properties.ProperName;
import reconcile.features.properties.ProperNameType;
import reconcile.features.properties.Property;
import reconcile.features.properties.SentNum;


public class RuleResolvers {

public static final String[] DEFINITES = { "the", "this", "that", "these", "those" };
private static HashSet<Annotation>[] clusters;
// private static UnionFind clust;
private static int[] ptrs;
private static FeatureVectorMap fvm;
private static boolean DEBUG = false;
private static boolean CACHE_FVS = true;
private static boolean PERFORM_CONSISTENCY_CHECK = false;
private static boolean LOOSE_MATCH = false;

public static boolean isDefinite(Annotation np, Document doc)
{
  String firstWord = doc.getWords(np)[0];
  if (FeatureUtils.containsProperName(np, doc)) return false;
  return FeatureUtils.isPronoun(firstWord) || FeatureUtils.memberArray(firstWord, DEFINITES);
}

public static boolean isIndefinite(Annotation np, Document doc)
{
  String firstWord = doc.getWords(np)[0];
  if (FeatureUtils.containsProperName(np, doc)) return false;
  if (FeatureUtils.isPronoun(firstWord)) return false;
  if (FeatureUtils.memberArray(firstWord, DEFINITES)) return false;
  return true;
}

public static boolean isSemidefinite(Annotation np, Document doc)
{
  // System.err.println(np+FeatureUtils.getText(np, text));
  String firstWord = doc.getWords(np)[0];
  if (FeatureUtils.isPronoun(firstWord)) return false;
  if (FeatureUtils.memberArray(firstWord, DEFINITES)) return false;
  return FeatureUtils.containsProperName(np, doc);
}

/*
 * For a given np get a list of all nps that are in possessive relation (signified by
 *  the words "of" and "'s"
 */
public static void addPossesives(Annotation np, Document doc, Map<Annotation, ArrayList<Annotation>> posessives)
{
  int end = np.getEndOffset();
  // get all nps that start within 2 to 5 characters of the end of the np
  AnnotationSet nps = doc.getAnnotationSet(Constants.NP);
  AnnotationSet candidates = nps.getOverlapping(end + 2, end + 5);
  ArrayList<Annotation> coresp = posessives.get(np);
  if (coresp == null) {
    coresp = new ArrayList<Annotation>();
  }
  if (candidates == null) {
    posessives.put(np, coresp);
    return;
  }
  for (Annotation c : candidates) {
    if (c.getStartOffset() >= end + 2) {
      String[] wds = FeatureUtils.getWords(doc.getAnnotText(end, c.getStartOffset()));
      if (wds != null && wds.length == 1) {
        if (wds[0].equalsIgnoreCase("of")) {
          // we have x of y -- x is the pos of y
          coresp.add(c);
        }
        if (wds[0].equalsIgnoreCase("'s")) {
          // we have y's x -- x is the pos of y
          ArrayList<Annotation> coresp2 = posessives.get(c);
          if (coresp2 == null) {
            coresp2 = new ArrayList<Annotation>();
            posessives.put(c, coresp2);
          }
          coresp2.add(np);
        }
      }
    }
  }
  posessives.put(np, coresp);
}

/*
 * Compute and add the annotations that are possesives for all annotations in the document
 */
public static void addAllPossesives(AnnotationSet nps, Document doc, Map<Annotation, ArrayList<Annotation>> posessives)
{
  for (Annotation np : nps) {
    addPossesives(np, doc, posessives);
  }
}

public static NPType getNPtype(Annotation np, Document doc, Map<Annotation, ArrayList<Annotation>> posessives)
{
  if (FeatureUtils.isPronoun(np, doc)) return NPType.PRONOUN;
  if (ProperName.getValue(np, doc)) return NPType.PROPER_NAME;
  if (isSemidefinite(np, doc)) return NPType.SEMIDEFINITE;
  // if annotation is in a posessive relation with a NE, then it is semidefinite
  ArrayList<Annotation> pos = posessives.get(np);
  for (Annotation a : pos)
    if (FeatureUtils.containsProperName(a, doc)) return NPType.SEMIDEFINITE;
  if (isIndefinite(np, doc)) return NPType.INDEFINITE;

  return NPType.DEFINITE;
}

public static void ruleResolvePronouns(Document doc)
{
  ruleResolvePronouns(doc.getAnnotationSet(Constants.NP), doc);
}

@SuppressWarnings("unchecked")
public static void ruleResolvePronouns(AnnotationSet basenp, Document doc)
{
  // System.out.println("Rule resolving ");
  // HashMap<Annotation, ArrayList<Annotation>> posessives = new HashMap<Annotation, ArrayList<Annotation>>();
  Annotation[] basenpArray = basenp.toArray();

  // Initialize coreference clusters
  int maxID = basenpArray.length;

  // Create an array of pointers
  // clust = new UnionFind(maxID);
  fvm = new RuleResolvers.FeatureVectorMap();
  ptrs = new int[maxID];
  clusters = new HashSet[maxID];
  for (int i = 0; i < clusters.length; i++) {
    ptrs[i] = i;
    HashSet<Annotation> cluster = new HashSet<Annotation>();
    cluster.add(basenpArray[i]);
    clusters[i] = cluster;
  }
  Annotation zero = new Annotation(-1, -1, -1, "zero");

  for (int i = 1; i < basenpArray.length; i++) {
    Annotation np2 = basenpArray[i];
    if (FeatureUtils.isPronoun(np2, doc)) {
      Annotation ant = ruleResolvePronoun(basenpArray, i, doc);
      if (ant != null) {
        np2.setProperty(Property.PRO_ANTES, ant);
      }
      else {
        np2.setProperty(Property.PRO_ANTES, zero);
      }
    }
  }
}

public static void ruleResolve(Document doc)
{
  ruleResolve(doc.getAnnotationSet(Constants.NP), doc);
}

@SuppressWarnings("unchecked")
public static void ruleResolve(AnnotationSet basenp, Document doc)
{
  // System.out.println("Rule resolving ");
  HashMap<Annotation, ArrayList<Annotation>> posessives = new HashMap<Annotation, ArrayList<Annotation>>();
  addAllPossesives(basenp, doc, posessives);
  Annotation[] basenpArray = basenp.toArray();

  // Initialize coreference clusters
  int maxID = basenpArray.length;

  // Create an array of pointers
  // clust = new UnionFind(maxID);
  fvm = new RuleResolvers.FeatureVectorMap();
  ptrs = new int[maxID];
  clusters = new HashSet[maxID];
  for (int i = 0; i < clusters.length; i++) {
    ptrs[i] = i;
    HashSet<Annotation> cluster = new HashSet<Annotation>();
    cluster.add(basenpArray[i]);
    clusters[i] = cluster;
  }
  Annotation zero = new Annotation(-1, -1, -1, "zero");

  for (int i = 1; i < basenpArray.length; i++) {
    Annotation np2 = basenpArray[i];
    NPType type = getNPtype(np2, doc, posessives);
    if (DEBUG) {
      System.err.println("Working on np#" + i + ": " + doc.getAnnotText(np2) + " :type " + type);
    }
    switch (type) {
      case PRONOUN:
        Annotation ant = ruleResolvePronoun(basenpArray, i, doc);
        if (ant != null) {
          np2.setProperty(Property.PRO_ANTES, ant);
        }
        else {
          np2.setProperty(Property.PRO_ANTES, zero);
        }
        break;
      case PROPER_NAME:
        ruleResolveProperName(basenpArray, i, doc);
        break;
      case INDEFINITE:
        ruleResolveIndefinite(basenpArray, i, doc);
        break;
      case DEFINITE:
        ruleResolveDefinite(basenpArray, i, doc, posessives);
        break;
      case SEMIDEFINITE:
        ruleResolveSemidefinite(basenpArray, i, doc);
        break;
      default:
        break;
    }
  }

  for (int i = 0; i < basenpArray.length; i++) {
    Annotation np = basenpArray[i];
    np.setProperty(Property.RULE_COREF_ID, find(i));
  }
}

public static Annotation ruleResolvePronoun(Annotation[] basenps, int num, Document doc)
{
  // Algorithm based on Baldwin's ('97) CogNIAC -- high precision pronoun resolution system
  Annotation np2 = basenps[num];
  // Get some properties of the np
  FeatureUtils.PRTypeEnum type2 = Pronoun.getValue(np2, doc);
  String str2 = doc.getAnnotString(np2);
  boolean reflexive = FeatureUtils.isReflexive(str2);
  // Get the possible antecedents
  ArrayList<Annotation> antes = new ArrayList<Annotation>();
  ArrayList<Integer> nums = new ArrayList<Integer>();
  if (DEBUG) {
    System.err.println("Pronoun: " + doc.getAnnotText(np2));
  }
  if (FeatureUtils.getPronounPerson(str2) == PersonPronounTypeEnum.FIRST) {
    if (NumberEnum.SINGLE.equals(Number.getValue(np2, doc)))
      return ruleResolvePronounI(np2, basenps, num, doc);
    else
      return ruleResolvePronounWe(np2, basenps, num, doc);
  }
  if (FeatureUtils.getPronounPerson(str2) == PersonPronounTypeEnum.SECOND)
    return ruleResolvePronounYou(np2, basenps, num, doc);
  else {
    int sentnum = 0;
    for (int i = num - 1; i >= 0 && sentnum <= 3; i--) {
      Annotation np1 = basenps[i];
      sentnum = sentNum(np1, np2, doc);
      if (DEBUG) {
        System.err.println("Possible antecedent: " + i + " :" + doc.getAnnotText(np1));
      }
      if (!isNumberIncompatible(np1, np2, doc) && !isGenderIncompatible(np1, np2, doc)
          && !isAnimacyIncompatible(np1, np2, doc) && isWNClassComp(np1, np2, doc) && isProComp(np1, np2, doc)
          && !Embedded.getValue(np1, doc) && isSyntax(np1, np2, doc)) {
        if (DEBUG) {
          System.err.println("Candidate antecedent: " + i + " :" + doc.getAnnotText(np1));
        }
        antes.add(0, np1);
        nums.add(0, i);
      }
    }
  }
  if (antes.size() == 0) return null;
  // Check for reflexsives
  if (FeatureUtils.isReflexive(doc.getAnnotText(np2))) {
    union(nums.get(nums.size() - 1).intValue(), num);
    return antes.get(antes.size() - 1);
  }
  if (antes.size() == 1) {
    // Rule 1: Unique in discourse
    if (DEBUG) {
      System.err.println("Rule 1 match!!!");
    }
    union(nums.get(0).intValue(), num);
    return antes.get(0);
  }
  // Rule 2: Reflexive -- the last possible antecedent
  if (reflexive) {
    if (DEBUG) {
      System.err.println("Rule 2 match!!!");
    }
    union(nums.get(nums.size() - 1).intValue(), num);
    return antes.get(antes.size() - 1);
  }
  // Rule 3: Unique current + Prior
  ArrayList<Annotation> antes1 = new ArrayList<Annotation>();
  ArrayList<Integer> nums1 = new ArrayList<Integer>();
  int counter = 0;
  for (Annotation np1 : antes) {
    if (sentNum(np1, np2, doc) < 2) {
      antes1.add(np1);
      nums1.add(nums.get(counter));
    }
    counter++;
  }
  if (antes1.size() == 1) {
    if (DEBUG) {
      System.err.println("Rule 3 match!!!");
    }
    union(nums1.get(0).intValue(), num);
    return antes1.get(0);
  }
  // Rule 4: Possesive Pro

  if (FeatureUtils.isPossesive(str2) && antes1.size() > 0) {
    Integer found = null;
    Annotation ant = null;
    boolean multiple = false;
    for (int i = 0; i < antes1.size() && !multiple; i++) {
      Annotation np1 = antes1.get(i);
      if (doc.getAnnotText(np1).equalsIgnoreCase(str2) && sentNum(np1, np2, doc) == 1) {
        if (found == null) {
          found = nums1.get(i);
          ant = antes1.get(i);
        }
        else {
          multiple = true;
        }
      }
    }
    if (!multiple && found != null) {
      if (DEBUG) {
        System.err.println("Rule 4 match!!!");
      }
      union(found.intValue(), num);
      return ant;
    }
  }
  // Rule #5: Unique in the current sentence
  ArrayList<Annotation> antes2 = new ArrayList<Annotation>();
  ArrayList<Integer> nums2 = new ArrayList<Integer>();
  counter = 0;
  for (Annotation np1 : antes1) {
    if (sentNum(np1, np2, doc) < 1) {
      antes2.add(np1);
      nums2.add(nums1.get(counter));
    }
    counter++;
  }
  if (antes2.size() == 1) {
    if (DEBUG) {
      System.err.println("Rule 5 match!!!");
    }
    union(nums2.get(0).intValue(), num);
    return antes2.get(0);
  }
  // Extra Rule: Unique in the current clause (or parent clauses
  AnnotationSet parse = doc.getAnnotationSet(Constants.PARSE);
  Annotation clause = SyntaxUtils.getClause(np2, parse);
  while (clause != null) {
    ArrayList<Annotation> antes3 = new ArrayList<Annotation>();
    ArrayList<Integer> nums3 = new ArrayList<Integer>();
    counter = 0;
    for (Annotation np1 : antes2) {
      if (clause.covers(np1)) {
        antes3.add(np1);
        nums3.add(nums2.get(counter));
      }
      counter++;
    }
    if (DEBUG) {
      System.err.println(antes3.size() + " antecedents in the clause");
    }
    if (antes3.size() == 1) {
      if (DEBUG) {
        System.err.println("Clause rule match!!!");
      }
      union(nums3.get(0).intValue(), num);
      return antes3.get(0);
    }
    clause = SyntaxUtils.getParentClause(clause, parse);
  }

  // Rule #6: Unique Subject

  // //Look for the subject in the current sentence
  // boolean unique = true;
  // Annotation subject = null;
  // Integer subjectNum = null;
  // for(int i=antes.size()-1; i>=0; i--){
  // Annotation np1 = antes.get(i);
  // if(sentNum(np1, np2, annotations, text)==0){
  // if(FeatureUtils.getGramRole(np1, annotations, text).equals("SUBJECT")){
  // //&&SyntaxUtils.isMainClause(np1, parse)){
  // if(DEBUG)
  // System.err.println("Rule 6 match!!!");
  // if(subjectNum!=null)
  // unique=false;
  // subjectNum = nums.get(i);
  // subject = antes.get(i);
  // }
  // }
  // }

  // if(subject!=null&&unique){
  // union(subjectNum.intValue(), num);
  // return subject;
  // }

  // Look for the subject in the previous sentence
  boolean unique = true;
  Annotation subject = null;
  Integer subjectNum = null;
  if (GramRole.getValue(np2, doc).equals("SUBJECT")) {
    // &&SyntaxUtils.isMainClause(np2, parse)){
    for (int i = antes.size() - 1; i >= 0; i--) {
      Annotation np1 = antes.get(i);
      if (sentNum(np1, np2, doc) == 1) {
        if (GramRole.getValue(np1, doc).equals("SUBJECT")) {
          // &&SyntaxUtils.isMainClause(np1, parse)){
          if (DEBUG) {
            System.err.println("Rule 6 match!!!");
          }
          if (subjectNum != null) {
            unique = false;
          }
          subjectNum = nums.get(i);
          subject = antes.get(i);
        }
      }
    }
  }
  if (subject != null && unique) {
    union(subjectNum.intValue(), num);
    return subject;
  }
  subjectNum = null;
  subject = null;

  // One more Rule -- assign possessive pronouns to the last subject
  if (type2.equals(FeatureUtils.PRTypeEnum.POSSESSIVE)) {
    for (int i = antes.size() - 1; i >= 0; i--) {
      Annotation np1 = antes.get(i);
      if (sentNum(np1, np2, doc) == 0) {
        if (GramRole.getValue(np1, doc).equals("SUBJECT")) {
          // &&SyntaxUtils.isMainClause(np1, parse)){
          if (DEBUG) {
            System.err.println("Rule 6a match!!!");
          }
          if (subject == null) {
            subjectNum = nums.get(i);
            subject = antes.get(i);
          }
        }
      }
    }
    if (subject != null) {
      union(subjectNum.intValue(), num);
      return subject;
    }
  }

  return null;
}

public static void ruleResolveIndefinite(Annotation[] basenps, int num, Document doc)
{
  Annotation np2 = basenps[num];
  // System.out.println("Working on "+FeatureUtils.getText(np2, text));
  for (int i = num - 1; i >= 0; i--) {
    Annotation np1 = basenps[i];
    // System.out.println("Considering "+FeatureUtils.getText(np1, text));
    if (!isTitle(np1, np2, doc)
        && (isAppositive(np1, np2, doc) || isPrednom(np1, np2, doc) || isQuantity(np1, np2, doc) || isAlias(np1, np2,
            doc))) {
      union(i, num);
      return;
    }
    else if (isAlias(np1, np2, doc) || isWordsStr(np1, np2, doc)) {
      if (passConsistencyCheck(basenps, i, num, doc)) {
        union(i, num);
        return;
      }
    }
    else {
      if (LOOSE_MATCH) {
        String[] wds1 = doc.getWords(np1);
        String[] wds2 = doc.getWords(np2);
        int sent1 = SentNum.getValue(np1, doc);
        int sent2 = SentNum.getValue(np2, doc);
        if (wds1.length == 1 && wds2.length == 1 && wnDist(np1, np2, doc) < 3 && !isTitle(np1, np2, doc)
            && (Math.abs(sent1 - sent2) <= 2) && passConsistencyCheck(basenps, i, num, doc)) {
          union(i, num);
          return;
        }
      }
    }
  }
}

public static void ruleResolveProperName(Annotation[] basenps, int num, Document doc)
{
  Annotation np2 = basenps[num];
  if (DEBUG) {
    System.err.println("Proper name: " + doc.getAnnotText(np2));
  }
  for (int i = num - 1; i >= 0; i--) {
    Annotation np1 = basenps[i];
    // if(DEBUG)
    // System.err.println("Working on: "+FeatureUtils.getText(np1, text));
    if ((isAppositive(np1, np2, doc)/*||isPNSubstr(np1, np2, annotations, text)*/|| isPNStr(np1, np2, doc) || isAlias(
        np1, np2, doc))
        && isWNClassComp(np1, np2, doc) && !isTitle(np1, np2, doc)) {
      if (DEBUG) {
        System.err.println("Candidate: " + doc.getAnnotText(np1));
      }
      if (passConsistencyCheck(basenps, i, num, doc)) {
        union(i, num);
        return;
      }
    }
  }
}

public static void ruleResolveSemidefinite(Annotation[] basenps, int num, Document doc)
{
  Annotation np2 = basenps[num];
  for (int i = num - 1; i >= 0; i--) {
    Annotation np1 = basenps[i];
    if (isWNClassComp(np1, np2, doc)
        && !isTitle(np1, np2, doc)
        && !isNumberIncompatible(np1, np2, doc)
        && (isAppositive(np1, np2, doc) || isPrednom(np1, np2, doc) || isQuantity(np1, np2, doc) || isAlias(np1, np2,
            doc))) {
      // if(passConsistencyCheck(basenps, i, num, annotations, text))
      union(i, num);
      return;
    }
  }
}

public static void ruleResolveDefinite(Annotation[] basenps, int num, Document doc,
    HashMap<Annotation, ArrayList<Annotation>> posessives)
{
  Annotation np2 = basenps[num];
  boolean coref = true;
  // use Vieira and Poesio's algorithm
  // Step 1: examine the lists of special nouns in order to identiy some of the
  // unfamiliar and larger situation uses of definite descriptions
  AnnotationSet posAnnots = doc.getAnnotationSet(Constants.POS);
  AnnotationSet pos = posAnnots.getContained(np2);

  for (Annotation p : pos) {
    if (p.compareSpan(np2) != 0) {
      if (superlative(p.getType()) || FeatureUtils.memberArray(doc.getAnnotText(p), SUP)) {
        coref = false;
      }
    }
    else {
      if (comparative(p.getType())) {
        coref = false;
      }
    }
  }
  // Step 2: check whether the definite NP occurs in an appositive
  // construction. If this test succeeds, a new discourse referent is
  // introduced, and the DD is classified as unfamiliar.
  /*
  if(coref && np2.getProperty(FeatureUtils.APPOSITIVE)!=null){
  	coref=false;
  }
  if(coref && np2.getProperty(FeatureUtils.PREDNOM)!=null){
  	coref=false;
  }*/
  // Step 3: try to find an antecedent for the definite desription using
  // a matching algorithm modified to deal with pre-modification and
  // respecting segmentation. When this test succedds the DD is classified
  // as anaphoric.
  for (int i = num - 1; coref && i >= 0; i--) {
    Annotation np1 = basenps[i];

    // The two nps are not in a pos relationship
    if (!pos(np1, np2, posessives)) {
      // assume that the antecedent cannot be an embedded noun
      if (// !FeatureUtils.isEmbedded(np1, annotations, text)
      // &&
      // if the np pair is in an appositive/prednom relation
      // and is one of the predefined neclasses, then coreferent
      // !isNumberIncompatible(np1, np2, annotations, text)&&
      (isAppositive(np1, np2, doc) || isPrednom(np1, np2, doc))) {
        if (passConsistencyCheck(basenps, num, i, doc)) {
          union(i, num);
          return;
        }
      }
      else {
        if (DEBUG) {
          System.err.println("Candidate: " + doc.getAnnotText(np1));
        }
        String hn1 = doc.getAnnotText(HeadNoun.getValue(np1, doc));
        String hn2 = doc.getAnnotText(HeadNoun.getValue(np2, doc));
        if (DEBUG) {
          System.err.println("hn: " + hn1.equalsIgnoreCase(hn2) + " mod " + compModifier(np1, np2, doc) + " sen "
              + sentNum(np1, np2, doc));
        }
        if (hn1.equalsIgnoreCase(hn2) && compModifier(np1, np2, doc) && sentNum(np1, np2, doc) <= 2) {

          if (passConsistencyCheck(basenps, num, i, doc)) {
            union(i, num);
            return;
          }
        }
        else {
          if (NPSemanticType.getValue(np1, doc).equals(NPSemanticType.getValue(np2, doc))
              && !NPSemanticType.getValue(np1, doc).equals(FeatureUtils.NPSemTypeEnum.UNKNOWN)
              && !isNumberIncompatible(np1, np2, doc) && compModifier(np1, np2, doc)
              && (LOOSE_MATCH || sentNum(np1, np2, doc) <= 3)) {
            if (passConsistencyCheck(basenps, num, i, doc)) {
              union(i, num);
              return;
            }
          }
        }
      }
    }
  }
}

public static Annotation getPronounAntecedent(Annotation np2, Document doc)
{
  if (!FeatureUtils.isPronoun(np2, doc)) return null;
  if (np2.getProperty(Property.RULE_COREF_ID) == null) {
    ruleResolve(doc);
  }
  return (Annotation) np2.getProperty(Property.PRO_ANTES);
}

public static Annotation getPronounAntecedentDoNotResolve(Annotation np2, Document doc)
{
  if (!FeatureUtils.isPronoun(np2, doc)) return null;
  if (Property.PRO_ANTES.getValueProp(np2, doc) == null) {
    ruleResolvePronouns(doc);
  }
  return (Annotation) np2.getProperty(Property.PRO_ANTES);
}

public static boolean passConsistencyCheck(Annotation[] basenps, int np1, int np2, Document doc)
{
  if (!PERFORM_CONSISTENCY_CHECK) return true;

  if (find(np1) == find(np2)) return true;

  HashSet<Annotation> cl1 = clusters[find(np1)];
  HashSet<Annotation> cl2 = clusters[find(np2)];
  for (Annotation n1 : cl1) {
    for (Annotation n2 : cl2) {
      if (incompatible(n1, n2, doc)) {
        if (DEBUG) {
          System.err.println("Incompatible " + doc.getAnnotText(n1) + " and " + doc.getAnnotText(n2));
        }
        return false;
      }
    }
  }
  return true;
}

private static Annotation ruleResolvePronounI(Annotation np2, Annotation[] basenps, int num, Document doc)
{
  int quote2 = InQuote.getValue(np2, doc);
  for (int i = num - 1; i >= 0; i--) {
    Annotation np1 = basenps[i];
    int quote1 = InQuote.getValue(np1, doc);
    if (DEBUG) {
      System.err.println("Possible I antecedent: " + i + " :" + doc.getAnnotText(np1));
    }
    if (FeatureUtils.isPronoun(np1, doc) && FeatureUtils.getPronounPerson(doc.getAnnotText(np1)) == PersonPronounTypeEnum.FIRST
        && NumberEnum.SINGLE.equals(Number.getValue(np1, doc)) && quote1 == quote2) {
      if (DEBUG) {
        System.err.println("Candidate I antecedent: " + i + " :" + doc.getAnnotText(np1));
      }
      union(i, num);
      return np1;
    }
  }
  if (quote2 < 0) {
    // The last NE person in the file
    for (int i = basenps.length - 1; i >= 0; i--) {
      Annotation np1 = basenps[i];
      FeatureUtils.NPSemTypeEnum type = ProperNameType.getValue(np1, doc);
      if (type != null && type.equals(FeatureUtils.NPSemTypeEnum.PERSON)) {
        union(i, num);
        return np1;
      }
    }
  }
  else {
    // Use an algorithm to find the person who's viewpoint is expressed
    for (int i = 0; i < basenps.length; i++) {
      Annotation np1 = basenps[i];
      if (isIAntes(np1, np2, doc)) {
        union(i, num);
        return np1;
      }
    }
  }
  return null;
}

private static Annotation ruleResolvePronounYou(Annotation np2, Annotation[] basenps, int num, Document doc)
{
  int quote2 = InQuote.getValue(np2, doc);
  for (int i = num - 1; i >= 0; i--) {
    Annotation np1 = basenps[i];
    int quote1 = InQuote.getValue(np1, doc);
    if (DEBUG) {
      System.err.println("Possible YOU antecedent: " + i + " :" + doc.getAnnotText(np1));
    }
    if (FeatureUtils.isPronoun(np1, doc) && FeatureUtils.getPronounPerson(doc.getAnnotText(np1)) == PersonPronounTypeEnum.SECOND
        && quote1 == quote2) {
      if (DEBUG) {
        System.err.println("Candidate YOU antecedent: " + i + " :" + doc.getAnnotText(np1));
      }
      union(i, num);
      return np1;
    }
  }
  return null;
}

private static Annotation ruleResolvePronounWe(Annotation np2, Annotation[] basenps, int num, Document doc)
{
  int quote2 = InQuote.getValue(np2, doc);
  for (int i = num - 1; i >= 0; i--) {
    Annotation np1 = basenps[i];
    int quote1 = InQuote.getValue(np1, doc);
    if (DEBUG) {
      System.err.println("Possible WE antecedent: " + i + " :" + doc.getAnnotText(np1));
    }
    if (FeatureUtils.isPronoun(np1, doc) && FeatureUtils.getPronounPerson(doc.getAnnotText(np1)) == PersonPronounTypeEnum.FIRST
        && NumberEnum.PLURAL.equals(Number.getValue(np1, doc)) && quote1 == quote2) {
      if (DEBUG) {
        System.err.println("Candidate WE antecedent: " + i + " :" + doc.getAnnotText(np1));
      }
      union(i, num);
      return np1;
    }
  }
  if (quote2 < 0) {
    // Do nothing in this case
  }
  else {
    // Use an algorithm to find the person who's viewpoint is expressed
    for (int i = 0; i < basenps.length; i++) {
      Annotation np1 = basenps[i];
      if (isWeAntes(np1, np2, doc)) {
        union(i, num);
        return np1;
      }
    }
  }
  return null;
}

private static boolean incompatible(Annotation np1, Annotation np2, Document doc)
{
  if (Constructor.createFeature("ProperName").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.INCOMPATIBLE)) {
    if (DEBUG) {
      System.err.println("Condition Proper Name");
    }
    return true;
  }
  if (Constructor.createFeature("Span").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.INCOMPATIBLE)) {
    if (DEBUG) {
      System.err.println("Condition Span");
    }
    return true;
    // }if(Constructor.createFeature("Contraindices").getValue(np1, np2, annotations, text, fvm.getFeatureVector(np1,
    // np2)).equals(NominalFeature.INCOMPATIBLE)){
    // if(DEBUG)
    // System.err.println("Condition Contraindicies");
    // return true;
  }
  if (!FeatureUtils.isPronoun(np1, doc) && !ProperName.getValue(np1, doc) && !FeatureUtils.isPronoun(np2, doc)
      && !ProperName.getValue(np2, doc) && wnDist(np1, np2, doc) > 2) {
    if (DEBUG) {
      System.err.println("Condition WNDist");
    }
    return true;
  }
  if (ProperName.getValue(np1, doc) && ProperName.getValue(np2, doc))
    if (!isPNSubstr(np1, np2, doc) && !isPNStr(np1, np2, doc) && !isAlias(np1, np2, doc)) return true;
  return false;
}

private static boolean pos(Annotation np1, Annotation np2, HashMap<Annotation, ArrayList<Annotation>> posessives)
{
  ArrayList<Annotation> p1 = posessives.get(np1);
  for (Annotation a : p1) {
    if (a.equals(np2)) return true;
  }
  ArrayList<Annotation> p2 = posessives.get(np2);
  for (Annotation a : p2) {
    if (a.equals(np1)) return true;
  }
  return false;
}

private static boolean isAppositive(Annotation np1, Annotation np2, Document doc)
{
  return Constructor.createFeature("Appositive").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.COMPATIBLE);
}

private static boolean isQuantity(Annotation np1, Annotation np2, Document doc)
{
  return Constructor.createFeature("Quantity").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.COMPATIBLE);
}

private static boolean isIAntes(Annotation np1, Annotation np2, Document doc)
{
  return Constructor.createFeature("IAntes").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.COMPATIBLE);
}

private static boolean isWeAntes(Annotation np1, Annotation np2, Document doc)
{
  return Constructor.createFeature("WeAntes").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.COMPATIBLE);
}

private static boolean isNumberIncompatible(Annotation np1, Annotation np2, Document doc)
{
  String number = Constructor.createFeature("Number").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2));
  // if(DEBUG)
  // System.err.println("Number: "+number);
  return number.equals(NominalFeature.INCOMPATIBLE);
}

private static boolean isGenderIncompatible(Annotation np1, Annotation np2, Document doc)
{
  String gender = Constructor.createFeature("Gender").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2));
  if (DEBUG) {
    System.err.println("Gender: " + gender);
  }
  return gender.equals(NominalFeature.INCOMPATIBLE);
}

private static boolean isAnimacyIncompatible(Annotation np1, Annotation np2, Document doc)
{
  String animacy = Constructor.createFeature("Animacy").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2));
  if (DEBUG) {
    System.err.println("Animacy: " + animacy);
  }
  return animacy.equals(NominalFeature.INCOMPATIBLE);
}

private static boolean isAlias(Annotation np1, Annotation np2, Document doc)
{
  return Constructor.createFeature("Alias").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.COMPATIBLE);
}

private static boolean isPNStr(Annotation np1, Annotation np2, Document doc)
{
  return Constructor.createFeature("PNStr").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.COMPATIBLE);
}

private static boolean isWordsStr(Annotation np1, Annotation np2, Document doc)
{
  return Constructor.createFeature("WordsStr").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.COMPATIBLE);
}

private static boolean compModifier(Annotation np1, Annotation np2, Document doc)
{
  String[] mod1 = Modifier.getValue(np1, doc);
  String[] mod2 = Modifier.getValue(np2, doc);
  if ((mod1 == null || mod1.length == 0) && (mod2 == null || mod2.length == 0)) return true;
  return Constructor.createFeature("Modifier").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.COMPATIBLE);
}

private static boolean isWNClassComp(Annotation np1, Annotation np2, Document doc)
{
  String wnclass = Constructor.createFeature("WordNetClass").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2));
  if (DEBUG) {
    System.err.println("WNClass : " + wnclass);
  }
  return !wnclass.equals(NominalFeature.INCOMPATIBLE);
}

private static boolean isProComp(Annotation np1, Annotation np2, Document doc)
{
  String proComp = Constructor.createFeature("ProComp").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2));
  if (DEBUG) {
    System.err.println("WNClass : " + proComp);
  }
  return proComp.equals(NominalFeature.COMPATIBLE);
}

private static boolean isSyntax(Annotation np1, Annotation np2, Document doc)
{
  String syntax = Constructor.createFeature("Syntax").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2));
  if (DEBUG) {
    System.err.println("Syntax : " + syntax);
  }
  return syntax.equals(NominalFeature.COMPATIBLE);
}

private static boolean isPNSubstr(Annotation np1, Annotation np2, Document doc)
{
  String pnsubstr = Constructor.createFeature("PNSubstr").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2));
  // if(DEBUG)
  // System.err.println("PNSubstr : "+pnsubstr);
  return pnsubstr.equals(NominalFeature.COMPATIBLE);
}

private static boolean isTitle(Annotation np1, Annotation np2, Document doc)
{
  return Constructor.createFeature("Title").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.INCOMPATIBLE);
}

private static boolean isPrednom(Annotation np1, Annotation np2, Document doc)
{
  return Constructor.createFeature("Prednom").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)).equals(
      NominalFeature.COMPATIBLE);
}

private static boolean superlative(String posTag)
{
  return posTag.equalsIgnoreCase("JJS") || posTag.equalsIgnoreCase("RBS");
}

private static boolean comparative(String posTag)
{
  return posTag.equalsIgnoreCase("JJR") || posTag.equalsIgnoreCase("RBR");
}

private static double wnDist(Annotation np1, Annotation np2, Document doc)
{
  return Double.parseDouble(Constructor.createFeature("WordNetDist").getValue(np1, np2, doc,
      fvm.getFeatureVector(np1, np2)))
      * NumericFeature.WN_MAX;
}

private static Integer sentNum(Annotation np1, Annotation np2, Document doc)
{
  return Integer.parseInt(Constructor.createFeature("SentNum").getValue(np1, np2, doc, fvm.getFeatureVector(np1, np2)));
}

private static int find(int i)
{
  // find the set number for the element
  // int other = clust.find(i);
  int ind = i;
  while (ind != ptrs[ind]) {
    ind = ptrs[ind];
  }
  // fix the link so that it is one hop only
  // note: this doesn't implement the full union-find update

  ptrs[i] = ind;

  return ind;
}

private static void union(int i, int j)
{
  if (DEBUG) {
    System.err.println("Joining clusters " + i + " and " + j);
  }
  int indI = find(i);
  int indJ = find(j);
  ptrs[indI] = indJ;
  // int n = clust.merge(i, j);
  // System.out.println(n+"-"+indI+","+indJ);
  // if(n!=indI){
  HashSet<Annotation> cluster = clusters[indI];
  cluster.addAll(clusters[indJ]);
  // }else{
  // HashSet<Annotation> cluster = clusters[indJ];
  // cluster.addAll(clusters[indI]);
  // }
}

final static String[] SUP = { "first", "last", "maximum", "minimum", "only" };

public enum NPType {
  PRONOUN, PROPER_NAME, DEFINITE, INDEFINITE, SEMIDEFINITE, UNKNOWN
};

private static class FeatureVectorMap {

// A class to cache the different feature values so they are not recomputed
private HashMap<Integer, HashMap<Integer, HashMap<Feature, String>>> fvm;

public FeatureVectorMap() {
  fvm = new HashMap<Integer, HashMap<Integer, HashMap<Feature, String>>>();
}

public HashMap<Feature, String> getFeatureVector(int np1, int np2)
{
  if (!CACHE_FVS) return new HashMap<Feature, String>();
  int first, second;
  if (np1 < np2) {
    first = np1;
    second = np2;
  }
  else {
    first = np2;
    second = np1;
  }
  HashMap<Integer, HashMap<Feature, String>> emb = fvm.get(first);
  if (emb == null) {
    emb = new HashMap<Integer, HashMap<Feature, String>>();
    fvm.put(first, emb);
  }
  HashMap<Feature, String> fv = emb.get(second);
  if (fv == null) {
    fv = new HashMap<Feature, String>();
    emb.put(second, fv);
  }
  return fv;
}

public HashMap<Feature, String> getFeatureVector(Annotation np1, Annotation np2)
{
  return getFeatureVector(np1.getId(), np2.getId());
}
}
}
