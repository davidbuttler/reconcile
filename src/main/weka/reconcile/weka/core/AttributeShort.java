/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Attribute.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package reconcile.weka.core;

import java.io.IOException;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

/** 
 * Class for handling an attribute. Once an attribute has been created,
 * it can't be changed. <p>
 *
 * Three attribute types are supported:
 * <ul>
 *    <li> numeric: <ul>
 *         This type of attribute represents a floating-point number.
 *    </ul>
 *    <li> nominal: <ul>
 *         This type of attribute represents a fixed set of nominal values.
 *    </ul>
 *    <li> string: <ul>
 *         This type of attribute represents a dynamically expanding set of
 *         nominal values. String attributes are not used by the learning
 *         schemes in Weka. They can be used, for example,  to store an 
 *         identifier with each instance in a dataset.
 *    </ul>
 * </ul>
 * Typical usage (code from the main() method of this class): <p>
 *
 * <code>
 * ... <br>
 *
 * // Create numeric attributes "length" and "weight" <br>
 * Attribute length = new Attribute("length"); <br>
 * Attribute weight = new Attribute("weight"); <br><br>
 * 
 * // Create vector to hold nominal values "first", "second", "third" <br>
 * FastVector my_nominal_values = new FastVector(3); <br>
 * my_nominal_values.addElement("first"); <br>
 * my_nominal_values.addElement("second"); <br>
 * my_nominal_values.addElement("third"); <br><br>
 *
 * // Create nominal attribute "position" <br>
 * Attribute position = new Attribute("position", my_nominal_values);<br>
 *
 * ... <br>
 * </code><p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
/**
 * @author ves
 *
 */
public class AttributeShort implements Copyable, Serializable {

  /** Constant set for numeric attributes. */
  public static final int NUMERIC = 0;

  /** Constant set for nominal attributes. */
  public static final int NOMINAL = 1;

  /** Constant set for attributes with string values. */
  public static final int STRING = 2;

  /** Constant set for attributes with date values. */
  public static final int DATE = 3;

  /** Constant set for symbolic attributes. */
  public static final int ORDERING_SYMBOLIC = 0;

  /** Constant set for ordered attributes. */
  public static final int ORDERING_ORDERED  = 1;

  /** Constant set for modulo-ordered attributes. */
  public static final int ORDERING_MODULO   = 2;

  /** The keyword used to denote the start of an arff attribute declaration */
  static String ARFF_ATTRIBUTE = "@attribute";

  /** A keyword used to denote a numeric attribute */
  static String ARFF_ATTRIBUTE_INTEGER = "integer";

  /** A keyword used to denote a numeric attribute */
  static String ARFF_ATTRIBUTE_REAL = "real";

  /** A keyword used to denote a numeric attribute */
  static String ARFF_ATTRIBUTE_NUMERIC = "numeric";

  /** The keyword used to denote a string attribute */
  static String ARFF_ATTRIBUTE_STRING = "string";

  /** The keyword used to denote a date attribute */
  static String ARFF_ATTRIBUTE_DATE = "date";

  /** Strings longer than this will be stored compressed. */
  private static final int STRING_COMPRESS_THRESHOLD = 200;

  /** The attribute's name. */
  private /*@ spec_public non_null @*/ String m_Name;

  /** The attribute's type. */
  private /*@ spec_public @*/ int m_Type;
  /*@ invariant m_Type == NUMERIC || 
                m_Type == DATE || 
                m_Type == STRING || 
                m_Type == NOMINAL;
  */

  /** The attribute's values (if nominal or string). */
  private /*@ spec_public @*/ FastVector m_Values;

  /** Mapping of values to indices (if nominal or string). */
  private Hashtable m_Hashtable;
  
  /** A mapping that is used in the reading phase for double attributes*/
  private Hashtable m_TempValues;
  
  private Hashtable m_TempOrigValues;
  
  /** Used to save correspondances for binarized nominal and string attributes*/
  protected ArrayList m_BinaryAttributeMap = null;

  /** For numeric attributes -- keep the different values sorted. */
  private Hashtable m_NumericValues;
  private Hashtable m_OriginalValues;

  /** Date format specification for date attributes */
  private SimpleDateFormat m_DateFormat;

  /** The attribute's index. */
  private /*@ spec_public @*/ int m_Index;

  /** The attribute's metadata. */
  private ProtectedProperties m_Metadata;

  /** The attribute's ordering. */
  private int m_Ordering;

  /** Whether the attribute is regular. */
  private boolean m_IsRegular;

  /** Whether the attribute is averagable. */
  private boolean m_IsAveragable;

  /** Whether the attribute has a zeropoint. */
  private boolean m_HasZeropoint;

  /** The attribute's weight. */
  private double m_Weight;

  /** The attribute's lower numeric bound. */
  private double m_LowerBound;

  /** Whether the lower bound is open. */
  private boolean m_LowerBoundIsOpen;

  /** The attribute's upper numeric bound. */
  private double m_UpperBound;

  /** Whether the upper bound is open */
  private boolean m_UpperBoundIsOpen;

  /** The constant used to normalize the values of the attribute **/
  public double m_NormalizingConstant;
  
  /** Direct vs. indirect attribute **/
  private boolean m_Direct = false;
  /**
   * Constructor for a numeric attribute.
   *
   * @param attributeName the name for the attribute
   */
  //@ requires attributeName != null;
  //@ ensures  m_Name == attributeName;
  public AttributeShort(String attributeName) {

    this(attributeName, new ProtectedProperties(new Properties()));
  }

  /**
   * Constructor for a numeric attribute, where metadata is supplied.
   *
   * @param attributeName the name for the attribute
   * @param metadata the attribute's properties
   */
  //@ requires attributeName != null;
  //@ requires metadata != null;
  //@ ensures  m_Name == attributeName;
  public AttributeShort(String attributeName, ProtectedProperties metadata) {

    m_Name = attributeName;
    m_Index = -1;
    m_Values = null;
    m_Hashtable = null;
    m_TempValues = null;
    m_Type = NUMERIC;
    setMetadata(metadata);
  }

