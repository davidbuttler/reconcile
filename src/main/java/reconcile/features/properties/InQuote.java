package reconcile.features.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.general.Constants;
import reconcile.general.SyntaxUtils;
import reconcile.general.Utils;


public class InQuote
    extends Property {

private static Property ref = null;

public static Property getInstance()
{
  if (ref == null) {
    ref = new InQuote(true, true);
  }
  return ref;
}

public static Integer getValue(Annotation np, Document doc)
{
  return (Integer) getInstance().getValueProp(np, doc);
}

private InQuote(boolean whole, boolean cached) {
  super(whole, cached);
}

@Override
public Object produceValue(Annotation np, Document doc)
{
  // Find all quote locations
  SortedSet<Integer> quoteLocations = new TreeSet<Integer>();
  Pattern p = Pattern.compile("\"|''|``|\u201C|\u201D");

  String text = doc.getText();
  Matcher m = p.matcher(text);

  boolean inQuote = false;
  while (m.find()) {
    int start = m.start();
    if (inQuote && (text.substring(start).startsWith("``") || text.substring(start).startsWith("\u201C"))) {
      // We have an opening quote; Make sure the previous quote is closed
      quoteLocations.add((start - 1));
      inQuote = false;
    }
    quoteLocations.add((start));
    inQuote = !inQuote;
  }
  // System.out.println("Quote locations: "+quoteLocations);

  // Figure out which noun corresponds to which quote
  AnnotationSet sent = doc.getAnnotationSet(Constants.SENT);
  Iterator<Integer> quoteIter = quoteLocations.iterator();
  HashMap<Integer, Annotation> reporters = new HashMap<Integer, Annotation>();
  HashMap<Integer, Annotation> companies = new HashMap<Integer, Annotation>();
  HashMap<Integer, Annotation> sentReporter = new HashMap<Integer, Annotation>();
  HashMap<Integer, Annotation> compReporter = new HashMap<Integer, Annotation>();
  int counter = 1;
  while (quoteIter.hasNext()) {
    int qStart = quoteIter.next();
    if (!quoteIter.hasNext()) {
      break;
    }
    int qEnd = quoteIter.next() + 1;

    AnnotationSet sentences = sent.getOverlapping(qStart, qEnd);

    // Three cases for the size of the sentences set
    Annotation match, compMatch;
    if (sentences.size() < 1) {
      System.out.println("Quote is not covered by any sentence:");
      int beg = qStart - 15;
      beg = beg < 0 ? 0 : beg;
      int en = qStart + 15;
      en = en >= text.length() ? text.length() - 1 : en;
      System.out.println(Utils.getAnnotText(beg, en, text));
      System.out.println("Position " + qStart);
      match = Annotation.getNullAnnot();
      compMatch = Annotation.getNullAnnot();
      // throw new RuntimeException("Quote is not covered by any sentence");
    }
    else if (sentences.size() == 1) {
      Annotation s = sentences.getFirst();
      // System.out.println("Sent: "+Utils.getAnnotText(s, text));
      if (s.properCovers(qStart, qEnd)) {
        match = findReporter(qStart, qEnd, s, doc);
        compMatch = findCompany(qStart, qEnd, s, doc);
        if (match.equals(Annotation.getNullAnnot())) {
          match = findReportCont(sentReporter, s, doc);
          compMatch = findReportCont(compReporter, s, doc);
        }
      }
      else {
        match = findReportCont(sentReporter, s, doc);
        compMatch = findReportCont(compReporter, s, doc);
      }
      sentReporter.put(Integer.decode(s.getAttribute("sentNum")), match);
      compReporter.put(Integer.decode(s.getAttribute("sentNum")), compMatch);
    }
    else {
      // The quoted string spans more than one sentence.
      Annotation beg = sentences.getFirst();
      // System.out.println("First sent: "+Utils.getAnnotText(beg, text));
      Annotation end = sentences.getLast();
      // System.out.println("Last sent: "+Utils.getAnnotText(end, text));
      match = Annotation.getNullAnnot();
      compMatch = Annotation.getNullAnnot();
      if (beg.getStartOffset() < qStart) {
        match = findReporter(qStart, qEnd, beg, doc);
        compMatch = findCompany(qStart, qEnd, beg, doc);
      }
      if (match.equals(Annotation.getNullAnnot()) && qEnd < end.getEndOffset()) {
        match = findReporter(qStart, qEnd, end, doc);
        compMatch = findCompany(qStart, qEnd, end, doc);
      }
      if (match.equals(Annotation.getNullAnnot())) {
        match = findReportCont(sentReporter, beg, doc);
        compMatch = findCompany(qStart, qEnd, end, doc);
      }
      sentReporter.put(Integer.parseInt(beg.getAttribute("sentNum")), match);
      sentReporter.put(Integer.parseInt(end.getAttribute("sentNum")), match);
      compReporter.put(Integer.parseInt(beg.getAttribute("sentNum")), compMatch);
      compReporter.put(Integer.parseInt(end.getAttribute("sentNum")), compMatch);

    }
    reporters.put(counter, match);
    companies.put(counter, compMatch);
    counter += 2;

    // System.out.println("Quote: "+Utils.getAnnotText(qStart, qEnd, text));
    // if(!match.equals(Annotation.getNullAnnot())){
    // System.out.println("Match: "+Utils.getAnnotText(match, text));
    // }else{
    // System.out.println("no match!");
    // }
  }
  int initial = quoteLocations.size();

  AnnotationSet nps = doc.getAnnotationSet(Constants.NP);
  for (Annotation a : nps.getOrderedAnnots()) {
    int s = a.getStartOffset();
    quoteLocations = quoteLocations.tailSet(s);
    int numQuotes = initial - quoteLocations.size();

    // System.err.println(numQuotes);
    if (numQuotes % 2 == 0) {
      a.setProperty(this, -1);
      a.setProperty(Property.AUTHOR, Annotation.getNullAnnot());
      a.setProperty(Property.COMP_AUTHOR, Annotation.getNullAnnot());
    }
    else {
      a.setProperty(this, numQuotes);
      a.setProperty(Property.AUTHOR, reporters.get(numQuotes));
      a.setProperty(Property.COMP_AUTHOR, companies.get(numQuotes));
      // if(FeatureUtils.isPronoun(a, annotations, text)&&FeatureUtils.getPronounPerson(FeatureUtils.getText(a,
      // text))==1
      // &&FeatureUtils.NumberEnum.SINGLE.equals(FeatureUtils.getNumber(a, annotations, text))){
      // Annotation repo = reporters.get(new Integer(numQuotes));
      // if(repo==null||repo.equals(Annotation.getNullAnnot())){
      // Annotation thisSent = sent.getOverlapping(a).getFirst();
      // System.out.println("*** No author in "+Utils.getAnnotText(thisSent, text+"***"));
      // }
      // }
    }
  }
  // System.out.println("End of inquote");

  return np.getProperty(this);
}

private Annotation findReporter(int qStart, int qEnd, Annotation sent, Document doc)
{
  Annotation result = Annotation.getNullAnnot();
  // Find reporting verbs in the unquoted part of the sentence
  // System.out.println("looking for reporter in sentence "+Utils.getAnnotText(sent, text));

  ArrayList<Annotation> rVerbs1 = new ArrayList<Annotation>();
  if (qStart > sent.getStartOffset()) {
    rVerbs1 = findRepVerbs(sent.getStartOffset(), qStart, doc);
  }
  if (qEnd < sent.getEndOffset()) {
    rVerbs1.addAll(findRepVerbs(qEnd, sent.getEndOffset(), doc));
  }
  // Now find possible nouns that are subjects of reporting verbs
  AnnotationSet dep = doc.getAnnotationSet(Constants.DEP);
  AnnotationSet parse = doc.getAnnotationSet(Constants.PARSE);
  ArrayList<Annotation> reps = new ArrayList<Annotation>();
  for (Annotation verb : rVerbs1) {
    // System.out.println("Reporting verb "+Utils.getAnnotText(verb, text));
    Annotation reporter = SyntaxUtils.getSubject(verb, sent, dep, parse);
    if (reporter != null) {
      // System.out.println("Reporter "+Utils.getAnnotText(reporter, text));
      reps.add(reporter);
    }
  }
  if (reps.size() == 1) {
    result = reps.get(0);
  }
  else if (reps.size() > 1) {
    // Multiple matches; find the match that's closest to the quotes
    int distance = Integer.MAX_VALUE;
    for (Annotation r : reps) {
      int curDist = getDistance(r, qStart, qEnd);
      if (curDist < distance) {
        result = r;
        distance = curDist;
      }
    }
  }
  return result;
}

private Annotation findCompany(int qStart, int qEnd, Annotation sent, Document doc)
{
  Annotation result = Annotation.getNullAnnot();
  // Find reporting verbs in the unquoted part of the sentence
  // System.out.println("looking for reporter in sentence "+Utils.getAnnotText(sent, text));

  ArrayList<Annotation> companies = new ArrayList<Annotation>();
  if (qStart > sent.getStartOffset()) {
    companies = findCompanies(sent.getStartOffset(), qStart, doc);
  }
  if (qEnd < sent.getEndOffset()) {
    companies.addAll(findCompanies(qEnd, sent.getEndOffset(), doc));
  }
  if (companies.size() == 1) {
    result = companies.get(0);
  }
  else if (companies.size() > 1) {
    // Multiple matches; find the match that's closest to the quotes
    int distance = Integer.MAX_VALUE;
    for (Annotation r : companies) {
      int curDist = getDistance(r, qStart, qEnd);
      if (curDist < distance) {
        result = r;
        distance = curDist;
      }
    }
  }
  return result;
}

private Annotation findReportCont(Map<Integer, Annotation> sentReporter, Annotation sent, Document doc)
{
  Annotation result = Annotation.getNullAnnot();
  // for(Integer i:sentReporter.keySet()){
  // System.out.println(i+":"+Utils.getAnnotText(sentReporter.get(i),text));
  // }
  int sentNum = Integer.parseInt(sent.getAttribute("sentNum")) - 1;
  // System.out.println("Continuation for sentence "+sentNum);
  Annotation prev = sentReporter.get(sentNum);
  while (prev != null && prev.equals(Annotation.getNullAnnot())) {
    // System.out.println("Looking at sentence "+sentNum);
    sentNum--;
    prev = sentReporter.get(sentNum);
  }
  if (prev != null) {
    result = prev;
  }
  return result;
}

public Annotation findCompanyCont(Map<Integer, Annotation> compReporter, Annotation sent, Document doc)
{
  Annotation result = Annotation.getNullAnnot();
  // for(Integer i:sentReporter.keySet()){
  // System.out.println(i+":"+Utils.getAnnotText(sentReporter.get(i),text));
  // }
  int sentNum = Integer.parseInt(sent.getAttribute("sentNum")) - 1;
  // System.out.println("Continuation for sentence "+sentNum);
  Annotation prev = compReporter.get(sentNum);
  while (prev != null && prev.equals(Annotation.getNullAnnot())) {
    // System.out.println("Looking at sentence "+sentNum);
    sentNum--;
    prev = compReporter.get(sentNum);
  }
  if (prev != null) {
    result = prev;
  }
  return result;
}

public ArrayList<Annotation> findRepVerbs(int start, int end, Document doc)
{
  ArrayList<Annotation> result = new ArrayList<Annotation>();
  AnnotationSet parse = doc.getAnnotationSet(Constants.PARSE).getContained(start, end);
  if (parse != null && parse.size() > 0) {
    for (Annotation a : parse) {
      if (FeatureUtils.isReportingVerb(doc.getAnnotText(a))) {
        result.add(a);
      }
    }
  }
  return result;
}

public ArrayList<Annotation> findCompanies(int start, int end, Document doc)
{
  ArrayList<Annotation> result = new ArrayList<Annotation>();
  AnnotationSet init = new AnnotationSet("init");
  AnnotationSet nps = doc.getAnnotationSet(Constants.NP).getContained(start, end);
  if (nps != null && nps.size() > 0) {
    for (Annotation a : nps) {
      if (FeatureUtils.NPSemTypeEnum.ORGANIZATION.equals(NPSemanticType.getValue(a, doc))
          || (FeatureUtils.memberArray("organization", WNSemClass.getValue(a, doc)))) {
        init.add(a);
      }
    }
  }
  for (Annotation a : init) {
    boolean add = true;
    AnnotationSet ovlap = init.getOverlapping(a);
    for (Annotation o : ovlap) {
      if (o.properCovers(a)) {
        add = false;
      }
    }
    if (add) {
      result.add(a);
    }
  }
  return result;
}

public int getDistance(Annotation a, int start, int end)
{
  int start1 = a.getStartOffset();
  int end1 = a.getEndOffset();
  int d1 = Math.abs(start - start1);
  int d2 = Math.abs(start - end1);
  int d3 = Math.abs(end - start1);
  int d4 = Math.abs(end - end1);
  d1 = d1 < d2 ? d1 : d2;
  d3 = d3 < d4 ? d3 : d4;
  return d1 < d3 ? d1 : d3;
}
}
