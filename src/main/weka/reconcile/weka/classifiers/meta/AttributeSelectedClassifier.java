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
 *    AttributeSelectedClassifier.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package reconcile.weka.classifiers.meta;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import reconcile.weka.attributeSelection.ASEvaluation;
import reconcile.weka.attributeSelection.ASSearch;
import reconcile.weka.attributeSelection.AttributeSelection;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.classifiers.SingleClassifierEnhancer;
import reconcile.weka.core.AdditionalMeasureProducer;
import reconcile.weka.core.Drawable;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Utils;
import reconcile.weka.core.WeightedInstancesHandler;


/**
 * Class for running an arbitrary classifier on data that has been reduced
 * through attribute selection. <p>
 *
 * Valid options from the command line are:<p>
 *
 * -W classifierstring <br>
 * Classifierstring should contain the full class name of a classifier.
 * Any options for the classifier should appear at the end of the command line
 * following a "--".
 *.<p>
 *
 * -E evaluatorstring <br>
 * Evaluatorstring should contain the full class name of an attribute
 * evaluator followed by any options.
 * (required).<p>
 *
 * -S searchstring <br>
 * Searchstring should contain the full class name of a search method
 * followed by any options.
 * (required). <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class AttributeSelectedClassifier extends SingleClassifierEnhancer
  implements OptionHandler, Drawable, AdditionalMeasureProducer,
             WeightedInstancesHandler {

  /** The attribute selection object */
  protected AttributeSelection m_AttributeSelection = null;

  /** The attribute evaluator to use */
  protected ASEvaluation m_Evaluator = 
    new reconcile.weka.attributeSelection.CfsSubsetEval();

  /** The search method to use */
  protected ASSearch m_Search = new reconcile.weka.attributeSelection.BestFirst();

  /** The header of the dimensionally reduced data */
  protected Instances m_ReducedHeader;

  /** The number of class vals in the training data (1 if class is numeric) */
  protected int m_numClasses;

  /** The number of attributes selected by the attribute selection phase */
  protected double m_numAttributesSelected;

  /** The time taken to select attributes in milliseconds */
  protected double m_selectionTime;

  /** The time taken to select attributes AND build the classifier */
  protected double m_totalTime;

  
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
  public AttributeSelectedClassifier() {
    m_Classifier = new reconcile.weka.classifiers.trees.J48();
  }

  /**
   * Returns a string describing this search method
   * @return a description of the search method suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Dimensionality of training and test data is reduced by "
      +"attribute selection before being passed on to a classifier.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration listOptions() {
     Vector newVector = new Vector(3);
    
    newVector.addElement(new Option(
	      "\tFull class name of attribute evaluator, followed\n"
	      + "\tby its options. (required)\n"
	      + "\teg: \"weka.attributeSelection.CfsSubsetEval -L\"",
	      "E", 1, "-E <attribute evaluator specification>"));

    newVector.addElement(new Option(
	      "\tFull class name of search method, followed\n"
	      + "\tby its options. (required)\n"
	      + "\teg: \"weka.attributeSelection.BestFirst -D 1\"",
	      "S", 1, "-S <search method specification>"));
    
    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classifierstring <br>
   * Classifierstring should contain the full class name of a classifier.
   * Any options for the classifier should appear at the end of the command line
   * following a "--".<p>
   *
   * -E evaluatorstring <br>
   * Evaluatorstring should contain the full class name of an attribute
   * evaluator followed by any options.
   * (required).<p>
   *
   * -S searchstring <br>
   * Searchstring should contain the full class name of a search method
   * followed by any options.
   * (required). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    // same for attribute evaluator
    String evaluatorString = Utils.getOption('E', options);
    if (evaluatorString.length() == 0) {
      throw new Exception("An attribute evaluator must be specified"
			  + " with the -E option.");
    }
    String [] evaluatorSpec = Utils.splitOptions(evaluatorString);
    if (evaluatorSpec.length == 0) {
      throw new Exception("Invalid attribute evaluator specification string");
    }
    String evaluatorName = evaluatorSpec[0];
    evaluatorSpec[0] = "";
    setEvaluator(ASEvaluation.forName(evaluatorName, evaluatorSpec));

    // same for search method
    String searchString = Utils.getOption('S', options);
    if (searchString.length() == 0) {
      throw new Exception("A search method must be specified"
			  + " with the -S option.");
    }
    String [] searchSpec = Utils.splitOptions(searchString);
    if (searchSpec.length == 0) {
      throw new Exception("Invalid search specification string");
    }
    String searchName = searchSpec[0];
    searchSpec[0] = "";
    setSearch(ASSearch.forName(searchName, searchSpec));

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 4];

    int current = 0;

    // same attribute evaluator
    options[current++] = "-E";
    options[current++] = "" +getEvaluatorSpec();
    
    // same for search
    options[current++] = "-S";
    options[current++] = "" + getSearchSpec();

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);
    
    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String evaluatorTipText() {
    return "Set the attribute evaluator to use. This evaluator is used "
      +"during the attribute selection phase before the classifier is "
      +"invoked.";
  }

  /**
   * Sets the attribute evaluator
   *
   * @param evaluator the evaluator with all options set.
   */
  public void setEvaluator(ASEvaluation evaluator) {
    m_Evaluator = evaluator;
  }

  /**
   * Gets the attribute evaluator used
   *
   * @return the attribute evaluator
   */
  public ASEvaluation getEvaluator() {
    return m_Evaluator;
  }

  /**
   * Gets the evaluator specification string, which contains the class name of
   * the attribute evaluator and any options to it
   *
   * @return the evaluator string.
   */
  protected String getEvaluatorSpec() {
    
    ASEvaluation e = getEvaluator();
    if (e instanceof OptionHandler) {
      return e.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)e).getOptions());
    }
    return e.getClass().getName();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String searchTipText() {
    return "Set the search method. This search method is used "
      +"during the attribute selection phase before the classifier is "
      +"invoked.";
  }
  
  /**
   * Sets the search method
   *
   * @param search the search method with all options set.
   */
  public void setSearch(ASSearch search) {
    m_Search = search;
  }

  /**
   * Gets the search method used
   *
   * @return the search method
   */
  public ASSearch getSearch() {
    return m_Search;
  }

  /**
   * Gets the search specification string, which contains the class name of
   * the search method and any options to it
   *
   * @return the search string.
   */
  protected String getSearchSpec() {
    
    ASSearch s = getSearch();
    if (s instanceof OptionHandler) {
      return s.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)s).getOptions());
    }
    return s.getClass().getName();
  }

  /**
   * Build the classifier on the dimensionally reduced data.
   *
   * @param data the training data
   * @exception Exception if the classifier could not be built successfully
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {
    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }

    if (m_Evaluator == null) {
      throw new Exception("No attribute evaluator has been set!");
    }

    if (m_Search == null) {
      throw new Exception("No search method has been set!");
    }
   
    Instances newData = new Instances(data);
    newData.deleteWithMissingClass();
    if (newData.numInstances() == 0) {
      m_Classifier.buildClassifier(newData);
      return;
    }
    if (newData.classAttribute().isNominal()) {
      m_numClasses = newData.classAttribute().numValues();
    } else {
      m_numClasses = 1;
    }

    Instances resampledData = null;
    if (!(m_Evaluator instanceof WeightedInstancesHandler) || 
        !(m_Classifier instanceof WeightedInstancesHandler)) {
      Random r = new Random(1);
      for (int i = 0; i < 10; i++) {
        r.nextDouble();
      }
      resampledData = newData.resampleWithWeights(r);
    }

    m_AttributeSelection = new AttributeSelection();
    m_AttributeSelection.setEvaluator(m_Evaluator);
    m_AttributeSelection.setSearch(m_Search);
    long start = System.currentTimeMillis();
    m_AttributeSelection.
      SelectAttributes((m_Evaluator instanceof WeightedInstancesHandler) 
                       ? newData
                       : resampledData);
    long end = System.currentTimeMillis();
    if (m_Classifier instanceof WeightedInstancesHandler) {
      newData = m_AttributeSelection.reduceDimensionality(newData);
      m_Classifier.buildClassifier(newData);
    } else {
      resampledData = m_AttributeSelection.reduceDimensionality(resampledData);
      m_Classifier.buildClassifier(resampledData);
    }

    long end2 = System.currentTimeMillis();
    m_numAttributesSelected = m_AttributeSelection.numberAttributesSelected();
    m_ReducedHeader = 
      new Instances((m_Classifier instanceof WeightedInstancesHandler) ?
                    newData
                    : resampledData, 0);
    m_selectionTime = (end - start);
    m_totalTime = (end2 - start);
  }

  /**
   * Classifies a given instance after attribute selection
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  @Override
  public double [] distributionForInstance(Instance instance)
    throws Exception {

    Instance newInstance;
    if (m_AttributeSelection == null) {
      //      throw new Exception("AttributeSelectedClassifier: No model built yet!");
      newInstance = instance;
    } else {
      newInstance = m_AttributeSelection.reduceDimensionality(instance);
    }

    return m_Classifier.distributionForInstance(newInstance);
  }

  /**
   *  Returns the type of graph this classifier
   *  represents.
   */   
  public int graphType() {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graphType();
    else 
      return Drawable.NOT_DRAWABLE;
  }

  /**
   * Returns graph describing the classifier (if possible).
   *
   * @return the graph of the classifier in dotty format
   * @exception Exception if the classifier cannot be graphed
   */
  public String graph() throws Exception {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graph();
    else throw new Exception("Classifier: " + getClassifierSpec()
			     + " cannot be graphed");
  }

  /**
   * Output a representation of this classifier
   */
  @Override
  public String toString() {
    if (m_AttributeSelection == null) {
      return "AttributeSelectedClassifier: No attribute selection possible.\n\n"
	+m_Classifier.toString();
    }

    StringBuffer result = new StringBuffer();
    result.append("AttributeSelectedClassifier:\n\n");
    result.append(m_AttributeSelection.toResultsString());
    result.append("\n\nHeader of reduced data:\n"+m_ReducedHeader.toString());
    result.append("\n\nClassifier Model\n"+m_Classifier.toString());

    return result.toString();
  }

  /**
   * Additional measure --- number of attributes selected
   * @return the number of attributes selected
   */
  public double measureNumAttributesSelected() {
    return m_numAttributesSelected;
  }

  /**
   * Additional measure --- time taken (milliseconds) to select the attributes
   * @return the time taken to select attributes
   */
  public double measureSelectionTime() {
    return m_selectionTime;
  }

  /**
   * Additional measure --- time taken (milliseconds) to select attributes
   * and build the classifier
   * @return the total time (select attributes + build classifier)
   */
  public double measureTime() {
    return m_totalTime;
  }

  /**
   * Returns an enumeration of the additional measure names
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    Vector newVector = new Vector(3);
    newVector.addElement("measureNumAttributesSelected");
    newVector.addElement("measureSelectionTime");
    newVector.addElement("measureTime");
    if (m_Classifier instanceof AdditionalMeasureProducer) {
      Enumeration en = ((AdditionalMeasureProducer)m_Classifier).
	enumerateMeasures();
      while (en.hasMoreElements()) {
	String mname = (String)en.nextElement();
	newVector.addElement(mname);
      }
    }
    return newVector.elements();
  }
  
  /**
   * Returns the value of the named measure
   * @param measureName the name of the measure to query for its value
   * @return the value of the named measure
   * @exception IllegalArgumentException if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    if (additionalMeasureName.compareToIgnoreCase("measureNumAttributesSelected") == 0) {
      return measureNumAttributesSelected();
    } else if (additionalMeasureName.compareToIgnoreCase("measureSelectionTime") == 0) {
      return measureSelectionTime();
    } else if (additionalMeasureName.compareToIgnoreCase("measureTime") == 0) {
      return measureTime();
    } else if (m_Classifier instanceof AdditionalMeasureProducer) {
      return ((AdditionalMeasureProducer)m_Classifier).
	getMeasure(additionalMeasureName);
    } else {
      throw new IllegalArgumentException(additionalMeasureName 
			  + " not supported (AttributeSelectedClassifier)");
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation
			 .evaluateModel(new AttributeSelectedClassifier(),
					argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
