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
 *    Resample.java
 *    Copyright (C) 2002 University of Waikato
 *
 */


package reconcile.weka.filters.unsupervised.instance;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.UnsupervisedFilter;


/** 
 * Produces a random subsample of a dataset using sampling with
 * replacement. The original dataset must fit entirely in memory. The
 * number of instances in the generated dataset may be specified. When
 * used in batch mode, subsequent batches are <b>not</b> resampled.
 *
 * Valid options are:<p>
 *
 * -S num <br>
 * Specify the random number seed (default 1).<p>
 *
 * -Z percent <br>
 * Specify the size of the output dataset, as a percentage of the input
 * dataset (default 100). <p>
 *
 * @author Len Trigg (len@reeltwo.com)
 * @version $Revision: 1.1 $ 
 *
 */
public class Resample extends Filter implements UnsupervisedFilter,
						OptionHandler {

  /** The subsample size, percent of original set, default 100% */
  private double m_SampleSizePercent = 100;
  
  /** The random number generator seed */
  private int m_RandomSeed = 1;

  /** True if the first batch has been done */
  private boolean m_FirstBatchDone = false;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Produces a random subsample of a dataset using sampling with"
      + "replacement. The original dataset must "
      +"fit entirely in memory. The number of instances in the generated "
      +"dataset may be specified.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
              "\tSpecify the random number seed (default 1)",
              "S", 1, "-S <num>"));
    newVector.addElement(new Option(
              "\tThe size of the output dataset, as a percentage of\n"
              +"\tthe input dataset (default 100)",
              "Z", 1, "-Z <num>"));

    return newVector.elements();
  }


  /**
   * Parses a list of options for this object. Valid options are:<p>
   *
   * -S num <br>
   * Specify the random number seed (default 1).<p>
   *
   * -Z percent <br>
   * Specify the size of the output dataset, as a percentage of the input
   * dataset (default 100). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setRandomSeed(Integer.parseInt(seedString));
    } else {
      setRandomSeed(1);
    }

    String sizeString = Utils.getOption('Z', options);
    if (sizeString.length() != 0) {
      setSampleSizePercent(Double.valueOf(sizeString).doubleValue());
    } else {
      setSampleSizePercent(100);
    }

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

    String [] options = new String [6];
    int current = 0;

    options[current++] = "-S"; options[current++] = "" + getRandomSeed();

    options[current++] = "-Z"; options[current++] = "" + getSampleSizePercent();

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String randomSeedTipText() {
    return "The seed used for random sampling.";
  }

  /**
   * Gets the random number seed.
   *
   * @return the random number seed.
   */
  public int getRandomSeed() {

    return m_RandomSeed;
  }
  
  /**
   * Sets the random number seed.
   *
   * @param newSeed the new random number seed.
   */
  public void setRandomSeed(int newSeed) {

    m_RandomSeed = newSeed;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String sampleSizePercentTipText() {
    return "Size of the subsample as a percentage of the original dataset.";
  }

  /**
   * Gets the subsample size as a percentage of the original set.
   *
   * @return the subsample size
   */
  public double getSampleSizePercent() {

    return m_SampleSizePercent;
  }
  
  /**
   * Sets the size of the subsample, as a percentage of the original set.
   *
   * @param newSampleSizePercent the subsample set size, between 0 and 100.
   */
  public void setSampleSizePercent(double newSampleSizePercent) {

    m_SampleSizePercent = newSampleSizePercent;
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
    m_FirstBatchDone = false;
    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input structure has been defined
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (m_FirstBatchDone) {
      push(instance);
      return true;
    } else {
      bufferInput(instance);
      return false;
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

    if (!m_FirstBatchDone) {
      // Do the subsample, and clear the input instances.
      createSubsample();
    }
    flushInput();

    m_NewBatch = true;
    m_FirstBatchDone = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Creates a subsample of the current set of input instances. The output
   * instances are pushed onto the output queue for collection.
   */
  private void createSubsample() {

    int origSize = getInputFormat().numInstances();
    int sampleSize = (int) (origSize * m_SampleSizePercent / 100);
    
    // Simple subsample
    
    Random random = new Random(m_RandomSeed);
    // Convert pending input instances
    for(int i = 0; i < sampleSize; i++) {
      int index = random.nextInt(origSize);
      push((Instance)getInputFormat().instance(index).copy());
    }
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
 	Filter.batchFilterFile(new Resample(), argv);
      } else {
	Filter.filterFile(new Resample(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








