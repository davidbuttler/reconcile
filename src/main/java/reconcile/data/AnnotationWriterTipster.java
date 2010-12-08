/*
 * AnnotationWriterTipster.java
 * 
 * ves; May 13, 2005
 */

package reconcile.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * This is an implementation of the AnnotationWriter interface. Writes out annotations in the Tipster.
 * 
 */

public class AnnotationWriterTipster
    implements AnnotationWriter {

/** Write out an annotation set */
public void write(AnnotationSet anns, PrintWriter out)
{
  write(anns, out, null);
}

public void write(AnnotationSet anns, String dirName)
{
  String filename = dirName + "/" + anns.getName();
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

  // get all the annotation types
  for (String type : anns.getAllTypes()) {
    AnnotationSet curAnns = anns.get(type);
    // first, get all attributes that are present in this annotation set
    Set<String> attrSet = new HashSet<String>();
    for (Annotation an : curAnns) {
      attrSet.addAll(an.getFeatures().keySet());
    }
    String[] attributes = attrSet.toArray(new String[] {});
    int attrNum = attributes == null ? 0 : attributes.length;
    // output the heading
    out.print("(\"" + type + "\" (");
    for (int i = 0; i < attrNum; i++) {
      out.print(" \"" + attributes[i] + "\"");
    }
    out.print(") (");

    // now output each of the annotations
    // using a hack to sort the set by annotation ids
    for (Annotation an : curAnns) {
      out.println();
      out.print("(\"" + an.getId() + "\" <" + an.getStartOffset() + " " + an.getEndOffset() + ">");
      for (int i = 0; i < attrNum; i++) {
        out.print(" \"" + an.getFeatures().get(attributes[i]) + "\"");
      }
      out.print(")");
    }
    out.println("))");
  }
  out.flush();
  out.close();
}
}
