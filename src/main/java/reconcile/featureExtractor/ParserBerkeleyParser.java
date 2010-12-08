package reconcile.featureExtractor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;
import edu.berkeley.nlp.PCFGLA.CoarseToFineMaxRuleParser;
import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.Lexicon;
import edu.berkeley.nlp.PCFGLA.ParserData;
import edu.berkeley.nlp.PCFGLA.TreeAnnotations;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.util.Numberer;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class ParserBerkeleyParser
    extends InternalAnnotator {

/**
 * 
 */
private static final String BERKELEY_PARSER_MODEL = "BerkeleyParser/models/eng_sm5.gr";

private static final int TOKEN_LIMIT = 100;
private static final String[] BREAKS = { ";", ":", ",", "." };
private CoarseToFineMaxRuleParser parser = null;
TreebankLanguagePack tlp;
GrammaticalStructureFactory gsf;

public ParserBerkeleyParser() {
  InputStream res = this.getClass().getClassLoader().getResourceAsStream(BERKELEY_PARSER_MODEL);

  System.out.println("Reading Berkeley Parser grammar... " + BERKELEY_PARSER_MODEL);
  ParserData pData = ParserData.Load(res);

  if (pData == null) {
    System.out.println("Failed to load grammar from file: " + BERKELEY_PARSER_MODEL + ".");
    throw new RuntimeException("Failed to load grammar from file: " + BERKELEY_PARSER_MODEL + ".");
  }

  System.out.println("Done reading grammar...");

  // set features
  Grammar grammar = pData.getGrammar();
  Lexicon lexicon = pData.getLexicon();
  Numberer.setNumberers(pData.getNumbs());

  double threshold = 1.0;

  boolean viterbiInsteadOfMaxRule = false;
  boolean outputSubCategories = false;
  boolean outputInsideScoresOnly = false;
  boolean accuracyOverEfficiency = false;

  // create a parser
  parser = new CoarseToFineMaxRuleParser(grammar, lexicon, threshold, -1, viterbiInsteadOfMaxRule, outputSubCategories,
      outputInsideScoresOnly, accuracyOverEfficiency, false, false);

  // Some additional components needed for the dependency parse conversions
  tlp = new PennTreebankLanguagePack();
  gsf = tlp.grammaticalStructureFactory();
}

public static void addChild(Annotation parent, Annotation child)
{
  if (!parent.equals(Annotation.getNullAnnot())) {
    String childIds = parent.getAttribute("CHILD_IDS");
    childIds = childIds == null ? Integer.toString(child.getId()) : childIds + "," + child.getId();
    parent.setAttribute("CHILD_IDS", childIds);
  }
}

// recursive method to traverse a tree while adding spans of nodes to the annotset
public static int addSpans(Tree<String> parseTree, int startTokenIndx, Object[] sentToks, AnnotationSet parsesSet,
    Annotation parent)
{
  int yieldLength = parseTree.getYield().size();
  Map<String, String> attrs = new TreeMap<String, String>();
  attrs.put("parent", Integer.toString(parent.getId()));
  int curId = parsesSet.add(((Annotation) sentToks[startTokenIndx]).getStartOffset(),
      ((Annotation) sentToks[yieldLength + startTokenIndx - 1]).getEndOffset(), parseTree.getLabel(), attrs);
  Annotation cur = parsesSet.get(curId);
  addChild(parent, cur);
  int offset = startTokenIndx;
  for (Tree<String> tr : parseTree.getChildren()) {
    if (!tr.isLeaf()) {
      offset += addSpans(tr, offset, sentToks, parsesSet, cur);
    }
  }
  return yieldLength;
}

public ArrayList<Annotation> splitSentence(AnnotationSet toks, Annotation sentence, String text)
{
  ArrayList<Annotation> result = new ArrayList<Annotation>();
  if (toks.size() <= TOKEN_LIMIT) {
    result.add(sentence);
    return result;
  }
  System.out.println("Long sentence: " + Utils.getAnnotText(sentence, text));
  List<Annotation> tokens = toks.getOrderedAnnots();
  int mid = tokens.size() / 2;

  boolean split = false;
  for (String br : BREAKS) {
    for (int indexLow = mid, indexHigh = mid; indexLow >= 0; indexLow--) {
      indexHigh++;
      Annotation tokLow = tokens.get(indexLow);
      if (br.equals(Utils.getAnnotText(tokLow, text))) {
        Annotation newSen = new Annotation(sentence.getId(), sentence.getStartOffset(), tokLow.getEndOffset(),
            "sentence");
        result = splitSentence(toks.getContained(newSen), newSen, text);
        Annotation newSen1 = new Annotation(sentence.getId(), tokLow.getEndOffset() + 1, sentence.getEndOffset(),
            "sentence");
        result.addAll(splitSentence(toks.getContained(newSen1), newSen1, text));
        System.out.println("Split into:" + Utils.getAnnotText(newSen, text) + "\nand\n"
            + Utils.getAnnotText(newSen1, text));
        split = true;
        break;
      }
      else {
        Annotation tokHigh = tokens.get(indexLow);
        if (br.equals(Utils.getAnnotText(tokHigh, text))) {
          Annotation newSen = new Annotation(sentence.getId(), sentence.getStartOffset(), tokHigh.getEndOffset(),
              "sentence");
          result = splitSentence(toks.getContained(newSen), newSen, text);
          Annotation newSen1 = new Annotation(sentence.getId(), tokHigh.getEndOffset() + 1, sentence.getEndOffset(),
              "sentence");
          result.addAll(splitSentence(toks.getContained(newSen1), newSen1, text));
          System.out.println("Split into:" + Utils.getAnnotText(newSen, text) + "\nand\n"
              + Utils.getAnnotText(newSen1, text));
          split = true;
          break;
        }
      }
      if (split) {
        break;
      }
    }
  }

  return result;
}

@Override
public void run(Document doc, String[] annSetNames)
{
  if (parser == null) throw new RuntimeException("Parser not initialized");

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
  Iterator<Annotation> sentenceItr = sentSet.iterator();

  int sNum = 1;

  while (sentenceItr.hasNext()) {

    Annotation sentence = sentenceItr.next();
    // The parser doesn't handle well sentences that are all caps.
    // Need to find those cases
    String sentText = doc.getAnnotText(sentence);
    boolean allCaps = false;
    // if the sentence contains no lower case characters and more than one word
    if (sentText.matches("[^a-z]+\\s[^a-z]+")) {
      allCaps = true;
    }

    if (allCaps) {
      System.out.println("On sentence (allCaps) " + (sNum++) + "...");
    }
    else {
      System.out.println("On sentence " + (sNum++) + "...");
    }
    // get the tokens in this sentence
    AnnotationSet sentenceTok = tokSet.getContained(sentence);
    ArrayList<Annotation> splitSent = splitSentence(sentenceTok, sentence, text);
    for (Annotation sent : splitSent) {
      AnnotationSet sentTok = tokSet.getContained(sent);
      // add all these tokens to a list
      ArrayList<String> tokList = new ArrayList<String>(sentTok.size());
      Iterator<Annotation> tokenItr = sentTok.iterator();

      while (tokenItr.hasNext()) {
        Annotation tok = (tokenItr.next());
        if (allCaps) {
          tokList.add(Utils.getAnnotText(tok, text).toLowerCase());
        }
        else {
          tokList.add(Utils.getAnnotText(tok, text));
        }
      }

      // System.out.println("Begin parsing sentence " + (sNum) +"...");

      // parse this sentence
      Tree<String> parsedTree = parser.getBestConstrainedParse(tokList, null);
      parsedTree = TreeAnnotations.unAnnotateTree(parsedTree);
      // System.out.println("Done parsing sentence " + (sNum++) +"...");
      // System.out.println(parsedTree);
      edu.stanford.nlp.trees.Tree stTree = reconcile.general.BerkeleyToStanfordTreeConverter.convert(parsedTree);
      // add each node in tree to the annotation set of parses
      // System.out.println(parsedTree);
      // System.out.println(stTree);
      addSpans(parsedTree, 0, sentTok.toArray(), parses, Annotation.getNullAnnot());
      
		GrammaticalStructure gs = gsf.newGrammaticalStructure(stTree);
      Collection<TypedDependency> dep = gs.typedDependencies();
      
		ParserStanfordParser.addDepSpans(dep, sentTok.toArray(), depAnnots);
      // ParserStanfordParser.removeConjunctions(depAnnots);
    }
  }

  addResultSet(doc,parses);
  addResultSet(doc,depAnnots);
}

}
