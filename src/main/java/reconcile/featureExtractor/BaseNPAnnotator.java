package reconcile.featureExtractor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import reconcile.data.Annotation;
import reconcile.data.AnnotationComparatorNestedLast;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.features.properties.HeadNoun;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;
import reconcile.general.Utils;

public class BaseNPAnnotator extends InternalAnnotator {

	private static boolean PRINTMARKS = false;
	// private static final String[] prepPOS = { "IN", "TO", ",", ":", ";" };

	private static final String[] TITLES = { "mr.", "mr", "messrs", "messrs.",
			"vice", "ms.", "ms", "mrs.", "mrs", "dr.", "dr", "prof.", "prof",
			"rev", "rev.", "rep.", "rep", "reps.", "reps", "sen.", "sen",
			"sens.", "sens", "gov.", "gov", "gen.", "gen", "maj.", "maj",
			"col.", "col", "lt.", "lt", "sgt.", "sgt" };

	// Strictly company designations
	public static String[] CORP_DESIG = { "co.", "co", "cie", "cie.", "cos.",
			"corp", "corp.", "inc.", "inc", "ltd.", "ltd", "ltda.", "ltda",
			"l.p.", "lp", "Assoc.", "groupe", "grupo", "bros", "bros.",
			"bancorp", "bancorp.", "sdn", "sdn.", "bhd", "bhd.", "plc", "plc.",
			"s.a.", "s.a", "sa", "M.e.T.A.", "M.e.T.A", "g.m.b.h.", "g.m.b.h",
			"gmbh", "s.p.a.", "s.p.a", "spa", "c.a.", "c.a", "a.g.", "a.g",
			"ag", "a.b.", "a.b", "ab", "aktiebolaget", "aktiengesellschaft",
			"n.v.", "n.v", "nv", "bv", "b.v.", "b.v", "p.c.", "p.c", "de c.v.",
			"de c.v", "de cv", "b.d.d.p.", "b.d.d.p", "bddp" };

	public static String[] REL_POS_PRONOUNS = { "whose" };

	private static Set<String> COMP_SET = null;

	public static Set<String> getCompanyDesig() {
		FeatureUtils.getTitles();
		if (COMP_SET == null) {
			COMP_SET = FeatureUtils.stringArray2TreeSet(CORP_DESIG);
		}

		return COMP_SET;
	}

	private static Set<String> TITLES_SET = null;

	public static Set<String> getTitles() {
		FeatureUtils.getTitles();
		if (TITLES_SET == null) {
			TITLES_SET = FeatureUtils.stringArray2TreeSet(TITLES);
		}

		return TITLES_SET;
	}

	public static Set<String> getAllTitles() {
		return FeatureUtils.getTitles();
	}

	public static boolean containsSameHead(AnnotationSet nps, Annotation np,
			boolean isNP, Document doc) {
		AnnotationSet overlap = nps.getOverlapping(np);
		AnnotationSet parse = doc.getAnnotationSet(Constants.PARSE);
		AnnotationSet ne = doc.getAnnotationSet(Constants.NE);

		if (overlap == null || overlap.size() < 1)
			return false;
		Annotation head = HeadNoun.getValue(np, doc);
		if (head.equals(Annotation.getNullAnnot()))
			return false;
		for (Annotation n : overlap) {
			Annotation h = HeadNoun.getValue(n, doc);
			if (head.compareSpan(h) == 0)
				if (!isNP
						|| !containsBreak(n.getStartOffset(), h.getEndOffset(),
								parse, ne, doc.getText()))
					return true;
				else if (np.compareSpan(n) == 0
						|| (n.getStartOffset() == np.getStartOffset() && (h
								.getEndOffset() == np.getEndOffset() || n
								.getEndOffset() == head.getEndOffset())))
					return true;
		}
		return false;
	}