  /**
   * Constructor for a date attribute.
   *
   * @param attributeName the name for the attribute
   * @param dateFormat a string suitable for use with
   * SimpleDateFormatter for parsing dates.
   */
  //@ requires attributeName != null;
  //@ requires dateFormat != null;
  //@ ensures  m_Name == attributeName;
  public AttributeShort(String attributeName, String dateFormat) {

    this(attributeName, dateFormat,
	 new ProtectedProperties(new Properties()));
  }

  /**
   * Constructor for a date attribute, where metadata is supplied.
   *
   * @param attributeName the name for the attribute
   * @param dateFormat a string suitable for use with
   * SimpleDateFormatter for parsing dates.
   * @param metadata the attribute's properties
   */
  //@ requires attributeName != null;
  //@ requires dateFormat != null;
  //@ requires metadata != null;
  //@ ensures  m_Name == attributeName;
  public AttributeShort(String attributeName, String dateFormat,
		   ProtectedProperties metadata) {

    m_Name = attributeName;
    m_Index = -1;
    m_Values = null;
    m_Hashtable = null;
    m_TempValues = null;
    m_Type = DATE;
    if (dateFormat != null) {
      m_DateFormat = new SimpleDateFormat(dateFormat);
    } else {
      m_DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }
    m_DateFormat.setLenient(false);
    setMetadata(metadata);
  }

  /**
   * Constructor for nominal attributes and string attributes.
   * If a null vector of attribute values is passed to the method,
   * the attribute is assumed to be a string.
   *
   * @param attributeName the name for the attribute
   * @param attributeValues a vector of strings denoting the 
   * attribute values. Null if the attribute is a string attribute.
   */
  //@ requires attributeName != null;
  //@ ensures  m_Name == attributeName;
  public AttributeShort(String attributeName,FastVector attributeValues) {

    this(attributeName, attributeValues,
	 new ProtectedProperties(new Properties()));
  }

  /**
   * Constructor for nominal attributes and string attributes, where
   * metadata is supplied. If a null vector of attribute values is passed
   * to the method, the attribute is assumed to be a string.
   *
   * @param attributeName the name for the attribute
   * @param attributeValues a vector of strings denoting the 
   * attribute values. Null if the attribute is a string attribute.
   * @param metadata the attribute's properties
   */
  //@ requires attributeName != null;
  //@ requires metadata != null;
  /*@ ensures  m_Name == attributeName;
      ensures  m_Index == -1;
      ensures  attributeValues == null && m_Type == STRING
            || attributeValues != null && m_Type == NOMINAL 
                  && m_Values.size() == attributeValues.size();
      signals (IllegalArgumentException ex) 
                 (* if duplicate strings in attributeValues *);
  */
  public AttributeShort(String attributeName, 
		   FastVector attributeValues,
		   ProtectedProperties metadata) {

    m_Name = attributeName;
    m_Index = -1;
    m_TempValues = null;
    if (attributeValues == null) {
      m_Values = new FastVector();
      m_Hashtable = new Hashtable();
      m_Type = STRING;
    } else {
      m_Values = new FastVector(attributeValues.size());
      m_Hashtable = new Hashtable(attributeValues.size());
      for (short i = 0; i < attributeValues.size(); i++) {
	Object store = attributeValues.elementAt(i);
	if (((String)store).length() > STRING_COMPRESS_THRESHOLD) {
	  try {
	    store = new SerializedObject(attributeValues.elementAt(i), true);
	  } catch (Exception ex) {
	    System.err.println("Couldn't compress nominal attribute value -"
			       + " storing uncompressed.");
	  }
	}
	if (m_Values.indexOf(store) >= 0) {
	  throw new IllegalArgumentException("A nominal attribute (" +
					     attributeName + ") cannot"
					     + " have duplicate labels (" + store + ").");
	}
	m_Values.addElement(store);
	m_Hashtable.put(store, new Short(i));
      }
      m_Type = NOMINAL;
    }
    setMetadata(metadata);
  }

  /**
   * Produces a shallow copy of this attribute.
   *
   * @return a copy of this attribute with the same index
   */
  //@ also ensures \result instanceof Attribute;
  public /*@ pure non_null @*/ Object copy() {

    AttributeShort copy = new AttributeShort(m_Name);

    copy.m_Index = m_Index;
    copy.m_Type = m_Type;
    copy.m_Values = m_Values;
    copy.m_Hashtable = m_Hashtable;
    copy.m_NumericValues = m_NumericValues;
    copy.m_OriginalValues = m_OriginalValues;
    copy.m_DateFormat = m_DateFormat;
    copy.m_TempValues = m_TempValues;
    copy.setMetadata(m_Metadata);
 
    return copy;
  }

  /**
   * Returns an enumeration of all the attribute's values if
   * the attribute is nominal or a string, null otherwise. 
   *
   * @return enumeration of all the attribute's values
   */
  public final /*@ pure @*/ Enumeration enumerateValues() {

    if (isNominal() || isString()) {
      final Enumeration ee = m_Values.elements();
      return new Enumeration () {
          public boolean hasMoreElements() {
            return ee.hasMoreElements();
          }
          public Object nextElement() {
            Object oo = ee.nextElement();
            if (oo instanceof SerializedObject) {
              return ((SerializedObject)oo).getObject();
            } else {
              return oo;
            }
          }
        };
    }
    return null;
  }

  /**
   * Tests if given attribute is equal to this attribute.
   *
   * @param other the Object to be compared to this attribute
   * @return true if the given attribute is equal to this attribute
   */
  public final /*@ pure @*/ boolean equals(Object other) {

    if ((other == null) || !(other.getClass().equals(this.getClass()))) {
      return false;
    }
    AttributeShort att = (AttributeShort) other;
    if (!m_Name.equals(att.m_Name)) {
      return false;
    }
    if (isNominal() && att.isNominal()) {
      if (m_Values.size() != att.m_Values.size()) {
        return false;
      }
      for (int i = 0; i < m_Values.size(); i++) {
        if (!m_Values.elementAt(i).equals(att.m_Values.elementAt(i))) {
          return false;
        }
      }
      return true;
    } else {
      return (type() == att.type());
    }
  }

