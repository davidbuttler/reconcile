/*
 * OffsetComparator.java
 * 
 * ves; July 1, 2005
 * 
 * This is a simple comparator that allows annnotations to be sorted by offset
 */

package reconcile.data;

import java.io.Serializable;
import java.util.Comparator;

/**
 * An implementation of Comparator interface Compare two annotations by the offsets
 * 
 */

public class OffsetComparatorNestedLast
    implements Comparator<Annotation>, Serializable {

/**
   * 
   */
private static final long serialVersionUID = 1L;

public int compare(Annotation a1, Annotation a2)
{
  int start1 = (a1).getStartOffset();
  int start2 = (a2).getStartOffset();
  if (start1 < start2) return -1;
  if (start2 < start1) return +1;
  int end1 = (a1).getEndOffset();
  int end2 = (a2).getEndOffset();
  if (end1 < end2) return +1;
  if (end2 < end1) return -1;
  int id1 = (a1).getId();
  int id2 = (a2).getId();
  return id1 < id2 ? -1 : id1 > id2 ? 1 : 0;
}

} // class AnnotationImpl
