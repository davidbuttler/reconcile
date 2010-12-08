package reconcile.featureExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.lexparser.Options;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

public class ParserStanfordParser
    extends InternalAnnotator {

public static final String MODEL_NAME = "Stanford/parser/englishFactored.ser.gz";

private static final boolean DEBUG = true;
private LexicalizedParser lp;
private GrammaticalStructureFactory gsf;

public ParserStanfordParser() {
  try {
    // set up the parser
    InputStream in = this.getClass().getClassLoader().getResourceAsStream(MODEL_NAME);

    System.out.println("Reading grammar..." + MODEL_NAME);
    Options op = new Options();
    op.doDep = true;

    lp = new LexicalizedParser(new ObjectInputStream(in));

    lp.setOptionFlags(new String[] { "-maxLength", "80", "-retainTmpSubcategories" });// ,"-sentences","-tokenized"});
    gsf = lp.getOp().tlpParams.treebankLanguagePack().grammaticalStructureFactory();
    if (DEBUG) {
      System.err.println("ParserPack is " + op.tlpParams.getClass().getName());
    }

    if (DEBUG) {
      System.err.print(op);
    }

    System.out.println("Done reading grammar...");
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }

}

// recursive method to traverse a tree while adding spans of nodes to the annotset
@SuppressWarnings("unchecked")
public int addSpans(Tree parseTree, int startTokenIndx, Object[] sentToks, AnnotationSet parsesSet, Annotation parent)
{
  // System.err.println("Adding "+parseTree+" - "+startTokenIndx);
  List yield = parseTree.yield();
  int len = yield.size();// sentToks.length;
  Map<String, String> attrs = new TreeMap<String, String>();
  attrs.put("parent", Integer.toString(parent.getId()));
  int cur = parsesSet.add(((Annotation) sentToks[startTokenIndx]).getStartOffset(), ((Annotation) sentToks[len
      + startTokenIndx - 1]).getEndOffset(), parseTree.value(), attrs);
  Annotation curAn = parsesSet.get(cur);
  addChild(parent, curAn);
  int offset = startTokenIndx;
  for (Tree tr : parseTree.children()) {
    offset += addSpans(tr, offset, sentToks, parsesSet, curAn);
  }
  return len;
}

public static void addDepSpans(Collection<TypedDependency> dep, Annotation[] sentToks, AnnotationSet parsesSet)
{
  Annotation[] sentDeps = new Annotation[sentToks.length];
  // First create all annotations
  int index = 0;
  for (Annotation tok : sentToks) {
    // System.err.println(d +" gov "+ d.gov().index());
    int id = parsesSet.add(tok.getStartOffset(), tok.getEndOffset(), "ROOT");
    Annotation added = parsesSet.get(id);
    sentDeps[index] = added;
    index++;
  }

  // Now add parents and children
  for (TypedDependency d : dep) {
    int dependent = d.dep().index() - 1;
    int gov = d.gov().index() - 1;

    Annotation depAn = sentDeps[dependent];
    Annotation govAn = sentDeps[gov];

    String offset = govAn.getStartOffset() + "," + govAn.getEndOffset();
    depAn.setAttribute("GOV", offset);
    depAn.setAttribute("GOV_ID", Integer.toString(govAn.getId()));
    addChild(govAn, depAn);

    GrammaticalRelation rel = d.reln();
    String relName = rel.toString();
    if (EnglishGrammaticalRelations.SUBJECT.isAncestor(rel)) {
      relName = "SUBJECT";
    }
    else if (EnglishGrammaticalRelations.OBJECT.isAncestor(rel)) {
      relName = "OBJECT";
    }
    else if (EnglishGrammaticalRelations.APPOSITIONAL_MODIFIER.isAncestor(rel)) {
      relName = "APPOS";
    }
    depAn.setType(relName);
  }
}

public static void addChild(Annotation parent, Annotation child)
{
  if (!parent.equals(Annotation.getNullAnnot())) {
    String childIds = parent.getAttribute("CHILD_IDS");
    childIds = childIds == null ? Integer.toString(child.getId()) : childIds + "," + child.getId();
    parent.setAttribute("CHILD_IDS", childIds);
  }
}

public static void removeConjunctions(AnnotationSet dep)
{
  // Conjunctions are uninteresting. Assign the dependency of the parent
  for (Annotation d : dep) {
    String type = d.getType();
    if (type.equalsIgnoreCase("conj")) {
      Annotation a = d;
      while (type.equalsIgnoreCase("conj")) {
        String[] span = (a.getAttribute("GOV")).split("\\,");
        int stSpan = Integer.parseInt(span[0]);
        int endSpan = Integer.parseInt(span[1]);
        a = dep.getContained(stSpan, endSpan).getFirst();
        if (a == null) {
          break;
        }
        type = a.getType();
      }
      if (a != null) {
        d.setType(type);
        d.setAttribute("GOV", a.getAttribute("GOV"));
      }
    }
  }
}

@Override
public void run(Document doc, String[] annSetNames)
{
  if (lp == null) throw new RuntimeException("Parser not initialized");
  AnnotationSet parses = new AnnotationSet(annSetNames[0]);
  AnnotationSet depAnnots = new AnnotationSet(annSetNames[1]);

  // get the sentences from precomputed annotation set on disk
  AnnotationSet sentSet = doc.getAnnotationSet(Constants.SENT);

  // get the tokens from precomputed annotation set on disk
  AnnotationSet tokSet = doc.getAnnotationSet(Constants.TOKEN);

  // Read in the text from the raw file
  String text = doc.getText();

  // System.out.println("Done loading text...");

  // loop through sentences
  int numWords = 0;
  int numSents = 0;
  int num = 0;
  for (Annotation sentence : sentSet) {
    num++;
    numSents++;
    AnnotationSet toks = tokSet.getContained(sentence);
    int len = toks.size();
    numWords += len;
    System.err.println("Parsing [sent. " + num + " len. " + len + "]: " + Utils.getAnnotText(sentence, text));

    List<String> sentWords = new ArrayList<String>();
    for (Annotation tok : toks) {
      sentWords.add(Utils.getAnnotText(tok, text));
    }
    Tree ansTree = lp.apply(sentWords);
    addSpans(ansTree, 0, toks.toArray(), parses, Annotation.getNullAnnot());
    // Tree depTree = lp.getBestDependencyParse();
    // System.out.println(ansTree);
    // addSpans(depTree, 0, toks.toArray(), parses);
    // depTree.indexLeaves();
    // Set<Dependency> dep = depTree.dependencies();
    // Add the dependencies
    GrammaticalStructure gs = gsf.newGrammaticalStructure(ansTree);
    Collection<TypedDependency> dep = gs.typedDependencies();
    addDepSpans(dep, toks.toArray(), depAnnots);
    // removeConjunctions(depAnnots);
  }
  addResultSet(doc,parses);
  addResultSet(doc,depAnnots);
}
}