  /**
   * Returns the index of this attribute.
   *
   * @return the index of this attribute
   */
  //@ ensures \result == m_Index;
  public final /*@ pure @*/ int index() {

    return m_Index;
  }

  /**
   * Returns the index of a given attribute value. (The index of
   * the first occurence of this value.)
   *
   * @param value the value for which the index is to be returned
   * @return the index of the given attribute value if attribute
   * is nominal or a string, -1 if it is numeric or the value 
   * can't be found
   */
  public final short indexOfValue(String value) {

    if (!isNominal() && !isString())
      return -1;
    Object store = value;
    if (value.length() > STRING_COMPRESS_THRESHOLD) {
      try {
        store = new SerializedObject(value, true);
      } catch (Exception ex) {
        System.err.println("Couldn't compress string attribute value -"
                           + " searching uncompressed.");
      }
    }
    Short val = (Short)m_Hashtable.get(store);
    if (val == null) return -1;
    else return val.shortValue();
  }
  
  public final short storeTemp(Double val){
    if(!isNumeric())
      return -1;
    if(!notRestricted())
      return val.shortValue();

    if(m_TempValues==null)
      m_TempValues = new Hashtable();
    if(m_TempValues.containsKey(val))
      return ((Short)m_TempValues.get(val)).shortValue();
    else{
      short size = (short)m_TempValues.size();
      //System.err.println("Adding "+newVal+" atrr "+toString());
      m_TempValues.put(val,new Short(size));
      return size;
    }
    
  }
  
  public void storeCoresp(Short i, Double val){
	    if(m_TempOrigValues==null)
	       m_TempOrigValues = new Hashtable();
	    m_TempOrigValues.put(i, val);
  }
  
  public void computeValueCorrespondence(){
    if(!isNumeric())
      return ;
    TreeSet values = new TreeSet();
    values.addAll(m_TempValues.keySet());
    Object[] valArray = values.toArray();
    Hashtable valMap = new Hashtable();
    m_OriginalValues = new Hashtable();
    for(short i=0; i<valArray.length; i++){
      Object index = m_TempValues.get(valArray[i]);
      valMap.put(index,new Short(i));
      m_OriginalValues.put(new Short(i), valArray[i]);
    }
    m_TempValues=null;
    m_NumericValues = valMap;
    //System.err.println("Attribute "+toString()+" with values - "+m_NumericValues.size());
  }
  
  public void normalizeValues(){
    if(!isNumeric())
      return ;
    //find the biggest element
    Double max = new Double(0);
    Double zero = new Double(0);
    Iterator valueIter = m_OriginalValues.values().iterator();
    while(valueIter.hasNext()){
      Object item = valueIter.next();
      //System.err.println(item);
      Double cur = (Double)item;
      //work with absolute values
      if(cur.compareTo(zero)<0)
        cur = new Double(-cur.doubleValue());
      if(max.compareTo(cur)<0)
        max = cur;
    }
    m_NormalizingConstant = max.doubleValue();
    if(m_NormalizingConstant==1)
      return;
    System.err.println("Normalizing "+toString()+" max value is "+m_NormalizingConstant);
    valueIter = m_OriginalValues.keySet().iterator();
    if(m_NormalizingConstant==0)
      return;
    while(valueIter.hasNext()){
      Object it = valueIter.next();
      Double cur = (Double)m_OriginalValues.get(it);
      Double newCur = new Double(cur.doubleValue()/m_NormalizingConstant);
      m_OriginalValues.put(it,newCur);
    }
  }
  
  public void normalizeValues(AttributeShort at){

    m_NormalizingConstant = at.m_NormalizingConstant;
    if(m_NormalizingConstant==1)
      return;
    Iterator valueIter = m_OriginalValues.keySet().iterator();
    if(m_NormalizingConstant==0)
      return;
    while(valueIter.hasNext()){
      Object it = valueIter.next();
      Double cur = (Double)m_OriginalValues.get(it);
      Double newCur = new Double(cur.doubleValue()/m_NormalizingConstant);
      m_OriginalValues.put(it,newCur);
    }
  }

  public short getValueCorrespondence(Short i){
    //System.err.println("Attribute "+toString()+" value "+i);
    if((Short)m_NumericValues.get(i)==null)
      throw new RuntimeException("Value "+i+" not found.");
    return ((Short)m_NumericValues.get(i)).shortValue();
  }
  
  public double getOriginalValue(Short i){
    //System.err.println("Attribute "+toString()+" value "+i);
    if(notRestricted()){
      if(!m_Direct)
        return (i.shortValue()==Short.MAX_VALUE)?Double.NaN:((Double)m_OriginalValues.get(i)).doubleValue();
    }
    return i.doubleValue();
  }
  
  public double getOriginalValueNoCleanup(Short i){
	    //System.err.println("Attribute "+toString()+" value "+i+" -- "+m_TempOrigValues);
	    if(notRestricted()){
	      if(!m_Direct&&isNumeric())
	        return (i.shortValue()==Short.MAX_VALUE)?Double.NaN:((Double)m_TempOrigValues.get(i)).doubleValue();
	    }
	    return i.doubleValue();
	  }
  /**
   * Test if the attribute is nominal.
   *
   * @return true if the attribute is nominal
   */
  //@ ensures \result <==> (m_Type == NOMINAL);
  public final /*@ pure @*/ boolean isNominal() {

    return (m_Type == NOMINAL);
  }
  
