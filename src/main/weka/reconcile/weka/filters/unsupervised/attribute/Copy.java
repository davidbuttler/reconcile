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
 *    Copy.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package reconcile.weka.filters.unsupervised.attribute;

import java.util.Enumeration;
import java.util.Vector;

import reconcile.weka.core.Attribute;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Range;
import reconcile.weka.core.SparseInstance;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.StreamableFilter;
import reconcile.weka.filters.UnsupervisedFilter;


/** 
 * An instance filter that copies a range of attributes in the dataset.
 * This is used in conjunction with other filters that overwrite attribute
 * during the course of their operation -- this filter allows the original
 * attributes to be kept as well as the new attributes.<p>
 *
 * Valid filter-specific options are:<p>
 *
 * -R index1,index2-index4,...<br>
 * Specify list of columns to copy. First and last are valid indexes.
 * Attribute copies are placed at the end of the dataset.
 * (default none)<p>
 *
 * -V<br>
 * Invert matching sense (i.e. copy all non-specified columns)<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class Copy extends Filter implements UnsupervisedFilter,
					    StreamableFilter, OptionHandler {

  /** Stores which columns to copy */
  protected Range m_CopyCols = new Range();

  /**
   * Stores the indexes of the selected attributes in order, once the
   * dataset is seen
   */
  protected int [] m_SelectedAttributes;

  /** 
   * Contains an index of string attributes in the input format
   * that survive the filtering process -- some entries may be duplicated 
   */
  protected int [] m_InputStringIndex;

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
              "\tSpecify list of columns to copy. First and last are valid\n"
	      +"\tindexes. (default none)",
              "R", 1, "-R <index1,index2-index4,...>"));
    newVector.addElement(new Option(
	      "\tInvert matching sense (i.e. copy all non-specified columns)",
              "V", 0, "-V"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options controlling the behaviour of this object.
   * Valid options are:<p>
   *
   * -R index1,index2-index4,...<br>
   * Specify list of columns to copy. First and last are valid indexes.
   * (default none)<p>
   *
   * -V<br>
   * Invert matching sense (i.e. copy all non-specified columns)<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String copyList = Utils.getOption('R', options);
    if (copyList.length() != 0) {
      setAttributeIndices(copyList);
    }
    setInvertSelection(Utils.getFlag('V', options));
    
    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [3];
    int current = 0;

    if (getInvertSelection()) {
      options[current++] = "-V";
    }
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
   * @exception Exception if a problem occurs setting the input format
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);
    
    m_CopyCols.setUpper(instanceInfo.numAttributes() - 1);

    // Create the output buffer
    Instances outputFormat = new Instances(instanceInfo, 0); 
    m_SelectedAttributes = m_CopyCols.getSelection();
    int inStrCopiedLen = 0;
    int [] inStrCopied = new int[m_SelectedAttributes.length];
    for (int i = 0; i < m_SelectedAttributes.length; i++) {
      int current = m_SelectedAttributes[i];
      // Create a copy of the attribute with a different name
      Attribute origAttribute = instanceInfo.attribute(current);
      outputFormat.insertAttributeAt((Attribute)origAttribute.copy(),
				     outputFormat.numAttributes());
      outputFormat.renameAttribute(outputFormat.numAttributes() - 1,
				   "Copy of " + origAttribute.name());
      if (origAttribute.type() == Attribute.STRING) {
        inStrCopied[inStrCopiedLen++] = current;
      }
    }
    int [] origIndex = getInputStringIndex(); 
    m_InputStringIndex = new int [origIndex.length + inStrCopiedLen];
    System.arraycopy(origIndex, 0, m_InputStringIndex, 0, origIndex.length);
    System.arraycopy(inStrCopied, 0, m_InputStringIndex, origIndex.length, 
                     inStrCopiedLen);
    setOutputFormat(outputFormat);
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

    float[] vals = new float[outputFormatPeek().numAttributes()];
    for(int i = 0; i < getInputFormat().numAttributes(); i++) {
      vals[i] = instance.value(i);
    }
    int j = getInputFormat().numAttributes();
    for (int i = 0; i < m_SelectedAttributes.length; i++) {
      int current = m_SelectedAttributes[i];
      vals[i + j] = instance.value(current);
    }
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      inst = new SparseInstance(instance.weight(), vals);
    } else {
      inst = new Instance(instance.weight(), vals);
    }
    copyStringValues(inst, false, instance.dataset(), m_InputStringIndex,
                     getOutputFormat(), getOutputStringIndex());
    inst.setDataset(getOutputFormat());
    push(inst);
    return true;
  }

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "An instance filter that copies a range of attributes in the"
      + " dataset. This is used in conjunction with other filters that"
      + " overwrite attribute values during the course of their operation --"
      + " this filter allows the original attributes to be kept as well"
      + " as the new attributes.";
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String invertSelectionTipText() {
    return "Sets copy selected vs unselected action."
      + " If set to false, only the specified attributes will be copied;"
      + " If set to true, non-specified attributes will be copied.";
  }

  /**
   * Get whether the supplied columns are to be removed or kept
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return m_CopyCols.getInvert();
  }

  /**
   * Set whether selected columns should be removed or kept. If true the 
   * selected columns are kept and unselected columns are copied. If false
   * selected columns are copied and unselected columns are kept.
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_CopyCols.setInvert(invert);
  }

  /**
   * Get the current range selection
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {

    return m_CopyCols.getRanges();
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
   * Set which attributes are to be copied (or kept if invert is true)
   *
   * @param rangeList a string representing the list of attributes.  Since
   * the string will typically come from a user, attributes are indexed from
   * 1. <br>
   * eg: first-3,5,6-last
   * @exception Exception if an invalid range list is supplied
   */
  public void setAttributeIndices(String rangeList) throws Exception {

    m_CopyCols.setRanges(rangeList);
  }

  /**
   * Set which attributes are to be copied (or kept if invert is true)
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
 	Filter.batchFilterFile(new Copy(), argv); 
      } else {
	Filter.filterFile(new Copy(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








