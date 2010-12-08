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
 *    Instances.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package reconcile.weka.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Random;

/**
 * Class for handling an ordered set of weighted instances. <p>
 *
 * Typical usage (code from the main() method of this class): <p>
 *
 * <code>
 * ... <br>
 * 
 * // Read all the instances in the file <br>
 * reader = new FileReader(filename); <br>
 * instances = new Instances(reader); <br><br>
 *
 * // Make the last attribute be the class <br>
 * instances.setClassIndex(instances.numAttributes() - 1); <br><br>
 * 
 * // Print header and instances. <br>
 * System.out.println("\nDataset:\n"); <br> 
 * System.out.println(instances); <br><br>
 *
 * ... <br>
 * </code><p>
 *
 * All methods that change a set of instances are safe, ie. a change
 * of a set of instances does not affect any other sets of
 * instances. All methods that change a datasets's attribute
 * information clone the dataset before it is changed.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
 */
public class Instances implements Serializable {
 
  /** The filename extension that should be used for arff files */
  public static String FILE_EXTENSION = ".arff";

  /** The filename extension that should be used for bin. serialized instances files */
  public static String SERIALIZED_OBJ_FILE_EXTENSION = ".bsi";

  /** The keyword used to denote the start of an arff header */
  static String ARFF_RELATION = "@relation";

  /** The keyword used to denote the start of the arff data section */
  static String ARFF_DATA = "@data";

  /** The dataset's name. */
  protected /*@spec_public non_null@*/ String m_RelationName;         

  /** The attribute information. */
  protected /*@spec_public non_null@*/ FastVector m_Attributes;
  /*  public invariant (\forall int i; 0 <= i && i < m_Attributes.size(); 
                    m_Attributes.elementAt(i) != null);
  */

  /** The instances. */
  protected /*@spec_public non_null@*/ FastVector m_Instances;

  /** The class attribute's index */
  protected int m_ClassIndex;
  //@ protected invariant classIndex() == m_ClassIndex;

  /** Buffer of values for sparse instance */
  protected double[] m_ValueBuffer;

  /** Buffer of indices for sparse instance */
  protected int[] m_IndicesBuffer;
  
  /** The number of documents in the data */
  protected int m_NumDocuments;
  