  public boolean notRestricted(){
    if(m_Name.equalsIgnoreCase("DOCNUM")||m_Name.equalsIgnoreCase("ID1")||m_Name.equalsIgnoreCase("ID2"))
      return false;
    return true;
    
  }
  public boolean isFeature(){
    if(m_Name.equalsIgnoreCase("DOCNUM")||m_Name.equalsIgnoreCase("ID1")||m_Name.equalsIgnoreCase("ID2"))
      return false;
    if(m_Name.equalsIgnoreCase("CLASS")||m_Name.equalsIgnoreCase("PREDICTED_CLASS")||m_Name.equalsIgnoreCase("REAL_CLASS"))
      return false;
    if(m_Name.equalsIgnoreCase("SOURCES")||m_Name.equalsIgnoreCase("COVERED")||m_Name.equalsIgnoreCase("DocNo"))
      return false;
    return true;
    
  }
  /**
   * Tests if the attribute is numeric.
   *
   * @return true if the attribute is numeric
   */
  //@ ensures \result <==> ((m_Type == NUMERIC) || (m_Type == DATE));
  public final /*@ pure @*/ boolean isNumeric() {

    return ((m_Type == NUMERIC) || (m_Type == DATE));
  }

  /**
   * Tests if the attribute is a string.
   *
   * @return true if the attribute is a string
   */
  //@ ensures \result <==> (m_Type == STRING);
  public final /*@ pure @*/ boolean isString() {

    return (m_Type == STRING);
  }

  /**
   * Tests if the attribute is a date type.
   *
   * @return true if the attribute is a date type
   */
  //@ ensures \result <==> (m_Type == DATE);
  public final /*@ pure @*/ boolean isDate() {

    return (m_Type == DATE);
  }

  /**
   * Returns the attribute's name.
   *
   * @return the attribute's name as a string
   */
  //@ ensures \result == m_Name;
  public final /*@ pure @*/ String name() {

    return m_Name;
  }
  
  /**
   * Returns the number of attribute values. Returns 0 for numeric attributes.
   *
   * @return the number of attribute values
   */
  public final /*@ pure @*/ int numValues() {

    if (isNumeric()) {
      return m_NumericValues.size();
    } else {
      return m_Values.size();
    }
  }

  /**
   * Returns a description of this attribute in ARFF format. Quotes
   * strings if they contain whitespace characters, or if they
   * are a question mark.
   *
   * @return a description of this attribute as a string
   */
  public final String toString() {
    //throw new RuntimeException();
    StringBuffer text = new StringBuffer();
    
    text.append(ARFF_ATTRIBUTE).append(" ").append(Utils.quote(m_Name)).append(" ");
    switch (m_Type) {
    case NOMINAL:
      text.append('{');
      Enumeration enu = enumerateValues();
      while (enu.hasMoreElements()) {
	text.append(Utils.quote((String) enu.nextElement()));
	if (enu.hasMoreElements())
	  text.append(',');
      }
      text.append('}');
      break;
    case NUMERIC:
      text.append(ARFF_ATTRIBUTE_NUMERIC);
      break;
    case STRING:
      text.append(ARFF_ATTRIBUTE_STRING);
      break;
    case DATE:
      text.append(ARFF_ATTRIBUTE_DATE).append(" ").append(Utils.quote(m_DateFormat.toPattern()));
      break;
    default:
      text.append("UNKNOWN");
      break;
    }
    return text.toString();
  }

  /**
   * Returns the attribute's type as an integer.
   *
   * @return the attribute's type.
   */
  //@ ensures \result == m_Type;
  public final /*@ pure @*/ int type() {

    return m_Type;
  }

  /**
   * Returns a value of a nominal or string attribute. 
   * Returns an empty string if the attribute is neither
   * nominal nor a string attribute.
   *
   * @param valIndex the value's index
   * @return the attribute's value as a string
   */
  public final /*@ non_null pure @*/ String value(int valIndex) {
    
    if (!isNominal() && !isString()) {
      return "";
    } else {
      Object val = m_Values.elementAt(valIndex);
      
      // If we're storing strings compressed, uncompress it.
      if (val instanceof SerializedObject) {
        val = ((SerializedObject)val).getObject();
      }
      return (String) val;
    }
  }

  /**
   * Constructor for a numeric attribute with a particular index.
   *
   * @param attributeName the name for the attribute
   * @param index the attribute's index
   */
  //@ requires attributeName != null;
  //@ requires index >= 0;
  //@ ensures  m_Name == attributeName;
  //@ ensures  m_Index == index;
  AttributeShort(String attributeName, int index) {

    this(attributeName);
    m_Index = index;
  }

  /**
   * Constructor for date attributes with a particular index.
   *
   * @param attributeName the name for the attribute
   * @param dateFormat a string suitable for use with
   * SimpleDateFormatter for parsing dates.  Null for a default format
   * string.
   * @param index the attribute's index
   */
  //@ requires attributeName != null;
  //@ requires index >= 0;
  //@ ensures  m_Name == attributeName;
  //@ ensures  m_Index == index;
  AttributeShort(String attributeName, String dateFormat, 
	    int index) {

    this(attributeName, dateFormat);
    m_Index = index;
  }

  /**
   * Constructor for nominal attributes and string attributes with
   * a particular index.
   * If a null vector of attribute values is passed to the method,
   * the attribute is assumed to be a string.
   *
   * @param attributeName the name for the attribute
   * @param attributeValues a vector of strings denoting the attribute values.
   * Null if the attribute is a string attribute.
   * @param index the attribute's index
   */
  //@ requires attributeName != null;
  //@ requires index >= 0;
  //@ ensures  m_Name == attributeName;
  //@ ensures  m_Index == index;
  AttributeShort(String attributeName, FastVector attributeValues, 
	    int index) {

    this(attributeName, attributeValues);
    m_Index = index;
  }

