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
 *    FirstOrder.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package reconcile.weka.filters.unsupervised.attribute;

import java.util.Enumeration;
import java.util.Vector;

import reconcile.weka.core.Attribute;
import reconcile.weka.core.FastVector;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Range;
import reconcile.weka.core.SparseInstance;
import reconcile.weka.core.UnsupportedAttributeTypeException;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.StreamableFilter;
import reconcile.weka.filters.UnsupervisedFilter;


/** 
 * This instance filter takes a range of N numeric attributes and replaces
 * them with N-1 numeric attributes, the values of which are the difference 
 * between consecutive attribute values from the original instance. eg: <P>
 *
 * Original attribute values <BR>
 * <code> 0.1, 0.2, 0.3, 0.1, 0.3 </code> <P>
 *
 * New attribute values <BR>
 * <code> 0.1, 0.1, -0.2, 0.2 </code> <P>
 *
 * The range of attributes used is taken in numeric order. That is, a range
 * spec of 7-11,3-5 will use the attribute ordering 3,4,5,7,8,9,10,11 for the
 * differences, <i>not</i> 7,8,9,10,11,3,4,5.<p>
 *
 * Valid filter-specific options are:<p>
 *
 * -R index1,index2-index4,...<br>
 * Specify list of columns to take the differences between. 
 * First and last are valid indexes.
 * (default none)<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class FirstOrder extends Filter
  implements UnsupervisedFilter, StreamableFilter, OptionHandler {

  /** Stores which columns to take differences between */
  protected Range m_DeltaCols = new Range();

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "This instance filter takes a range of N numeric attributes and replaces "
      + "them with N-1 numeric attributes, the values of which are the difference "
      + "between consecutive attribute values from the original instance. eg: \n\n"
      + "Original attribute values\n\n"
      + "   0.1, 0.2, 0.3, 0.1, 0.3\n\n"
      + "New attribute values\n\n"
      + "   0.1, 0.1, -0.2, 0.2\n\n"
      + "The range of attributes used is taken in numeric order. That is, a range "
      + "spec of 7-11,3-5 will use the attribute ordering 3,4,5,7,8,9,10,11 for the "
      + "differences, NOT 7,8,9,10,11,3,4,5.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
              "\tSpecify list of columns to take the differences between.\n"
	      + "\tFirst and last are valid indexes.\n"
	      + "\t(default none)",
              "R", 1, "-R <index1,index2-index4,...>"));

    return newVector.elements();
  }


  /**
   * Parses a given list of options controlling the behaviour of this object.
   * Valid options are:<p>
   *
   * -R index1,index2-index4,...<br>
   * Specify list of columns to take the differences between. 
   * First and last are valid indexes.
   * (default none)<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String deltaList = Utils.getOption('R', options);
    if (deltaList.length() != 0) {
      setAttributeIndices(deltaList);
    } else {
      setAttributeIndices("");
    }
    
    if (getInputFormat() != null)
      setInputFormat(getInputFormat());
  }


  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [2];
    int current = 0;

    if (!getAttributeIndices().equals("")) {
      options[current++] = "-R"; options[current++] = getAttributeIndices();
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception UnsupportedAttributeTypeException if any of the
   * selected attributes are not numeric 
   * @exception Exception if only one attribute has been selected.
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);

    m_DeltaCols.setUpper(getInputFormat().numAttributes() - 1);
    int selectedCount = 0;
    for (int i = getInputFormat().numAttributes() - 1; i >= 0; i--) {
      if (m_DeltaCols.isInRange(i)) {
        selectedCount++;
        if (!getInputFormat().attribute(i).isNumeric()) {
          throw new UnsupportedAttributeTypeException("Selected attributes must be all numeric");
        }
      }
    }
    if (selectedCount == 1) {
      throw new Exception("Cannot select only one attribute.");
    }

    // Create the output buffer
    FastVector newAtts = new FastVector();
    boolean inRange = false;
    String foName = null;
    for(int i = 0; i < instanceInfo.numAttributes(); i++) {
      if (m_DeltaCols.isInRange(i)) {
	if (inRange) {
	  Attribute newAttrib = new Attribute(foName);
          newAtts.addElement(newAttrib);
	}
        foName = instanceInfo.attribute(i).name();
        foName = "'FO " + foName.replace('\'', ' ').trim() + '\'';
        inRange = true;
      } else {
	newAtts.addElement((Attribute)instanceInfo.attribute(i).copy());
      }      
    }
    setOutputFormat(new Instances(instanceInfo.relationName(), newAtts, 0));
    return true;
  }
  

  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been defined.
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    Instances outputFormat = outputFormatPeek();
    float[] vals = new float[outputFormat.numAttributes()];
    boolean inRange = false;
    double lastVal = Instance.missingValue();
    int i, j;
    for(i = 0, j = 0; j < outputFormat.numAttributes(); i++) {
      if (m_DeltaCols.isInRange(i)) {
	if (inRange) {
	  if (Instance.isMissingValue(lastVal) || instance.isMissing(i)) {
	    vals[j++] = Instance.missingValue();
	  } else {
	    vals[j++] = (float)(instance.value(i) - lastVal);
	  }
	} else {
	  inRange = true;
	}
	lastVal = instance.value(i);
      } else {
	vals[j++] = instance.value(i);
      }
    }

    Instance inst = null;
    if (instance instanceof SparseInstance) {
      inst = new SparseInstance(instance.weight(), vals);
    } else {
      inst = new Instance(instance.weight(), vals);
    }
    copyStringValues(inst, false, instance.dataset(), getInputStringIndex(),
                     getOutputFormat(), getOutputStringIndex());
    inst.setDataset(getOutputFormat());
    push(inst);
    return true;
  }


  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeIndicesTipText() {
    return "Specify range of attributes to act on."
      + " This is a comma separated list of attribute indices, with"
      + " \"first\" and \"last\" valid values. Specify an inclusive"
      + " range with \"-\". E.g: \"first-3,5,6-10,last\".";
  }

  /**
   * Get the current range selection
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {

    return m_DeltaCols.getRanges();
  }

  /**
   * Set which attributes are to be deleted (or kept if invert is true)
   *
   * @param rangeList a string representing the list of attributes. Since
   * the string will typically come from a user, attributes are indexed from
   * 1. <br>
   * eg: first-3,5,6-last
   * @exception Exception if an invalid range list is supplied
   */
  public void setAttributeIndices(String rangeList) throws Exception {

    m_DeltaCols.setRanges(rangeList);
  }

  /**
   * Set which attributes are to be deleted (or kept if invert is true)
   *
   * @param attributes an array containing indexes of attributes to select.
   * Since the array will typically come from a program, attributes are indexed
   * from 0.
   * @exception Exception if an invalid set of ranges is supplied
   */
  public void setAttributeIndicesArray(int [] attributes) throws Exception {

    setAttributeIndices(Range.indicesToRangeList(attributes));
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new FirstOrder(), argv);
      } else {
	Filter.filterFile(new FirstOrder(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








