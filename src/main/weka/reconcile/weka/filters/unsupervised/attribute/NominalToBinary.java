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
 *    NominalToBinary.java
 *    Copyright (C) 1999 Eibe Frank
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
import reconcile.weka.core.Utils;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.UnsupervisedFilter;


/** 
 * Converts all nominal attributes into binary numeric attributes. An
 * attribute with k values is transformed into k binary attributes
 * (using the one-attribute-per-value approach).
 * Binary attributes are left binary.
 *
 * Valid filter-specific options are: <p>
 *
 * -N <br>
 * If binary attributes are to be coded as nominal ones.<p>
 *
 * -R col1,col2-col4,... <br>
 * Specifies list of columns to convert. First
 * and last are valid indexes. (default: first-last) <p>
 *
 * -V <br>
 * Invert matching sense.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.1 $ 
 */
public class NominalToBinary extends Filter implements UnsupervisedFilter,
						       OptionHandler {

  /** Stores which columns to act on */
  protected Range m_Columns = new Range();

  /** The sorted indices of the attribute values. */
  private int[][] m_Indices = null;

  /** Are the new attributes going to be nominal or numeric ones? */
  private boolean m_Numeric = true;

  /** Constructor - initialises the filter */
  public NominalToBinary() {

    setAttributeIndices("first-last");
  }

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Converts all nominal attributes into binary numeric attributes. An "
      + "attribute with k values is transformed into k binary attributes if "
      + "the class is nominal (using the one-attribute-per-value approach). "
      + "Binary attributes are left binary."
      + "If the class is numeric, you might want to use the supervised version of "
      + "this filter.";
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the input format can't be set 
   * successfully
   */
  public boolean setInputFormat(Instances instanceInfo) 
       throws Exception {

    super.setInputFormat(instanceInfo);

    m_Columns.setUpper(instanceInfo.numAttributes() - 1);

    setOutputFormat();
    m_Indices = null;
    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been set
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    convertInstance(instance);
    return true;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
	      "\tSets if binary attributes are to be coded as nominal ones.",
	      "N", 0, "-N"));

    newVector.addElement(new Option(
              "\tSpecifies list of columns to act on. First"
	      + " and last are valid indexes.\n"
	      + "\t(default: first-last)",
              "R", 1, "-R <col1,col2-col4,...>"));

    newVector.addElement(new Option(
              "\tInvert matching sense of column indexes.",
              "V", 0, "-V"));

    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -N <br>
   * If binary attributes are to be coded as nominal ones.<p>
   *
   * -R col1,col2-col4,... <br>
   * Specifies list of columns to convert. First
   * and last are valid indexes. (default none) <p>
   *
   * -V <br>
   * Invert matching sense.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    setBinaryAttributesNominal(Utils.getFlag('N', options));
    
    String convertList = Utils.getOption('R', options);
    if (convertList.length() != 0) {
      setAttributeIndices(convertList);
    } else {
      setAttributeIndices("first-last");
    }
    setInvertSelection(Utils.getFlag('V', options));

    if (getInputFormat() != null)
      setInputFormat(getInputFormat());
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [4];
    int current = 0;

    if (getBinaryAttributesNominal()) {
      options[current++] = "-N";
    }

    if (!getAttributeIndices().equals("")) {
      options[current++] = "-R"; options[current++] = getAttributeIndices();
    }
    if (getInvertSelection()) {
      options[current++] = "-V";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }
    
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String binaryAttributesNominalTipText() {
    return "Whether resulting binary attributes will be nominal.";
  }

  /**
   * Gets if binary attributes are to be treated as nominal ones.
   *
   * @return true if binary attributes are to be treated as nominal ones
   */
  public boolean getBinaryAttributesNominal() {

    return !m_Numeric;
  }

  /**
   * Sets if binary attributes are to be treates as nominal ones.
   *
   * @param bool true if binary attributes are to be treated as nominal ones
   */
  public void setBinaryAttributesNominal(boolean bool) {

    m_Numeric = !bool;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String invertSelectionTipText() {

    return "Set attribute selection mode. If false, only selected"
      + " (numeric) attributes in the range will be discretized; if"
      + " true, only non-selected attributes will be discretized.";
  }

  /**
   * Gets whether the supplied columns are to be removed or kept
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return m_Columns.getInvert();
  }

  /**
   * Sets whether selected columns should be removed or kept. If true the 
   * selected columns are kept and unselected columns are deleted. If false
   * selected columns are deleted and unselected columns are kept.
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_Columns.setInvert(invert);
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
   * Gets the current range selection
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {

    return m_Columns.getRanges();
  }

  /**
   * Sets which attributes are to be acted on.
   *
   * @param rangeList a string representing the list of attributes. Since
   * the string will typically come from a user, attributes are indexed from
   * 1. <br>
   * eg: first-3,5,6-last
   * @exception IllegalArgumentException if an invalid range list is supplied 
   */
  public void setAttributeIndices(String rangeList) {

    m_Columns.setRanges(rangeList);
  }

  /**
   * Set the output format if the class is nominal.
   */
  private void setOutputFormat() {

    FastVector newAtts;
    int newClassIndex;
    StringBuffer attributeName;
    Instances outputFormat;
    FastVector vals;

    // Compute new attributes

    newClassIndex = getInputFormat().classIndex();
    newAtts = new FastVector();
    for (int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if (!att.isNominal() || (j == getInputFormat().classIndex()) ||
	  !m_Columns.isInRange(j)) {
	newAtts.addElement(att.copy());
      } else {
	if (att.numValues() <= 2) {
	  if (m_Numeric) {
	    newAtts.addElement(new Attribute(att.name()));
	  } else {
	    newAtts.addElement(att.copy());
	  }
	} else {

	  if (newClassIndex >= 0 && j < getInputFormat().classIndex()) {
	    newClassIndex += att.numValues() - 1;
	  }

	  // Compute values for new attributes
	  for (int k = 0; k < att.numValues(); k++) {
	    attributeName = 
	      new StringBuffer(att.name() + "=");
	    attributeName.append(att.value(k));
	    if (m_Numeric) {
	      newAtts.
		addElement(new Attribute(attributeName.toString()));
	    } else {
	      vals = new FastVector(2);
	      vals.addElement("f"); vals.addElement("t");
	      newAtts.
		addElement(new Attribute(attributeName.toString(), vals));
	    }
	  }
	}
      }
    }
    outputFormat = new Instances(getInputFormat().relationName(),
				 newAtts, 0);
    outputFormat.setClassIndex(newClassIndex);
    setOutputFormat(outputFormat);
  }

  /**
   * Convert a single instance over if the class is nominal. The converted 
   * instance is added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstance(Instance instance) {
  
    float [] vals = new float [outputFormatPeek().numAttributes()];
    int attSoFar = 0;

    for(int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if (!att.isNominal() || (j == getInputFormat().classIndex()) ||
	  !m_Columns.isInRange(j)) {
	vals[attSoFar] = instance.value(j);
	attSoFar++;
      } else {
	if (att.numValues() <= 2) {
	  vals[attSoFar] = instance.value(j);
	  attSoFar++;
	} else {
	  if (instance.isMissing(j)) {
	    for (int k = 0; k < att.numValues(); k++) {
              vals[attSoFar + k] = instance.value(j);
	    }
	  } else {
	    for (int k = 0; k < att.numValues(); k++) {
	      if (k == (int)instance.value(j)) {
                vals[attSoFar + k] = 1;
	      } else {
                vals[attSoFar + k] = 0;
	      }
	    }
	  }
	  attSoFar += att.numValues();
	}
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
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new NominalToBinary(), argv);
      } else {
	Filter.filterFile(new NominalToBinary(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








