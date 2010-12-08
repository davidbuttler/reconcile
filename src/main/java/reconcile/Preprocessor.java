package reconcile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reconcile.data.Document;
import reconcile.featureExtractor.Annotator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class Preprocessor {

private Map<String, Annotator> mElements;
private SystemConfig config;
private boolean verbose = true;

public Preprocessor(SystemConfig systemConfig) {
  config = systemConfig;
}

public void setVerbose(boolean v)
{
  verbose = v;
}

public long preprocessingStepStart(String name, Document doc)
{
  long stTime = System.currentTimeMillis();
  System.out.println("Running " + name + " (" + doc.getDocumentId() + ")...");
  return stTime;
}

public long preprocessingStepEnd(String name, long stTime)
{
  long opTime = System.currentTimeMillis() - stTime;
  System.out.println(name + " completed in " + Long.toString(opTime / 1000) + " seconds.");
  return stTime;
}




public void preprocess(Document doc, String preprocessingElement, boolean overwrite)
{
  List<String> elNames = Lists.newArrayList(preprocessingElement);
  preprocess(doc, elNames, overwrite);
}

public void preprocess(Document doc, List<String> preprocessingElements, boolean overwrite)
{
  HashMap<String, String[]> elSetNames = config.getPreprocessingElSetNames();

  // Initialize the element
  Map<String, Annotator> elements = getElements();

  preprocessDoc(overwrite, preprocessingElements, elSetNames, elements, doc, 0);
}

public void preprocess(Document doc, boolean overwrite)
{
  List<String> elNames = config.getPreprocessingElements();
  HashMap<String, String[]> elSetNames = config.getPreprocessingElSetNames();

  // Initialize the element
  Map<String, Annotator> elements = getElements(elNames);

  preprocessDoc(overwrite, elNames, elSetNames, elements, doc, 0);
}

/**
 * @return
 */
private Map<String, Annotator> getElements(List<String> elNames)
{
  if (mElements == null) {
    mElements = Maps.newHashMap();
  }

  for (String el : elNames) {
    Annotator a = mElements.get(el);
    if (a == null) {
      a = Constructor.createInternalAnnotator(el);
      mElements.put(el, a);
    }
  }
  return mElements;
}

/**
 * @return
 */
private Map<String, Annotator> getElements()
{
  return getElements(config.getPreprocessingElements());
}


/**
 * @param args
 *          - an array of the directories containing the raw text
 */
public void preprocess(Iterable<Document> corpus, String annotator, boolean overwrite)
{
  ArrayList<String> elNames = Lists.newArrayList(annotator);
  HashMap<String, String[]> elSetNames = config.getPreprocessingElSetNames();

  // Initialize the element
  Map<String, Annotator> elements = getElements(elNames);

  int i = 0;
  for (Document doc : corpus) {
    preprocessDoc(overwrite, elNames, elSetNames, elements, doc, i++);
  }
}

/**
 * @param args
 *          - an array of the directories containing the raw text
 */
public void preprocess(Iterable<Document> corpus, List<String> elNames, boolean overwrite)
{
  HashMap<String, String[]> elSetNames = config.getPreprocessingElSetNames();

  // Initialize the element
  Map<String, Annotator> elements = getElements(elNames);

  int i = 0;
  for (Document doc : corpus) {
    preprocessDoc(overwrite, elNames, elSetNames, elements, doc, i++);
  }
}

/**
 * @param args
 *          - an array of the directories containing the raw text
 */
public void preprocess(Iterable<Document> corpus, boolean overwrite)
{
  ArrayList<String> elNames = config.getPreprocessingElements();
  HashMap<String, String[]> elSetNames = config.getPreprocessingElSetNames();

  // Initialize the element
  Map<String, Annotator> elements = getElements(elNames);

  int i = 0;
  for (Document doc : corpus) {
    preprocessDoc(overwrite, elNames, elSetNames, elements, doc, i++);
  }
}

private void preprocessDoc(boolean overwrite, List<String> elNames, HashMap<String, String[]> elSetNames,
    Map<String, Annotator> elements, Document doc, int i)
{
  long docStart = System.currentTimeMillis();
  if (verbose) {
    System.out.println("Preprocessing: " + doc.getDocumentId() + " document #" + (i + 1));
  }
  long opTime = 0;
  for (int j = 0; j < elNames.size(); j++) {
    String name = elNames.get(j);
    Annotator element = elements.get(name);
    if (verbose) {
      opTime = preprocessingStepStart(name, doc);
    }
    element.run(doc, elSetNames.get(name), overwrite);
    if (verbose) {
      preprocessingStepEnd(name, opTime);
    }
  }

  if (verbose) {
    long docEnd = System.currentTimeMillis();
    System.out.println("Finished: " + doc.getDocumentId() + " document #" + (i + 1) + " in "
        + Long.toString((docEnd - docStart) / 1000));
  }
}

}
