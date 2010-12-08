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
 *    StackingC.java
 *    Copyright (C) 1999 Eibe Frank
 *    Copyright (C) 2002 Alexander K. Seewald
 *
 */

package reconcile.weka.classifiers.meta;

import java.util.Random;

import reconcile.weka.classifiers.Classifier;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.classifiers.functions.LinearRegression;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.unsupervised.attribute.MakeIndicator;
import reconcile.weka.filters.unsupervised.attribute.Remove;


/**
 * Implements StackingC (more efficient version of stacking). For more information, see<p>
 *
 *  Seewald A.K.: <i>How to Make Stacking Better and Faster While Also Taking Care
 *  of an Unknown Weakness</i>, in Sammut C., Hoffmann A. (eds.), Proceedings of the
 *  Nineteenth International Conference on Machine Learning (ICML 2002), Morgan
 *  Kaufmann Publishers, pp.554-561, 2002.<p>
 *
 * Valid options are:<p>
 *
 * -X num_folds <br>
 * The number of folds for the cross-validation (default 10).<p>
 *
 * -S seed <br>
 * Random number seed (default 1).<p>
 *
 * -B classifierstring <br>
 * Classifierstring should contain the full class name of a base scheme
 * followed by options to the classifier.
 * (required, option should be used once for each classifier).<p>
 *
 * -M classifierstring <br>
 * Classifierstring for the meta classifier. Same format as for base
 * classifiers. Has to be a numeric prediction scheme, defaults to Linear
 * Regression as in the original paper.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Alexander K. Seewald (alex@seewald.at)
 * @version $Revision: 1.1 $ 
 */
public class StackingC extends Stacking implements OptionHandler {
  
  /** The meta classifiers (one for each class, like in ClassificationViaRegression) */
  protected Classifier [] m_MetaClassifiers = null;
  