  /**
   * Adds a string value to the list of valid strings for attributes
   * of type STRING and returns the index of the string.
   *
   * @param value The string value to add
   * @return the index assigned to the string, or -1 if the attribute is not
   * of type Attribute.STRING 
   */
  /*@ requires value != null;
      ensures  isString() && 0 <= \result && \result < m_Values.size() ||
             ! isString() && \result == -1;
  */
  public short addStringValue(String value) {

    if (!isString()) {
      return -1;
    }
    Object store = value;

    if (value.length() > STRING_COMPRESS_THRESHOLD) {
      try {
        store = new SerializedObject(value, true);
      } catch (Exception ex) {
        System.err.println("Couldn't compress string attribute value -"
                           + " storing uncompressed.");
      }
    }
    Short index = (Short)m_Hashtable.get(store);
    if (index != null) {
      return index.shortValue();
    } else {
      short intIndex = (short)m_Values.size();
      m_Values.addElement(store);
      m_Hashtable.put(store, new Short(intIndex));
      return intIndex;
    }
  }

  /**
   * Adds a string value to the list of valid strings for attributes
   * of type STRING and returns the index of the string. This method is
   * more efficient than addStringValue(String) for long strings.
   *
   * @param src The Attribute containing the string value to add.
   * @param int index the index of the string value in the source attribute.
   * @return the index assigned to the string, or -1 if the attribute is not
   * of type Attribute.STRING 
   */
  /*@ requires src != null;
      requires 0 <= index && index < src.m_Values.size();
      ensures  isString() && 0 <= \result && \result < m_Values.size() ||
             ! isString() && \result == -1;
  */
  public int addStringValue(AttributeShort src, int index) {

    if (!isString()) {
      return -1;
    }
    Object store = src.m_Values.elementAt(index);
    Short oldIndex = (Short)m_Hashtable.get(store);
    if (oldIndex != null) {
      return oldIndex.intValue();
    } else {
      short intIndex = (short)m_Values.size();
      m_Values.addElement(store);
      m_Hashtable.put(store, new Short(intIndex));
      return intIndex;
    }
  }

  /**
   * Adds an attribute value. Creates a fresh list of attribute
   * values before adding it.
   *
   * @param value the attribute value
   */
  final void addValue(String value) {

    m_Values = (FastVector)m_Values.copy();
    m_Hashtable = (Hashtable)m_Hashtable.clone();
    forceAddValue(value);
  }

  /**
   * Produces a shallow copy of this attribute with a new name.
   *
   * @param newName the name of the new attribute
   * @return a copy of this attribute with the same index
   */
  //@ requires newName != null;
  //@ ensures \result.m_Name  == newName;
  //@ ensures \result.m_Index == m_Index;
  //@ ensures \result.m_Type  == m_Type;
  public final /*@ pure non_null @*/ AttributeShort copy(String newName) {

    AttributeShort copy = new AttributeShort(newName);

    copy.m_Index = m_Index;
    copy.m_DateFormat = m_DateFormat;
    copy.m_Type = m_Type;
    copy.m_Values = m_Values;
    copy.m_Hashtable = m_Hashtable;
    copy.setMetadata(m_Metadata);
 
    return copy;
  }

  /**
   * Removes a value of a nominal or string attribute. Creates a 
   * fresh list of attribute values before removing it.
   *
   * @param index the value's index
   * @exception IllegalArgumentException if the attribute is not nominal
   */
  //@ requires isNominal() || isString();
  //@ requires 0 <= index && index < m_Values.size();
  final void delete(int index) {
    
    if (!isNominal() && !isString()) 
      throw new IllegalArgumentException("Can only remove value of" +
                                         "nominal or string attribute!");
    else {
      m_Values = (FastVector)m_Values.copy();
      m_Values.removeElementAt(index);
      Hashtable hash = new Hashtable(m_Hashtable.size());
      Enumeration enu = m_Hashtable.keys();
      while (enu.hasMoreElements()) {
	Object string = enu.nextElement();
	Short valIndexObject = (Short)m_Hashtable.get(string);
	short valIndex = valIndexObject.shortValue();
	if (valIndex > index) {
	  hash.put(string, new Short((short)(valIndex - 1)));
	} else if (valIndex < index) {
	  hash.put(string, valIndexObject);
	}
      }
      m_Hashtable = hash;
    }
  }

  /**
   * Adds an attribute value.
   *
   * @param value the attribute value
   */
  //@ requires value != null;
  //@ ensures  m_Values.size() == \old(m_Values.size()) + 1;
  final void forceAddValue(String value) {

    Object store = value;
    if (value.length() > STRING_COMPRESS_THRESHOLD) {
      try {
        store = new SerializedObject(value, true);
      } catch (Exception ex) {
        System.err.println("Couldn't compress string attribute value -"
                           + " storing uncompressed.");
      }
    }
    m_Values.addElement(store);
    m_Hashtable.put(store, new Short((short)(m_Values.size() - 1)));
  }

  /**
   * Sets the index of this attribute.
   *
   * @param the index of this attribute
   */
  //@ requires 0 <= index;
  //@ assignable m_Index;
  //@ ensures m_Index == index;
  final void setIndex(int index) {

    m_Index = index;
  }

  /**
   * Sets a value of a nominal attribute or string attribute.
   * Creates a fresh list of attribute values before it is set.
   *
   * @param index the value's index
   * @param string the value
   * @exception IllegalArgumentException if the attribute is not nominal or 
   * string.
   */
  //@ requires string != null;
  //@ requires isNominal() || isString();
  //@ requires 0 <= index && index < m_Values.size();
  final void setValue(short index, String string) {
    
    switch (m_Type) {
    case NOMINAL:
    case STRING:
      m_Values = (FastVector)m_Values.copy();
      m_Hashtable = (Hashtable)m_Hashtable.clone();
      Object store = string;
      if (string.length() > STRING_COMPRESS_THRESHOLD) {
        try {
          store = new SerializedObject(string, true);
        } catch (Exception ex) {
          System.err.println("Couldn't compress string attribute value -"
                             + " storing uncompressed.");
        }
      }
      m_Hashtable.remove(m_Values.elementAt(index));
      m_Values.setElementAt(store, index);
      m_Hashtable.put(store, new Short(index));
      break;
    default:
      throw new IllegalArgumentException("Can only set values for nominal"
                                         + " or string attributes!");
    }
  }