	public static boolean containsConflict(AnnotationSet nps, Annotation np,
			Document doc) {
		AnnotationSet overlap = nps.getOverlapping(np);
		for (Annotation n : overlap) {
			if (!n.covers(np) && !np.covers(n))
				return true;
		}
		return false;
	}

	public static boolean isNested(Annotation a, AnnotationSet annots) {
		AnnotationSet overlap = annots.getOverlapping(a);
		for (Annotation o : overlap) {
			if (o.properCovers(a))
				return true;
		}
		return false;
	}

	public static boolean containsPhrase(Annotation parseNode,
			AnnotationSet parse, AnnotationSet basenp, String text) {
		if (parseNode == null)
			return false;

		AnnotationSet contained = parse.getContained(parseNode);
		AnnotationSet containedNPs = new AnnotationSet("cnp");
		boolean npChild = false, containsNP = false, isPhrase = false;
		for (Annotation c : contained) {
			if (c.getType().equalsIgnoreCase("NP") && parseNode.properCovers(c)) {
				containsNP = true;
				containedNPs.add(c);
				Annotation parent = SyntaxUtils.getParent(c, parse);
				if (parent != null && parent.equals(parseNode)) {
					npChild = true;
				}
			}
			if (c.getType().startsWith("S")
					|| c.getType().equalsIgnoreCase("VP")) {
				isPhrase = true;
			}
		}

		if (!containsNP)
			return false;

		if (containsDisjunction(parseNode, parse, text))
			return true;

		if (!npChild) {
			for (Annotation c : contained) {
				if (isNoun(c, text) && !containedNPs.coversSpan(c)) {
					basenp.add(c);
				}
			}
		}

		if (isPhrase)
			return true;

		boolean appos = true;
		int numNps = 0;
		int numCommas = 0;
		for (Annotation c : contained) {
			Annotation parent = SyntaxUtils.getParent(c, parse);
			if (parent != null && parent.equals(parseNode)) {
				if (c.getType().equalsIgnoreCase("NP")
						|| (c.getType().equalsIgnoreCase("ADJP"))) {
					numNps++;
				} else if (c.getType().equals(",")) {
					numCommas++;
				} else {
					appos = false;
					break;
				}
			}
		}
		appos = numNps >= 2 && numCommas >= 1;
		return appos;
		// return false;
	}

	public static boolean containsAppositive(Annotation parseNode,
			AnnotationSet parse, String text) {
		AnnotationSet contained = parse.getContained(parseNode);
		boolean appos = true;
		int numNps = 0;
		int numCommas = 0;
		for (Annotation c : contained) {
			Annotation parent = SyntaxUtils.getParent(c, parse);
			if (parent != null && parent.equals(parseNode)) {
				if (c.getType().equalsIgnoreCase("NP")
						|| (c.getType().equalsIgnoreCase("ADJP"))) {
					numNps++;
				} else if (c.getType().equals(",")) {
					numCommas++;
				} else {
					appos = false;
					break;
				}
			}
		}
		appos = numNps >= 2 && numCommas >= 1;
		return appos;
	}

	public static boolean containsBreak(Annotation a, AnnotationSet parse,
			AnnotationSet ne, String text) {
		return containsBreak(a.getStartOffset(), a.getEndOffset(), parse, ne,
				text);
	}

	public static boolean containsBreak(int start, int end,
			AnnotationSet parse, AnnotationSet ne, String text) {
		AnnotationSet overlap = parse.getContained(start, end);
		AnnotationSet overlapNps = new AnnotationSet("onps");
		for (Annotation o : overlap)
			if (o.getType().equals("NP")
					&& (o.getStartOffset() != start || o.getEndOffset() != end)) {
				overlapNps.add(o);
			}

		// if(overlapNps.size()<1)
		// return false;
		for (Annotation o : overlap) {
			// System.out.println("NP: "+Utils.getAnnotText(o, text));
			if (o.getType().equals("CC")
					&& !"or".equalsIgnoreCase(Utils.getAnnotText(o, text))
					&& !ne.coversSpan(o)
					&& (overlapNps == null || !overlapNps.coversSpan(o)))
				return true;
		}
		return false;
	}

