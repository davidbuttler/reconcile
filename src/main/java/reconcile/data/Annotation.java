/*
 * Annotation.java
 * 
 * ves; March 28, 2007
 * 
 * This implements the Annotation interface needed to handle single annotation
 */

package reconcile.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import reconcile.features.properties.Property;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;


/**
 * An implementation of summary.Annotation. Handles single annotations.
 * 
 */
public class Annotation
    implements Comparable<Annotation>, Serializable {

/**
   * 
   */
private static final long serialVersionUID = 1L;

/**
 * Debug flag
 */
// private static final boolean DEBUG = false;

/** The id. */
int id;

/** The type of the annotation */
String type;

/** The features */
Map<String, String> features;

/** Properties (used mostly for processing annotations) */
transient Map<Property, Object> properties;

/** The start offset */
int start;

/** The end offset */
int end;

/* A holder for the string content of the annotation span*/
String strContent = null;
/* A holder for the text content of the annotation span (cleaned up string)*/
String textContent = null;
/* A holder for the word content of the annotation span*/
String[] words = null;

/** The equivalent of a "null" annotation */
private static Annotation nullAnnot;

private Annotation() {
  properties = Maps.newHashMap();
}
/** Constructor. */
public Annotation(int id, int start, int end, String type) {
  this(id, start, end, type, new TreeMap<String, String>());
}

/** Constructor. */
public Annotation(int id, int start, int end, String type, Map<String, String> features) {
  this(id, start, end, type, features, new HashMap<Property, Object>());
}

public Annotation(int id, int start, int end, String type, Map<String, String> features,
    Map<Property, Object> properties) {
  this();
  this.id = id;
  this.start = start;
  this.end = end;
  this.type = type;
  this.features = features;
  if (properties != null) {
    this.properties = properties;
  }
} // AnnotationImpl

public Annotation copy()
{
  return new Annotation(id, start, end, type, features, properties);
}

/** The ID of the annotation. */
public int getId()
{
  return id;
} // getId()

/** Set the ID of the annotation. */
public void setId(int i)
{
  id = i;
} // setId()

/** The type of the annotation */
public String getType()
{
  return type;
} // getType()

/** The type of the annotation */
public void setType(String t)
{
  type = t;
} // setType()

/** The start offset. */
public int getStartOffset()
{
  return start;
} // getStartOffset()

/** The end offset. */
public int getEndOffset()
{
  return end;
} // getEndOffset()

public int getLength()
{
  return end - start;
}

/** Se the start offset. */
public void setStartOffset(int s)
{
  start = s;
} // setStartOffset()

/** Set the end offset. */
public void setEndOffset(int e)
{
  end = e;
} // setEndOffset()

/** The features */
public Map<String, String> getFeatures()
{
  return features;
}// getFeatures()

/** Output representation of the annotation */
@Override
public String toString()
{
  return "AnnotationImpl: id=" + id + "; type=" + type + "; features=" + features + "; start=" + start + "; end=" + end
      + System.getProperty("line.separator");
} // toString()

/* (non-Javadoc)
 * @see java.lang.Object#hashCode()
 */
@Override
public int hashCode()
{
  return Objects.hashCode(type, start, end);

}

/**
 * Returns true if two annotation are Equals. Two Annotation are equals if their offsets, types, id and features are the
 * same.
 */
@Override
public boolean equals(Object obj)
{
  if (obj == null) return false;

  Annotation other;

  if (obj instanceof Annotation) {
    other = (Annotation) obj;
  }
  else
    return false;

  // If their types are not equals then return false
  if ((type == null) ^ (other.getType() == null)) return false;

  if (type != null && (!type.equals(other.getType()))) return false;

  // If their start offset is not the same then return false
  if ((start < 0) ^ (other.getStartOffset() < 0)) return false;

  if (start >= 0) if (other.getStartOffset() != start) return false;

  // If their end offset is not the same then return false
  if ((end < 0) ^ (other.getEndOffset() < 0)) return false;

  if (end >= 0) if (other.getEndOffset() != end) return false;

  // If their featureMaps are not equals then return false
  if ((features == null) ^ (other.getFeatures() == null)) return false;

  if (features != null && (!features.equals(other.getFeatures()))) return false;

  if (id != other.id) return false;

  return true;
}// equals

/** Set the feature set. */
public void setFeatures(Map<String, String> features)
{
  this.features = features;
}

/** Set the property set. */
public void setProperties(Map<Property, Object> props)
{
  this.properties = props;
}

/** Get the property set. */
public Map<Property, Object> getProperties()
{
  return this.properties;
}

/** This method tells if <b>this</b> and annotation A overlap. */
public boolean overlaps(Annotation A)
{
  if (A == null) return false;

  return this.overlaps(A.getStartOffset(), A.getEndOffset());
}// overlaps

/** This method tells if <b>this</b> overlaps the designated span. */
public boolean overlaps(int start, int end)
{
  if (start < 0 || end < 0) return false;

  if (end <= this.getStartOffset()) return false;

  if (start >= this.getEndOffset()) return false;

  return true;
}// overlaps

/**
 * This method is used to determine if this annotation covers completely in span annotation A
 */
public boolean covers(Annotation A)
{
  if (A == null) return false;

  return this.covers(A.getStartOffset(), A.getEndOffset());
}// covers

/**
 * This method is used to determine if this annotation covers completely the designated span
 */
public boolean covers(int start, int end)
{
  if (start < 0 || end < 0) return false;

  if (this.getStartOffset() <= start && end <= this.getEndOffset()) return true;

  return false;
}// covers

/**
 * This method is used to determine if this annotation is covered completely the designated span
 */
public boolean covered(int start, int end)
{
  if (start < 0 || end < 0) return false;

  if (this.getStartOffset() >= start && end >= this.getEndOffset()) return true;

  return false;
}// covered

public boolean properCovers(Annotation A)
{
  if (A == null) return false;
  return this.properCovers(A.getStartOffset(), A.getEndOffset());
}

/*
 * This method is used to determine if this annotation properly covers the
 * span given.
 */
public boolean properCovers(int start, int end)
{
  if (start < 0 || end < 0) return false;

  if ((this.getStartOffset() < start && end <= this.getEndOffset())
      || (this.getStartOffset() <= start && end < this.getEndOffset())) return true;

  return false;
}

/**
 * The lenght of the annotation span.
 * 
 * @return the length of the span.
 */
public int spanSize()
{
  return end - start;
}

/**
 * Compares annotation offsets
 * 
 * @return 0 is the two annotations have the same span, -11 if <b>this</b> comes first (starts or ends before A), and -1
 *         if A comes first.
 */
public int compareSpan(Annotation A)
{
  if (A == null) throw new NullPointerException();

  if (this.getStartOffset() < A.getStartOffset()) return -1;

  if (this.getStartOffset() > A.getStartOffset()) return 1;

  // the start offsets are equal
  if (this.getEndOffset() < A.getEndOffset()) return -1;

  if (this.getEndOffset() > A.getEndOffset()) return 1;

  return 0;
}// compareSpan

/**
 * Compares annotation offsets
 * 
 * @return 0 is the two annotations have the same span, -11 if <b>this</b> comes first (starts or ends before A), and -1
 *         if A comes first.
 */
public int compareSpan(int startOffset, int endOffset)
{
  if (this.getStartOffset() < startOffset) return -1;

  if (this.getStartOffset() > startOffset) return 1;

  // the start offsets are equal
  if (this.getEndOffset() < endOffset) return -1;

  if (this.getEndOffset() > endOffset) return 1;

  return 0;
}// compareSpan

public int compareTo(Annotation o)
    throws ClassCastException
{

  if (o == null) throw new NullPointerException();
  int spanCompare = this.compareSpan(o);
  if (spanCompare != 0) return spanCompare;
  int typeCompare = type.compareTo(o.type);
  if (typeCompare != 0) return typeCompare;
  
  if (o.features.size() > features.size()) return -1;
  if (o.features.size() < features.size()) return 1;
  TreeSet<String> aKeys = new TreeSet<String>(o.features.keySet());
  TreeSet<String> mKeys = new TreeSet<String>(features.keySet());
  Iterator<String> ai = aKeys.iterator();
  Iterator<String> mi = mKeys.iterator();
  while (ai.hasNext()) {
    String aK = ai.next();
    String mK = mi.next();
    if (mK.compareTo(aK) != 0) return mK.compareTo(aK);
    String aV = o.features.get(aK);
    String mV = features.get(mK);
    if (aV == null && mV == null) {
      continue;
    }
    if (aV == null) return 1;
    if (mV == null) return -1;
    if (mV.compareTo(aV) != 0) return mV.compareTo(aV);

  }
  int idCompare = Integer.valueOf(id).compareTo(o.id);
  if (idCompare != 0) return idCompare;

  return 0;
}

public String getAttribute(String s)
{
  return features.get(s);
}

public void setAttribute(String n, String o)
{
  features.put(n, o);
}

public String removeAttribute(String n)
{
  return features.remove(n);
}

public Object getProperty(Property p)
{
  if (properties == null) return null;
  return properties.get(p);
}

public void setProperty(Property p, Object o)
{
  if (properties == null) {
    properties = new HashMap<Property, Object>();
  }
  properties.put(p, o);
}

/*
 * Return an equvalent of a "null" value for annotation
 */
public static Annotation getNullAnnot()
{
  if (nullAnnot == null) {
    nullAnnot = new Annotation(-1, -1, -1, "null", new HashMap<String, String>(0), null);
  }
  return nullAnnot;
}

} // class AnnotationImpl