  //@ requires isDate();
  public /*@pure@*/ String formatDate(double date) {
    switch (m_Type) {
    case DATE:
      return m_DateFormat.format(new Date((long)date));
    default:
      throw new IllegalArgumentException("Can only format date values for date"
                                         + " attributes!");
    }
  }

  //@ requires isDate();
  //@ requires string != null;
  public short parseDate(String string) throws ParseException {
    switch (m_Type) {
    case DATE:
      long time = m_DateFormat.parse(string).getTime();
      // TODO put in a safety check here if we can't store the value in a double.
      return (short)time;
    default:
      throw new IllegalArgumentException("Can only parse date values for date"
                                         + " attributes!");
    }
  }

  /**
   * Returns the properties supplied for this attribute.
   *
   * @return metadata for this attribute
   */  
  public final /*@ pure @*/ ProtectedProperties getMetadata() {

    return m_Metadata;
  }

  /**
   * Returns the ordering of the attribute. One of the following:
   * 
   * ORDERING_SYMBOLIC - attribute values should be treated as symbols.
   * ORDERING_ORDERED  - attribute values have a global ordering.
   * ORDERING_MODULO   - attribute values have an ordering which wraps.
   *
   * @return the ordering type of the attribute
   */
  public final /*@ pure @*/ int ordering() {

    return m_Ordering;
  }

  /**
   * Returns whether the attribute values are equally spaced.
   *
   * @return whether the attribute is regular or not
   */
  public final /*@ pure @*/ boolean isRegular() {

    return m_IsRegular;
  }

  /**
   * Returns whether the attribute can be averaged meaningfully.
   *
   * @return whether the attribute can be averaged or not
   */
  public final /*@ pure @*/ boolean isAveragable() {

    return m_IsAveragable;
  }

  /**
   * Returns whether the attribute has a zeropoint and may be
   * added meaningfully.
   *
   * @return whether the attribute has a zeropoint or not
   */
  public final /*@ pure @*/ boolean hasZeropoint() {

    return m_HasZeropoint;
  }

  /**
   * Returns the attribute's weight.
   *
   * @return the attribute's weight as a double
   */
  public final /*@ pure @*/ double weight() {

    return m_Weight;
  }

  /**
   * Returns the lower bound of a numeric attribute.
   *
   * @return the lower bound of the specified numeric range
   */
  public final /*@ pure @*/ double getLowerNumericBound() {

    return m_LowerBound;
  }

  /**
   * Returns whether the lower numeric bound of the attribute is open.
   *
   * @return whether the lower numeric bound is open or not (closed)
   */
  public final /*@ pure @*/ boolean lowerNumericBoundIsOpen() {

    return m_LowerBoundIsOpen;
  }

  /**
   * Returns the upper bound of a numeric attribute.
   *
   * @return the upper bound of the specified numeric range
   */
  public final /*@ pure @*/ double getUpperNumericBound() {

    return m_UpperBound;
  }

  /**
   * Returns whether the upper numeric bound of the attribute is open.
   *
   * @return whether the upper numeric bound is open or not (closed)
   */
  public final /*@ pure @*/ boolean upperNumericBoundIsOpen() {

    return m_UpperBoundIsOpen;
  }

  /**
   * Determines whether a value lies within the bounds of the attribute.
   *
   * @return whether the value is in range
   */
  public final /*@ pure @*/ boolean isInRange(short value) {

    // dates and missing values are a special case 
    if (m_Type == DATE || value == InstanceShort.missingValue()) return true;
    if (m_Type != NUMERIC) {
      // do label range check
      int intVal = (int) value;
      if (intVal < 0 || intVal >= m_Hashtable.size()) return false;
    } else {
      // do numeric bounds check
      if (m_LowerBoundIsOpen) {
	if (value <= m_LowerBound) return false;
      } else {
	if (value < m_LowerBound) return false;
      }
      if (m_UpperBoundIsOpen) {
	if (value >= m_UpperBound) return false;
      } else {
	if (value > m_UpperBound) return false;
      }
    }
    return true;
  }
  
  public ArrayList getValueMap(){
    if(m_BinaryAttributeMap!=null)
      return m_BinaryAttributeMap;
    m_BinaryAttributeMap = new ArrayList();
    Hashtable originalVals = new Hashtable();
    originalVals.put(new Short((short)0),new Double(0));
    originalVals.put(new Short((short)1),new Double(1));

    Hashtable tempValues=new Hashtable();
    tempValues.put(new Short((short)0),new Double(0));
    tempValues.put(new Short((short)1),new Double(1));
    for(int i=0; i<m_Values.size(); i++){
      AttributeShort at = new AttributeShort(this.name()+"_"+this.value(i), this.getMetadata());
      at.m_OriginalValues = originalVals;
      at.m_TempOrigValues = tempValues;
      m_BinaryAttributeMap.add(at);
    }
    return m_BinaryAttributeMap;
  }
  
  public AttributeShort getBinaryAttribute(short value){
    return (AttributeShort)m_BinaryAttributeMap.get(value);
  }

  /**
   * This function returns the number of values for a numeric attribute. As a
   * side effect the function creates a hash map of all attribute values.
   * WARNING: this function assumes that the attribute is always associated
   * with the same set of instances.
   *
   * @return the number of distinct numeric values
   */
  /*
  public int countAndComputeValues(InstancesShort instances){
  	if(!isNumeric())
  		return -1;
  	//if(true)
  	//	return 0;
  	if(m_NumericValues==null){
  		TreeSet values = new TreeSet();
  		for(int i=0; i<instances.numInstances();i++){
  			InstanceShort ins = instances.instance(i);
  			Double val = new Double(ins.value(this));
  			values.add(val);
  			}  		
  		m_NumericValues = (Double[])values.toArray(new Double[1]);
  	}
  	return m_NumericValues.length;
  }
  
  public double getValNum(int num){
  	if(!isNumeric())
  		throw new RuntimeException("Function only works for numeric attributes");
  	if(num<0||num>=m_NumericValues.length)
  		throw new RuntimeException("Value index out of bounds");
  	return m_NumericValues[num].doubleValue();
  }
  */