	public static boolean containsConj(Annotation a, AnnotationSet parse,
			String text) {
		AnnotationSet overlap = parse.getContained(a);
		for (Annotation o : overlap) {
			if (o.getType().equals("CC")
					&& !"or".equalsIgnoreCase(Utils.getAnnotText(o, text)))
				return true;
		}
		return false;
	}

	public static boolean containsDisjunction(Annotation a,
			AnnotationSet parse, String text) {
		AnnotationSet overlap = parse.getContained(a);
		for (Annotation o : overlap) {
			if (o.getType().equals("CC")
					&& "or".equalsIgnoreCase(Utils.getAnnotText(o, text)))
				return true;
		}
		return false;
	}

	public static boolean isNoun(Annotation a, String text) {
		if (Utils.getDatasetName().toLowerCase().startsWith("muc6"))
			return isMUC6Noun(a, text);
		else if (Utils.getDatasetName().toLowerCase().startsWith("muc7"))
			return isMUC7Noun(a, text);
		else if (Utils.getDatasetName().toLowerCase().startsWith("uw"))
			return isUWNoun(a, text);
		else
			return isACENoun(a, text);
	}

	public static boolean isMUC7Noun(Annotation a, String text) {
		if (a == null)
			return false;
		String type = a.getType();
		// if(type.startsWith("NN")||type.startsWith("PRP"))
		// return true;
		if (type.startsWith("NN")
				&& FeatureUtils.isCapitalized(Utils.getAnnotText(a, text)))
			return true;
		if (type.startsWith("PRP"))
			return true;
		return false;
	}

	public static boolean isMUC6Noun(Annotation a, String text) {
		if (a == null)
			return false;
		String type = a.getType();
		if (type.startsWith("NN") || type.startsWith("PRP"))
			return true;
		return false;
	}

	public static boolean isACENoun(Annotation a, String text) {
		if (a == null)
			return false;
		String type = a.getType();
		// if(type.startsWith("NNP"))
		// return true;
		if (type.startsWith("NN")
				&& FeatureUtils.isCapitalized(Utils.getAnnotText(a, text)))
			return true;
		if (type.equals("JJ")
				&& FeatureUtils.isCapitalized(Utils.getAnnotText(a, text)))
			return true;
		if (type.startsWith("PRP"))
			return true;
		return false;
	}

	public static boolean isUWNoun(Annotation a, String text) {
		if (a == null)
			return false;
		String type = a.getType();
		// if(type.startsWith("NNP"))
		// return true;
		// if (type.startsWith("NN") &&
		// FeatureUtils.isCapitalized(Utils.getAnnotText(a, text))) return true;
		// if (type.equals("JJ") &&
		// FeatureUtils.isCapitalized(Utils.getAnnotText(a, text))) return true;
		if (type.startsWith("PRP"))
			return true;
		return false;
	}

	public static boolean isNP(Annotation an, String text) {
		if (Utils.getDatasetName().toLowerCase().startsWith("muc"))
			return isMUCNP(an, text);
		else {
			if (Utils.getDatasetName().toLowerCase().startsWith("uw"))
				return isUWNP(an, text);
			return isACENP(an, text);
		}
	}

	public static boolean isMUCNP(Annotation an, String text) {
		try {
			String type = an.getType();

			if (FeatureUtils.memberArray(type, SyntaxUtils.NPType))
				return true;
		} catch (NullPointerException npe) {
			return false;
		}

		return false;
	}

	public static boolean isACENP(Annotation an, String text) {
		try {
			String type = an.getType();

			if (FeatureUtils.memberArray(type, SyntaxUtils.NPType))
				return true;
			if (type.equalsIgnoreCase("WHNP")
					&& !"that".equalsIgnoreCase(Utils.getAnnotText(an, text)))
				return true;

		} catch (NullPointerException npe) {
			return false;
		}

		return false;
	}

