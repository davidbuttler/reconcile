/*
 *    RDG1.java
 *    Copyright (C) 2000 Gabi Schmidberger.
 *
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

package reconcile.weka.datagenerators;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import reconcile.weka.core.Attribute;
import reconcile.weka.core.FastVector;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Utils;


/** 
 * Class to generate data randomly by producing a decision list.
 * The decision list consists of rules.
 * Instances are generated randomly one by one. If decision list fails
 * to classify the current instance, a new rule according to this current
 * instance is generated and added to the decision list.<p>
 *
 * The option -V switches on voting, which means that at the end
 * of the generation all instances are
 * reclassified to the class value that is supported by the most rules.<p>
 *
 * This data generator can generate 'boolean' attributes (= nominal with
 * the values {true, false}) and numeric attributes. The rules can be
 * 'A' or 'NOT A' for boolean values and 'B < random_value' or
 * 'B >= random_value' for numeric values.<p> 
 *
 * Valid options are:<p>
 *
 * -R num <br>
 * The maximum number of attributes chosen to form a rule (default 10).<p>
 *
 * -M num <br>
 * The minimum number of attributes chosen to form a rule (default 1).<p>
 *
 * -I num <br>
 * The number of irrelevant attributes (default 0).<p>
 *
 * -N num <br>
 * The number of numeric attributes (default 0).<p>
 *
 * -S seed <br>
 * Random number seed for random function used (default 1). <p>
 *
 * -V <br>
 * Flag to use voting. <p>
 *
 * Following an example of a generated dataset: <br>
 *
 * %<br>
 * % weka.datagenerators.RDG1 -r expl -a 2 -c 3 -n 4 -N 1 -I 0 -M 2 -R 10 -S 2<br>
 * %<br>
 * relation expl<br>
 *<br>
 * attribute a0 {false,true}<br>
 * attribute a1 numeric<br>
 * attribute class {c0,c1,c2}<br>
 *<br>
 * data<br>
 *<br>
 * true,0.496823,c0<br>
 * false,0.743158,c1<br>
 * false,0.408285,c1<br>
 * false,0.993687,c2<br>
 * %<br>
 * % Number of attributes chosen as irrelevant = 0<br>
 * %<br>
 * % DECISIONLIST (number of rules = 3):<br>
 * % RULE 0:   c0 := a1 < 0.986, a0<br>
 * % RULE 1:   c1 := a1 < 0.95, not(a0)<br>
 * % RULE 2:   c2 := not(a0), a1 >= 0.562<br>
 *<p>
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
 **/