  /**
   * Sets the metadata for the attribute. Processes the strings stored in the
   * metadata of the attribute so that the properties can be set up for the
   * easy-access metadata methods. Any strings sought that are omitted will
   * cause default values to be set.
   * 
   * The following properties are recognised:
   * ordering, averageable, zeropoint, regular, weight, and range.
   *
   * All other properties can be queried and handled appropriately by classes
   * calling the getMetadata() method.
   *
   * @param metadata the metadata
   * @exception IllegalArgumentException if the properties are not consistent
   */
  //@ requires metadata != null;
  private void setMetadata(ProtectedProperties metadata) {
    
    m_Metadata = metadata;

    if (m_Type == DATE) {
      m_Ordering = ORDERING_ORDERED;
      m_IsRegular = true;
      m_IsAveragable = false;
      m_HasZeropoint = false;
    } else {

      // get ordering
      String orderString = m_Metadata.getProperty("ordering","");
      
      // numeric ordered attributes are averagable and zeropoint by default
      String def;
      if (m_Type == NUMERIC
	  && orderString.compareTo("modulo") != 0
	  && orderString.compareTo("symbolic") != 0)
	def = "true";
      else def = "false";
      
      // determine boolean states
      m_IsAveragable =
	(m_Metadata.getProperty("averageable",def).compareTo("true") == 0);
      m_HasZeropoint =
	(m_Metadata.getProperty("zeropoint",def).compareTo("true") == 0);
      // averagable or zeropoint implies regular
      if (m_IsAveragable || m_HasZeropoint) def = "true";
      m_IsRegular =
	(m_Metadata.getProperty("regular",def).compareTo("true") == 0);
      
      // determine ordering
      if (orderString.compareTo("symbolic") == 0)
	m_Ordering = ORDERING_SYMBOLIC;
      else if (orderString.compareTo("ordered") == 0)
	m_Ordering = ORDERING_ORDERED;
      else if (orderString.compareTo("modulo") == 0)
	m_Ordering = ORDERING_MODULO;
      else {
	if (m_Type == NUMERIC || m_IsAveragable || m_HasZeropoint)
	  m_Ordering = ORDERING_ORDERED;
	else m_Ordering = ORDERING_SYMBOLIC;
      }
    }

    // consistency checks
    if (m_IsAveragable && !m_IsRegular)
      throw new IllegalArgumentException("An averagable attribute must be"
					 + " regular");
    if (m_HasZeropoint && !m_IsRegular)
      throw new IllegalArgumentException("A zeropoint attribute must be"
					 + " regular");
    if (m_IsRegular && m_Ordering == ORDERING_SYMBOLIC)
      throw new IllegalArgumentException("A symbolic attribute cannot be"
					 + " regular");
    if (m_IsAveragable && m_Ordering != ORDERING_ORDERED)
      throw new IllegalArgumentException("An averagable attribute must be"
					 + " ordered");
    if (m_HasZeropoint && m_Ordering != ORDERING_ORDERED)
      throw new IllegalArgumentException("A zeropoint attribute must be"
					 + " ordered");

    // determine weight
    m_Weight = 1.0;
    String weightString = m_Metadata.getProperty("weight");
    if (weightString != null) {
      try{
	m_Weight = Double.valueOf(weightString).doubleValue();
      } catch (NumberFormatException e) {
	// Check if value is really a number
	throw new IllegalArgumentException("Not a valid attribute weight: '" 
					   + weightString + "'");
      }
    }

    // determine numeric range
    if (m_Type == NUMERIC) setNumericRange(m_Metadata.getProperty("range"));
  }

