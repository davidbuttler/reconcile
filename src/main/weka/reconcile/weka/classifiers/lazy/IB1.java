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
 *    IB1.java
 *    Copyright (C) 1999 Stuart Inglis,Len Trigg,Eibe Frank
 *
 */

package reconcile.weka.classifiers.lazy;

import java.util.Enumeration;

import reconcile.weka.classifiers.Classifier;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.classifiers.UpdateableClassifier;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.UnsupportedAttributeTypeException;
import reconcile.weka.core.Utils;



/**
 * IB1-type classifier. Uses a simple distance measure to find the training
 * instance closest to the given test instance, and predicts the same class
 * as this training instance. If multiple instances are
 * the same (smallest) distance to the test instance, the first one found is
 * used.  For more information, see <p>
 * 
 * Aha, D., and D. Kibler (1991) "Instance-based learning algorithms",
 * <i>Machine Learning</i>, vol.6, pp. 37-66.<p>
 *
 * @author Stuart Inglis (singlis@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class IB1 extends Classifier implements UpdateableClassifier {

  /** The training instances used for classification. */
  private Instances m_Train;

  /** The minimum values for numeric attributes. */
  private double [] m_MinArray;

  /** The maximum values for numeric attributes. */
  private double [] m_MaxArray;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Nearest-neighbour classifier. Uses normalized Euclidean distance to " 
      + "find the training instance closest to the given test instance, and predicts "
      + "the same class as this training instance. If multiple instances have "
      + "the same (smallest) distance to the test instance, the first one found is "
      + "used.  For more information, see \n\n"
      + "Aha, D., and D. Kibler (1991) \"Instance-based learning algorithms\", "
      + "Machine Learning, vol.6, pp. 37-66.";
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {
    
    if (instances.classAttribute().isNumeric()) {
       throw new Exception("IB1: Class is numeric!");
    }
    if (instances.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("IB1: Cannot handle string attributes!");
    }
    // Throw away training instances with missing class
    m_Train = new Instances(instances, 0, instances.numInstances());
    m_Train.deleteWithMissingClass();

    m_MinArray = new double [m_Train.numAttributes()];
    m_MaxArray = new double [m_Train.numAttributes()];
    for (int i = 0; i < m_Train.numAttributes(); i++) {
      m_MinArray[i] = m_MaxArray[i] = Double.NaN;
    }
    Enumeration enu = m_Train.enumerateInstances();
    while (enu.hasMoreElements()) {
      updateMinMax((Instance) enu.nextElement());
    }
  }

  /**
   * Updates the classifier.
   *
   * @param instance the instance to be put into the classifier
   * @exception Exception if the instance could not be included successfully
   */
  public void updateClassifier(Instance instance) throws Exception {
  
    if (m_Train.equalHeaders(instance.dataset()) == false) {
      throw new Exception("Incompatible instance types");
    }
    if (instance.classIsMissing()) {
      return;
    }
    m_Train.add(instance);
    updateMinMax(instance);
  }

  /**
   * Classifies the given test instance.
   *
   * @param instance the instance to be classified
   * @return the predicted class for the instance 
   * @exception Exception if the instance can't be classified
   */
  public double classifyInstance(Instance instance) throws Exception {
    
    if (m_Train.numInstances() == 0) {
      throw new Exception("No training instances!");
    }

    double distance, minDistance = Double.MAX_VALUE, classValue = 0;
    updateMinMax(instance);
    Enumeration enu = m_Train.enumerateInstances();
    while (enu.hasMoreElements()) {
      Instance trainInstance = (Instance) enu.nextElement();
      if (!trainInstance.classIsMissing()) {
	distance = distance(instance, trainInstance);
	if (distance < minDistance) {
	  minDistance = distance;
	  classValue = trainInstance.classValue();
	}
      }
    }

    return classValue;
  }

  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString() {

    return ("IB1 classifier");
  }

  /**
   * Calculates the distance between two instances
   *
   * @param first the first instance
   * @param second the second instance
   * @return the distance between the two given instances
   */          
  private double distance(Instance first, Instance second) {
    
    double diff, distance = 0;

    for(int i = 0; i < m_Train.numAttributes(); i++) { 
      if (i == m_Train.classIndex()) {
	continue;
      }
      if (m_Train.attribute(i).isNominal()) {

	// If attribute is nominal
	if (first.isMissing(i) || second.isMissing(i) ||
	    ((int)first.value(i) != (int)second.value(i))) {
	  distance += 1;
	}
      } else {
	
	// If attribute is numeric
	if (first.isMissing(i) || second.isMissing(i)){
	  if (first.isMissing(i) && second.isMissing(i)) {
	    diff = 1;
	  } else {
	    if (second.isMissing(i)) {
	      diff = norm(first.value(i), i);
	    } else {
	      diff = norm(second.value(i), i);
	    }
	    if (diff < 0.5) {
	      diff = 1.0 - diff;
	    }
	  }
	} else {
	  diff = norm(first.value(i), i) - norm(second.value(i), i);
	}
	distance += diff * diff;
      }
    }
    
    return distance;
  }
    
  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private double norm(double x,int i) {

    if (Double.isNaN(m_MinArray[i])
	|| Utils.eq(m_MaxArray[i], m_MinArray[i])) {
      return 0;
    } else {
      return (x - m_MinArray[i]) / (m_MaxArray[i] - m_MinArray[i]);
    }
  }

  /**
   * Updates the minimum and maximum values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance) {
    
    for (int j = 0;j < m_Train.numAttributes(); j++) {
      if ((m_Train.attribute(j).isNumeric()) && (!instance.isMissing(j))) {
	if (Double.isNaN(m_MinArray[j])) {
	  m_MinArray[j] = instance.value(j);
	  m_MaxArray[j] = instance.value(j);
	} else {
	  if (instance.value(j) < m_MinArray[j]) {
	    m_MinArray[j] = instance.value(j);
	  } else {
	    if (instance.value(j) > m_MaxArray[j]) {
	      m_MaxArray[j] = instance.value(j);
	    }
	  }
	}
      }
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain command line arguments for evaluation
   * (see Evaluation).
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new IB1(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}




