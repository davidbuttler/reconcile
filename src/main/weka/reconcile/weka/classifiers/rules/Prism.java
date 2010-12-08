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
 *    Prism.java
 *    Copyright (C) 1999 Ian H. Witten
 *
 */

package reconcile.weka.classifiers.rules;

import java.io.Serializable;
import java.util.Enumeration;

import reconcile.weka.classifiers.Classifier;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.core.Attribute;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.NoSupportForMissingValuesException;
import reconcile.weka.core.UnsupportedAttributeTypeException;
import reconcile.weka.core.UnsupportedClassTypeException;


/**
 * Class for building and using a PRISM rule set for classifcation.  
 * Can only deal with nominal attributes. Can't deal with missing values.
 * Doesn't do any pruning. For more information, see <p>
 *
 * J. Cendrowska (1987). <i>PRISM: An algorithm for
 * inducing modular rules</i>. International Journal of Man-Machine
 * Studies. Vol.27, No.4, pp.349-370.<p>
 * 
 * @author Ian H. Witten (ihw@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
*/
public class Prism extends Classifier {

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
  return "Class for building and using a PRISM rule set for classification. "
    + "Can only deal with nominal attributes. Can't deal with missing values. "
    + "Doesn't do any pruning. For more information, see \n\n"
    + "J. Cendrowska (1987). \"PRISM: An algorithm for "
    + "inducing modular rules\". International Journal of Man-Machine "
    + "Studies. Vol.27, No.4, pp.349-370.";
  }

  /**
   * Class for storing a PRISM ruleset, i.e. a list of rules
   */
  private class PrismRule implements Serializable {
    
    /** The classification */
    private int m_classification;

    /** The instance */
    private Instances m_instances;

    /** First test of this rule */
    private Test m_test; 

    /** Number of errors made by this rule (will end up 0) */
    private int m_errors; 

    /** The next rule in the list */
    private PrismRule m_next;

    /**
     * Constructor that takes instances and the classification.
     *
     * @param data the instances
     * @param cl the class
     * @exception Exception if something goes wrong
     */
    public PrismRule(Instances data, int cl) throws Exception {

      m_instances = data;
      m_classification = cl;
      m_test = null;
      m_next = null;
      m_errors = 0;
      Enumeration enu = data.enumerateInstances();
      while (enu.hasMoreElements()) {
        if ((int) ((Instance) enu.nextElement()).classValue() != cl) {
	  m_errors++;
	}
      }
      m_instances = new Instances(m_instances, 0);
    }  

    /**
     * Returns the result assigned by this rule to a given instance.
     *
     * @param inst the instance to be classified
     * @return the classification
     */
    public int resultRule(Instance inst) {

      if (m_test == null || m_test.satisfies(inst)) {
	return m_classification;
      } else {
	return -1;
      }
    }

    /**
     * Returns the result assigned by these rules to a given instance.
     *
     * @param inst the instance to be classified
     * @return the classification
     */
    public int resultRules(Instance inst) {

      if (resultRule(inst) != -1) {
	return m_classification;
      } else if (m_next != null) {
	return m_next.resultRules(inst);
      } else {
	return -1;
      }
    }

    /**
     * Returns the set of instances that are covered by this rule.
     *
     * @param data the instances to be checked
     * @return the instances covered
     */
    public Instances coveredBy(Instances data) {

      Instances r = new Instances(data, data.numInstances());
      Enumeration enu = data.enumerateInstances();
      while (enu.hasMoreElements()) {
	Instance i = (Instance) enu.nextElement();
	if (resultRule(i) != -1) {
	  r.add(i);
	}
      }
      r.compactify();
      return r;
    }

    /**
     * Returns the set of instances that are not covered by this rule.
     *
     * @param data the instances to be checked
     * @return the instances not covered
     */
    public Instances notCoveredBy(Instances data) {

      Instances r = new Instances(data, data.numInstances());
      Enumeration enu = data.enumerateInstances();
      while (enu.hasMoreElements()) {
	Instance i = (Instance) enu.nextElement();
	if (resultRule(i) == -1) {
	  r.add(i);
	}
      }
      r.compactify();
      return r;
    }

    /**
     * Prints the set of rules.
     *
     * @return a description of the rules as a string
     */
    public String toString() {

      try {
	StringBuffer text = new StringBuffer();
	if (m_test != null) {
	  text.append("If ");
	  for (Test t = m_test; t != null; t = t.m_next) {
	    if (t.m_attr == -1) {
	      text.append("?");
	    } else {
	      text.append(m_instances.attribute(t.m_attr).name() + " = " +
			  m_instances.attribute(t.m_attr).value(t.m_val));
	    }
	    if (t.m_next != null) {
	      text.append("\n   and ");
	    }
	  }
	  text.append(" then ");
	}
	text.append(m_instances.classAttribute().value(m_classification) + "\n");
	if (m_next != null) {
	  text.append(m_next.toString());
	}
	return text.toString();
      } catch (Exception e) {
	return "Can't print Prism classifier!";
      }
    }
  }
  
  /**
   * Class for storing a list of attribute-value tests
   */
  private class Test implements Serializable { 

    /** Attribute to test */
    private int m_attr = -1; 

    /** The attribute's value */
    private int m_val; 

    /** The next test in the rule */
    private Test m_next = null; 

    /**
     * Returns whether a given instance satisfies this test.
     *
     * @param inst the instance to be tested
     * @return true if the instance satisfies the test
     */
    private boolean satisfies(Instance inst) {

      if ((int) inst.value(m_attr) == m_val) {
        if (m_next == null) {
	  return true;
	} else {
	  return m_next.satisfies(inst);
	}
      }
      return false;    
    }
  }

  /** The first rule in the list of rules */
  private PrismRule m_rules;

  /**
   * Classifies a given instance.
   *
   * @param inst the instance to be classified
   * @return the classification
   */
  public double classifyInstance(Instance inst) {

    int result = m_rules.resultRules(inst);
    if (result == -1) {
      return Instance.missingValue();
    } else {
      return (double)result;
    }
  }

  /**
   * Generates the classifier.
   *
   * @param data the data to be used
   * @exception Exception if the classifier can't built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    int cl; // possible value of theClass
    Instances E, ruleE, emptyDataset;
    PrismRule rule = null;
    Test test = null, oldTest = null;
    int bestCorrect, bestCovers, attUsed;

    if (data.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }
    if (data.classAttribute().isNumeric()) {
      throw new UnsupportedClassTypeException("Prism can't handle a numeric class!");
    }
    data = new Instances(data);
    Enumeration enumAtt = data.enumerateAttributes();
    while (enumAtt.hasMoreElements()) {
      Attribute attr = (Attribute) enumAtt.nextElement();
      if (!attr.isNominal()) {
	throw new UnsupportedAttributeTypeException("Prism can only deal with nominal attributes!");
      }
      Enumeration enu = data.enumerateInstances();
      while (enu.hasMoreElements()) {
	if (((Instance) enu.nextElement()).isMissing(attr)) {
	  throw new NoSupportForMissingValuesException("Prism can't handle attributes with missing values!");
	}
      }
    }
    data.deleteWithMissingClass(); // delete all instances with a missing class
    if (data.numInstances() == 0) {
      throw new Exception("No instances with a class value!");
    }

    for (cl = 0; cl < data.numClasses(); cl++) { // for each class cl
      E = data; // initialize E to the instance set
      while (contains(E, cl)) { // while E contains examples in class cl
        rule = addRule(rule, new PrismRule(E, cl)); // make a new rule
        ruleE = E; // examples covered by this rule
        while (rule.m_errors != 0) { // until the rule is perfect
          test = new Test(); // make a new test
          bestCorrect = bestCovers = attUsed = 0;

          // for every attribute not mentioned in the rule
          enumAtt = ruleE.enumerateAttributes();
          while (enumAtt.hasMoreElements()) {
            Attribute attr = (Attribute) enumAtt.nextElement();
            if (isMentionedIn(attr, rule.m_test)) {
	      attUsed++; 
	      continue;
	    }
            int M = attr.numValues();
            int[] covers = new int [M];
            int[] correct = new int [M];
            for (int j = 0; j < M; j++) {
	      covers[j] = correct[j] = 0;
	    }

            // ... calculate the counts for this class
            Enumeration enu = ruleE.enumerateInstances();
            while (enu.hasMoreElements()) {
              Instance i = (Instance) enu.nextElement();
              covers[(int) i.value(attr)]++;
              if ((int) i.classValue() == cl) {
                correct[(int) i.value(attr)]++;
	      }
            }

            // ... for each value of this attribute, see if this test is better
            for (int val = 0; val < M; val ++) {
              int diff = correct[val] * bestCovers - bestCorrect * covers[val];

              // this is a ratio test, correct/covers vs best correct/covers
              if (test.m_attr == -1
                  || diff > 0 || (diff == 0 && correct[val] > bestCorrect)) {

                // update the rule to use this test
                bestCorrect = correct[val];
                bestCovers = covers[val];
                test.m_attr = attr.index();
                test.m_val = val;
                rule.m_errors = bestCovers - bestCorrect;
              }
            }
          }
	  if (test.m_attr == -1) { // Couldn't find any sensible test
	    break;
	  }
	  oldTest = addTest(rule, oldTest, test);
	  ruleE = rule.coveredBy(ruleE);
	  if (attUsed == (data.numAttributes() - 1)) { // Used all attributes.
	    break;
	  }
        }
        E = rule.notCoveredBy(E);
      }
    }
  }

  /**
   * Add a rule to the ruleset.
   *
   * @param lastRule the last rule in the rule set
   * @param newRule the rule to be added
   * @return the new last rule in the rule set
   */
  private PrismRule addRule(PrismRule lastRule, PrismRule newRule) {

    if (lastRule == null) {
      m_rules = newRule;
    } else {
      lastRule.m_next = newRule;
    }
    return newRule;
  }

  /**
   * Add a test to this rule.
   *
   * @param rule the rule to which test is to be added
   * @param lastTest the rule's last test
   * @param newTest the test to be added
   * @return the new last test of the rule
   */
  private Test addTest(PrismRule rule, Test lastTest, Test newTest) {

    if (rule.m_test == null) {
      rule.m_test = newTest;
    } else {
      lastTest.m_next = newTest;
    }
    return newTest;
  }

  /**
   * Does E contain any examples in the class C?
   *
   * @param E the instances to be checked
   * @param C the class
   * @return true if there are any instances of class C
   */
  private static boolean contains(Instances E, int C) throws Exception {

    Enumeration enu = E.enumerateInstances();
    while (enu.hasMoreElements()) {
      if ((int) ((Instance) enu.nextElement()).classValue() == C) {
	return true;
      }
    }
    return false;
  }

  /**
   * Is this attribute mentioned in the rule?
   *
   * @param attr the attribute to be checked for
   * @param t test contained by rule
   */
  private static boolean isMentionedIn(Attribute attr, Test t) {

    if (t == null) { 
      return false;
    }
    if (t.m_attr == attr.index()) {
      return true;
    }
    return isMentionedIn(attr, t.m_next);
  }    

  /**
   * Prints a description of the classifier.
   *
   * @return a description of the classifier as a string
   */
  public String toString() {

    if (m_rules == null) {
      return "Prism: No model built yet.";
    }
    return "Prism rules\n----------\n" + m_rules.toString();
  }

  /**
   * Main method for testing this class
   */
  public static void main(String[] args) {

    try {
      System.out.println(Evaluation.evaluateModel(new Prism(), args));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}




