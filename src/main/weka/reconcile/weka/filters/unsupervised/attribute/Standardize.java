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
 *    Standardize.java
 *    Copyright (C) 2002 Eibe Frank
 *
 */

package reconcile.weka.filters.unsupervised.attribute;

import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.SparseInstance;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.UnsupervisedFilter;


/** 
 * Standardizes all numeric attributes in the given dataset
 * to have zero mean and unit variance.
 * intervals.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.1 $
 */
public class Standardize extends PotentialClassIgnorer implements UnsupervisedFilter {

  /** The means */
  private double [] m_Means;
  
  /** The variances */
  private double [] m_StdDevs;

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Standardizes all numeric attributes in the given dataset "
      + "to have zero mean and unit variance (apart from the class attribute, if set).";
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
    setOutputFormat(instanceInfo);
    m_Means = m_StdDevs = null;
    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been set.
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (m_Means == null) {
      bufferInput(instance);
      return false;
    } else {
      convertInstance(instance);
      return true;
    }
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @exception IllegalStateException if no input structure has been defined
   */
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_Means == null) {
      Instances input = getInputFormat();
      m_Means = new double[input.numAttributes()];
      m_StdDevs = new double[input.numAttributes()];
      for (int i = 0; i < input.numAttributes(); i++) {
	if (input.attribute(i).isNumeric() &&
	    (input.classIndex() != i)) {
	  m_Means[i] = input.meanOrMode(i);
	  m_StdDevs[i] = Math.sqrt(input.variance(i));
	}
      }

      // Convert pending input instances
      for(int i = 0; i < input.numInstances(); i++) {
	convertInstance(input.instance(i));
      }
    } 
    // Free memory
    flushInput();

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Convert a single instance over. The converted instance is 
   * added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstance(Instance instance) {
  
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      float[] newVals = new float[instance.numAttributes()];
      int[] newIndices = new int[instance.numAttributes()];
      float[] vals = instance.toFloatArray();
      int ind = 0;
      for (int j = 0; j < instance.numAttributes(); j++) {
        float value;
	if (instance.attribute(j).isNumeric() &&
	    (!Instance.isMissingValue(vals[j])) &&
	    (getInputFormat().classIndex() != j)) {
	  
	  // Just subtract the mean if the standard deviation is zero
	  if (m_StdDevs[j] > 0) { 
	    value = (float)((vals[j] - m_Means[j]) / m_StdDevs[j]);
	  } else {
	    value = (float)(vals[j] - m_Means[j]);
	  }
	  if (value != 0.0) {
	    newVals[ind] = value;
	    newIndices[ind] = j;
	    ind++;
	  }
	} else {
	  value = vals[j];
	  if (value != 0.0) {
	    newVals[ind] = value;
	    newIndices[ind] = j;
	    ind++;
	  }
	}
      }	
      float[] tempVals = new float[ind];
      int[] tempInd = new int[ind];
      System.arraycopy(newVals, 0, tempVals, 0, ind);
      System.arraycopy(newIndices, 0, tempInd, 0, ind);
      inst = new SparseInstance(instance.weight(), tempVals, tempInd,
                                instance.numAttributes());
    } else {
      float[] vals = instance.toFloatArray();
      for (int j = 0; j < getInputFormat().numAttributes(); j++) {
	if (instance.attribute(j).isNumeric() &&
	    (!Instance.isMissingValue(vals[j])) &&
	    (getInputFormat().classIndex() != j)) {
	  
	  // Just subtract the mean if the standard deviation is zero
	  if (m_StdDevs[j] > 0) { 
	    vals[j] = (float)((vals[j] - m_Means[j]) / m_StdDevs[j]);
	  } else {
	    vals[j] = (float)((vals[j] - m_Means[j]));
	  }
	}
      }	
      inst = new Instance(instance.weight(), vals);
    }
    inst.setDataset(instance.dataset());
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
 	Filter.batchFilterFile(new Standardize(), argv);
      } else {
	Filter.filterFile(new Standardize(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








