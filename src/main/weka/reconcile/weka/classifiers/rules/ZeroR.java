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
 *    ZeroR.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package reconcile.weka.classifiers.rules;

import java.util.Enumeration;

import reconcile.weka.classifiers.Classifier;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.core.Attribute;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Utils;
import reconcile.weka.core.WeightedInstancesHandler;


/**
 * Class for building and using a 0-R classifier. Predicts the mean
 * (for a numeric class) or the mode (for a nominal class).
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class ZeroR extends Classifier implements WeightedInstancesHandler {

  /** The class value 0R predicts. */
  private double m_ClassValue;

  /** The number of instances in each class (null if class numeric). */
  private double [] m_Counts;
  
  /** The class attribute. */
  private Attribute m_Class;
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for building and using a 0-R classifier. Predicts the mean " 
      + "(for a numeric class) or the mode (for a nominal class).";	    
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    double sumOfWeights = 0;

    m_Class = instances.classAttribute();
    m_ClassValue = 0;
    switch (instances.classAttribute().type()) {
    case Attribute.NUMERIC:
      m_Counts = null;
      break;
    case Attribute.NOMINAL:
      m_Counts = new double [instances.numClasses()];
      for (int i = 0; i < m_Counts.length; i++) {
	m_Counts[i] = 1;
      }
      sumOfWeights = instances.numClasses();
      break;
    default:
      throw new Exception("ZeroR can only handle nominal and numeric class"
			  + " attributes.");
    }
    Enumeration enu = instances.enumerateInstances();
    while (enu.hasMoreElements()) {
      Instance instance = (Instance) enu.nextElement();
      if (!instance.classIsMissing()) {
	if (instances.classAttribute().isNominal()) {
	  m_Counts[(int)instance.classValue()] += instance.weight();
	} else {
	  m_ClassValue += instance.weight() * instance.classValue();
	}
	sumOfWeights += instance.weight();
      }
    }
    if (instances.classAttribute().isNumeric()) {
      if (Utils.gr(sumOfWeights, 0)) {
	m_ClassValue /= sumOfWeights;
      }
    } else {
      m_ClassValue = Utils.maxIndex(m_Counts);
      Utils.normalize(m_Counts, sumOfWeights);
    }
  }

  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be classified
   * @return index of the predicted class
   */
  public double classifyInstance(Instance instance) {

    return m_ClassValue;
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if class is numeric
   */
  public double [] distributionForInstance(Instance instance) 
       throws Exception {
	 
    if (m_Counts == null) {
      double[] result = new double[1];
      result[0] = m_ClassValue;
      return result;
    } else {
      return (double []) m_Counts.clone();
    }
  }
  
  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {

    if (m_Class ==  null) {
      return "ZeroR: No model built yet.";
    }
    if (m_Counts == null) {
      return "ZeroR predicts class value: " + m_ClassValue;
    } else {
      return "ZeroR predicts class value: " + m_Class.value((int) m_ClassValue);
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new ZeroR(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}


