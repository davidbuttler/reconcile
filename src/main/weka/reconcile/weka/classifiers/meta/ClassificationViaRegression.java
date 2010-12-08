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
 *    ClassificationViaRegression.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package reconcile.weka.classifiers.meta;

import reconcile.weka.classifiers.Classifier;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.classifiers.SingleClassifierEnhancer;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.UnsupportedClassTypeException;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.unsupervised.attribute.MakeIndicator;


/**
 * Class for doing classification using regression methods. For more
 * information, see <p>
 * 
 * E. Frank, Y. Wang, S. Inglis, G. Holmes, and I.H. Witten (1998)
 * "Using model trees for classification", <i>Machine Learning</i>,
 * Vol.32, No.1, pp. 63-76.<p>
 *
 * Valid options are:<p>
 *
 * -W classname <br>
 * Specify the full class name of a numeric predictor as the basis for 
 * the classifier (required).<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
*/
public class ClassificationViaRegression extends SingleClassifierEnhancer {

  /** The classifiers. (One for each class.) */
  private Classifier[] m_Classifiers;

  /** The filters used to transform the class. */
  private MakeIndicator[] m_ClassFilters;
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
 
    return "Class for doing classification using regression methods. Class is "
      + "binarized and one regression model is built for each class value. For more "
      + "information, see, for example\n\n"
      + "E. Frank, Y. Wang, S. Inglis, G. Holmes, and I.H. Witten (1998) "
      + "\"Using model trees for classification\", Machine Learning, "
      + "Vol.32, No.1, pp. 63-76.";
  }

  /**
   * String describing default classifier.
   */
  @Override
  protected String defaultClassifierString() {
    
  return "reconcile.weka.classifiers.trees.M5P";
  }

  /**
   * Default constructor.
   */
  public ClassificationViaRegression() {
    
    m_Classifier = new reconcile.weka.classifiers.trees.M5P();
  }

  /**
   * Builds the classifiers.
   *
   * @param insts the training data.
   * @exception Exception if a classifier can't be built
   */
  @Override
  public void buildClassifier(Instances insts) throws Exception {

    Instances newInsts;

    if (insts.classAttribute().isNumeric()) {
      throw new UnsupportedClassTypeException("ClassificationViaRegression can't "
					      + "handle a numeric class!");
    }
    m_Classifiers = Classifier.makeCopies(m_Classifier, insts.numClasses());
    m_ClassFilters = new MakeIndicator[insts.numClasses()];
    for (int i = 0; i < insts.numClasses(); i++) {
      m_ClassFilters[i] = new MakeIndicator();
      m_ClassFilters[i].setAttributeIndex("" + (insts.classIndex() + 1));
      m_ClassFilters[i].setValueIndex(i);
      m_ClassFilters[i].setNumeric(true);
      m_ClassFilters[i].setInputFormat(insts);
      newInsts = Filter.useFilter(insts, m_ClassFilters[i]);
      m_Classifiers[i].buildClassifier(newInsts);
    }
  }

  /**
   * Returns the distribution for an instance.
   *
   * @exception Exception if the distribution can't be computed successfully
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {
    
    double[] probs = new double[inst.numClasses()];
    Instance newInst;
    double sum = 0, max = Double.MIN_VALUE, min = Double.MAX_VALUE;

    for (int i = 0; i < inst.numClasses(); i++) {
      m_ClassFilters[i].input(inst);
      m_ClassFilters[i].batchFinished();
      newInst = m_ClassFilters[i].output();
      probs[i] = m_Classifiers[i].classifyInstance(newInst);
      if (probs[i] > 1) {
        probs[i] = 1;
      }
      if (probs[i] < 0){
	probs[i] = 0;
      }
      sum += probs[i];
    }
    if (sum != 0) {
      Utils.normalize(probs, sum);
    } 
    return probs;
  }

  /**
   * Prints the classifiers.
   */
  @Override
  public String toString() {

    if (m_Classifiers == null) {
      return "Classification via Regression: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("Classification via Regression\n\n");
    for (int i = 0; i < m_Classifiers.length; i++) {
      text.append("Classifier for class with index " + i + ":\n\n");
      text.append(m_Classifiers[i].toString() + "\n\n");
    }
    return text.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options for the learner
   */
  public static void main(String [] argv){

    Classifier scheme;

    try {
      scheme = new ClassificationViaRegression();
      System.out.println(Evaluation.evaluateModel(scheme,argv));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