  /** Filters to transform metaData */
  protected Remove m_attrFilter = null;
  protected MakeIndicator m_makeIndicatorFilter = null;
      
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Implements StackingC (more efficient version of stacking). For more "
      + "information, see\n\n"
      + "Seewald A.K.: \"How to Make Stacking Better and Faster While Also Taking Care "
      + "of an Unknown Weakness\", in Sammut C., Hoffmann A. (eds.), Proceedings of the "
      + "Nineteenth International Conference on Machine Learning (ICML 2002), Morgan "
      + "Kaufmann Publishers, pp.554-561, 2002.\n\n" 
      + "Note: requires meta classifier to be a numeric prediction scheme.";
  }

  /**
   * The constructor.
   */
  public StackingC() {
    m_MetaClassifier = new reconcile.weka.classifiers.functions.LinearRegression();
    ((LinearRegression)(getMetaClassifier())).
      setAttributeSelectionMethod(new 
	reconcile.weka.core.SelectedTag(1, LinearRegression.TAGS_SELECTION));
  }  

  /**
   * String describing option for setting meta classifier
   */
  protected String metaOption() {

    return "\tFull name of meta classifier, followed by options.\n"
      + "\tMust be a numeric prediction scheme. Default: Linear Regression.";
  }

  /**
   * Process options setting meta classifier.
   */
  protected void processMetaOptions(String[] options) throws Exception {

    String classifierString = Utils.getOption('M', options);
    String [] classifierSpec = Utils.splitOptions(classifierString);
    if (classifierSpec.length != 0) {
      String classifierName = classifierSpec[0];
      classifierSpec[0] = "";
      setMetaClassifier(Classifier.forName(classifierName, classifierSpec));
    } else {
        ((LinearRegression)(getMetaClassifier())).
	  setAttributeSelectionMethod(new 
	    reconcile.weka.core.SelectedTag(1,LinearRegression.TAGS_SELECTION));
    }
  }

  /**
   * Method that builds meta level.
   */
  protected void generateMetaLevel(Instances newData, Random random) 
    throws Exception {

    Instances metaData = metaFormat(newData);
    m_MetaFormat = new Instances(metaData, 0);
    for (int j = 0; j < m_NumFolds; j++) {
      Instances train = newData.trainCV(m_NumFolds, j, random);

      // Build base classifiers
      for (int i = 0; i < m_Classifiers.length; i++) {
	getClassifier(i).buildClassifier(train);
      }

      // Classify test instances and add to meta data
      Instances test = newData.testCV(m_NumFolds, j);
      for (int i = 0; i < test.numInstances(); i++) {
	metaData.add(metaInstance(test.instance(i)));
      }
    }
    
    m_MetaClassifiers = Classifier.makeCopies(m_MetaClassifier,
					      m_BaseFormat.numClasses());
    
    int [] arrIdc = new int[m_Classifiers.length + 1];
    arrIdc[m_Classifiers.length] = metaData.numAttributes() - 1;
    Instances newInsts;
    for (int i = 0; i < m_MetaClassifiers.length; i++) {
      for (int j = 0; j < m_Classifiers.length; j++) {
	arrIdc[j] = m_BaseFormat.numClasses() * j + i;
      }
      m_makeIndicatorFilter = new reconcile.weka.filters.unsupervised.attribute.MakeIndicator();
      m_makeIndicatorFilter.setAttributeIndex("" + (metaData.classIndex() + 1));
      m_makeIndicatorFilter.setNumeric(true);
      m_makeIndicatorFilter.setValueIndex(i);
      m_makeIndicatorFilter.setInputFormat(metaData);
      newInsts = Filter.useFilter(metaData,m_makeIndicatorFilter);
      
      m_attrFilter = new reconcile.weka.filters.unsupervised.attribute.Remove();
      m_attrFilter.setInvertSelection(true);
      m_attrFilter.setAttributeIndicesArray(arrIdc);
      m_attrFilter.setInputFormat(m_makeIndicatorFilter.getOutputFormat());
      newInsts = Filter.useFilter(newInsts,m_attrFilter);
      
      newInsts.setClassIndex(newInsts.numAttributes()-1);
      
      m_MetaClassifiers[i].buildClassifier(newInsts);
    }
  }

  /**
   * Classifies a given instance using the stacked classifier.
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    int [] arrIdc = new int[m_Classifiers.length+1];
    arrIdc[m_Classifiers.length] = m_MetaFormat.numAttributes() - 1;
    double [] classProbs = new double[m_BaseFormat.numClasses()];
    Instance newInst;
    double sum = 0;

    for (int i = 0; i < m_MetaClassifiers.length; i++) {
      for (int j = 0; j < m_Classifiers.length; j++) {
          arrIdc[j] = m_BaseFormat.numClasses() * j + i;
      }
      m_makeIndicatorFilter.setAttributeIndex("" + (m_MetaFormat.classIndex() + 1));
      m_makeIndicatorFilter.setNumeric(true);
      m_makeIndicatorFilter.setValueIndex(i);
      m_makeIndicatorFilter.setInputFormat(m_MetaFormat);
      m_makeIndicatorFilter.input(metaInstance(instance));
      m_makeIndicatorFilter.batchFinished();
      newInst = m_makeIndicatorFilter.output();

      m_attrFilter.setAttributeIndicesArray(arrIdc);
      m_attrFilter.setInvertSelection(true);
      m_attrFilter.setInputFormat(m_makeIndicatorFilter.getOutputFormat());
      m_attrFilter.input(newInst);
      m_attrFilter.batchFinished();
      newInst = m_attrFilter.output();

      classProbs[i]=m_MetaClassifiers[i].classifyInstance(newInst);
      if (classProbs[i] > 1) { classProbs[i] = 1; }
      if (classProbs[i] < 0) { classProbs[i] = 0; }
      sum += classProbs[i];
    }

    if (sum!=0) Utils.normalize(classProbs,sum);

    return classProbs;
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if (m_MetaFormat == null) {
      return "StackingC: No model built yet.";
    }
    String result = "StackingC\n\nBase classifiers\n\n";
    for (int i = 0; i < m_Classifiers.length; i++) {
      result += getClassifier(i).toString() +"\n\n";
    }
   
    result += "\n\nMeta classifiers (one for each class)\n\n";
    for (int i = 0; i< m_MetaClassifiers.length; i++) {
      result += m_MetaClassifiers[i].toString() +"\n\n";
    }

    return result;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new StackingC(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}


