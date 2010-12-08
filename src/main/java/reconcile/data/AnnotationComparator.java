/*
 * AnnotationComparator.java
 * 
 * Dave Golland; May 6, 2007
 * 
 * This is a simple comparator that allows annnotations to be sorted by offset and ID
 */

package reconcile.data;

/**
 * An implementation of Comparator interface Compare two annotations by first the offsets then the ID
 * 
 */
public class AnnotationComparator
    extends OffsetComparator {

/**
   * 
   */
private static final long serialVersionUID = 1L;

@Override
public int compare(Annotation a1, Annotation a2)
{
  if (a1.equals(a2)) return 0;
  int superComp = super.compare(a1, a2);

  int result = (superComp != 0) ? superComp : (a1).id < (a2).id ? -1 : 1;
  return result;
}

} // class AnnotationComparator