  /**
   * Sets the numeric range based on a string. If the string is null the range
   * will default to [-inf,+inf]. A square brace represents a closed interval, a
   * curved brace represents an open interval, and 'inf' represents infinity.
   * Examples of valid range strings: "[-inf,20)","(-13.5,-5.2)","(5,inf]"
   *
   * @param rangeString the string to parse as the attribute's numeric range
   * @exception IllegalArgumentException if the range is not valid
   */
  //@ requires rangeString != null;
  private void setNumericRange(String rangeString)
  {
    // set defaults
    m_LowerBound = Double.NEGATIVE_INFINITY;
    m_LowerBoundIsOpen = false;
    m_UpperBound = Double.POSITIVE_INFINITY;
    m_UpperBoundIsOpen = false;

    if (rangeString == null) return;

    // set up a tokenzier to parse the string
    StreamTokenizer tokenizer =
      new StreamTokenizer(new StringReader(rangeString));
    tokenizer.resetSyntax();         
    tokenizer.whitespaceChars(0, ' ');    
    tokenizer.wordChars(' '+1,'\u00FF');
    tokenizer.ordinaryChar('[');
    tokenizer.ordinaryChar('(');
    tokenizer.ordinaryChar(',');
    tokenizer.ordinaryChar(']');
    tokenizer.ordinaryChar(')');

    try {

      // get opening brace
      tokenizer.nextToken();
    
      if (tokenizer.ttype == '[') m_LowerBoundIsOpen = false;
      else if (tokenizer.ttype == '(') m_LowerBoundIsOpen = true;
      else throw new IllegalArgumentException("Expected opening brace on range,"
					      + " found: "
					      + tokenizer.toString());

      // get lower bound
      tokenizer.nextToken();
      if (tokenizer.ttype != tokenizer.TT_WORD)
	throw new IllegalArgumentException("Expected lower bound in range,"
					   + " found: "
					   + tokenizer.toString());
      if (tokenizer.sval.compareToIgnoreCase("-inf") == 0)
	m_LowerBound = Double.NEGATIVE_INFINITY;
      else if (tokenizer.sval.compareToIgnoreCase("+inf") == 0)
	m_LowerBound = Double.POSITIVE_INFINITY;
      else if (tokenizer.sval.compareToIgnoreCase("inf") == 0)
	m_LowerBound = Double.NEGATIVE_INFINITY;
      else try {
	m_LowerBound = Double.valueOf(tokenizer.sval).doubleValue();
      } catch (NumberFormatException e) {
	throw new IllegalArgumentException("Expected lower bound in range,"
					   + " found: '" + tokenizer.sval + "'");
      }

      // get separating comma
      if (tokenizer.nextToken() != ',')
	throw new IllegalArgumentException("Expected comma in range,"
					   + " found: "
					   + tokenizer.toString());

      // get upper bound
      tokenizer.nextToken();
      if (tokenizer.ttype != tokenizer.TT_WORD)
	throw new IllegalArgumentException("Expected upper bound in range,"
					   + " found: "
					   + tokenizer.toString());
      if (tokenizer.sval.compareToIgnoreCase("-inf") == 0)
	m_UpperBound = Double.NEGATIVE_INFINITY;
      else if (tokenizer.sval.compareToIgnoreCase("+inf") == 0)
	m_UpperBound = Double.POSITIVE_INFINITY;
      else if (tokenizer.sval.compareToIgnoreCase("inf") == 0)
	m_UpperBound = Double.POSITIVE_INFINITY;
      else try {
	m_UpperBound = Double.valueOf(tokenizer.sval).doubleValue();
      } catch (NumberFormatException e) {
	throw new IllegalArgumentException("Expected upper bound in range,"
					   + " found: '" + tokenizer.sval + "'");
      }

      // get closing brace
      tokenizer.nextToken();
    
      if (tokenizer.ttype == ']') m_UpperBoundIsOpen = false;
      else if (tokenizer.ttype == ')') m_UpperBoundIsOpen = true;
      else throw new IllegalArgumentException("Expected closing brace on range,"
					      + " found: "
					      + tokenizer.toString());

      // check for rubbish on end
      if (tokenizer.nextToken() != tokenizer.TT_EOF)
	throw new IllegalArgumentException("Expected end of range string,"
					   + " found: "
					   + tokenizer.toString());

    } catch (IOException e) {
      throw new IllegalArgumentException("IOException reading attribute range"
					 + " string: " + e.getMessage());
    }

    if (m_UpperBound < m_LowerBound)
      throw new IllegalArgumentException("Upper bound (" + m_UpperBound
					 + ") on numeric range is"
					 + " less than lower bound ("
					 + m_LowerBound + ")!");
  }

  public void setDirect(boolean dir){
    m_Direct = dir;
  }
  /**
   * Simple main method for testing this class.
   */
  //@ requires ops != null;
  //@ requires \nonnullelements(ops);
  public static void main(String[] ops) {

    try {
      
      // Create numeric attributes "length" and "weight"
      AttributeShort length = new AttributeShort("length");
      AttributeShort weight = new AttributeShort("weight");

      // Create date attribute "date"
      AttributeShort date = new AttributeShort("date", "yyyy-MM-dd HH:mm:ss");

      System.out.println(date);
      double dd = date.parseDate("2001-04-04 14:13:55");
      System.out.println("Test date = " + dd);
      System.out.println(date.formatDate(dd));

      dd = new Date().getTime();
      System.out.println("Date now = " + dd);
      System.out.println(date.formatDate(dd));
      
      // Create vector to hold nominal values "first", "second", "third" 
      FastVector my_nominal_values = new FastVector(3); 
      my_nominal_values.addElement("first"); 
      my_nominal_values.addElement("second"); 
      my_nominal_values.addElement("third"); 
      
      // Create nominal attribute "position" 
      AttributeShort position = new AttributeShort("position", my_nominal_values);

      // Print the name of "position"
      System.out.println("Name of \"position\": " + position.name());

      // Print the values of "position"
      Enumeration attValues = position.enumerateValues();
      while (attValues.hasMoreElements()) {
	String string = (String)attValues.nextElement();
	System.out.println("Value of \"position\": " + string);
      }

      // Shallow copy attribute "position"
      AttributeShort copy = (AttributeShort) position.copy();

      // Test if attributes are the same
      System.out.println("Copy is the same as original: " + copy.equals(position));

      // Print index of attribute "weight" (should be unset: -1)
      System.out.println("Index of attribute \"weight\" (should be -1): " + 
			 weight.index());

      // Print index of value "first" of attribute "position"
      System.out.println("Index of value \"first\" of \"position\" (should be 0): " +
			 position.indexOfValue("first"));

      // Tests type of attribute "position"
      System.out.println("\"position\" is numeric: " + position.isNumeric());
      System.out.println("\"position\" is nominal: " + position.isNominal());
      System.out.println("\"position\" is string: " + position.isString());

      // Prints name of attribute "position"
      System.out.println("Name of \"position\": " + position.name());
    
      // Prints number of values of attribute "position"
      System.out.println("Number of values for \"position\": " + position.numValues());

      // Prints the values (againg)
      for (int i = 0; i < position.numValues(); i++) {
	System.out.println("Value " + i + ": " + position.value(i));
      }

      // Prints the attribute "position" in ARFF format
      System.out.println(position);

      // Checks type of attribute "position" using constants
      switch (position.type()) {
      case AttributeShort.NUMERIC:
	System.out.println("\"position\" is numeric");
	break;
      case AttributeShort.NOMINAL:
	System.out.println("\"position\" is nominal");
	break;
      case AttributeShort.STRING:
	System.out.println("\"position\" is string");
	break;
      case AttributeShort.DATE:
	System.out.println("\"position\" is date");
	break;
      default:
	System.out.println("\"position\" has unknown type");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
  
