/*
 * AnnotationReader.java
 * 
 * ves; March 28, 2007
 */

package reconcile.data;

import java.io.InputStream;

/**
 * This interface is used to read in annotations from annotation files in different formats.
 * 
 */

public interface AnnotationReader {

/** Read in annotations and return them in a set named annSetName */
public AnnotationSet read(InputStream file, String annSetName);
}
