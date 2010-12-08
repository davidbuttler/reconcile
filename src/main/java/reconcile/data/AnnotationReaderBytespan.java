/*
 * AnnotationReaderBytespan.java
 * 
 * ves; August 2, 2004
 */

package reconcile.data;

import gov.llnl.text.util.InputStreamLineIterable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reconcile.general.Constants;


/**
 * This is an implementation of the AnnotationReader interface. Reads annotations saved in the Bytespan format.
 * 
 */

public class AnnotationReaderBytespan
    implements AnnotationReader {

private static final Pattern pNameValue = Pattern.compile("\\s*([^\"]+)=(\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")");
private static final Pattern pComment = Pattern.compile("^\\s*(#.*)?$");
private static final Pattern pTab = Pattern.compile("\t");
private static final Pattern pComma = Pattern.compile(",");

public final boolean DEBUG = Constants.DEBUG;


/** Read in the annotations from file */
public AnnotationSet read(InputStream in, String annSetName)
{
  AnnotationSet result = new AnnotationSet(annSetName);
  if (in == null) {
    System.out.println("input stream is null for: " + annSetName);
    return result;
  }

  try {
    for (String line : InputStreamLineIterable.iterateOverCommentedLines(in)) {
      // if we encounter an error on one line, skip forward to the next
      try {
        if (!pComment.matcher(line).matches()) {
          // System.err.println(line);
          Map<String, String> features = new HashMap<String, String>();
          String[] words = pTab.split(line, 5);
          int id;

          try {
            id = Integer.parseInt(words[0]);
          }
          catch (NumberFormatException e) {
            id = -1;
          }

          String[] offsets = pComma.split(words[1]);
          int start = Integer.parseInt(offsets[0]);
          int end = Integer.parseInt(offsets[1]);

          // I'm not sure what to use the data type for...
          // String dataType = words[2];
          String type = words[3];

          if (words.length > 4 && words[4] != null && words[4].trim().length() > 0) {

            Matcher match = pNameValue.matcher(words[4].trim());

            while (match.find()) {
              String featureName = match.group(1);
              String featureValue = match.group(2);
              // remove quotes
              featureValue = featureValue.substring(1, featureValue.length() - 1);
              features.put(featureName, featureValue);
            }
          }

          if (id >= 0 && result.get(id) == null) {
            result.add(id, start, end, type, features);
          }
          else {
            result.add(start, end, type, features);
          }

          if (DEBUG) {
            System.out.println("Read annotation " + type + "--" + id + ":" + start + "-" + end);
          }
        }
      }
      catch (NumberFormatException e) {
        System.out.println("error in inputstream. line: " + line);
        e.printStackTrace();
      }
      catch (ArrayIndexOutOfBoundsException e) {
        System.out.println("error in inputstream. line: " + line);
        e.printStackTrace();
      }
    }
  }
  catch (IOException e) {
    e.printStackTrace();
    throw new RuntimeException(e);
  }

  return result;
}

}
