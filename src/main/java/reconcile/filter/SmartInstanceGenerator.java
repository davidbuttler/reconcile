package reconcile.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import reconcile.data.Annotation;
import reconcile.data.Document;
import reconcile.features.FeatureUtils;
import reconcile.features.FeatureUtils.PersonPronounTypeEnum;
import reconcile.features.properties.ProperNameType;
import reconcile.features.properties.SentNum;
import reconcile.general.Constants;
import reconcile.general.RuleResolvers;


public class SmartInstanceGenerator
    extends PairGenerator {

// A list to store all the pairs
ArrayList<Annotation[]> pairs;
Iterator<Annotation[]> pairIter;

@Override
public void initialize(Annotation[] nps, Document doc, boolean training)
{
  super.initialize(nps, doc, training);
  pairs = new ArrayList<Annotation[]>();
  HashMap<Annotation, ArrayList<Annotation>> posessives = new HashMap<Annotation, ArrayList<Annotation>>();

  RuleResolvers.addAllPossesives(doc.getAnnotationSet(Constants.NP), doc, posessives);
  int propNames = 0, regular = 0, is = 0, def = 0;
  for (int j = nps.length - 1; j >= 0; j--) {
    Annotation np2 = nps[j];
    RuleResolvers.NPType type2 = RuleResolvers.getNPtype(np2, doc, posessives);
    // int par2 = ParNum.getValue(np2, doc);
    int sen2 = SentNum.getValue(np2, doc);
    boolean pn2 = type2.equals(RuleResolvers.NPType.PROPER_NAME);
    boolean pron2 = type2.equals(RuleResolvers.NPType.PRONOUN);
    boolean def2 = !pron2 && !pn2 && !FeatureUtils.isIndefinite(np2, doc);
    boolean specPronoun2 = pron2 && FeatureUtils.getPronounPerson(doc.getAnnotText(np2)) != PersonPronounTypeEnum.THIRD;
    boolean person2 = pn2 && ProperNameType.getValue(np2, doc).equals(FeatureUtils.NPSemTypeEnum.PERSON);
    boolean done = false;
    for (int k = j - 1; k >= 0 && !done; k--) {
      Annotation np1 = nps[k];
      // Get the type of the first np
      RuleResolvers.NPType type1 = RuleResolvers.getNPtype(np1, doc, posessives);
      // int par1 = ParNum.getValue(np1, doc);
      // int parNum = Math.abs(par1 - par2);
      int sen1 = SentNum.getValue(np1, doc);
      int senNum = Math.abs(sen1 - sen2);
      boolean pron1 = type1.equals(RuleResolvers.NPType.PRONOUN);
      boolean pn1 = type1.equals(RuleResolvers.NPType.PROPER_NAME);
      boolean specPronoun1 = pron1 && FeatureUtils.getPronounPerson(doc.getAnnotText(np1)) != PersonPronounTypeEnum.THIRD;
      boolean person1 = pn1 && ProperNameType.getValue(np1, doc).equals(FeatureUtils.NPSemTypeEnum.PERSON);
      boolean includePair = false;
      if (pn1 && pn2 && ProperNameType.getValue(np1, doc).equals(ProperNameType.getValue(np2, doc))) {
        includePair = true;
        propNames++;
      }
      else if (person2 && specPronoun1) {
        includePair = true;
        is++;
      }
      else if (specPronoun1 && (specPronoun2 || person2)) {
        includePair = true;
        is++;
      }
      else if (specPronoun2 && (specPronoun1 || person1)) {
        includePair = true;
        is++;
      }
      else if (def2 && !pron1 && (senNum <= 6)) {
        includePair = true;
        def++;
      }
      else if (senNum <= 2) {
        includePair = true;
        regular++;
      }
      if (includePair) {
        pairs.add(new Annotation[] { np1, np2 });
      }
      else {
        if (!pn2 && !specPronoun2 && (!def2 || (senNum > 6))) {
          done = true;
        }
      }
    }

  }
  pairIter = pairs.iterator();
}

@Override
public boolean hasNext()
{
  return pairIter.hasNext();
}

@Override
public Annotation[] nextPair()
{
  return pairIter.next();
}
}