	public static boolean isUWNP(Annotation an, String text) {
		try {
			String type = an.getType();

			if (FeatureUtils.memberArray(type, SyntaxUtils.NPType))
				return true;
			if (type.equalsIgnoreCase("WHNP")
					&& FeatureUtils.memberArray(Utils.getAnnotText(an, text),
							REL_POS_PRONOUNS))
				return true;

		} catch (NullPointerException npe) {
			return false;
		}

		return false;
	}

	public static boolean isGerund(Annotation p, String text) {
		if (!p.getType().equals("NP")
				&& Utils.getAnnotText(p, text).endsWith("ing")) {
			// System.out.println("Gerund " + Utils.getAnnotText(p, text));
			return true;
		}
		return false;
	}

	public static boolean isJunk(Annotation a, String text) {
		// If the np contatins no letter, then it is not interesting
		if (a == null)
			return false;

		String annotText = Utils.getAnnotText(a, text);

		if (annotText.matches("\\W*"))
			return true;
		if (annotText.matches("\\W*\\w\\W*") && !annotText.matches("\\W*I\\W*"))
			return true;
		if (getTitles().contains(annotText.toLowerCase()))
			return true;
		if (getCompanyDesig().contains(annotText.toLowerCase()))
			return true;
		return false;
	}

	public static void add(AnnotationSet result, Annotation a, String text) {
		result.add(a.getStartOffset(), a.getEndOffset(), a.getType(), a
				.getFeatures());
	}

	public static AnnotationSet removeRecursives(AnnotationSet annots) {

		AnnotationSet result = new AnnotationSet(annots.getName());
		boolean recursive = false;

		for (Annotation a : annots) {
			AnnotationSet overlap = annots.getOverlapping(a);
			for (Annotation o : overlap) {
				if (a.properCovers(o)) {
					recursive = true;
					break;
				}
			}

			if (!recursive) {
				result.add(a);
			}

			recursive = false;
		}

		return result;
	}

	/*
	 * This method cleans potential nps by removing leading and trailing junk as
	 * well as possesives
	 */
	public static Annotation trimNP(Annotation np, AnnotationSet parse,
			String text) {
		AnnotationSet contained = parse.getContained(np);
		Annotation result = np;
		// Remove possesive 's as well as some other junk that occurs at end of
		// nps
		for (Annotation p : contained) {
			if ((p.getType().equals("POS") || p.getType().equals("\"") || Utils
					.getAnnotText(p, text).matches("\\(|\\)|\\\"|\\.|\\,"))
					&& p.getEndOffset() == np.getEndOffset()) {
				result = new Annotation(np.getId(), np.getStartOffset(), p
						.getStartOffset(), "newNP",
						new TreeMap<String, String>());
				break;
			}
		}
		// Remove trailing punctuation
		String anText = Utils.getAnnotText(result, text);
		int endOffset = result.getEndOffset();
		while (anText.length() > 0
				&& anText.substring(anText.length() - 1).matches(
						"\\(|\\)|\\\"|\\,|\\s|\\_")) {// &&
			// !anText.substring(anText.length()-1).matches("\\.")){
			endOffset--;
			anText = Utils.getAnnotText(result.getStartOffset(), endOffset,
					text);
		}
		if (endOffset != result.getEndOffset()) {
			result = new Annotation(result.getId(), result.getStartOffset(),
					endOffset, "punctStrip", result.getFeatures());
		}

		// Remove leading punctuation
		anText = Utils.getAnnotText(result, text);
		int startOffset = result.getStartOffset();
		while (anText.length() > 0
				&& anText.substring(0, 1).matches("\\(|\\)|\\\"|\\,|\\s|\\_")) {// &&
			// !anText.substring(anText.length()-1).matches("\\.")){
			startOffset++;
			anText = Utils.getAnnotText(startOffset, result.getEndOffset(),
					text);
		}
		if (startOffset != result.getStartOffset()) {
			result = new Annotation(result.getId(), startOffset, result
					.getEndOffset(), "punctStrip", result.getFeatures());
		}

		return result;

	}

