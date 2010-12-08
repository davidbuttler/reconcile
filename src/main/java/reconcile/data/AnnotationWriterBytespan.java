/*
 * AnnotationWriterBytespan.java
 *
 * ves; March 28, 2007
 */

package reconcile.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * This is an implementation of the AnnotationWriter interface. Writes out annotations in the bytespan format.
 *
 */

public class AnnotationWriterBytespan
    implements AnnotationWriter {

/** Write out an annotation set */
public void write(AnnotationSet anns, PrintWriter out)
{
  write(anns, out, null);
}

public void write(AnnotationSet anns, String dirName)
{
  String filename = dirName + "/" + anns.getName();
  // System.out.println("Printing out annotation set "+filename);
  try {
    PrintWriter out = new PrintWriter(new File(filename));
    write(anns, out, null);
  }
  catch (FileNotFoundException fnfe) {
    throw new RuntimeException(fnfe);
  }
}

public void write(AnnotationSet anns, String dirName, String comment)
{
  String filename = dirName + "/" + anns.getName();
  try {
    PrintWriter out = new PrintWriter(new File(filename));
    write(anns, out, comment);
  }
  catch (FileNotFoundException fnfe) {
    throw new RuntimeException(fnfe);
  }
}

/** Write out an annotation set and a comment */
public void write(AnnotationSet anns, PrintWriter out, String comment)
{
  if (comment != null) {
    out.println(comment);
  }
  for (Annotation curr : anns) {
    out.print(curr.getId() + "\t" + curr.getStartOffset() + "," + curr.getEndOffset() + "\tstring\t" + curr.getType()
        + "\t");
    Map<String, String> features = curr.getFeatures();
    if (features != null) {
      for (String currFeat : features.keySet()) {
        String featVal = features.get(currFeat);
        if (featVal == null) {
          continue;
        }
        out.print(currFeat + "=\"" + featVal.replaceAll("\\n", " ") + "\" ");
      }
    }
    out.println();
  }
  out.flush();
  out.close();
}
}
