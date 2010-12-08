/*
 * AnnotationSet.java
 * 
 * ves; July 28, 2004
 */

package reconcile.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import reconcile.features.properties.Property;


/**
 * AnnotationSet is used to store and annotations. Annotations are defined in term of their type, offset, attributes and
 * id. The annotation ids only have meaning within the annotation set. Thus, the annotation ids have to be unique within
 * the annotation set, which means that the id of an annotation might be changed when it is added to the annotation set.
 * 
 * The name annotation set is somewhat misleading because when a new annotation is added to the set, it is duplicated if
 * an "equal" annotation is already in the set.
 * 
 */

public class AnnotationSet
    implements Collection<Annotation>, Serializable {

/**
   * 
   */
private static final long serialVersionUID = 1L;

/** Debug flag */
public boolean DEBUG = false;

/** The name of this set */
private String name = null;

/** The Hash map that will keep the actual annotations */
private HashMap<Integer, Annotation> annotations;

private TreeSet<Annotation> orderedAnnotations;

/** Keeps track of the annotation ids */
private int nextAnnotationId = 0;

/** Keeps track of all annotation types that are in the set */
private HashMap<String, Integer> types;

private boolean ALLOW_CROSSING_ANNOTS = true;


/** Constructor from name. */
public AnnotationSet(String name) {
  this.name = name;
  annotations = new HashMap<Integer, Annotation>();

  orderedAnnotations = new TreeSet<Annotation>(new AnnotationComparator());

  types = new HashMap<String, Integer>();
} // construction from name

/** Constructor from name. */
public AnnotationSet(String name, boolean allowCrossingAnnots) {
  this(name);
  ALLOW_CROSSING_ANNOTS = allowCrossingAnnots;
} // construction from name

/** Construction from Collection (which must be an AnnotationSet) */

public AnnotationSet(String name, Collection<Annotation> c) {
  this(name);
  addAll(c);
} // construction from collection

/** Remove an element from this set. */
public boolean remove(Object o)
    throws ClassCastException
{

  Annotation a = (Annotation) o;
  if (!orderedAnnotations.contains(a)) return false;
  // since IDs are not part of equality, we have to find the id that matches the given annotation
  Annotation myA = null;
  for (Annotation ia : orderedAnnotations) {
    if (ia.equals(a)) {
      myA = ia;
      break;
    }
  }

  if (!orderedAnnotations.remove(myA)) return false;
  annotations.remove(myA.getId());
  String type = myA.getType();
  removeType(type);
  return true;
} // remove(o)

private void removeType(String type)
{
  Integer typeCount = types.get(type);
  if (typeCount != null) {
    if (typeCount.equals(1)) {
      types.remove(type);
    }
    else {
      types.put(type, typeCount - 1);
    }
  }
}

/** The size of this set */
public int size()
{
  return annotations.size();
}

/** Find annotations by id */
public Annotation get(int id)
{
  return annotations.get(id);
} // get(id)

/** Get all annotations */
public AnnotationSet get()
{
  AnnotationSet resultSet = new AnnotationSet(this.getName());
  resultSet.addAllKeepIDs(orderedAnnotations);
//   if (resultSet.isEmpty()) return null;
  return resultSet;
} // get()

/** Select annotations by type */
public AnnotationSet get(String type)
{
  if (type == null) return this.get();
  AnnotationSet resultSet = new AnnotationSet(this.getName());
  if (isEmpty()) return null;
  Iterator<Annotation> annIterator = iterator();
  while (annIterator.hasNext()) {
    Annotation curr = annIterator.next();
    if (type.equals(curr.getType())) {
      resultSet.addRef(curr);
    }
  }
  if (resultSet.isEmpty()) return null;
  return resultSet;
} // get()

/** Select annotations by type */
public AnnotationSet get(String[] types)
{
  if (types == null) return this.get();
  AnnotationSet resultSet = new AnnotationSet(this.getName());
  if (isEmpty()) return null;
  for (String type : types) {
    Iterator<Annotation> annIterator = iterator();
    while (annIterator.hasNext()) {
      Annotation curr = annIterator.next();
      if (type.equals(curr.getType())) {
        resultSet.addRef(curr);
      }
    }
  }
  if (resultSet.isEmpty()) return null;
  return resultSet;
} // get()

/**
 * Select annotations by offset. Returns all the annotations that overlap the specified offset.
 */
public AnnotationSet getOverlapping(int start, int end)
{
  AnnotationSet resultSet = new AnnotationSet(this.getName());
  Iterator<Annotation> annotsIter = iterator();
  while (annotsIter.hasNext()) {
    Annotation currentAnnot = annotsIter.next();
    if (currentAnnot.overlaps(start, end)) {
      resultSet.addRef(currentAnnot);
    }
  }

  return resultSet;
}// getOverlapping(start, end)

public AnnotationSet getOverlapping(Annotation an)
{
  return getOverlapping(an.getStartOffset(), an.getEndOffset());
}

public Annotation getFirst()
{
  if (orderedAnnotations == null || orderedAnnotations.isEmpty()) return null;
  return orderedAnnotations.first();
}

public Annotation getLast()
{
  if (orderedAnnotations == null || orderedAnnotations.isEmpty()) return null;
  return orderedAnnotations.last();
}

public AnnotationSet getContained(Annotation an)
{
  return getContained(an.getStartOffset(), an.getEndOffset());
}

public List<Annotation> getOrderedAnnots()
{
  return new ArrayList<Annotation>(orderedAnnotations);
}

public Annotation[] getOrderedAnnots(Comparator<Annotation> c)
{
  TreeSet<Annotation> res = new TreeSet<Annotation>(c);
  for (Annotation a : this) {
    res.add(a);
  }
  return res.toArray(new Annotation[0]);
}

/**
 * Select annotations by offset that start at a position between the start and end before the end offset
 */
public AnnotationSet getContained(int start, int end)
{
  // the result will include all the annotations that both:
  // start at a position between the start and end before the end offsets
  if (start > end) return null;
  Annotation lowerBound = new Annotation(Integer.MIN_VALUE, start, start, "", null, null);
  Annotation upperBound = new Annotation(Integer.MAX_VALUE, end, end, "", null, null);
  SortedSet<Annotation> subset = orderedAnnotations.subSet(lowerBound, upperBound);
  // System.err.println("Retrieved "+subset);
  AnnotationSet resultSet = new AnnotationSet(getName());
  for (Annotation annot : subset) {
    if (annot.getEndOffset() <= end) {
      resultSet.addRef(annot);
    }
  }

  return resultSet;

}// getContained(start, end)

/*
 * Returns an annotation set that contains all annotations that exactly match the span
 * of the given annotation.
 */
public AnnotationSet getDuplicates(int start, int end)
{
  // the result will include all the annotations that both:
  // start at a position between the start and end before the end offsets
  if (start > end) {
    System.out.println("start=" + start + ">end=" + end);
    return null;
  }
  Annotation lowerBound = new Annotation(Integer.MIN_VALUE, start, start, "", null, null);
  Annotation upperBound = new Annotation(Integer.MAX_VALUE, end, end, "", null, null);
  SortedSet<Annotation> subset = orderedAnnotations.subSet(lowerBound, upperBound);
  AnnotationSet resultSet = new AnnotationSet(getName());
  for (Annotation annot : subset) {
    if (annot.getEndOffset() == end) {
      resultSet.addRef(annot);
    }
  }
  return resultSet;
}

public AnnotationSet getDuplicates(Annotation an)
{
  return getDuplicates(an.getStartOffset(), an.getEndOffset());
}

/** Add an annotation */
public int add(int start, int end, String type)
{
  return add(start, end, type, new TreeMap<String, String>());
}

/** Add an annotation */
public int add(int start, int end, String type, Map<String, String> features)
{
  // the id of the new annotation
  int id = getNextAnnotationId();

  // construct an annotation
  add(id, start, end, type, features);
  return id;
} // add(int, int, String, Map)

public int add(int id, int start, int end, String type)
{
  return add(id, start, end, type, new TreeMap<String, String>(), null);
}
public int add(int id, int start, int end, String type, Map<String, String> features)
{
  return add(id, start, end, type, features, null);
}

/** Add an annotation */
public int add(int id, int start, int end, String type, Map<String, String> features, Map<Property, Object> properties)
{
  // construct an annotation

  Annotation a = new Annotation(id, start, end, type, features, properties);

  Annotation an;
  if (!ALLOW_CROSSING_ANNOTS && (an = getCrossing(a)) != null)
    throw new RuntimeException("Trying to add crossing annotation: " + a + " and: " + an);
  addRef(a);
  return id;
} // add(int, int, String, Map)

/** Add an existing annotation. Returns true when the set is modified. */
public boolean add(Annotation A)
    throws ClassCastException
{
  Annotation an;
  if (!ALLOW_CROSSING_ANNOTS && (an = getCrossing(A)) != null)
    throw new RuntimeException("Trying to add crossing annotation: " + A + " and: " + an);
  if (annotations.containsKey(A.getId())) {
    A = new Annotation(getNextAnnotationId(), A.getStartOffset(), A.getEndOffset(), A.getType(), A.getFeatures(), A
        .getProperties());
  }
  else {
    A = A.copy();
  }
  if (annotations.containsKey(A.getId())) {
    System.out.println("Duplicate");
  }
  boolean added = orderedAnnotations.add(A);
  if (added) {
    annotations.put(A.getId(), A);
    addType(A);
  }
  else {
    System.out.println("Not added " + A);
  }
  return added;
} // add(A)

/**
 * Add an existing annotation and keep the id. Returns true when the set is modified.
 * 
 * Warning: This method will overwrite an existing annotation which might have the id of the annotation that is being
 * added.
 * 
 * */
public boolean addKeepID(Annotation A)
    throws ClassCastException
{
  Annotation an;
  if (!ALLOW_CROSSING_ANNOTS && (an = getCrossing(A)) != null)
    throw new RuntimeException("Trying to add crossing annotation: " + A + " and: " + an);
  if (annotations.containsKey(A.getId())) {
    System.out.println("Duplicate");
  }
  A = A.copy();
  if (annotations.containsKey(A.getId())) {
    orderedAnnotations.remove(annotations.get(A.getId()));
  }
  boolean added = orderedAnnotations.add(A);
  if (added) {
    annotations.put(A.getId(), A);
    addType(A);
  }
  else {
    System.out.println("Not added " + A);
  }

  return added;
} // add(A)

/**
 * Add an existing annotation "by reference". Returns true when the set is modified.
 * 
 * Warning: This method will overwrite an existing annotation which might have the id of the annotation that is being
 * added.
 * 
 * */
public boolean addRef(Annotation A)
    throws ClassCastException
{
  Annotation an;
  if (!ALLOW_CROSSING_ANNOTS && (an = getCrossing(A)) != null)
    throw new RuntimeException("Trying to add crossing annotation: " + A + " and: " + an);
  if (annotations.containsKey(A.getId())) {
    System.out.println("Duplicate");
  }
  if (annotations.containsKey(A.getId())) {
    orderedAnnotations.remove(annotations.get(A.getId()));
  }
  boolean added = orderedAnnotations.add(A);
  if (added) {
    annotations.put(A.getId(), A);
    addType(A);
  }
  else {
    System.out.println("Not added " + A);
  }

  return added;
} // add(A)

/**
 * Adds multiple annotations to this set in one go. All the objects in the provided collection should be of
 * {@link summary.Annotation} type, otherwise a ClassCastException will be thrown. The provided annotations will be used
 * to create new annotations using the appropriate add() methods from this set. The new annotations will have different
 * IDs from the old ones (which is required in order to preserve the uniqueness of IDs inside an annotation set).
 * 
 * @param c
 *          a collection of annotations
 * @return <tt>true</tt> if the set has been modified as a result of this call.
 */

public boolean addAll(Collection<? extends Annotation> c)
{

  boolean changed = false;
  for (Annotation a : c) {
    add(a);
    changed = true;
  }

  return changed;
}

/**
 * Adds multiple annotations to this set in one go. All the objects in the provided collection should be of
 * {@link summary.Annotation} type, otherwise a ClassCastException will be thrown. This method does not create copies of
 * the annotations like addAll() does but simply adds the new annotations to the set. It is intended to be used solely
 * by annotation sets in order to construct the results for various get(...) methods.
 * 
 * @param c
 *          a collection of annotations
 * @return <tt>true</tt> if the set has been modified as a result of this call.
 */
protected boolean addAllKeepIDs(Collection<Annotation> c)
{
  boolean changed = false;

  for (Annotation a : c) {
    changed |= addKeepID(a);
  }

  return changed;
}

/** Generates and returns the next annotation id */
private int getNextAnnotationId()
{
  while (get(++nextAnnotationId) != null) {
  }
  // if(this.name.equals("basenp"))
  // System.out.println(" Id "+nextAnnotationId);
  return nextAnnotationId;
}

/** Get the name of this set. */
public String getName()
{
  return name;
}

/** Set the name of the annotation set. **/
public void setName(String n)
{
  name = n;
}

/**
 * Get a set of java.lang.String objects representing all the annotation types present in this annotation set.
 */

public Set<String> getAllTypes()
{
  return types.keySet();
}

/** Returns an iterator for the annotations */
public Iterator<Annotation> iterator()
{
  return orderedAnnotations.iterator();
}

/** String representation of the set */
@Override
public String toString()
{
  // annotsById
  String aBI = orderedAnnotations.toString();

  return "AnnotationSetImpl: " + "name=" + name + "; annotsById=" + aBI + "; ";

} // toString()

/** Adds the type of the annotation to the list of annotation types */
private void addType(Annotation A)
{
  String type = A.getType();
  if (!types.containsKey(type)) {
    types.put(type, 1);
  }
  else {
    types.put(type, (types.get(type)).intValue() + 1);
  }
}

public boolean isEmpty()
{
  return annotations.isEmpty();
}

public Annotation[] toArray()
{
  Annotation[] result = new Annotation[0];
  result = orderedAnnotations.toArray(result);
  return result;
}

public void clear()
{
  annotations.clear();
  orderedAnnotations.clear();
  types.clear();
}

public boolean contains(Object arg0)
{
  return orderedAnnotations.contains(arg0);
}

public boolean containsSpan(Annotation a)
{
  return containsSpan(a.getStartOffset(), a.getEndOffset());
}

public boolean containsSpan(int start, int end)
{

  AnnotationSet ans = this.getOverlapping(start, end);

  for (Annotation an : ans) {
    if (an.getStartOffset() == start && an.getEndOffset() == end) return true;
  }

  return false;
}

public Annotation getCrossing(Annotation a)
{
  int start1 = a.getStartOffset();
  int end1 = a.getEndOffset();
  AnnotationSet ans = this.getOverlapping(a);

  for (Annotation an : ans) {
    int start2 = an.getStartOffset();
    int end2 = an.getEndOffset();
    if (start1 < start2 && end1 > start2 && end1 < end2) return an;
    if (start1 > start2 && start1 < end2 && end1 > end2) return an;
  }

  return null;
}

public Annotation checkForCrossingAnnotations()
{
  Annotation result = null;
  for (Annotation a : get()) {
    if ((result = getCrossing(a)) != null) return result;
  }
  return null;
}

public void checkForCrossingWordBoundaries(Document doc)
{
  for (Annotation a : get()) {
    if (a.getStartOffset() != 0 && doc.getAnnotString(a.getStartOffset() - 1, a.getStartOffset()).matches("\\w")) {
      System.out.println("NOT ALLIGNED: " + doc.getAnnotText(a));
      System.out.println("ANNOTATION: " + a);
    }
    else if (a.getEndOffset() != doc.getText().length()
        && doc.getAnnotString(a.getEndOffset(), a.getEndOffset() + 1).matches("\\w")) {
      System.out.println("NOT ALLIGNED: " + doc.getAnnotText(a));
      System.out.println("ANNOTATION: " + a);
    }
  }
}

public boolean coversSpan(int start, int end)
{
  AnnotationSet ans = this.getOverlapping(start, end);

  for (Annotation an : ans) {
    if (an.covers(start, end)) return true;
  }

  return false;
}

public boolean coversSpan(Annotation a)
{
  return coversSpan(a.getStartOffset(), a.getEndOffset());
}

public boolean containsAll(Collection<?> arg0)
{
  return orderedAnnotations.containsAll(arg0);
}

public boolean removeAll(Collection<?> arg0)
{
  throw new UnsupportedOperationException("Remove all not implemented");
}

public boolean retainAll(Collection<?> arg0)
{
  throw new UnsupportedOperationException("Remove all not implemented");
}

public <T> T[] toArray(T[] arg0)
{
  return orderedAnnotations.toArray(arg0);
}

} // AnnotationSetImpl