	public static AnnotationSet getBaseNP(String annSetName, Document doc) {
		AnnotationSet bnp = new AnnotationSet("bnp");
		AnnotationSet parseAnns = doc.getAnnotationSet(Constants.PARSE);

		// Start with all nps
		for (Annotation p : parseAnns) {
			if (isNP(p, doc.getText())) {
				bnp.add(p);
			}
		}

		AnnotationSet filtered = new AnnotationSet("bnp");

		// Remove nps that contain PP's or S's
		for (Annotation p : bnp) {
			if (!containsPhrase(p, parseAnns, filtered, doc.getText())) {
				filtered.add(p);
				// if(containsConj(p,parseAnns,text))
				// System.out.println("Contains conjunction:
				// "+Utils.getAnnotText(p, text));
			}
			// else
			// System.out.println("Contains phrase: "+Utils.getAnnotText(p,
			// text));
		}

		// Remove all nested nps
		AnnotationSet result = new AnnotationSet(annSetName);

		// Remove nps that are nested
		for (Annotation p : filtered) {
			if (!isNested(p, filtered)) {
				result.add(p);
			}
		}

		// System.out.print(result);

		// return filtered;
		return result;
	}

	public static void fixNumbering(AnnotationSet nps) {
		int counter = 0;
		for (Annotation np : nps) {
			np.setAttribute(Constants.CE_ID, Integer.toString(counter++));
		}
	}

	public static AnnotationSet extractMarkables(AnnotationSet bnp,
			AnnotationSet parse, String annSetName, Document doc) {
		AnnotationSet nerAnns = doc.getAnnotationSet(Constants.NE);

		// remove overlapping nes
		AnnotationSet nes = removeRecursives(nerAnns);

		AnnotationSet origNes = new AnnotationSet("nes", nes);
		AnnotationSet result = new AnnotationSet(annSetName, false);

		// Keep track of the foolowing categories -- basenp's that overlap ne's,
		// other basenps
		// and ne's that do not overlap any nps
		TreeSet<Annotation> basenps = new TreeSet<Annotation>(
				new AnnotationComparatorNestedLast());
		basenps.addAll(bnp);

		for (Annotation np : basenps) {
			np = trimNP(np, parse, doc.getText());
			if (np.getLength() <= 0) {
				// System.out.println("0 annotation from "+content);
			} else if (!result.containsSpan(np) && !origNes.coversSpan(np)
					&& !isJunk(np, doc.getText())
					&& (!containsSameHead(result, np, true, doc))) {
				addNP(result, np, nes, origNes, parse, doc);
				addNested(np, parse, result, origNes, nes, doc);
			}
		}

		// Add all other NEs
		for (Annotation a : nes) {
			// a = trimNP(a, parse, text);
			// System.out.print("Adding NE "+Utils.getAnnotText(a, text)+"...");
			if (a.getLength() > 0 && !result.containsSpan(a)
					&& !containsSameHead(result, a, true, doc)
					&& !isJunk(a, doc.getText())
					&& !a.getType().equalsIgnoreCase("number")) {
				if (!containsConflict(result, a, doc)) {
					if (!Utils.getDatasetName().toLowerCase().startsWith("uw")
							|| !result.coversSpan(a))
						result.add(a.getStartOffset(), a.getEndOffset(), "NE");
					// System.out.println("added.");
				}
			}
			// else{
			// System.out.println("-----not added.----"+containsSameHead(result,
			// a, annots, text));
			// }
		}

		AnnotationSet intermediateResult = new AnnotationSet("im");
		intermediateResult.addAll(result);

		// for(Annotation r:intermediateResult){
		// addNested(r, parse, result, origNes, nes, annots, text);
		// }

		for (Annotation r : intermediateResult) {
			addNestedNonNPs(r, parse, result, origNes, nes, doc);
		}

		return result;
	}

