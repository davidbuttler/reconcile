/*
 * An interface to the Stanford NER
 */

package reconcile.featureExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.Document;
import reconcile.general.Constants;
import reconcile.general.Utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.util.StringUtils;

public class NamedEntityStanford extends InternalAnnotator {

private static Pattern pWord = Pattern.compile("\\w");

List<CRFClassifier> finders;

private Map<String, String> mTypeMap = Maps.newHashMap();

private static CRFClassifier getClassifier(InputStream model)
    throws ClassCastException, IOException, ClassNotFoundException
{
  String[] args2 = {};
  Properties props = StringUtils.argsToProperties(args2);

  CRFClassifier classifier = new CRFClassifier(props);
  classifier.loadClassifier(model, props);
  return classifier;
}

public NamedEntityStanford() {
  // ////////
  String modelStr = Utils.getConfig().getString("StanfordTaggerModelNames");
  System.out.println("stanford NER model names: " + modelStr);
  String[] model_names = Utils.getConfig().getStringArray("StanfordTaggerModelNames");

  if (model_names.length == 0) {
    System.out.println("No models specified for Stanford Tagger; using default");
    model_names = new String[] { "ner-eng-ie.crf-3-all2008-distsim" };
  }

  finders = Lists.newArrayList();
  for (String modelName : model_names) {
    System.out.println("model name: " + modelName);
    try {
      InputStream res = this.getClass().getClassLoader().getResourceAsStream("Stanford/NER/" + modelName + ".ser.gz");
      if (res != null) {
        finders.add(getClassifier(new GZIPInputStream(res)));
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    catch (ClassCastException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  // set up mapping for type names
  String[] mapping = Utils.getConfig().getStringArray("semantic_type_mapping");
  for (int j = 0; j < mapping.length; j += 2) {
    mTypeMap.put(mapping[j].toUpperCase(), mapping[j + 1].toUpperCase());
  }

}

@Override
public void run(Document doc, String[] annSetNames)
{

  AnnotationSet namedEntities = new AnnotationSet(annSetNames[0]);

  // get the sentences from the input
  AnnotationSet sentSet = doc.getAnnotationSet(Constants.SENT);

  // Read in the text from the raw file
  String text = doc.getText();

  for (Annotation sent : sentSet) {
    String sentText = Utils.getAnnotText(sent, text);
    parseSentence(sentText, sent.getStartOffset(), namedEntities);
  }

  addResultSet(doc,namedEntities);
}

/**
 * @param sent
 * @param namedEntities
 * @param namedEntities_desc
 */
public void parseSentence(String sentText, int sentStart, AnnotationSet namedEntities)
{

  if (!acceptableSentence(sentText)) return;

  for (CRFClassifier findr : finders) {
    // Tag the sentence
    List<edu.stanford.nlp.util.Triple<String, Integer, Integer>> sentTags = findr.classifyToCharacterOffsets(sentText);

    // loop through named entity tags
    for (edu.stanford.nlp.util.Triple<String, Integer, Integer> tag : sentTags) {
      String entity_type = tag.first();

      int entity_start = tag.second() + sentStart;
      int entity_end = tag.third() + sentStart;

      entity_type = translateTypeName(entity_type);
      namedEntities.add(entity_start, entity_end, entity_type.toUpperCase());
    }
  }

}

/**
 * @param entity_type
 * @return
 */
private String translateTypeName(String entity_type)
{

  if (mTypeMap.containsKey(entity_type.toUpperCase())) {
    entity_type = mTypeMap.get(entity_type.toUpperCase());
  }
  if (entity_type.toLowerCase().startsWith("date")) {
    entity_type = "date";
  }
  if (entity_type.toLowerCase().equals("cardinal") || entity_type.toLowerCase().equals("ordinal")) {
    entity_type = "number";
  }

  return entity_type;
}

/**
 * A sentence is not acceptable to try to parse if it contains less than 5 letters
 * 
 * @param sentStr
 * @return
 */
private boolean acceptableSentence(String sentStr)
{
  int len = sentStr.length();
  String nonWord = pWord.matcher(sentStr).replaceAll("");
  int count = len - nonWord.length();
  if (count < 5) return false;
  return true;
}

}
