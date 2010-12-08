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
 *    OrdinalClassClassifier.java
 *    Copyright (C) 2001 Mark Hall
 *
 */

package reconcile.weka.classifiers.meta;

import java.util.Enumeration;
import java.util.Vector;

import reconcile.weka.classifiers.Classifier;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.classifiers.SingleClassifierEnhancer;
import reconcile.weka.classifiers.rules.ZeroR;
import reconcile.weka.core.Attribute;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.UnsupportedClassTypeException;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.unsupervised.attribute.MakeIndicator;


/**
 * Meta classifier for transforming an ordinal class problem to a series
 * of binary class problems. For more information see: <p>
 *
 * Frank, E. and Hall, M. (in press). <i>A simple approach to ordinal 
 * prediction.</i> 12th European Conference on Machine Learning. 
 * Freiburg, Germany. <p>
 *
 * Valid options are: <p>
 *
 * -W classname <br>
 * Specify the full class name of a learner as the basis for 
 * the ordinalclassclassifier (required).<p>
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision 1.0 $
 * @see OptionHandler
 */
public class OrdinalClassClassifier extends SingleClassifierEnhancer 
implements OptionHandler {

  /** The classifiers. (One for each class.) */
  private Classifier [] m_Classifiers;

  /** The filters used to transform the class. */
  private MakeIndicator[] m_ClassFilters;

  /** Internal copy of the class attribute for output purposes */
  private Attribute m_ClassAttribute;

  /** ZeroR classifier for when all base classifier return zero probability. */
  private ZeroR m_ZeroR;

  /**
   * String describing default classifier.
   */
  @Override
  protected String defaultClassifierString() {
    
  return "reconcile.weka.classifiers.trees.J48";
  }

  /**
   * Default constructor.
   */
  public OrdinalClassClassifier() {
    m_Classifier = new reconcile.weka.classifiers.trees.J48();
  }

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return " Meta classifier that allows standard classification algorithms "
      +"to be applied to ordinal class problems.  For more information see: "
      +"Frank, E. and Hall, M. (in press). A simple approach to ordinal "
      +"prediction. 12th European Conference on Machine Learning. Freiburg, "
      +"Germany.";
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

    if (!insts.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException("OrdinalClassClassifier: class should " +
					      "be declared nominal!");
    }
    
    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }
    m_ZeroR = new ZeroR();
    m_ZeroR.buildClassifier(insts);

    int numClassifiers = insts.numClasses() - 1;

    numClassifiers = (numClassifiers == 0) ? 1 : numClassifiers;

    if (numClassifiers == 1) {
      m_Classifiers = Classifier.makeCopies(m_Classifier, 1);
      m_Classifiers[0].buildClassifier(insts);
    } else {
      m_Classifiers = Classifier.makeCopies(m_Classifier, numClassifiers);
      m_ClassFilters = new MakeIndicator[numClassifiers];

      for (int i = 0; i < m_Classifiers.length; i++) {
	m_ClassFilters[i] = new MakeIndicator();
	m_ClassFilters[i].setAttributeIndex("" + (insts.classIndex() + 1));
	m_ClassFilters[i].setValueIndices(""+(i+2)+"-last");
	m_ClassFilters[i].setNumeric(false);
	m_ClassFilters[i].setInputFormat(insts);
	newInsts = Filter.useFilter(insts, m_ClassFilters[i]);
	m_Classifiers[i].buildClassifier(newInsts);
      }
    }
    m_ClassAttribute = insts.classAttribute();
  }
  
  /**
   * Returns the distribution for an instance.
   *
   * @exception Exception if the distribution can't be computed successfully
   */
  @Override
  public double [] distributionForInstance(Instance inst) throws Exception {
    
    if (m_Classifiers.length == 1) {
      return m_Classifiers[0].distributionForInstance(inst);
    }

    double [] probs = new double[inst.numClasses()];
    
    double [][] distributions = new double[m_ClassFilters.length][0];
    for(int i = 0; i < m_ClassFilters.length; i++) {
      m_ClassFilters[i].input(inst);
      m_ClassFilters[i].batchFinished();
      
      distributions[i] = m_Classifiers[i].
	distributionForInstance(m_ClassFilters[i].output());
      
    }

    for (int i = 0; i < inst.numClasses(); i++) {
      if (i == 0) {
	probs[i] = distributions[0][0];
      } else if (i == inst.numClasses() - 1) {
	probs[i] = distributions[i - 1][1];
      } else {
	probs[i] = distributions[i - 1][1] - distributions[i][1];
	if (!(probs[i] > 0)) {
	  System.err.println("Warning: estimated probability " + probs[i] +
	  		     ". Rounding to 0.");
	  probs[i] = 0;
	}
      }
    }

    if (Utils.gr(Utils.sum(probs), 0)) {
      Utils.normalize(probs);
      return probs;
    } else {
      return m_ZeroR.distributionForInstance(inst);
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration listOptions()  {

    Vector vec = new Vector();

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      vec.addElement(enu.nextElement());
    }
    return vec.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classname <br>
   * Specify the full class name of a learner as the basis for 
   * the ordinalclassclassifier (required).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
  
    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String [] getOptions() {
    
    return super.getOptions();
  }
  
  /**
   * Prints the classifiers.
   */
  @Override
  public String toString() {
    
    if (m_Classifiers == null) {
      return "OrdinalClassClassifier: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("OrdinalClassClassifier\n\n");
    for (int i = 0; i < m_Classifiers.length; i++) {
      text.append("Classifier ").append(i + 1);
      if (m_Classifiers[i] != null) {
	 if ((m_ClassFilters != null) && (m_ClassFilters[i] != null)) {
          text.append(", using indicator values: ");
          text.append(m_ClassFilters[i].getValueRange());
        }
        text.append('\n');
        text.append(m_Classifiers[i].toString() + "\n");
      } else {
        text.append(" Skipped (no training examples)\n");
      }
    }

    return text.toString();
  }


  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    Classifier scheme;

    try {
      scheme = new OrdinalClassClassifier();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