  /** Used to debug the datastructures */
  protected static final boolean data_Debug = false;
  /**
   * Reads an ARFF file from a reader, and assigns a weight of
   * one to each instance. Lets the index of the class 
   * attribute be undefined (negative).
   *
   * @param reader the reader
   * @exception IOException if the ARFF file is not read 
   * successfully
   */
  public Instances(/*@non_null@*/Reader reader) throws IOException {

    StreamTokenizer tokenizer;

    tokenizer = new StreamTokenizer(reader);
    initTokenizer(tokenizer);
    readHeader(tokenizer);
    m_ClassIndex = -1;
    m_Instances = new FastVector(1000);
    while (getInstance(tokenizer, true)) {};
    compactify();
		if (data_Debug) {
			System.err.println("New ModifiedInstances " + numInstances() + " isnts "
					+ m_NumDocuments + " docs.");
			try {
				throw new RuntimeException("");
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
		}
 
  /**
   * Reads the header of an ARFF file from a reader and 
   * reserves space for the given number of instances. Lets
   * the class index be undefined (negative).
   *
   * @param reader the reader
   * @param capacity the capacity
   * @exception IllegalArgumentException if the header is not read successfully
   * or the capacity is negative.
   * @exception IOException if there is a problem with the reader.
   */
  //@ requires capacity >= 0;
  //@ ensures classIndex() == -1;
  public Instances(/*@non_null@*/Reader reader, int capacity)
    throws IOException {
    StreamTokenizer tokenizer;

    if (capacity < 0) {
      throw new IllegalArgumentException("Capacity has to be positive!");
    }
    tokenizer = new StreamTokenizer(reader); 
    initTokenizer(tokenizer);
    readHeader(tokenizer);
    m_ClassIndex = -1;
    m_Instances = new FastVector(capacity);
		if (data_Debug) {
			System.err.println("New ModifiedInstances " + numInstances() + " isnts "
					+ m_NumDocuments + " docs.");
			try {
				throw new RuntimeException("");
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
  }

  /**
   * Constructor copying all instances and references to
   * the header information from the given set of instances.
   *
   * @param instances the set to be copied
   */
  public Instances(/*@non_null@*/Instances dataset) {
    this(dataset, dataset.numInstances());

    dataset.copyInstances(0, this, dataset.numInstances());
		if (data_Debug) {
			System.err.println("New ModifiedInstances " + numInstances() + " isnts "
					+ m_NumDocuments + " docs.");
			try {
				throw new RuntimeException("");
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
  }

  /**
   * Constructor creating an empty set of instances. Copies references
   * to the header information from the given set of instances. Sets
   * the capacity of the set of instances to 0 if its negative.
   *
   * @param instances the instances from which the header 
   * information is to be taken
   * @param capacity the capacity of the new dataset 
   */
  public Instances(/*@non_null@*/Instances dataset, int capacity) {
    if (capacity < 0) {
      capacity = 0;
    }
    
    // Strings only have to be "shallow" copied because
    // they can't be modified.
    m_ClassIndex = dataset.m_ClassIndex;
    m_RelationName = dataset.m_RelationName;
    m_Attributes = dataset.m_Attributes;
    m_Instances = new FastVector(capacity);
		if (data_Debug) {
			System.err.println("New ModifiedInstances " + numInstances() + " isnts "
					+ m_NumDocuments + " docs.");
			try {
				throw new RuntimeException("");
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
  }

  /**
   * Creates a new set of instances by copying a 
   * subset of another set.
   *
   * @param source the set of instances from which a subset 
   * is to be created
   * @param first the index of the first instance to be copied
   * @param toCopy the number of instances to be copied
   * @exception IllegalArgumentException if first and toCopy are out of range
   */
  //@ requires 0 <= first;
  //@ requires 0 <= toCopy;
  //@ requires first + toCopy <= source.numInstances();
  public Instances(/*@non_null@*/Instances source, int first, int toCopy) {
    this(source, toCopy);

    if ((first < 0) || ((first + toCopy) > source.numInstances())) {
      throw new IllegalArgumentException("Parameters first and/or toCopy out "+
                                         "of range");
    }
    source.copyInstances(first, this, toCopy);
		if (data_Debug) {
			System.err.println("New ModifiedInstances " + numInstances() + " isnts "
					+ m_NumDocuments + " docs.");
			try {
				throw new RuntimeException("");
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
  }

  /**
   * Creates an empty set of instances. Uses the given
   * attribute information. Sets the capacity of the set of 
   * instances to 0 if its negative. Given attribute information
   * must not be changed after this constructor has been used.
   *
   * @param name the name of the relation
   * @param attInfo the attribute information
   * @param capacity the capacity of the set
   */
  public Instances(/*@non_null@*/String name, 
		   /*@non_null@*/FastVector attInfo, int capacity) {
    m_RelationName = name;
    m_ClassIndex = -1;
    m_Attributes = attInfo;
    for (int i = 0; i < numAttributes(); i++) {
      attribute(i).setIndex(i);
    }
    m_Instances = new FastVector(capacity);
		if (data_Debug) {
			System.err.println("New ModifiedInstances " + numInstances() + " isnts "
					+ m_NumDocuments + " docs.");
			try {
				throw new RuntimeException("");
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
  }
 
  /**
   * Create a copy of the structure, but "cleanse" string types (i.e.
   * doesn't contain references to the strings seen in the past).
   *
   * @return a copy of the instance structure.
   */
  public Instances stringFreeStructure() {

    FastVector atts = (FastVector)m_Attributes.copy();
    for (int i = 0 ; i < atts.size(); i++) {
      Attribute att = (Attribute)atts.elementAt(i);
      if (att.type() == Attribute.STRING) {
        atts.setElementAt(new Attribute(att.name(), (FastVector)null), i);
      }
    }
    Instances result = new Instances(relationName(), atts, 0);
    result.m_ClassIndex = m_ClassIndex;
		if (data_Debug) {
			System.err.println("StringFreeInstances " + numInstances() + " isnts "
					+ m_NumDocuments + " docs.");
			try {
				throw new RuntimeException("");
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
    return result;
  }

  /**
   * Adds one instance to the end of the set. 
   * Shallow copies instance before it is added. Increases the
   * size of the dataset if it is not large enough. Does not
   * check if the instance is compatible with the dataset.
   *
   * @param instance the instance to be added
   */
  public void add(/*@non_null@*/ Instance instance) {

    Instance newInstance = (Instance)instance.copy();

    newInstance.setDataset(this);
    m_Instances.addElement(newInstance);
  }

  /**
   * Returns an attribute.
   *
   * @param index the attribute's index
   * @return the attribute at the given position
   */
  //@ requires 0 <= index;
  //@ requires index < m_Attributes.size();
  //@ ensures \result != null;
  public /*@pure@*/ Attribute attribute(int index) {
    
    return (Attribute) m_Attributes.elementAt(index);
  }

  /**
   * Returns an attribute given its name. If there is more than
   * one attribute with the same name, it returns the first one.
   * Returns null if the attribute can't be found.
   *
   * @param name the attribute's name
   * @return the attribute with the given name, null if the
   * attribute can't be found
   */ 
  public /*@pure@*/ Attribute attribute(String name) {
    
    for (int i = 0; i < numAttributes(); i++) {
      if (attribute(i).name().equals(name)) {
	return attribute(i);
      }
    }
    return null;
  }
  
  public int attributeIndex(String name) {	 
	 for (int i = 0; i < numAttributes(); i++) {
		 if (attribute(i).name().equals(name)) {
			 return i;
	     }
	 }
	 
	 return -1;
  }

  /**
   * Checks for string attributes in the dataset
   *
   * @return true if string attributes are present, false otherwise
   */
  public /*@pure@*/ boolean checkForStringAttributes() {

    int i = 0;
   
    while (i < m_Attributes.size()) {
      if (attribute(i++).isString()) {
	return true;
      }
    }
    return false;
  }

  /**
   * Checks if the given instance is compatible
   * with this dataset. Only looks at the size of
   * the instance and the ranges of the values for 
   * nominal and string attributes.
   *
   * @return true if the instance is compatible with the dataset 
   */
  public /*@pure@*/ boolean checkInstance(Instance instance) {

    if (instance.numAttributes() != numAttributes()) {
      return false;
    }
    for (int i = 0; i < numAttributes(); i++) {
      if (instance.isMissing(i)) {
	continue;
      } else if (attribute(i).isNominal() ||
		 attribute(i).isString()) {
	if (!(Utils.eq(instance.value(i),
		       (double)(int)instance.value(i)))) {
	  return false;
	} else if (Utils.sm(instance.value(i), 0) ||
		   Utils.gr(instance.value(i),
			    attribute(i).numValues())) {
	  return false;
	}
      }
    }
    return true;
  }
	
  /**
   * Returns the class attribute.
   *
   * @return the class attribute
   * @exception UnassignedClassException if the class is not set
   */
  //@ requires classIndex() >= 0;
  public /*@pure@*/ Attribute classAttribute() {

    if (m_ClassIndex < 0) {
      throw new UnassignedClassException("Class index is negative (not set)!");
    }
    return attribute(m_ClassIndex);
  }

  /**
   * Returns the class attribute's index. Returns negative number
   * if it's undefined.
   *
   * @return the class index as an integer
   */
  // ensures \result == m_ClassIndex;
  public /*@pure@*/ int classIndex() {
    
    return m_ClassIndex;
  }
 
  /**
   * Compactifies the set of instances. Decreases the capacity of
   * the set so that it matches the number of instances in the set.
   */
  public void compactify() {

    m_Instances.trimToSize();
  }

  /**
   * Removes all instances from the set.
   */
  public void delete() {
    
    m_Instances = new FastVector();
  }

  /**
   * Removes an instance at the given position from the set.
   *
   * @param index the instance's position
   */
  //@ requires 0 <= index && index < numInstances();
  public void delete(int index) {
    
    m_Instances.removeElementAt(index);
  }

  /**
   * Deletes an attribute at the given position 
   * (0 to numAttributes() - 1). A deep copy of the attribute
   * information is performed before the attribute is deleted.
   *
   * @param pos the attribute's position
   * @exception IllegalArgumentException if the given index is out of range 
   *            or the class attribute is being deleted
   */
  //@ requires 0 <= position && position < numAttributes();
  //@ requires position != classIndex();
  public void deleteAttributeAt(int position) {
	 
    if ((position < 0) || (position >= m_Attributes.size())) {
      throw new IllegalArgumentException("Index out of range");
    }
    if (position == m_ClassIndex) {
      throw new IllegalArgumentException("Can't delete class attribute");
    }
    freshAttributeInfo();
    if (m_ClassIndex > position) {
      m_ClassIndex--;
    }
    m_Attributes.removeElementAt(position);
    for (int i = position; i < m_Attributes.size(); i++) {
      Attribute current = (Attribute)m_Attributes.elementAt(i);
      current.setIndex(current.index() - 1);
    }
    for (int i = 0; i < numInstances(); i++) {
      instance(i).forceDeleteAttributeAt(position); 
    }
  }

  /**
   * Deletes all string attributes in the dataset. A deep copy of the attribute
   * information is performed before an attribute is deleted.
   *
   * @exception IllegalArgumentException if string attribute couldn't be 
   * successfully deleted (probably because it is the class attribute).
   */
  public void deleteStringAttributes() {

    int i = 0;
    while (i < m_Attributes.size()) {
      if (attribute(i).isString()) {
	deleteAttributeAt(i);
      } else {
	i++;
      }
    }
  }

  /**
   * Removes all instances with missing values for a particular
   * attribute from the dataset.
   *
   * @param attIndex the attribute's index
   */
  //@ requires 0 <= attIndex && attIndex < numAttributes();
  public void deleteWithMissing(int attIndex) {

    FastVector newInstances = new FastVector(numInstances());

    for (int i = 0; i < numInstances(); i++) {
      if (!instance(i).isMissing(attIndex)) {
	newInstances.addElement(instance(i));
      }
    }
    m_Instances = newInstances;
  }

  /**
   * Removes all instances with missing values for a particular
   * attribute from the dataset.
   *
   * @param att the attribute
   */
  public void deleteWithMissing(/*@non_null@*/ Attribute att) {

    deleteWithMissing(att.index());
  }

  /**
   * Removes all instances with a missing class value
   * from the dataset.
   *
   * @exception UnassignedClassException if class is not set
   */
  public void deleteWithMissingClass() {

    if (m_ClassIndex < 0) {
      throw new UnassignedClassException("Class index is negative (not set)!");
    }
    deleteWithMissing(m_ClassIndex);
  }

  /**
   * Returns an enumeration of all the attributes.
   *
   * @return enumeration of all the attributes.
   */
  public /*@non_null pure@*/ Enumeration enumerateAttributes() {
  	//throw new RuntimeException("here");
    return m_Attributes.elements(m_ClassIndex);
  }

  /**
   * Returns an enumeration of all instances in the dataset.
   *
   * @return enumeration of all instances in the dataset
   */
  public /*@non_null pure@*/ Enumeration enumerateInstances() {

    return m_Instances.elements();
  }

  /**
   * Checks if two headers are equivalent.
   *
   * @param dataset another dataset
   * @return true if the header of the given dataset is equivalent 
   * to this header
   */
  public /*@pure@*/ boolean equalHeaders(Instances dataset){

    // Check class and all attributes
    if (m_ClassIndex != dataset.m_ClassIndex) {
      return false;
    }
    if (m_Attributes.size() != dataset.m_Attributes.size()) {
      return false;
    }
    for (int i = 0; i < m_Attributes.size(); i++) {
      if (!(attribute(i).equals(dataset.attribute(i)))) {
	return false;
      }
    }
    return true;
  }
 
  /**
   * Returns the first instance in the set.
   *
   * @return the first instance in the set
   */
  //@ requires numInstances() > 0;
  public /*@non_null pure@*/ Instance firstInstance() {
    
    return (Instance)m_Instances.firstElement();
  }

  /**
   * Returns a random number generator. The initial seed of the random
   * number generator depends on the given seed and the hash code of
   * a string representation of a instances chosen based on the given
   * seed. 
   *
   * @param seed the given seed
   * @return the random number generator
   */
  public Random getRandomNumberGenerator(long seed) {

    Random r = new Random(seed);
    r.setSeed(instance(r.nextInt(numInstances())).toString().hashCode() + seed);
    return r;
  }
 
  /**
   * Inserts an attribute at the given position (0 to 
   * numAttributes()) and sets all values to be missing.
   * Shallow copies the attribute before it is inserted, and performs
   * a deep copy of the existing attribute information.
   *
   * @param att the attribute to be inserted
   * @param pos the attribute's position
   * @exception IllegalArgumentException if the given index is out of range
   */
  //@ requires 0 <= position;
  //@ requires position <= numAttributes();
  public void insertAttributeAt(/*@non_null@*/ Attribute att, int position) {
	 
    if ((position < 0) ||
	(position > m_Attributes.size())) {
      throw new IllegalArgumentException("Index out of range");
    }
    att = (Attribute)att.copy();
    freshAttributeInfo();
    att.setIndex(position);
    m_Attributes.insertElementAt(att, position);
    for (int i = position + 1; i < m_Attributes.size(); i++) {
      Attribute current = (Attribute)m_Attributes.elementAt(i);
      current.setIndex(current.index() + 1);
    }
    for (int i = 0; i < numInstances(); i++) {
      instance(i).forceInsertAttributeAt(position);
    }
    if (m_ClassIndex >= position) {
      m_ClassIndex++;
    }
  }

  /**
   * Returns the instance at the given position.
   *
   * @param index the instance's index
   * @return the instance at the given position
   */
  //@ requires 0 <= index;
  //@ requires index < numInstances();
  public /*@non_null pure@*/ Instance instance(int index) {

    return (Instance)m_Instances.elementAt(index);
  }

  /**
   * Returns the kth-smallest attribute value of a numeric attribute.
   * Note that calling this method will change the order of the data!
   *
   * @param att the Attribute object
   * @param k the value of k
   * @return the kth-smallest value
   */
  public double kthSmallestValue(Attribute att, int k) {

    return kthSmallestValue(att.index(), k);
  }

  /**
   * Returns the kth-smallest attribute value of a numeric attribute.
   * Note that calling this method will change the order of the data!
   * The number of non-missing values in the data must be as least
   * as last as k for this to work.
   *
   * @param attIndex the attribute's index
   * @param k the value of k
   * @return the kth-smallest value
   */
  public double kthSmallestValue(int attIndex, int k) {
    
    if (!attribute(attIndex).isNumeric()) {
      throw new IllegalArgumentException("Instances: attribute must be numeric to compute kth-smallest value.");
    }

    int i,j;

    // move all instances with missing values to end
    j = numInstances() - 1;
    i = 0;
    while (i <= j) {
      if (instance(j).isMissing(attIndex)) {
	j--;
      } else {
	if (instance(i).isMissing(attIndex)) {
	  swap(i,j);
	  j--;
	}
	i++;
      }
    }

    if ((k < 0) || (k > j)) {
      throw new IllegalArgumentException("Instances: value for k for computing kth-smallest value too large.");
    }

    return instance(select(attIndex, 0, j, k)).value(attIndex);
  }

  /**
   * Returns the last instance in the set.
   *
   * @return the last instance in the set
   */
  //@ requires numInstances() > 0;
  public /*@non_null pure@*/ Instance lastInstance() {
    
    return (Instance)m_Instances.lastElement();
  }

  /**
   * Returns the mean (mode) for a numeric (nominal) attribute as
   * a floating-point value. Returns 0 if the attribute is neither nominal nor 
   * numeric. If all values are missing it returns zero.
   *
   * @param attIndex the attribute's index
   * @return the mean or the mode
   */
  public /*@pure@*/ float meanOrMode(int attIndex) {

    float result, found;
    int [] counts;

    if (attribute(attIndex).isNumeric()) {
      result = found = 0;
      for (int j = 0; j < numInstances(); j++) {
	if (!instance(j).isMissing(attIndex)) {
	  found += instance(j).weight();
	  result += instance(j).weight()*instance(j).value(attIndex);
	}
      }
      if (found <= 0) {
	return 0;
      } else {
	return result / found;
      }
    } else if (attribute(attIndex).isNominal()) {
      counts = new int[attribute(attIndex).numValues()];
      for (int j = 0; j < numInstances(); j++) {
	if (!instance(j).isMissing(attIndex)) {
	  counts[(int) instance(j).value(attIndex)] += instance(j).weight();
	}
      }
      return (float)Utils.maxIndex(counts);
    } else {
      return 0;
    }
  }

  /**
   * Returns the mean (mode) for a numeric (nominal) attribute as a
   * floating-point value.  Returns 0 if the attribute is neither
   * nominal nor numeric.  If all values are missing it returns zero.
   *
   * @param att the attribute
   * @return the mean or the mode 
   */
  public /*@pure@*/ double meanOrMode(Attribute att) {

    return meanOrMode(att.index());
  }

  /**
   * Returns the number of attributes.
   *
   * @return the number of attributes as an integer
   */
  //@ ensures \result == m_Attributes.size();
  public /*@pure@*/ int numAttributes() {

    return m_Attributes.size();
  }

  /**
   * Returns the number of class labels.
   *
   * @return the number of class labels as an integer if the class 
   * attribute is nominal, 1 otherwise.
   * @exception UnassignedClassException if the class is not set
   */
  //@ requires classIndex() >= 0;
  public /*@pure@*/ int numClasses() {
    
    if (m_ClassIndex < 0) {
      throw new UnassignedClassException("Class index is negative (not set)!");
    }
    if (!classAttribute().isNominal()) {
      return 1;
    } else {
      return classAttribute().numValues();
    }
  }

  /**
   * Returns the number of distinct values of a given attribute.
   * Returns the number of instances if the attribute is a
   * string attribute. The value 'missing' is not counted.
   *
   * @param attIndex the attribute
   * @return the number of distinct values of a given attribute
   */
  //@ requires 0 <= attIndex;
  //@ requires attIndex < numAttributes();
  public /*@pure@*/ int numDistinctValues(int attIndex) {

    if (attribute(attIndex).isNumeric()) {
      double [] attVals = attributeToDoubleArray(attIndex);
      int [] sorted = Utils.sort(attVals);
      double prev = 0;
      int counter = 0;
      for (int i = 0; i < sorted.length; i++) {
	Instance current = instance(sorted[i]);
	if (current.isMissing(attIndex)) {
	  break;
	}
	if ((i == 0) || 
	    (current.value(attIndex) > prev)) {
	  prev = current.value(attIndex);
	  counter++;
	}
      }
      return counter;
    } else {
      return attribute(attIndex).numValues();
    }
  }

  /**
   * Returns the number of distinct values of a given attribute.
   * Returns the number of instances if the attribute is a
   * string attribute. The value 'missing' is not counted.
   *
   * @param att the attribute
   * @return the number of distinct values of a given attribute
   */
  public /*@pure@*/ int numDistinctValues(/*@non_null@*/Attribute att) {

    return numDistinctValues(att.index());
  }
  
  /**
   * Returns the number of instances in the dataset.
   *
   * @return the number of instances in the dataset as an integer
   */
  //@ ensures \result == m_Instances.size();
  public /*@pure@*/ int numInstances() {

    return m_Instances.size();
  }

  /**
   * Shuffles the instances in the set so that they are ordered 
   * randomly.
   *
   * @param random a random number generator
   */
  public void randomize(Random random) {

    for (int j = numInstances() - 1; j > 0; j--)
      swap(j, random.nextInt(j+1));
  }

  /**
   * Reads a single instance from the reader and appends it
   * to the dataset.  Automatically expands the dataset if it
   * is not large enough to hold the instance. This method does
   * not check for carriage return at the end of the line.
   *
   * @param reader the reader 
   * @return false if end of file has been reached
   * @exception IOException if the information is not read 
   * successfully
   */ 
  public boolean readInstance(Reader reader) 
       throws IOException {

    StreamTokenizer tokenizer = new StreamTokenizer(reader);
    
    initTokenizer(tokenizer);
    return getInstance(tokenizer, false);
  }    

  /**
   * Returns the relation's name.
   *
   * @return the relation's name as a string
   */
  //@ ensures \result == m_RelationName;
  public /*@pure@*/ String relationName() {

    return m_RelationName;
  }

  /**
   * Renames an attribute. This change only affects this
   * dataset.
   *
   * @param att the attribute's index
   * @param name the new name
   */
  public void renameAttribute(int att, String name) {

    Attribute newAtt = attribute(att).copy(name);
    FastVector newVec = new FastVector(numAttributes());

    for (int i = 0; i < numAttributes(); i++) {
      if (i == att) {
	newVec.addElement(newAtt);
      } else {
	newVec.addElement(attribute(i));
      }
    }
    m_Attributes = newVec;
  }

  /**
   * Renames an attribute. This change only affects this
   * dataset.
   *
   * @param att the attribute
   * @param name the new name
   */
  public void renameAttribute(Attribute att, String name) {

    renameAttribute(att.index(), name);
  }

  /**
   * Renames the value of a nominal (or string) attribute value. This
   * change only affects this dataset.
   *
   * @param att the attribute's index
   * @param val the value's index
   * @param name the new name 
   */
  public void renameAttributeValue(int att, int val, String name) {

    Attribute newAtt = (Attribute)attribute(att).copy();
    FastVector newVec = new FastVector(numAttributes());

    newAtt.setValue(val, name);
    for (int i = 0; i < numAttributes(); i++) {
      if (i == att) {
	newVec.addElement(newAtt);
      } else {
	newVec.addElement(attribute(i));
      }
    }
    m_Attributes = newVec;
  }

  /**
   * Renames the value of a nominal (or string) attribute value. This
   * change only affects this dataset.
   *
   * @param att the attribute
   * @param val the value
   * @param name the new name
   */
  public void renameAttributeValue(Attribute att, String val, 
                                         String name) {

    int v = att.indexOfValue(val);
    if (v == -1) throw new IllegalArgumentException(val + " not found");
    renameAttributeValue(att.index(), v, name);
  }

  /**
   * Creates a new dataset of the same size using random sampling
   * with replacement.
   *
   * @param random a random number generator
   * @return the new dataset
   */
  public Instances resample(Random random) {

    Instances newData = new Instances(this, numInstances());
    while (newData.numInstances() < numInstances()) {
      newData.add(instance(random.nextInt(numInstances())));
    }
    return newData;
  }

  /**
   * Creates a new dataset of the same size using random sampling
   * with replacement according to the current instance weights. The
   * weights of the instances in the new dataset are set to one.
   *
   * @param random a random number generator
   * @return the new dataset
   */
  public Instances resampleWithWeights(Random random) {

    double [] weights = new double[numInstances()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = instance(i).weight();
    }
    return resampleWithWeights(random, weights);
  }


  /**
   * Creates a new dataset of the same size using random sampling
   * with replacement according to the given weight vector. The
   * weights of the instances in the new dataset are set to one.
   * The length of the weight vector has to be the same as the
   * number of instances in the dataset, and all weights have to
   * be positive.
   *
   * @param random a random number generator
   * @param weights the weight vector
   * @return the new dataset
   * @exception IllegalArgumentException if the weights array is of the wrong
   * length or contains negative weights.
   */
  public Instances resampleWithWeights(Random random, 
					     double[] weights) {

    if (weights.length != numInstances()) {
      throw new IllegalArgumentException("weights.length != numInstances.");
    }
    Instances newData = new Instances(this, numInstances());
    if (numInstances() == 0) {
      return newData;
    }
    double[] probabilities = new double[numInstances()];
    double sumProbs = 0, sumOfWeights = Utils.sum(weights);
    for (int i = 0; i < numInstances(); i++) {
      sumProbs += random.nextDouble();
      probabilities[i] = sumProbs;
    }
    Utils.normalize(probabilities, sumProbs / sumOfWeights);

    // Make sure that rounding errors don't mess things up
    probabilities[numInstances() - 1] = sumOfWeights;
    int k = 0; int l = 0;
    sumProbs = 0;
    while ((k < numInstances() && (l < numInstances()))) {
      if (weights[l] < 0) {
	throw new IllegalArgumentException("Weights have to be positive.");
      }
      sumProbs += weights[l];
      while ((k < numInstances()) &&
	     (probabilities[k] <= sumProbs)) { 
	newData.add(instance(l));
	newData.instance(k).setWeight(1);
	k++;
      }
      l++;
    }
    return newData;
  }

  /** 
   * Sets the class attribute.
   *
   * @param att attribute to be the class
   */
  public void setClass(Attribute att) {

    m_ClassIndex = att.index();
  }

  public int getNumDocuments() {
		return m_NumDocuments;
	}

	public void setNumDocuments(int numDocuments) {
		m_NumDocuments = numDocuments;
	}

	/** 
   * Sets the class index of the set.
   * If the class index is negative there is assumed to be no class.
   * (ie. it is undefined)
   *
   * @param classIndex the new class index
   * @exception IllegalArgumentException if the class index is too big or < 0
   */
  public void setClassIndex(int classIndex) {

    if (classIndex >= numAttributes()) {
      throw new IllegalArgumentException("Invalid class index: " + classIndex);
    }
    m_ClassIndex = classIndex;
  }

  /**
   * Sets the relation's name.
   *
   * @param newName the new relation name.
   */
  public void setRelationName(/*@non_null@*/String newName) {
    
    m_RelationName = newName;
  }

  /**
   * Sorts the instances based on an attribute. For numeric attributes, 
   * instances are sorted in ascending order. For nominal attributes, 
   * instances are sorted based on the attribute label ordering 
   * specified in the header. Instances with missing values for the 
   * attribute are placed at the end of the dataset.
   *
   * @param attIndex the attribute's index
   */
  public void sort(int attIndex) {

    int i,j;

    // move all instances with missing values to end
    j = numInstances() - 1;
    i = 0;
    while (i <= j) {
      if (instance(j).isMissing(attIndex)) {
	j--;
      } else {
	if (instance(i).isMissing(attIndex)) {
	  swap(i,j);
	  j--;
	}
	i++;
      }
    }
    quickSort(attIndex, 0, j);
  }

  /**
   * Sorts the instances based on an attribute. For numeric attributes, 
   * instances are sorted into ascending order. For nominal attributes, 
   * instances are sorted based on the attribute label ordering 
   * specified in the header. Instances with missing values for the 
   * attribute are placed at the end of the dataset.
   *
   * @param att the attribute
   */
  public void sort(Attribute att) {

    sort(att.index());
  }

  /**
   * Stratifies a set of instances according to its class values 
   * if the class attribute is nominal (so that afterwards a 
   * stratified cross-validation can be performed).
   *
   * @param numFolds the number of folds in the cross-validation
   * @exception UnassignedClassException if the class is not set
   */
  public void stratify(int numFolds) {
    
    if (numFolds <= 0) {
      throw new IllegalArgumentException("Number of folds must be greater than 1");
    }
    if (m_ClassIndex < 0) {
      throw new UnassignedClassException("Class index is negative (not set)!");
    }
    if (classAttribute().isNominal()) {

      // sort by class
      int index = 1;
      while (index < numInstances()) {
	Instance instance1 = instance(index - 1);
	for (int j = index; j < numInstances(); j++) {
	  Instance instance2 = instance(j);
	  if ((instance1.classValue() == instance2.classValue()) ||
	      (instance1.classIsMissing() && 
	       instance2.classIsMissing())) {
	    swap(index,j);
	    index++;
	  }
	}
	index++;
      }
      stratStep(numFolds);
    }
  }
 
  /**
   * Computes the sum of all the instances' weights.
   *
   * @return the sum of all the instances' weights as a double
   */
  public /*@pure@*/ double sumOfWeights() {
    
    double sum = 0;
 
    for (int i = 0; i < numInstances(); i++) {
      sum += instance(i).weight();
    }
    return sum;
  }
  
  /**
   * Computes the sum of the uncovered instances' weights.
   *
   * @param covered the covered attribute
   * @return the sum of all the instances' weights as a double
   */
  public /*@pure@*/ double sumOfUncovWeights(Attribute covered) {
    
    double sum = 0;
    int positive = covered.indexOfValue("+");
    for (int i = 0; i < numInstances(); i++) {
    	double cov = instance(i).value(covered);
    	boolean notCovered = Double.isNaN(cov) || (int)cov != positive;
    	if(notCovered)
    		sum += instance(i).weight();
    }
    return sum;
  }

  /**
   * Creates the test set for one fold of a cross-validation on 
   * the dataset.
   *
   * @param numFolds the number of folds in the cross-validation. Must
   * be greater than 1.
   * @param numFold 0 for the first fold, 1 for the second, ...
   * @return the test set as a set of weighted instances
   * @exception IllegalArgumentException if the number of folds is less than 2
   * or greater than the number of instances.
   */
  //@ requires 2 <= numFolds && numFolds < numInstances();
  //@ requires 0 <= numFold && numFold < numFolds;
  public Instances testCV(int numFolds, int numFold) {

    int numInstForFold, first, offset;
    Instances test;
    
    if (numFolds < 2) {
      throw new IllegalArgumentException("Number of folds must be at least 2!");
    }
    if (numFolds > numInstances()) {
      throw new IllegalArgumentException("Can't have more folds than instances!");
    }
    numInstForFold = numInstances() / numFolds;
    if (numFold < numInstances() % numFolds){
      numInstForFold++;
      offset = numFold;
    }else
      offset = numInstances() % numFolds;
    test = new Instances(this, numInstForFold);
    first = numFold * (numInstances() / numFolds) + offset;
    copyInstances(first, test, numInstForFold);
    return test;
  }
 
  /**
   * Returns the dataset as a string in ARFF format. Strings
   * are quoted if they contain whitespace characters, or if they
   * are a question mark.
   *
   * @return the dataset in ARFF format as a string
   */
  public String toString() {
    
    return toString(true);
  }
  
  /**
   * Returns the dataset as a string in ARFF format. The
   * caller can select whether to print the attributes.
   *
   * @return the dataset in ARFF format as a string
   */
  public String toString(boolean printAttributes) {

    StringBuffer text = new StringBuffer();
    if(printAttributes){
      text.append(ARFF_RELATION).append(" ").append(Utils.quote(m_RelationName))
        .append("\n\n");
    
      for(int i = 0; i < numAttributes(); i++){
        text.append(attribute(i)).append("\n");
      }
    }
    text.append("\n").append(ARFF_DATA).append("\n");
    for(int i = 0; i < numInstances(); i++){
      text.append(instance(i));
      if(i < numInstances() - 1){
        text.append('\n');
      }
    }
    return text.toString();
  }

	public String printHeader() {
		StringBuffer text = new StringBuffer();
		text.append(ARFF_RELATION).append(" ").append(Utils.quote(m_RelationName))
				.append("\n\n");

		for (int i = 0; i < numAttributes(); i++) {
			text.append(attribute(i)).append("\n");
		}
		return text.toString();
	}

  /**
   * Creates the training set for one fold of a cross-validation 
   * on the dataset. 
   *
   * @param numFolds the number of folds in the cross-validation. Must
   * be greater than 1.
   * @param numFold 0 for the first fold, 1 for the second, ...
   * @return the training set 
   * @exception IllegalArgumentException if the number of folds is less than 2
   * or greater than the number of instances.
   */
  //@ requires 2 <= numFolds && numFolds < numInstances();
  //@ requires 0 <= numFold && numFold < numFolds;
  public Instances trainCV(int numFolds, int numFold) {

    int numInstForFold, first, offset;
    Instances train;
 
    if (numFolds < 2) {
      throw new IllegalArgumentException("Number of folds must be at least 2!");
    }
    if (numFolds > numInstances()) {
      throw new IllegalArgumentException("Can't have more folds than instances!");
    }
    numInstForFold = numInstances() / numFolds;
    if (numFold < numInstances() % numFolds) {
      numInstForFold++;
      offset = numFold;
    }else
      offset = numInstances() % numFolds;
    train = new Instances(this, numInstances() - numInstForFold);
    first = numFold * (numInstances() / numFolds) + offset;
    copyInstances(0, train, first);
    copyInstances(first + numInstForFold, train,
		  numInstances() - first - numInstForFold);

    return train;
  }

  /**
   * Creates the training set for one fold of a cross-validation 
   * on the dataset. The data is subsequently randomized based
   * on the given random number generator.
   *
   * @param numFolds the number of folds in the cross-validation. Must
   * be greater than 1.
   * @param numFold 0 for the first fold, 1 for the second, ...
   * @param random the random number generator
   * @return the training set 
   * @exception IllegalArgumentException if the number of folds is less than 2
   * or greater than the number of instances.
   */
  //@ requires 2 <= numFolds && numFolds < numInstances();
  //@ requires 0 <= numFold && numFold < numFolds;
  public Instances trainCV(int numFolds, int numFold, Random random) {

    Instances train = trainCV(numFolds, numFold);
    train.randomize(random);
    return train;
  }

  /**
   * Computes the variance for a numeric attribute.
   *
   * @param attIndex the numeric attribute
   * @return the variance if the attribute is numeric
   * @exception IllegalArgumentException if the attribute is not numeric
   */
  public /*@pure@*/ double variance(int attIndex) {
  
    double sum = 0, sumSquared = 0, sumOfWeights = 0;

    if (!attribute(attIndex).isNumeric()) {
      throw new IllegalArgumentException("Can't compute variance because attribute is " +
			  "not numeric!");
    }
    for (int i = 0; i < numInstances(); i++) {
      if (!instance(i).isMissing(attIndex)) {
	sum += instance(i).weight() * 
	  instance(i).value(attIndex);
	sumSquared += instance(i).weight() * 
	  instance(i).value(attIndex) *
	  instance(i).value(attIndex);
	sumOfWeights += instance(i).weight();
      }
    }
    if (sumOfWeights <= 1) {
      return 0;
    }
    double result = (sumSquared - (sum * sum / sumOfWeights)) / 
      (sumOfWeights - 1);

    // We don't like negative variance
    if (result < 0) {
      return 0;
    } else {
      return result;
    }
  }

  /**
   * Computes the variance for a numeric attribute.
   *
   * @param att the numeric attribute
   * @return the variance if the attribute is numeric
   * @exception IllegalArgumentException if the attribute is not numeric
   */
  public /*@pure@*/ double variance(Attribute att) {
    
    return variance(att.index());
  }
  
  /**
   * Calculates summary statistics on the values that appear in this
   * set of instances for a specified attribute.
   *
   * @param index the index of the attribute to summarize.
   * @return an AttributeStats object with it's fields calculated.
   */
  //@ requires 0 <= index && index < numAttributes();
  public AttributeStats attributeStats(int index) {

    AttributeStats result = new AttributeStats();
    if (attribute(index).isNominal()) {
      result.nominalCounts = new int [attribute(index).numValues()];
    }
    if (attribute(index).isNumeric()) {
      result.numericStats = new reconcile.weka.experiment.Stats();
    }
    result.totalCount = numInstances();

    double [] attVals = attributeToDoubleArray(index);
    int [] sorted = Utils.sort(attVals);
    int currentCount = 0;
    double prev = Instance.missingValue();
    for (int j = 0; j < numInstances(); j++) {
      Instance current = instance(sorted[j]);
      if (current.isMissing(index)) {
	result.missingCount = numInstances() - j;
	break;
      }
      if (current.value(index) == prev) {
	currentCount++;
      } else {
	result.addDistinct(prev, currentCount);
	currentCount = 1;
	prev = current.value(index);
      }
    }
    result.addDistinct(prev, currentCount);
    result.distinctCount--; // So we don't count "missing" as a value 
    return result;
  }
  
  /**
   * Gets the value of all instances in this dataset for a particular
   * attribute. Useful in conjunction with Utils.sort to allow iterating
   * through the dataset in sorted order for some attribute.
   *
   * @param index the index of the attribute.
   * @return an array containing the value of the desired attribute for
   * each instance in the dataset. 
   */
  //@ requires 0 <= index && index < numAttributes();
  public /*@pure@*/ double [] attributeToDoubleArray(int index) {

    double [] result = new double[numInstances()];
    for (int i = 0; i < result.length; i++) {
      result[i] = instance(i).value(index);
    }
    return result;
  }

  /**
   * Generates a string summarizing the set of instances. Gives a breakdown
   * for each attribute indicating the number of missing/discrete/unique
   * values and other information.
   *
   * @return a string summarizing the dataset
   */
  public String toSummaryString() {

    StringBuffer result = new StringBuffer();
    result.append("Relation Name:  ").append(relationName()).append('\n');
    result.append("Num Instances:  ").append(numInstances()).append('\n');
    result.append("Num Attributes: ").append(numAttributes()).append('\n');
    result.append('\n');

    result.append(Utils.padLeft("", 5)).append(Utils.padRight("Name", 25));
    result.append(Utils.padLeft("Type", 5)).append(Utils.padLeft("Nom", 5));
    result.append(Utils.padLeft("Int", 5)).append(Utils.padLeft("Real", 5));
    result.append(Utils.padLeft("Missing", 12));
    result.append(Utils.padLeft("Unique", 12));
    result.append(Utils.padLeft("Dist", 6)).append('\n');
    for (int i = 0; i < numAttributes(); i++) {
      Attribute a = attribute(i);
      AttributeStats as = attributeStats(i);
      result.append(Utils.padLeft("" + (i + 1), 4)).append(' ');
      result.append(Utils.padRight(a.name(), 25)).append(' ');
      long percent;
      switch (a.type()) {
      case Attribute.NOMINAL:
	result.append(Utils.padLeft("Nom", 4)).append(' ');
	percent = Math.round(100.0 * as.intCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	result.append(Utils.padLeft("" + 0, 3)).append("% ");
	percent = Math.round(100.0 * as.realCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	break;
      case Attribute.NUMERIC:
	result.append(Utils.padLeft("Num", 4)).append(' ');
	result.append(Utils.padLeft("" + 0, 3)).append("% ");
	percent = Math.round(100.0 * as.intCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	percent = Math.round(100.0 * as.realCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	break;
      case Attribute.DATE:
	result.append(Utils.padLeft("Dat", 4)).append(' ');
	result.append(Utils.padLeft("" + 0, 3)).append("% ");
	percent = Math.round(100.0 * as.intCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	percent = Math.round(100.0 * as.realCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	break;
      case Attribute.STRING:
	result.append(Utils.padLeft("Str", 4)).append(' ');
	percent = Math.round(100.0 * as.intCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	result.append(Utils.padLeft("" + 0, 3)).append("% ");
	percent = Math.round(100.0 * as.realCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	break;
      default:
	result.append(Utils.padLeft("???", 4)).append(' ');
	result.append(Utils.padLeft("" + 0, 3)).append("% ");
	percent = Math.round(100.0 * as.intCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	percent = Math.round(100.0 * as.realCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	break;
      }
      result.append(Utils.padLeft("" + as.missingCount, 5)).append(" /");
      percent = Math.round(100.0 * as.missingCount / as.totalCount);
      result.append(Utils.padLeft("" + percent, 3)).append("% ");
      result.append(Utils.padLeft("" + as.uniqueCount, 5)).append(" /");
      percent = Math.round(100.0 * as.uniqueCount / as.totalCount);
      result.append(Utils.padLeft("" + percent, 3)).append("% ");
      result.append(Utils.padLeft("" + as.distinctCount, 5)).append(' ');
      result.append('\n');
    }
    return result.toString();
  }
  
  /**
   * Reads a single instance using the tokenizer and appends it
   * to the dataset. Automatically expands the dataset if it
   * is not large enough to hold the instance.
   *
   * @param tokenizer the tokenizer to be used
   * @param flag if method should test for carriage return after 
   * each instance
   * @return false if end of file has been reached
   * @exception IOException if the information is not read 
   * successfully
   */ 
  protected boolean getInstance(StreamTokenizer tokenizer, 
				boolean flag) 
       throws IOException {
    
    // Check if any attributes have been declared.
    if (m_Attributes.size() == 0) {
      errms(tokenizer,"no header information available");
    }

    // Check if end of file reached.
    getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      return false;
    }
    
    // Parse instance
    if (tokenizer.ttype == '{') {
      return getInstanceSparse(tokenizer, flag);
    } else {
      return getInstanceFull(tokenizer, flag);
    }
  }

  /**
   * Reads a single instance using the tokenizer and appends it
   * to the dataset. Automatically expands the dataset if it
   * is not large enough to hold the instance.
   *
   * @param tokenizer the tokenizer to be used
   * @param flag if method should test for carriage return after 
   * each instance
   * @return false if end of file has been reached
   * @exception IOException if the information is not read 
   * successfully
   */ 
  protected boolean getInstanceSparse(StreamTokenizer tokenizer, 
				      boolean flag) 
       throws IOException {

    int valIndex, numValues = 0, maxIndex = -1;
    
    // Get values
    do {
      
      // Get index
      getIndex(tokenizer);
      if (tokenizer.ttype == '}') {
	break;
      }
       
      // Is index valid?
      try{
	m_IndicesBuffer[numValues] = Integer.valueOf(tokenizer.sval).intValue();
      } catch (NumberFormatException e) {
	errms(tokenizer,"index number expected");
      }
      if (m_IndicesBuffer[numValues] <= maxIndex) {
	errms(tokenizer,"indices have to be ordered");
      }
      if ((m_IndicesBuffer[numValues] < 0) || 
	  (m_IndicesBuffer[numValues] >= numAttributes())) {
	errms(tokenizer,"index out of bounds");
      }
      maxIndex = m_IndicesBuffer[numValues];

      // Get value;
      getNextToken(tokenizer);

      // Check if value is missing.
      if  (tokenizer.ttype == '?') {
	m_ValueBuffer[numValues] = Instance.missingValue();
      } else {

	// Check if token is valid.
	if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
	  errms(tokenizer,"not a valid value");
	}
        switch (attribute(m_IndicesBuffer[numValues]).type()) {
          case Attribute.NOMINAL:
            // Check if value appears in header.
            valIndex = 
              attribute(m_IndicesBuffer[numValues]).indexOfValue(tokenizer.sval);
            if (valIndex == -1) {
              errms(tokenizer,"nominal value not declared in header");
            }
            m_ValueBuffer[numValues] = (double)valIndex;
            break;
	case Attribute.NUMERIC:
	  // Check if value is really a number.
	  try{
	    m_ValueBuffer[numValues] = Double.valueOf(tokenizer.sval).
	      doubleValue();
	  } catch (NumberFormatException e) {
	    errms(tokenizer,"number expected");
	  }
          break;
	case Attribute.STRING:
	  m_ValueBuffer[numValues] = 
	    attribute(m_IndicesBuffer[numValues]).addStringValue(tokenizer.sval);
          break;
        case Attribute.DATE:
          try {
            m_ValueBuffer[numValues] = 
              attribute(m_IndicesBuffer[numValues]).parseDate(tokenizer.sval);
          } catch (ParseException e) {
            errms(tokenizer,"unparseable date: " + tokenizer.sval);
          }
          break;
        default:
          errms(tokenizer,"unknown attribute type in column " + m_IndicesBuffer[numValues]);
	}
      }
      numValues++;
    } while (true);
    if (flag) {
      getLastToken(tokenizer,true);
    }
      
    // Add instance to dataset
    float[] tempValues = new float[numValues];
    int[] tempIndices = new int[numValues];
    System.arraycopy(m_ValueBuffer, 0, tempValues, 0, numValues);
    System.arraycopy(m_IndicesBuffer, 0, tempIndices, 0, numValues);
    add(new SparseInstance(1, tempValues, tempIndices, numAttributes()));
    return true;
  }

  /**
   * Reads a single instance using the tokenizer and appends it
   * to the dataset. Automatically expands the dataset if it
   * is not large enough to hold the instance.
   *
   * @param tokenizer the tokenizer to be used
   * @param flag if method should test for carriage return after 
   * each instance
   * @return false if end of file has been reached
   * @exception IOException if the information is not read 
   * successfully
   */ 
  protected boolean getInstanceFull(StreamTokenizer tokenizer, 
				    boolean flag) 
       throws IOException {

    float[] instance = new float[numAttributes()];
    int index;
    
    // Get values for all attributes.
    for (int i = 0; i < numAttributes(); i++){
      
      // Get next token
      if (i > 0) {
	getNextToken(tokenizer);
      }
            
      // Check if value is missing.
      if  (tokenizer.ttype == '?') {
	instance[i] = Instance.missingValue();
      } else {

	// Check if token is valid.
	if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
	  errms(tokenizer,"not a valid value");
	}
        switch (attribute(i).type()) {
        case Attribute.NOMINAL:
	  // Check if value appears in header.
	  index = attribute(i).indexOfValue(tokenizer.sval);
	  if (index == -1) {
	    errms(tokenizer,"nominal value not declared in header");
	  }
	  instance[i] = (float)index;
          break;
	case Attribute.NUMERIC:
	  // Check if value is really a number.
	  try{
	    instance[i] = Float.valueOf(tokenizer.sval).
	      floatValue();
	  } catch (NumberFormatException e) {
	    errms(tokenizer,"number expected");
	  }
          break;
	case Attribute.STRING:
	  instance[i] = attribute(i).addStringValue(tokenizer.sval);
          break;
        case Attribute.DATE:
          try {
            instance[i] = attribute(i).parseDate(tokenizer.sval);
          } catch (ParseException e) {
            errms(tokenizer,"unparseable date: " + tokenizer.sval);
          }
          break;
        default:
          errms(tokenizer,"unknown attribute type in column " + i);
	}
      }
    }
    if (flag) {
      getLastToken(tokenizer,true);
    }
      
    // Add instance to dataset
    add(new Instance(1, instance));
    return true;
  }

  /**
   * Reads and stores header of an ARFF file.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if the information is not read 
   * successfully
   */ 
  protected void readHeader(StreamTokenizer tokenizer) 
     throws IOException {
    
    String attributeName;
    FastVector attributeValues;
    int i;

    // Get name of relation.
    getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      errms(tokenizer,"premature end of file");
    }
    if (ARFF_RELATION.equalsIgnoreCase(tokenizer.sval)) {
      getNextToken(tokenizer);
      m_RelationName = tokenizer.sval;
      getLastToken(tokenizer,false);
    } else {
      errms(tokenizer,"keyword " + ARFF_RELATION + " expected");
    }

    // Create vectors to hold information temporarily.
    m_Attributes = new FastVector();
 
    // Get attribute declarations.
    getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      errms(tokenizer,"premature end of file");
    }

    while (Attribute.ARFF_ATTRIBUTE.equalsIgnoreCase(tokenizer.sval)) {

      // Get attribute name.
      getNextToken(tokenizer);
      attributeName = tokenizer.sval;
      getNextToken(tokenizer);

      // Check if attribute is nominal.
      if (tokenizer.ttype == StreamTokenizer.TT_WORD) {

	// Attribute is real, integer, or string.
	if (tokenizer.sval.equalsIgnoreCase(Attribute.ARFF_ATTRIBUTE_REAL) ||
	    tokenizer.sval.equalsIgnoreCase(Attribute.ARFF_ATTRIBUTE_INTEGER) ||
	    tokenizer.sval.equalsIgnoreCase(Attribute.ARFF_ATTRIBUTE_NUMERIC)) {
	  m_Attributes.addElement(new Attribute(attributeName, numAttributes()));
	  readTillEOL(tokenizer);
	} else if (tokenizer.sval.equalsIgnoreCase(Attribute.ARFF_ATTRIBUTE_STRING)) {
	  m_Attributes.
	    addElement(new Attribute(attributeName, (FastVector)null,
				     numAttributes()));
	  readTillEOL(tokenizer);
	} else if (tokenizer.sval.equalsIgnoreCase(Attribute.ARFF_ATTRIBUTE_DATE)) {
          String format = null;
          if (tokenizer.nextToken() != StreamTokenizer.TT_EOL) {
            if ((tokenizer.ttype != StreamTokenizer.TT_WORD) &&
                (tokenizer.ttype != '\'') &&
                (tokenizer.ttype != '\"')) {
              errms(tokenizer,"not a valid date format");
            }
            format = tokenizer.sval;
            readTillEOL(tokenizer);
          } else {
            tokenizer.pushBack();
          }
	  m_Attributes.addElement(new Attribute(attributeName, format,
                                                numAttributes()));

	} else {
	  errms(tokenizer,"no valid attribute type or invalid "+
		"enumeration");
	}
      } else {

	// Attribute is nominal.
	attributeValues = new FastVector();
	tokenizer.pushBack();
	
	// Get values for nominal attribute.
	if (tokenizer.nextToken() != '{') {
	  errms(tokenizer,"{ expected at beginning of enumeration");
	}
	while (tokenizer.nextToken() != '}') {
	  if (tokenizer.ttype == StreamTokenizer.TT_EOL) {
	    errms(tokenizer,"} expected at end of enumeration");
	  } else {
	    attributeValues.addElement(tokenizer.sval);
	  }
	}
	if (attributeValues.size() == 0) {
	  errms(tokenizer,"no nominal values found");
	}
	m_Attributes.
	  addElement(new Attribute(attributeName, attributeValues,
				   numAttributes()));
      }
      getLastToken(tokenizer,false);
      getFirstToken(tokenizer);
      if (tokenizer.ttype == StreamTokenizer.TT_EOF)
	errms(tokenizer,"premature end of file");
    }

    // Check if data part follows. We can't easily check for EOL.
    if (!ARFF_DATA.equalsIgnoreCase(tokenizer.sval)) {
      errms(tokenizer,"keyword " + ARFF_DATA + " expected");
    }
    
    // Check if any attributes have been declared.
    if (m_Attributes.size() == 0) {
      errms(tokenizer,"no attributes declared");
    }

    // Allocate buffers in case sparse instances have to be read
    m_ValueBuffer = new double[numAttributes()];
    m_IndicesBuffer = new int[numAttributes()];
  }

  /**
   * Copies instances from one set to the end of another 
   * one.
   *
   * @param source the source of the instances
   * @param from the position of the first instance to be copied
   * @param dest the destination for the instances
   * @param num the number of instances to be copied
   */
  //@ requires 0 <= from && from <= numInstances() - num;
  //@ requires 0 <= num;
  protected void copyInstances(int from, /*@non_null@*/ Instances dest, int num) {
    
    for (int i = 0; i < num; i++) {
      dest.add(instance(from + i));
    }
  }
  
  /**
   * Throws error message with line number and last token read.
   *
   * @param theMsg the error message to be thrown
   * @param tokenizer the stream tokenizer
   * @throws IOExcpetion containing the error message
   */
  protected void errms(StreamTokenizer tokenizer, String theMsg) 
       throws IOException {
    
    throw new IOException(theMsg + ", read " + tokenizer.toString());
  }
  
  /**
   * Replaces the attribute information by a clone of
   * itself.
   */
  protected void freshAttributeInfo() {

    m_Attributes = (FastVector) m_Attributes.copyElements();
  }

  /**
   * Gets next token, skipping empty lines.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if reading the next token fails
   */
  protected void getFirstToken(StreamTokenizer tokenizer) 
    throws IOException {
    
    while (tokenizer.nextToken() == StreamTokenizer.TT_EOL){};
    if ((tokenizer.ttype == '\'') ||
	(tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    } else if ((tokenizer.ttype == StreamTokenizer.TT_WORD) &&
	       (tokenizer.sval.equals("?"))){
      tokenizer.ttype = '?';
    }
  }

  /**
   * Gets index, checking for a premature and of line.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if it finds a premature end of line
   */
  protected void getIndex(StreamTokenizer tokenizer) throws IOException {
    
    if (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
      errms(tokenizer,"premature end of line");
    }
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      errms(tokenizer,"premature end of file");
    }
  }
  
  /**
   * Gets token and checks if its end of line.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if it doesn't find an end of line
   */
  protected void getLastToken(StreamTokenizer tokenizer, boolean endOfFileOk) 
       throws IOException {

    if ((tokenizer.nextToken() != StreamTokenizer.TT_EOL) &&
	((tokenizer.ttype != StreamTokenizer.TT_EOF) || !endOfFileOk)) {
      errms(tokenizer,"end of line expected");
    }
  }

  /**
   * Gets next token, checking for a premature and of line.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if it finds a premature end of line
   */
  protected void getNextToken(StreamTokenizer tokenizer) 
       throws IOException {
    
    if (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
      errms(tokenizer,"premature end of line");
    }
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      errms(tokenizer,"premature end of file");
    } else if ((tokenizer.ttype == '\'') ||
	       (tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    } else if ((tokenizer.ttype == StreamTokenizer.TT_WORD) &&
	       (tokenizer.sval.equals("?"))){
      tokenizer.ttype = '?';
    }
  }
	
  /**
   * Initializes the StreamTokenizer used for reading the ARFF file.
   *
   * @param tokenizer the stream tokenizer
   */
  protected void initTokenizer(StreamTokenizer tokenizer){

    tokenizer.resetSyntax();         
    tokenizer.whitespaceChars(0, ' ');    
    tokenizer.wordChars(' '+1,'\u00FF');
    tokenizer.whitespaceChars(',',',');
    tokenizer.commentChar('%');
    tokenizer.quoteChar('"');
    tokenizer.quoteChar('\'');
    tokenizer.ordinaryChar('{');
    tokenizer.ordinaryChar('}');
    tokenizer.eolIsSignificant(true);
  }
 
  /**
   * Returns string including all instances, their weights and
   * their indices in the original dataset.
   *
   * @return description of instance and its weight as a string
   */
  protected /*@pure@*/ String instancesAndWeights(){

    StringBuffer text = new StringBuffer();

    for (int i = 0; i < numInstances(); i++) {
      text.append(instance(i) + " " + instance(i).weight());
      if (i < numInstances() - 1) {
	text.append("\n");
      }
    }
    return text.toString();
  }
  
  /**
   * Partitions the instances around a pivot. Used by quicksort and
   * kthSmallestValue.
   *
   * @param attIndex the attribute's index
   * @param left the first index of the subset 
   * @param right the last index of the subset 
   *
   * @return the index of the middle element
   */
  //@ requires 0 <= attIndex && attIndex < numAttributes();
  //@ requires 0 <= left && left <= right && right < numInstances();
  protected int partition(int attIndex, int l, int r) {
    
    double pivot = instance((l + r) / 2).value(attIndex);

    while (l < r) {
      while ((instance(l).value(attIndex) < pivot) && (l < r)) {
        l++;
      }
      while ((instance(r).value(attIndex) > pivot) && (l < r)) {
        r--;
      }
      if (l < r) {
        swap(l, r);
        l++;
        r--;
      }
    }
    if ((l == r) && (instance(r).value(attIndex) > pivot)) {
      r--;
    } 

    return r;
  }
  
  /**
   * Implements quicksort according to Manber's "Introduction to
   * Algorithms".
   *
   * @param attIndex the attribute's index
   * @param left the first index of the subset to be sorted
   * @param right the last index of the subset to be sorted
   */
  //@ requires 0 <= attIndex && attIndex < numAttributes();
  //@ requires 0 <= first && first <= right && right < numInstances();
  protected void quickSort(int attIndex, int left, int right) {

    if (left < right) {
      int middle = partition(attIndex, left, right);
      quickSort(attIndex, left, middle);
      quickSort(attIndex, middle + 1, right);
    }
  }

  /**
   * Reads and skips all tokens before next end of line token.
   *
   * @param tokenizer the stream tokenizer
   */
  protected void readTillEOL(StreamTokenizer tokenizer) 
       throws IOException {
    
    while (tokenizer.nextToken() != StreamTokenizer.TT_EOL) {};
    tokenizer.pushBack();
  }
  
  /**
   * Implements computation of the kth-smallest element according
   * to Manber's "Introduction to Algorithms".
   *
   * @param attIndex the attribute's index
   * @param left the first index of the subset 
   * @param right the last index of the subset 
   * @param k the value of k
   *
   * @return the index of the kth-smallest element
   */
  //@ requires 0 <= attIndex && attIndex < numAttributes();
  //@ requires 0 <= first && first <= right && right < numInstances();
  protected int select(int attIndex, int left, int right, int k) {
    
    if (left == right) {
      return left;
    } else {
      int middle = partition(attIndex, left, right);
      if ((middle - left + 1) >= k) {
        return select(attIndex, left, middle, k);
      } else {
        return select(attIndex, middle + 1, right, k - (middle - left + 1));
      }
    }
  }

  /**
   * Help function needed for stratification of set.
   *
   * @param numFolds the number of folds for the stratification
   */
  protected void stratStep (int numFolds){
    
    FastVector newVec = new FastVector(m_Instances.capacity());
    int start = 0, j;

    // create stratified batch
    while (newVec.size() < numInstances()) {
      j = start;
      while (j < numInstances()) {
	newVec.addElement(instance(j));
	j = j + numFolds;
      }
      start++;
    }
    m_Instances = newVec;
  }
  
  /**
   * Swaps two instances in the set.
   *
   * @param i the first instance's index
   * @param j the second instance's index
   */
  //@ requires 0 <= i && i < numInstances();
  //@ requires 0 <= j && j < numInstances();
  public void swap(int i, int j){
    
    m_Instances.swap(i, j);
  }

  /**
   * Merges two sets of Instances together. The resulting set will have
   * all the attributes of the first set plus all the attributes of the 
   * second set. The number of instances in both sets must be the same.
   *
   * @param first the first set of Instances
   * @param second the second set of Instances
   * @return the merged set of Instances
   * @exception IllegalArgumentException if the datasets are not the same size
   */
  public static Instances mergeInstances(Instances first, Instances second) {

    if (first.numInstances() != second.numInstances()) {
      throw new IllegalArgumentException("Instance sets must be of the same size");
    }

    // Create the vector of merged attributes
    FastVector newAttributes = new FastVector();
    for (int i = 0; i < first.numAttributes(); i++) {
      newAttributes.addElement(first.attribute(i));
    }
    for (int i = 0; i < second.numAttributes(); i++) {
      newAttributes.addElement(second.attribute(i));
    }
    
    // Create the set of Instances
    Instances merged = new Instances(first.relationName() + '_'
				     + second.relationName(), 
				     newAttributes, 
				     first.numInstances());
    // Merge each instance
    for (int i = 0; i < first.numInstances(); i++) {
      merged.add(first.instance(i).mergeInstance(second.instance(i)));
    }
    return merged;
  }

  /**
   * Method for testing this class.
   *
   * @param argv should contain one element: the name of an ARFF file
   */
  //@ requires argv != null;
  //@ requires argv.length == 1;
  //@ requires argv[0] != null;
  public static void test(String [] argv) {

    Instances instances, secondInstances, train, test, transformed, empty;
    Instance instance;
    Random random = new Random(2);
    Reader reader;
    int start, num;
    double newWeight;
    FastVector testAtts, testVals;
    int i,j;
    
    try{
      if (argv.length > 1) {
	throw (new Exception("Usage: Instances [<filename>]"));
      }
      
      // Creating set of instances from scratch
      testVals = new FastVector(2);
      testVals.addElement("first_value");
      testVals.addElement("second_value");
      testAtts = new FastVector(2);
      testAtts.addElement(new Attribute("nominal_attribute", testVals));
      testAtts.addElement(new Attribute("numeric_attribute"));
      instances = new Instances("test_set", testAtts, 10);
      instances.add(new Instance(instances.numAttributes()));
      instances.add(new Instance(instances.numAttributes()));
      instances.add(new Instance(instances.numAttributes()));
      instances.setClassIndex(0);
      System.out.println("\nSet of instances created from scratch:\n");
      System.out.println(instances);
      
      if (argv.length == 1) {
	String filename = argv[0];
	reader = new FileReader(filename);
	
	// Read first five instances and print them
	System.out.println("\nFirst five instances from file:\n");
	instances = new Instances(reader, 1);
	instances.setClassIndex(instances.numAttributes() - 1);
	i = 0;
	while ((i < 5) && (instances.readInstance(reader))) {
	  i++;
	}
	System.out.println(instances);

	// Read all the instances in the file
	reader = new FileReader(filename);
	instances = new Instances(reader);

	// Make the last attribute be the class 
	instances.setClassIndex(instances.numAttributes() - 1);
	
	// Print header and instances.
	System.out.println("\nDataset:\n");
	System.out.println(instances);
	System.out.println("\nClass index: "+instances.classIndex());
      }
      
      // Test basic methods based on class index.
      System.out.println("\nClass name: "+instances.classAttribute().name());
      System.out.println("\nClass index: "+instances.classIndex());
      System.out.println("\nClass is nominal: " +
			 instances.classAttribute().isNominal());
      System.out.println("\nClass is numeric: " +
			 instances.classAttribute().isNumeric());
      System.out.println("\nClasses:\n");
      for (i = 0; i < instances.numClasses(); i++) {
	System.out.println(instances.classAttribute().value(i));
      }
      System.out.println("\nClass values and labels of instances:\n");
      for (i = 0; i < instances.numInstances(); i++) {
	Instance inst = instances.instance(i);
	System.out.print(inst.classValue() + "\t");
	System.out.print(inst.toString(inst.classIndex()));
	if (instances.instance(i).classIsMissing()) {
	  System.out.println("\tis missing");
	} else {
	  System.out.println();
	}
      }
      
      // Create random weights.
      System.out.println("\nCreating random weights for instances.");
      for (i = 0; i < instances.numInstances(); i++) {
	instances.instance(i).setWeight(random.nextFloat()); 
      }
      
      // Print all instances and their weights (and the sum of weights).
      System.out.println("\nInstances and their weights:\n");
      System.out.println(instances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(instances.sumOfWeights());
      
      // Insert an attribute
      secondInstances = new Instances(instances);
      Attribute testAtt = new Attribute("Inserted");
      secondInstances.insertAttributeAt(testAtt, 0);
      System.out.println("\nSet with inserted attribute:\n");
      System.out.println(secondInstances);
      System.out.println("\nClass name: "
			 + secondInstances.classAttribute().name());
      
      // Delete the attribute
      secondInstances.deleteAttributeAt(0);
      System.out.println("\nSet with attribute deleted:\n");
      System.out.println(secondInstances);
      System.out.println("\nClass name: "
			 + secondInstances.classAttribute().name());
      
      // Test if headers are equal
      System.out.println("\nHeaders equal: "+
			 instances.equalHeaders(secondInstances) + "\n");
      
      // Print data in internal format.
      System.out.println("\nData (internal values):\n");
      for (i = 0; i < instances.numInstances(); i++) {
	for (j = 0; j < instances.numAttributes(); j++) {
	  if (instances.instance(i).isMissing(j)) {
	    System.out.print("? ");
	  } else {
	    System.out.print(instances.instance(i).value(j) + " ");
	  }
	}
	System.out.println();
      }
      
      // Just print header
      System.out.println("\nEmpty dataset:\n");
      empty = new Instances(instances, 0);
      System.out.println(empty);
      System.out.println("\nClass name: "+empty.classAttribute().name());

      // Create copy and rename an attribute and a value (if possible)
      if (empty.classAttribute().isNominal()) {
	Instances copy = new Instances(empty, 0);
	copy.renameAttribute(copy.classAttribute(), "new_name");
	copy.renameAttributeValue(copy.classAttribute(), 
				  copy.classAttribute().value(0), 
				  "new_val_name");
	System.out.println("\nDataset with names changed:\n" + copy);
	System.out.println("\nOriginal dataset:\n" + empty);
      }

      // Create and prints subset of instances.
      start = instances.numInstances() / 4;
      num = instances.numInstances() / 2;
      System.out.print("\nSubset of dataset: ");
      System.out.println(num + " instances from " + (start + 1) 
			 + ". instance");
      secondInstances = new Instances(instances, start, num);
      System.out.println("\nClass name: "
			 + secondInstances.classAttribute().name());

      // Print all instances and their weights (and the sum of weights).
      System.out.println("\nInstances and their weights:\n");
      System.out.println(secondInstances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(secondInstances.sumOfWeights());
      
      // Create and print training and test sets for 3-fold
      // cross-validation.
      System.out.println("\nTrain and test folds for 3-fold CV:");
      if (instances.classAttribute().isNominal()) {
	instances.stratify(3);
      }
      for (j = 0; j < 3; j++) {
        train = instances.trainCV(3,j, new Random(1));
	test = instances.testCV(3,j);
                      
	// Print all instances and their weights (and the sum of weights).
	System.out.println("\nTrain: ");
	System.out.println("\nInstances and their weights:\n");
	System.out.println(train.instancesAndWeights());
	System.out.print("\nSum of weights: ");
	System.out.println(train.sumOfWeights());
	System.out.println("\nClass name: "+train.classAttribute().name());
	System.out.println("\nTest: ");
	System.out.println("\nInstances and their weights:\n");
	System.out.println(test.instancesAndWeights());
	System.out.print("\nSum of weights: ");
	System.out.println(test.sumOfWeights());
	System.out.println("\nClass name: "+test.classAttribute().name());
      }

      // Randomize instances and print them.
      System.out.println("\nRandomized dataset:");
      instances.randomize(random);
      
      // Print all instances and their weights (and the sum of weights).
      System.out.println("\nInstances and their weights:\n");
      System.out.println(instances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(instances.sumOfWeights());

      // Sort instances according to first attribute and
      // print them.
      System.out.print("\nInstances sorted according to first attribute:\n ");
      instances.sort(0);
        
      // Print all instances and their weights (and the sum of weights).
      System.out.println("\nInstances and their weights:\n");
      System.out.println(instances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(instances.sumOfWeights());
    } catch (Exception e) {
      e.printStackTrace(); 
    }
  }

  /**
   * Main method for this class -- just prints a summary of a set
   * of instances.
   *
   * @param argv should contain one element: the name of an ARFF file
   */
  public static void main(String [] args) {

    try {
      Reader r = null;
      if (args.length > 1) {
	throw (new Exception("Usage: Instances <filename>"));
      } else if (args.length == 0) {
        r = new BufferedReader(new InputStreamReader(System.in));
      } else {
        r = new BufferedReader(new FileReader(args[0]));
      }
      Instances i = new Instances(r);
      System.out.println(i.toSummaryString());
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}

     