	private static void addNP(AnnotationSet result, Annotation np,
			AnnotationSet nes, AnnotationSet origNes, AnnotationSet parse,
			Document doc) {
		np = trimNP(np, parse, doc.getText());
		if (!containsSameHead(result, np, true, doc)) {

			AnnotationSet overlapNe = nes.getOverlapping(np);

			// System.err.println(np);
			if (!overlapNe.isEmpty()) {
				Annotation ne = overlapNe.getLast();

				// Special case, the np overlaps two ne's
				// do not add it in this case
				if (overlapNe.size() > 1) {
					Annotation first = overlapNe.getFirst();
					if (first.getStartOffset() < np.getStartOffset()
							&& ne.getEndOffset() > np.getEndOffset())
						return;
				}
				// System.err.println(ne);

				// Merging a named entity and a noun phrase to create a NPE
				if (np.covers(ne)
						|| (np.getStartOffset() <= ne.getStartOffset() && doc
								.getAnnotText(np.getEndOffset(),
										ne.getEndOffset()).matches("\\W+"))) {
					if (!result.containsSpan(np)) {
						if (!isJunk(np, doc.getText())) {
							int end = np.getEndOffset() >= ne.getEndOffset() ? np
									.getEndOffset()
									: ne.getEndOffset();
							Annotation newAnn = new Annotation(0, np
									.getStartOffset(), end, "NPE", np
									.getFeatures());
							newAnn = trimNP(newAnn, parse, doc.getText());
							if (!containsConflict(result, np, doc)) {
								result.add(newAnn);
							}
						}
					}

					if (HeadNoun.getValue(ne, doc).compareSpan(
							HeadNoun.getValue(np, doc)) == 0
							&& !containsBreak(np, parse, origNes, doc.getText())) {
						nes.remove(ne);

						// System.err.println("Removed from nes1: " + ne);
					}

				} else {

					// System.err.println("Removed from nes2: " + ne);

					int startNP = np.getStartOffset();
					int startNE = ne.getStartOffset();
					int start = startNP < startNE ? startNP : startNE;
					int endNP = np.getEndOffset();
					int endNE = ne.getEndOffset();
					int end = endNP > endNE ? endNP : endNE;
					Annotation snp = null;
					if (!result.containsSpan(start, end)) {
						snp = new Annotation(0, start, end, "sNP");
						snp = trimNP(snp, parse, doc.getText());
						if (!containsConflict(result, snp, doc)) {
							result.add(snp);
						} else {
							snp = null;
						}
					}
					if (snp != null
							&& HeadNoun.getValue(ne, doc).compareSpan(
									HeadNoun.getValue(snp, doc)) == 0
							&& !containsBreak(snp, parse, origNes, doc
									.getText())) {
						nes.remove(ne);
					}
				}
			} else {
				if (!containsSameHead(result, np, true, doc)
						&& !containsConflict(result, np, doc)) {
					result.add(np);
				}
			}
		}

	}

	private static void addNested(Annotation parent, AnnotationSet parse,
			AnnotationSet result, AnnotationSet origNes, AnnotationSet nes,
			Document doc) {

		AnnotationSet contained = parse.getContained(parent);
		// System.out.println("Adding nested inside
		// "+FeatureUtils.getText(parent, text));

		for (Annotation par1 : contained) {
			if (par1.getType().equals("NP")) {
				Annotation np = trimNP(par1, parse, doc.getText());
				if (np.getLength() <= 0) {
					// System.out.println("0 annotation from
					// "+Utils.getAnnotText(par1, text));
				} else if (parent.properCovers(np) && !result.containsSpan(np)
						&& !origNes.coversSpan(np)
						&& !isJunk(np, doc.getText())
						&& !containsSameHead(result, np, true, doc) /*
																	 * &&
																	 * !containsAppositive(np,
																	 * parse,
																	 * text)
																	 */) {
					// System.out.println("Found "+FeatureUtils.getText(np,
					// text));
					// add(result, np, text);
					if (!containsConflict(result, np, doc)) {
						addNP(result, np, nes, origNes, parse, doc);
					}
				}
			}
		}

		// endOffsets.add(new Integer(parent.getEndOffset()));
	}

