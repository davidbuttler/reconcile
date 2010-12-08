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
 *    BatchClassifierEvent.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package reconcile.weka.gui.beans;

import java.util.EventObject;

import reconcile.weka.classifiers.Classifier;


/**
 * Class encapsulating a built classifier and a batch of instances to
 * test on.
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1 $
 * @since 1.0
 * @see EventObject
 */
public class BatchClassifierEvent extends EventObject {

  /**
   * The classifier
   */
  protected Classifier m_classifier;
  //  protected Instances m_trainingSet;

  /**
   * Instances that can be used for testing the classifier
   */
  //  protected Instances m_testSet;
  protected DataSetEvent m_testSet;

  /**
   * The set number for the test set
   */
  protected int m_setNumber;


  /**
   * The last set number for this series
   */
  protected int m_maxSetNumber;

  /**
   * Creates a new <code>BatchClassifierEvent</code> instance.
   *
   * @param source the source object
   * @param scheme a Classifier
   * @param tstI the test instances
   * @param setNum the set number of the test instances
   * @param maxSetNum the last set number in the series
   */
  public BatchClassifierEvent(Object source, Classifier scheme,
			 DataSetEvent tstI, int setNum,
			 int maxSetNum) {
    super(source);
    //    m_trainingSet = trnI;
    m_classifier = scheme;
    m_testSet = tstI;
    m_setNumber = setNum;
    m_maxSetNumber = maxSetNum;
  }

//    /**
//     * Get the training instances
//     *
//     * @return the training instances
//     */
//    public Instances getTrainingSet() {
//      return m_trainingSet;
//    }

  /**
   * Get the classifier
   *
   * @return the classifier
   */
  public Classifier getClassifier() {
    return m_classifier;
  }

  /**
   * Get the test set
   *
   * @return the testing instances
   */
  public DataSetEvent getTestSet() {
    return m_testSet;
  }

  /**
   * Get the set number (ie which fold this is)
   *
   * @return the set number for the training and testing data sets
   * encapsulated in this event
   */
  public int getSetNumber() {
    return m_setNumber;
  }

  /**
   * Get the maximum set number (ie the total number of training
   * and testing sets in the series).
   *
   * @return the maximum set number
   */
  public int getMaxSetNumber() {
    return m_maxSetNumber;
  }
}

