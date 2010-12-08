/*
 * AnnotationReaderTipster.java
 * 
 * ves; March 28, 2007
 */

package reconcile.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is an implementation of the AnnotationReader interface.<br>
 * Reads annotations saved in the Tipster format (used in the coref systems). <br>
 * The file format is a list (not a LISP list) of annotation list <br>
 * specifications. Each of these specifications are as follows:<br>
 * <code> <br>
 * (type attribute-name-list <br>
 *   ( <br>
 *    (annotation-id span-or-spans attr-val-1 attr-val-2 ...) <br>
 *    (annotation-id span-or-spans attr-val-1 attr-val-2 ...) <br>
 *  ...) </code><br>
 * where: <br>
 * type is a string <br>
 * attribute-name-list is a list of strings <br>
 * annotation-id is a string <br>
 * span-or-spans is a single span or a list of spans written in &lt; &gt; notation <br>
 * attr-val-? are attribute values which can be strings or object references, <br>
 * references are written in [ ] notation, defaults for references <br>
 * are taken from the document argument <br>
 * An example: <code><br>
 * ("token" ("pos") ( <br>
 * ("1" <0 5> "NP") <br>
 * ("2" <6 13> "VBD") <br>
 * ("3" <14 17> "DT") <br>
 * ("4" <18 23> "NN") <br>
 * ("5" <22 23> "."))) <br> 
 * <br>
 * ("name" ("name_type") ( <br>
 *  ("6" <0 5> "person"))) <br>
 * <br>
 * ("sentence" ("constits") ( <br>
 * ("7" <0 23> ([1] [2] [3] [4] [5])))<br>
 * </code>
 */

public class AnnotationReaderTipster
    implements AnnotationReader {

public AnnotationSet read(InputStream filename, String annSetName)
{
  return read(filename, annSetName, false);
}

/** Read in the annotations from file named filename */
public AnnotationSet read(InputStream filename, String annSetName, boolean newIds)
{
  final boolean DEBUG = false;
  String line, fileText;
  StringBuilder ftext = new StringBuilder();
  AnnotationSet result = new AnnotationSet(annSetName);

  try {
    BufferedReader input = new BufferedReader(new InputStreamReader(filename));
    while ((line = input.readLine()) != null) {
      // collect the text of the file but skip comments
      if (!line.matches("\\s*(#.*)?$")) {
        ftext.append(line);
      }
    }
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
  fileText = ftext.toString();

  // the code implements a DFA to parse the annotation file
  final int start = 0, error = -1, type = 1, attributeList = 2, list1 = 3; // , list2 = 4;
  final int annotation = 5, accept = 6;
  String typeName = "";
  ArrayList<String> attributes = new ArrayList<String>();
  Map<String, String> attrMap;
  int state = start;
  int index;
  String curAttr;
  String attr = "((\\((?:[^\\\")]|\"(?:[^\"\\\\]|\\\\\")*\")*\\)|(?:[^\\(\\)\"]|\"(?:[^\"\\\\]|\\\\\")*\")*)*)";
  Pattern annotPattern = Pattern.compile("\\s*\\(\"(\\w*)\"\\s*\\<\\s*(\\d+)\\s+(\\d+)\\s*\\>\\s*" + attr + "\\)\\s*");
  // Pattern annotPattern = Pattern.compile("\\s*\\(\\\"(\\w*)");
  if (DEBUG) {
    System.out.println("Using pattern " + annotPattern.pattern());
  }
  Pattern attrPattern = Pattern.compile("(\"([^\"]*)\"|\\([^\\)]*\\))");
  // First, remove the white space
  fileText = fileText.replaceAll("^\\s*", "");
  while (fileText != null && fileText.length() > 0) {
    if (DEBUG) {
      System.out.println(fileText);
    }
    switch (state) {
      case start:
        if (DEBUG) {
          System.out.println("In start state");
        }
        // the start state
        attributes = new ArrayList<String>();
        if (fileText.charAt(0) == '(') {
          fileText = fileText.substring(1);
          state = type;
        }
        else {
          state = error;
        }
        break;
      case type:
        if (DEBUG) {
          System.out.println("In type state");
        }
        // read the type of the annotation
        if (fileText.charAt(0) != '"') {
          state = error;
        }
        else {
          fileText = fileText.substring(1);

          index = fileText.indexOf('"');
          if (index < 0) {
            state = error;
          }
          else {
            typeName = fileText.substring(0, index);
            fileText = fileText.substring(index + 1);
            state = attributeList;
          }
        }
        if (DEBUG) {
          System.out.println("Parsed type: " + typeName);
        }
        break;
      case attributeList:
        if (DEBUG) {
          System.out.println("In attributeList state");
        }
        // read the attribute list
        if (fileText.charAt(0) == '(') {
          fileText = fileText.substring(1);
          state = list1;
        }
        else {
          state = error;
        }
        break;
      case list1:
        if (DEBUG) {
          System.out.println("In list1 state");
        }
        // read a list
        if (fileText.charAt(0) == ')') {
          // end of the list
          fileText = fileText.substring(1);
          state = annotation;
        }
        else if (fileText.charAt(0) == '"') {

          fileText = fileText.substring(1);
          index = fileText.indexOf('"');

          if (index < 0) {
            state = error;
          }
          else {
            curAttr = fileText.substring(0, index);
            fileText = fileText.substring(index + 1);

            if (DEBUG) {
              System.out.println("Parsed attribute " + curAttr);
            }

            attributes.add(curAttr);
          }

        }
        else {
          state = error;
        }

        break;
      case annotation:
        if (DEBUG) {
          System.out.println("In annotation state");
        }

        if (fileText.charAt(0) != ')') {
          if (fileText.charAt(0) == '(') {
            fileText = fileText.substring(1);
          }
          else {
            state = error;
          }
          Matcher annotMatcher = annotPattern.matcher(fileText);
          int counter = 1;
          while (state != error && fileText.charAt(0) != ')') {
            if (annotMatcher.lookingAt()) {
              int id = Integer.parseInt(annotMatcher.group(1));
              int startOffset = Integer.parseInt(annotMatcher.group(2));
              int endOffset = Integer.parseInt(annotMatcher.group(3));
              String attrString = annotMatcher.group(4);
              // now read the attributes from the string and put them in a FeatureMap
              attrMap = new TreeMap<String, String>();
              Matcher attrMatcher = attrPattern.matcher(attrString);

              for (int i = 0; i < attributes.size(); i++) {

                if (DEBUG) {
                  System.out.println("Matching " + attributes.get(i));
                }
                attrMatcher.find();
                String attributeValue;

                try {
                  attributeValue = attrMatcher.group(1);
                }
                catch (Exception e) {
                  throw new RuntimeException(e + " Text:" + attrString);
                }

                attributeValue = attributeValue.substring(1, attributeValue.length() - 1);
                attrMap.put(attributes.get(i), attributeValue);
              }

              if (DEBUG) {
                System.out.println("Parsed annotation with id: " + id);
              }
              if (newIds) {
                result.add(counter++, startOffset, endOffset, typeName, attrMap);
              }
              else {
                result.add(id, startOffset, endOffset, typeName, attrMap);
              }
              fileText = annotMatcher.replaceFirst("");
              annotMatcher.reset(fileText);
            }
            else {
              state = error;
            }
          }
        }
        if (state != error && fileText.charAt(0) == ')') {
          fileText = fileText.substring(1);
          state = accept;
        }
        else {
          state = error;
        }
        break;
      case accept:
        if (fileText.charAt(0) == ')') {
          fileText = fileText.substring(1);
          if (DEBUG) {
            System.out.println("Done with annotation type " + typeName);
          }
          state = start;
        }
        else {
          state = error;
        }
        break;
      case error:
      default:
        System.err.println("Error parsing file " + filename);
        fileText = null;
    }
    if (fileText != null) {
      fileText = fileText.replaceAll("^\\s*", "");
    }
  }

  return result;
}
}
