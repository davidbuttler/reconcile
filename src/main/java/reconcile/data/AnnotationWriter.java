/*
 * AnnotationWriter.java
 * 
 * ves; March 28, 2007
 */

package reconcile.data;

import java.io.PrintWriter;

/**
 * This interface is used to write out annotations in different formats.
 * 
 */

public interface AnnotationWriter {

/** Write out annotations ans using the stream out */
public void write(AnnotationSet anns, PrintWriter out);

/** Write out annotations and a comment */
public void write(AnnotationSet anns, PrintWriter out, String comment);

/** Write out annotations ans using the stream out */
public void write(AnnotationSet anns, String dirName);

/** Write out annotations and a comment */
public void write(AnnotationSet anns, String dirName, String comment);

}
