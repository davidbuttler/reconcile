package reconcile.featureExtractor;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

import mstparser.Alphabet;
import mstparser.DependencyDecoder;
import mstparser.DependencyInstance;
import mstparser.DependencyPipe;
import mstparser.FeatureVector;
import mstparser.Parameters;
import mstparser.ParserOptions;
import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;

public class MSTParser
    extends InternalAnnotator {

private static final String args[] = { "test", "model-name:" + Utils.getDataDirectory() + "/dep.model",
    "test-file:test.txt", "output-file:out.txt", "format:MST" };
private DependencyPipe pipe;
private DependencyDecoder decoder;
private Parameters params;

//private GrammaticalStructureFactory gsf;

public MSTParser() {
  try {
    // set up the parser
    ParserOptions options = new ParserOptions(args);
    pipe = new DependencyPipe(options);
    pipe.labeled = true;

    // DependencyParser dp = new DependencyParser(pipe, options);
    decoder = new DependencyDecoder(pipe);
    params = new Parameters(pipe.dataAlphabet.size());
    System.out.print("\tLoading model...");
    loadModel(Utils.getDataDirectory() + Utils.SEPARATOR + "dep.model");
    System.out.println("done.");

    pipe.closeAlphabets();

    // dp.outputParses();
  }
  catch (Exception e) {
    throw new RuntimeException(e);
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

public void loadModel(String file)
    throws Exception
{
  System.out.println("Filename: " + file);
  ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
  params.parameters = (double[]) in.readObject();
  System.out.println("params: " + params);

  Alphabet a = (Alphabet) in.readObject();
  pipe.dataAlphabet = a;
  pipe.typeAlphabet = (Alphabet) in.readObject();
  in.close();
  pipe.closeAlphabets();
}

public DependencyInstance parseSentence(DependencyInstance instance)
{
  String[] forms = instance.forms;

  // for(int i=0;i<forms.length;i++){
  // System.out.print(forms[i]+":"+instance.postags[i]);
  // }
//  int length = forms.length;

  FeatureVector[][][] fvs = new FeatureVector[forms.length][forms.length][2];
  double[][][] probs = new double[forms.length][forms.length][2];
  FeatureVector[][][][] nt_fvs = new FeatureVector[forms.length][pipe.types.length][2][2];
  double[][][][] nt_probs = new double[forms.length][pipe.types.length][2][2];
//  FeatureVector[][][] fvs_trips = new FeatureVector[length][length][length];
//  double[][][] probs_trips = new double[length][length][length];
//  FeatureVector[][][] fvs_sibs = new FeatureVector[length][length][2];
//  double[][][] probs_sibs = new double[length][length][2];
  pipe.fillFeatureVectors(instance, fvs, probs, nt_fvs, nt_probs, params);

  int K = 1;
  Object[][] d = null;

  d = decoder.decodeNonProjective(instance, fvs, probs, nt_fvs, nt_probs, K);

  String[] res = ((String) d[0][1]).split(" ");

  String[] pos = instance.cpostags;

  String[] formsNoRoot = new String[forms.length - 1];
  String[] posNoRoot = new String[formsNoRoot.length];
  String[] labels = new String[formsNoRoot.length];
  int[] heads = new int[formsNoRoot.length];

  // System.out.println(Arrays.toString(forms));
  // System.out.println(Arrays.toString(res));

  for (int j = 0; j < formsNoRoot.length; j++) {
    formsNoRoot[j] = forms[j + 1];
    posNoRoot[j] = pos[j + 1];

    String[] trip = res[j].split("[\\|:]");
    labels[j] = pipe.types[Integer.parseInt(trip[2])];
    heads[j] = Integer.parseInt(trip[0]);
  }

//  String line1 = "";
  // String line2 = "";
  // String line3 = "";
  // String line4 = "";
  // for (int j = 1; j < pos.length; j++) {
  // String[] trip = res[j - 1].split("[\\|:]");
  // line2 += pos[j] + "\t";
  // line4 += trip[0] + "\t";
  // line3 += pipe.types[Integer.parseInt(trip[2])] + "\t";
  // }
  // System.out.print(line1.trim() + "\n" + line2.trim() + "\n"
  // + (pipe.labeled ? line3.trim() + "\n" : "")
  // + line4.trim() + "\n\n");

  DependencyInstance di = new DependencyInstance(formsNoRoot, posNoRoot, labels, heads);
  // System.out.println(Arrays.toString(di.forms));
  // System.out.println(Arrays.toString(di.postags));
  // System.out.println(Arrays.toString(di.deprels));
  // System.out.println(Arrays.toString(di.heads));

  return di;
}

@Override
public void run(Document doc, String[] annSetNames)
{
  if (pipe == null) throw new RuntimeException("Parser not initialized");
  AnnotationSet depAnnots = new AnnotationSet(annSetNames[0]);

  // get the sentences from precomputed annotation set on disk
  AnnotationSet sentSet = doc.getAnnotationSet(Constants.SENT);

  // get the pos from precomputed annotation set on disk
  AnnotationSet posSet = doc.getAnnotationSet(Constants.POS);

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
    AnnotationSet toks = posSet.getContained(sentence);
    int len = toks.size();
    numWords += len;
    System.out.println("MST:Parsing [sent. " + num + " len. " + len + "] ");// + Utils.getAnnotText(sentence,text));

    String[] forms = new String[toks.size() + 1];
    String[] pos = new String[toks.size() + 1];
    String[] deprels = new String[toks.size() + 1];
    int[] heads = new int[toks.size() + 1];

    forms[0] = "<root>";
    pos[0] = "<root-POS>";
    deprels[0] = "<no-type>";
    heads[0] = -1;

    int n = 1;

    for (Annotation tok : toks) {
      forms[n] = Utils.getAnnotText(tok, text);
      pos[n] = tok.getType();
      deprels[n] = "<no-type>";
      heads[n] = -1;
      n++;
    }

    DependencyInstance instance = new DependencyInstance(forms, pos, deprels, heads);

    // set up the course pos tags as just the first letter of the fine-grained ones
    String[] cpostags = new String[pos.length];
    cpostags[0] = "<root-CPOS>";
    for (int i = 1; i < pos.length; i++) {
      cpostags[i] = pos[i].substring(0, 1);
    }
    instance.cpostags = cpostags;

    // set up the lemmas as just the first 5 characters of the forms
    String[] lemmas = new String[forms.length];
    cpostags[0] = "<root-LEMMA>";
    for (int i = 1; i < forms.length; i++) {
      int formLength = forms[i].length();
      lemmas[i] = formLength > 5 ? forms[i].substring(0, 5) : forms[i];
    }
    instance.lemmas = lemmas;
    instance.feats = new String[0][0];

    DependencyInstance parsedInst = parseSentence(instance);

    Annotation[] sentToks = toks.toArray();
    String[] rels = parsedInst.deprels;
    int[] resHeads = parsedInst.heads;
    Annotation[] depToks = new Annotation[sentToks.length];
    for (int i = 0; i < toks.size(); i++) {
      Annotation depTok = sentToks[i];
      int id = depAnnots.add(depTok.getStartOffset(), depTok.getEndOffset(), rels[i]);
      Annotation added = depAnnots.get(id);
      depToks[i] = added;
    }
    for (int i = 0; i < toks.size(); i++) {
      Annotation depTok = depToks[i];
      // HashMap<String, String> hm = Maps.newTreeMap();
      if (resHeads[i] > 0) {
        Annotation govTok = depToks[resHeads[i] - 1];
        String offset = govTok.getStartOffset() + "," + govTok.getEndOffset();

        depTok.setAttribute("GOV", offset);
        depTok.setAttribute("GOV_ID", Integer.toString(govTok.getId()));
        String child = govTok.getAttribute("CHILD_IDS");
        child = child == null ? Integer.toString(depTok.getId()) : child + "," + depTok.getId();
        govTok.setAttribute("CHILD_IDS", child);
      }
      else {
        // hm.put("GOV", "root");
      }

      // depAnnots.add(depTok.getStartOffset(), depTok.getEndOffset(), rels[i], hm);

    }
    // List<String> sentWords = new ArrayList<String>();

    // Tree ansTree=(Tree)lp.apply(sentWords);
    // addSpans(ansTree, 0, toks.toArray(), parses, -1);

    // Tree depTree = lp.getBestDependencyParse();
    // System.out.println(ansTree);
    // addSpans(depTree, 0, toks.toArray(), parses);
    // depTree.indexLeaves();
    // Set<Dependency> dep = depTree.dependencies();
    // Add the dependencies
    // GrammaticalStructure gs = gsf.newGrammaticalStructure(ansTree);
    // Collection<TypedDependency> dep = gs.typedDependencies();
    // addDepSpans(dep, toks.toArray(), depAnnots);
    // removeConjunctions(depAnnots);
  }
  // writeAnnotationSet(parses, dirName);
  addResultSet(doc,depAnnots);
}
}