	private static void addNestedNonNPs(Annotation parent, AnnotationSet parse,
			AnnotationSet result, AnnotationSet origNes, AnnotationSet nes,
			Document doc) {

		// System.out.println("Adding NON NPs inside
		// "+FeatureUtils.getText(parent, text));
		AnnotationSet pContained = parse.getContained(parent);
		for (Annotation p : pContained) {
			if (isNoun(p, doc.getText()) && parent.properCovers(p)
					&& !containsSameHead(result, p, false, doc)
					&& !result.containsSpan(p) && !origNes.coversSpan(p)
					&& !isJunk(p, doc.getText())) {
				if (!getAllTitles().contains(doc.getAnnotText(p).toLowerCase())) {// &&FeatureUtils.isCapitalized(Utils.getAnnotText(p,
					// text)))
					p = trimNP(p, parse, doc.getText());
					if (!containsSameHead(result, p, false, doc)
							&& !result.containsSpan(p))
						if (!containsConflict(result, p, doc)) {
							add(result, p, doc.getText());
						}
				}
			}
		}
	}

	/*
	 * Prints out a snapshot of what markables we are extracting.
	 */
	public static void pprintMarkables(AnnotationSet basenp, Document doc) throws IOException {
		FileWriter fw;
		fw = new FileWriter("/home/ngilbert/xspace/" + Utils.SEPARATOR + "muc6-markables", true);

		for (Annotation curr : basenp) {
			/*
			fw.write(curr.getId() + "\t" + curr.getStartOffset() + ","
					+ curr.getEndOffset() + "\tstring\t" + curr.getType()
					+ "\t");

			Map<String, String> features = curr.getFeatures();
			if (features != null) {
				Iterator<String> featuresIter = features.keySet().iterator();
				while (featuresIter.hasNext()) {
					Object currFeat = featuresIter.next();
					fw.write(currFeat + "=\""
							+ features.get(currFeat).replaceAll("\\n", " ")
							+ "\" ");				
				}
			}
			*/			
			fw.write(doc.getAnnotText(curr) + "\n");
			//fw.write(Utils.getAnnotTextClean(curr, text) + "\n");
		}

		fw.flush();
		fw.close();
	}

	// This method extracts the base NPs that are used in coreference resolution
	// Uses the MUC definition of NP
	@Override
	public void run(Document doc, String[] annSetNames) {
		String annSetName = annSetNames[0];
		doc.loadAnnotationSetsByName(new String[] { Constants.PARSE,
				Constants.POS, Constants.NE });
		AnnotationSet bnp = run(annSetName, doc);

		Annotation problem;
		if ((problem = bnp.checkForCrossingAnnotations()) != null)
			throw new RuntimeException("Crossing annotation: " + problem);
		addResultSet(doc, bnp);
		
		 /*
		 try { 
			 pprintMarkables(bnp, doc); } 
		 catch(IOException ioe) {
				 ioe.printStackTrace(); 
		 }
		 */		
	}

	// This method extracts the base NPs that are used in coreference resolution
	// Uses the MUC definition of NP
	public static AnnotationSet run(String annSetName, Document doc) {
		AnnotationSet parseAnns = doc.getAnnotationSet(Constants.PARSE);
		AnnotationSet bnp = getBaseNP(annSetName, doc);
		AnnotationSet result = extractMarkables(bnp, parseAnns, annSetName, doc);
		fixNumbering(result);

		AnnotationSet renumbered = new AnnotationSet(annSetName);

		for (Annotation r : result) {
			add(renumbered, r, doc.getText());
		}

		return renumbered;
	}
}