public class RDG1 extends Generator implements OptionHandler,
			                       Serializable {

  /*
   * class to represent decisionlist
   */
  private class RuleList implements Serializable {

    /**@serial rule list */
    private FastVector m_RuleList = null;
    
    /**@serial class */
    double m_ClassValue = 0.0;

    public double getClassValue() { return m_ClassValue; }
    
    public void setClassValue(double newClassValue) {
      m_ClassValue = newClassValue;
    }
    
    private void addTest (Test newTest) { 
      if (m_RuleList == null)
	m_RuleList = new FastVector();
      
      m_RuleList.addElement(newTest);
    }
    
    private double classifyInstance (Instance example) throws Exception {
      boolean passedAllTests = true;
      for (Enumeration e = m_RuleList.elements(); 
	   passedAllTests && e.hasMoreElements(); ) {
	Test test = (Test) e.nextElement();
	passedAllTests = test.passesTest(example);
      }
      if (passedAllTests) return m_ClassValue;
      else return -1.0;
    }
    
    public String toString () {
      StringBuffer str = new StringBuffer();
      str = str.append("  c" + (int) m_ClassValue + " := ");
      Enumeration e = m_RuleList.elements();
      if (e.hasMoreElements()) {
	Test test = (Test) e.nextElement();
	str = str.append(test.toPrologString()); 
      }
      while (e.hasMoreElements()) {
	Test test = (Test) e.nextElement();
	str = str.append(", " + test.toPrologString());       
      }
      return str.toString();
    } 
    
  } /*end class RuleList ******/

  /**@serial maximum rule size*/ 
  private int m_MaxRuleSize = 10;
  
  /**@serial minimum rule size*/ 
  private int m_MinRuleSize = 1;
  
  /**@serial number of irrelevant attributes.*/
  private int m_NumIrrelevant = 0;

  /**@serial number of numeric attribute*/
  private int m_NumNumeric = 0;

  /**@serial random number generator seed*/ 
  private int m_Seed = 1;
 
  /**@serial flag that stores if voting is wished*/ 
  private boolean m_VoteFlag = false;

  /**@serial dataset format*/ 
  private Instances m_DatasetFormat = null;

  /**@serial random number generator*/ 
  private Random m_Random = null;

   /**@serial decision list */
  private FastVector m_DecisionList = null;

  /**@serial array defines which attributes are irrelevant, with: */
  /* true = attribute is irrelevant; false = attribute is not irrelevant*/
  boolean [] m_AttList_Irr;

  /**@serial debug flag*/ 
  private int m_Debug = 0;

  /**
   * Returns a string describing this data generator.
   *
   * @return a description of the data generator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    
    return "A data generator that produces data randomly "
           + "with \'boolean\' (nominal with values {false,true}) and"
           + "numeric attributes by producing a decisionlist.";
  }

 /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(5);

    newVector.addElement(new Option(
              "\tmaximum size for rules (default 10) ",
              "R", 1, "-R <num>"));
    newVector.addElement(new Option(
              "\tminimum size for rules (default 1) ",
              "M", 1, "-M <num>"));
    newVector.addElement(new Option(
              "\tnumber of irrelevant attributes (default 0)",
              "I", 1, "-I <num>"));
    newVector.addElement(new Option(
              "\tnumber of numeric attributes (default 0)",
              "N", 1, "-N"));
    newVector.addElement(new Option(
              "\tseed for random function (default 1)",
              "S", 1, "-S"));
    newVector.addElement(new Option(
              "\tswitch on voting (default is no voting)",
              "V", 1, "-V"));
    return newVector.elements();
  }

  /**
   * Parses a list of options for this object. <p>
   *
   * For list of valid options see class description.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    boolean voting = false;

    String ruleSizeString = Utils.getOption('R', options);
    if (ruleSizeString.length() != 0) {
      setMaxRuleSize((int)Double.valueOf(ruleSizeString).doubleValue());
    } else {
      setMaxRuleSize(10);
    }

    ruleSizeString = Utils.getOption('M', options);
    if (ruleSizeString.length() != 0) {
      setMinRuleSize((int)Double.valueOf(ruleSizeString).doubleValue());
    } else {
      setMinRuleSize(1);
    }

    String numIrrelevantString = Utils.getOption('I', options);
    if (numIrrelevantString.length() != 0) {
      setNumIrrelevant((int)Double.valueOf(numIrrelevantString).doubleValue());
    } else {
      setNumIrrelevant(0);
    }

    if ((getNumAttributes() - getNumIrrelevant()) < getMinRuleSize())
       throw new Exception("Possible rule size is below minimal rule size.");

    String numNumericString = Utils.getOption('N', options);
    if (numNumericString.length() != 0) {
      setNumNumeric((int)Double.valueOf(numNumericString).doubleValue());
    } else {
      setNumNumeric(0);
    }

    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setSeed(Integer.parseInt(seedString));
    } else {
      setSeed(1);
    }
   
    voting = Utils.getFlag('V', options);
    setVoteFlag(voting);
  }

  /**
   * Gets the current settings of the datagenerator RDG1.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [12];
    int current = 0;
    options[current++] = "-N"; options[current++] = "" + getNumNumeric();
    options[current++] = "-I"; options[current++] = "" + getNumIrrelevant();
    options[current++] = "-M"; options[current++] = "" + getMinRuleSize();
    options[current++] = "-R"; options[current++] = "" + getMaxRuleSize();
    options[current++] = "-S"; options[current++] = "" + getSeed();
    if (getVoteFlag()) {
      options[current++] = "-V"; 
      options[current++] = "";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }
    
  /**
   * Gets the random generator.
   *
   * @return the random generator
   */
  public Random getRandom() {
    if (m_Random == null) {
      m_Random = new Random (getSeed());
    }
    return m_Random;
  }
  
  /**
   * Sets the random generator.
   *
   * @param newRandom is the random generator.
   */
  public void setRandom(Random newRandom) {
    m_Random = newRandom;
  }

  /**
   * Gets the maximum number of tests in rules.
   *
   * @return the maximum number of tests allowed in rules
   */
  public int getMaxRuleSize() { return m_MaxRuleSize; }
  
  /**
   * Sets the maximum number of tests in rules.
   *
   * @param newMaxRuleSize new maximum number of tests allowed in rules.
   */
  public void setMaxRuleSize(int newMaxRuleSize) {
    m_MaxRuleSize = newMaxRuleSize;
  }

  /**
   * Gets the minimum number of tests in rules.
   *
   * @return the minimum number of tests allowed in rules
   */
  public int getMinRuleSize() { return m_MinRuleSize; }
  
  /**
   * Sets the minimum number of tests in rules.
   *
   * @param newMinRuleSize new minimum number of test in rules.
   */
  public void setMinRuleSize(int newMinRuleSize) {
    m_MinRuleSize = newMinRuleSize;
  }

  /**
   * Gets the number of irrelevant attributes.
   *
   * @return the number of irrelevant attributes
   */
  public int getNumIrrelevant() { return m_NumIrrelevant; }
  
  /**
   * Sets the number of irrelevant attributes.
   *
   * @param the number of irrelevant attributes.
   */
  public void setNumIrrelevant(int newNumIrrelevant) {
    m_NumIrrelevant = newNumIrrelevant;
  }

  /**
   * Gets the number of numerical attributes.
   *
   * @return the number of numerical attributes.
   */
  public int getNumNumeric() { return m_NumNumeric; }
  
  /**
   * Sets the number of numerical attributes.
   *
   * @param the number of numerical attributes.
   */
  public void setNumNumeric(int newNumNumeric) { 
    m_NumNumeric = newNumNumeric;
  }

  /**
   * Gets the vote flag.
   *
   * @return voting flag.
   */
  public boolean getVoteFlag() { return m_VoteFlag; }
  
  /**
   * Sets the vote flag.
   *
   * @param newVoteFlag boolean with the new setting of the vote flag.
   */
  public void setVoteFlag(boolean newVoteFlag) { m_VoteFlag = newVoteFlag; }  

  /**
   * Gets the single mode flag.
   *
   * @return true if methode generateExample can be used.
   */
  public boolean getSingleModeFlag() { return (getVoteFlag() == false); }
  
  /**
   * Gets the random number seed.
   *
   * @return the random number seed.
   */
  public int getSeed() { return m_Seed; }
  
  /**
   * Sets the random number seed.
   *
   * @param newSeed the new random number seed.
   */
  public void setSeed(int newSeed) { m_Seed = newSeed; }  

  /**
   * Gets the dataset format.
   *
   * @return the dataset format.
   */
  public Instances getDatasetFormat() { return m_DatasetFormat; }
  
  /**
   * Sets the dataset format.
   *
   * @param newDatasetFormat the new dataset format.
   */
  public void setDatasetFormat(Instances newDatasetFormat) { 
    m_DatasetFormat = newDatasetFormat;
  }  

  /**
   * Gets the array that defines which of the attributes
   * are seen to be irrelevant.
   *
   * @return the array that defines the irrelevant attributes
   */
  public boolean [] getAttList_Irr() { return m_AttList_Irr; }
  
  /**
   * Sets the array that defines which of the attributes
   * are seen to be irrelevant.
   *
   * @param newAttList_Irr array that defines the irrelevant attributes.
   */
  public void setAttList_Irr(boolean [] newAttList_Irr) {

    m_AttList_Irr = newAttList_Irr;
  }

  /**
   * Initializes the format for the dataset produced. 
   *
   * @return the output data format
   * @exception Exception data format could not be defined 
   */

  public Instances defineDataFormat() throws Exception {

    Instances dataset;
    Random random = new Random (getSeed());
    setRandom(random);

    m_DecisionList = new FastVector();

    // number of examples is the same as given per option
    setNumExamplesAct(getNumExamples());

    // define dataset
    dataset = defineDataset(random);
    return dataset; 
  }

  /**
   * Generate an example of the dataset dataset. 
   * @return the instance generated
   * @exception Exception if format not defined or generating <br>
   * examples one by one is not possible, because voting is chosen
   */

  public Instance generateExample() throws Exception {

    Random random = getRandom();
    Instances format = getDatasetFormat();
    if (format == null) throw new Exception("Dataset format not defined.");
    if (getVoteFlag()) throw new Exception("Examples cannot be generated" +
                                           " one by one.");

    // generate values for all attributes
    format = generateExamples(1, random, format);

    return (format.lastInstance());
  }

  /**
   * Generate all examples of the dataset. 
   * @return the instance generated
   * @exception Exception if format not defined or generating <br>
   * examples one by one is not possible, because voting is chosen
   */

  public Instances generateExamples() throws Exception {

    Random random = getRandom();
    Instances format = getDatasetFormat();
    if (format == null) throw new Exception("Dataset format not defined.");

    // generate values for all attributes
    format = generateExamples(getNumExamplesAct(), random, format);

    // vote all examples, and set new class value
    if (getVoteFlag())
      format = voteDataset(format);

    return (format);
  }

  /**
   * Generate all examples of the dataset. 
   * @return the instance generated
   * @exception Exception if format not defined or generating <br>
   * examples one by one is not possible, because voting is chosen
   */

  public Instances generateExamples(int num, 
                                   Random random,
                                   Instances format) throws Exception {

    if (format == null) throw new Exception("Dataset format not defined.");
    
    // generate values for all attributes
    for (int i = 0; i < num; i++)  {
      // over all examples to be produced
      Instance example =  generateExample(random, format);

      // set class of example using decision list
      boolean classDefined = classifyExample(example);
      if (!classDefined) {
        // set class with newly generated rule
        example = updateDecisionList(random, example);
      }
      example.setDataset(format);
      format.add(example);
    }

    return (format);
  }

 /**
   * Generates a new rule for the decision list.
   * and classifies the new example
   * @param random random number generator
   * @param example example used to update decision list 
   */
  private Instance updateDecisionList(Random random, Instance example)
   throws Exception {

    FastVector TestList;
    Instances format = getDatasetFormat();
    if (format == null) throw new Exception("Dataset format not defined.");

    TestList = generateTestList(random, example);

    int maxSize = getMaxRuleSize() < TestList.size() ? 
                            getMaxRuleSize() : TestList.size();
    int ruleSize = ((int) (random.nextDouble() * 
                             (double) (maxSize - getMinRuleSize())))
                                   + getMinRuleSize();

    RuleList newRule = new RuleList();
    for (int i=0; i < ruleSize; i++) {
      int testIndex = (int) (random.nextDouble() * (double) TestList.size());
      Test test = (Test) TestList.elementAt(testIndex);
          
      newRule.addTest(test);
      TestList.removeElementAt(testIndex);
//      newRule.addTest((Test) TestList.elementAt(
//                       (int) (random.nextDouble() * (double) ruleSize)));
    }
    double newClassValue = 0.0;
    if (m_DecisionList.size() > 0) {
      RuleList r = (RuleList)(m_DecisionList.lastElement());
      double oldClassValue = (double) 
                        (r.getClassValue());
      newClassValue = (double)((int)oldClassValue + 1)
                               % getNumClasses();
    }
    newRule.setClassValue(newClassValue);
    m_DecisionList.addElement(newRule);
    example = (Instance)example.copy();
    example.setDataset(format);
    example.setClassValue((float)newClassValue);
    return example;
  }

 /**
   * Generates a new rule for the decision list
   * and classifies the new example.
   *
   * @param random random number generator
   * @param example 
   */
  private FastVector generateTestList(Random random, Instance example) 
   throws Exception {

    Instances format = getDatasetFormat();
    if (format == null) throw new Exception("Dataset format not defined.");

    int numTests = getNumAttributes() - getNumIrrelevant();
    FastVector TestList = new FastVector(numTests);
    boolean [] irrelevant = getAttList_Irr();

    for (int i = 0; i < getNumAttributes(); i++) {
      if (!irrelevant[i]) {
        Test newTest = null;
        Attribute att = example.attribute(i);
        if (att.isNumeric()) {
          double newSplit = random.nextDouble();
          boolean newNot = newSplit < example.value(i);
          newTest = new Test(i, newSplit, format, newNot);
        } else {
          newTest = new Test(i, example.value(i), format, false);
        }
      TestList.addElement (newTest);     
      }
    }
  return TestList;
  }

 /**
   * Generates an example with its classvalue set to missing
   * and binds it to the datasets.
   *
   * @param random random number generator
   * @param dataset dataset the example gets bind to
   */
  private Instance generateExample(Random random, Instances format) 
    throws Exception {     
    float [] attributes;
    Instance example;

    attributes = new float[getNumAttributes() + 1];
    for (int i = 0; i < getNumAttributes(); i++) {
      double value = random.nextDouble();
      if (format.attribute(i).isNumeric()) {
        attributes[i] = (float)value; 
      } else {
	if (format.attribute(i).isNominal()) {
	  attributes[i] = (value > 0.5)? 1 : 0;
	} else {
	  throw new Exception ("Attribute type is not supported.");
	}
      }
    }
    example = new Instance(0, attributes);
    example.setDataset(format);
    example.setClassMissing();
    return example; 
  }

 /**
   * Tries to classify an example. 
   * 
   * @param example
   */
  private boolean classifyExample(Instance example) throws Exception {
    double classValue = -1.0;  

    for (Enumeration e = m_DecisionList.elements(); 
         e.hasMoreElements() && classValue < 0.0;) {
      RuleList rl = (RuleList) e.nextElement();
      classValue = rl.classifyInstance(example);   
    }
    if (classValue >= 0.0) {
      example.setClassValue((float)classValue);
      return true;
    } else return false;
  }

 /**
   * Classify example with maximum vote the following way.
   * With every rule in the decisionlist, it is evaluated if
   * the given instance could be the class of the rule.
   * Finally the class value that receives the highest number of votes
   * is assigned to the example.
   * 
   * @param example example to be reclassified
   * @return instance with new class value
   */
  private Instance votedReclassifyExample(Instance example) throws Exception {

    boolean classDefined = false; 
    int classVotes [] = new int [getNumClasses()]; 
    for (int i = 0; i < classVotes.length; i++) classVotes[i] = 0; 

    for (Enumeration e = m_DecisionList.elements(); 
         e.hasMoreElements();) {
      RuleList rl = (RuleList) e.nextElement();
      int classValue = (int) rl.classifyInstance(example);
      if (classValue >= 0) classVotes[classValue]++;  
    }
    int maxVote = 0;
    int vote = -1;
    for (int i = 0; i < classVotes.length; i++) {
      if (classVotes[i] > maxVote) {
        maxVote = classVotes[i];
        vote = i; 
      }
    }
    if (vote >= 0) {
      example.setClassValue((float) vote);
    } else
      throw new Exception ("Error in instance classification.");
  return example;
  }

 /**
   * Returns a dataset header.
   * @param random random number generator
   * @return dataset header
   */
  private Instances defineDataset(Random random) throws Exception {

    boolean [] attList_Irr;
    int [] attList_Num;
    FastVector attributes = new FastVector();
    Attribute attribute;
    FastVector nominalValues = new FastVector (2);
    nominalValues.addElement("false"); 
    nominalValues.addElement("true"); 
    FastVector classValues = new FastVector (getNumClasses());
    Instances dataset;
     
    // set randomly those attributes that are irrelevant
    attList_Irr = defineIrrelevant(random);
    setAttList_Irr(attList_Irr);

    // set randomly those attributes that are numeric
    attList_Num = defineNumeric(random); 

    // define dataset
    for (int i = 0; i < getNumAttributes(); i++) {
      if (attList_Num[i] == Attribute.NUMERIC) {
        attribute = new Attribute("a" + i); 
      }
      else {       
        attribute = new Attribute("a" + i, nominalValues); 
      }
      attributes.addElement(attribute);
    }
    int s = classValues.capacity();
    for (int i = 0; i < classValues.capacity(); i++) {
      classValues.addElement("c" + i);
    }
    attribute = new Attribute ("class", classValues); 
    attributes.addElement(attribute);

    dataset = new Instances(getRelationName(), attributes,
                            getNumExamplesAct());
    dataset.setClassIndex(getNumAttributes());

    // set dataset format of this class
    Instances format = new Instances(dataset, 0);
    setDatasetFormat(format);
    return dataset; 
  } 

 /**
   * Defines randomly the attributes as irrelevant.
   * Number of attributes to be set as irrelevant is either set
   * with a preceeding call of setNumIrrelevant() or is per default 0.
   *
   * @param random
   * @return list of boolean values with one value for each attribute,
   * and each value set true or false according to if the corresponding
   * attribute was defined irrelevant or not
   */
  private boolean [] defineIrrelevant(Random random) {

    boolean [] irr = new boolean [getNumAttributes()];
 
    // initialize
    for (int i = 0; i < irr.length; i++) {
      irr[i] = false;
    }
    // set randomly
    int numIrr = 0;
    for (int i = 0; 
         (numIrr < getNumIrrelevant()) && (i < getNumAttributes() * 5);
          i++) {
      int maybeNext = (int) (random.nextDouble() * (double) irr.length);
      if (irr[maybeNext] == false) {
        irr [maybeNext] = true;
        numIrr++;
      }
    }
    return irr;
  }

 /**
   * Chooses randomly the attributes that get datatyp numeric.
   * @param random
   * @return list of integer values, with one value for each attribute,
   * and each value set to Attribut.NOMINAL or Attribut.NUMERIC
   */
  private int [] defineNumeric(Random random) {
    
    int [] num = new int [getNumAttributes()];

    // initialize
    for (int i = 0; i < num.length; i++) {
      num[i] = Attribute.NOMINAL;
    }
    int numNum = 0;
    for (int i = 0;
         (numNum < getNumNumeric()) && (i < getNumAttributes() * 5); i++) {
      int maybeNext = (int) (random.nextDouble() * (double) num.length);
      if (num[maybeNext] != Attribute.NUMERIC) {
        num[maybeNext] = Attribute.NUMERIC;
        numNum++;
      }
    }
    return num;
  }

  /**
   * Compiles documentation about the data generation. This is the number of
   * irrelevant attributes and the decisionlist with all rules.
   * Considering that the decisionlist might get enhanced until
   * the last instance is generated, this method should be called at the
   * end of the data generation process. 
   *
   * @return string with additional information about generated dataset
   * @exception Exception no input structure has been defined
   */
  public String generateFinished() throws Exception {

    StringBuffer dLString = new StringBuffer();

    // string for output at end of ARFF-File
    boolean [] attList_Irr = getAttList_Irr();
    Instances format = getDatasetFormat();
    dLString.append("\n%\n% Number of attributes chosen as irrelevant = " +
                    getNumIrrelevant() + "\n");
    for (int i = 0; i < attList_Irr.length; i++) {
      if (attList_Irr[i])
        dLString.append("% " + format.attribute(i).name() + "\n");
    }

    dLString.append("%\n% DECISIONLIST (number of rules = " +
                    m_DecisionList.size() + "):\n");
     
    for (int i = 0; i < m_DecisionList.size(); i++) {
      RuleList rl = (RuleList) m_DecisionList.elementAt(i);
      dLString.append("% RULE " + i + ": " + rl.toString() + "\n");
      
    }
    
    return dLString.toString();
  }

 /**
   * Resets the class values of all instances using voting.
   * For each instance the class value that satisfies the most rules
   * is choosen as new class value.
   *
   * @param dataset
   * @return the changed instances
   */
  private Instances voteDataset(Instances dataset) throws Exception {
 
  for (int i = 0; i < dataset.numInstances(); i++) {
    Instance inst = dataset.firstInstance();
    inst = votedReclassifyExample(inst); 
    dataset.add(inst);
    dataset.delete(0);
    }  
  return dataset;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments for the data producer: 
   */
  public static void main(String [] argv) {

    try {
      Generator.makeData(new RDG1(), argv);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
