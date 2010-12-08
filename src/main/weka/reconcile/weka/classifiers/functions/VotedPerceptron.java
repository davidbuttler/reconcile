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
 *    VotedPerceptron.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */


package reconcile.weka.classifiers.functions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import reconcile.weka.classifiers.Classifier;
import reconcile.weka.classifiers.CostMatrix;
import reconcile.weka.classifiers.rules.StRipShort;
import reconcile.weka.core.AttributeShort;
import reconcile.weka.core.Instance;
import reconcile.weka.core.InstanceShort;
import reconcile.weka.core.Instances;
import reconcile.weka.core.InstancesShort;
import reconcile.weka.core.ModifiedInstancesShort;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Range;
import reconcile.weka.core.UnsupportedAttributeTypeException;
import reconcile.weka.core.UnsupportedClassTypeException;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.unsupervised.attribute.NominalToBinary;
import reconcile.weka.filters.unsupervised.attribute.ReplaceMissingValues;


/**
 * Implements the voted perceptron algorithm by Freund and
 * Schapire. Globally replaces all missing values, and transforms
 * nominal attributes into binary ones. For more information, see<p>
 *
 * Y. Freund and R. E. Schapire (1998). <i> Large margin
 * classification using the perceptron algorithm</i>.  Proc. 11th
 * Annu. Conf. on Comput. Learning Theory, pp. 209-217, ACM Press, New
 * York, NY. <p>
 *
 * Valid options are:<p>
 *
 * -I num <br>
 * The number of iterations to be performed. (default 1)<p>
 *
 * -E num <br>
 * The exponent for the polynomial kernel. (default 1)<p>
 *
 * -S num <br>
 * The seed for the random number generator. (default 1)<p>
 *
 * -M num <br>
 * The maximum number of alterations allowed. (default 10000) <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
*/
public class VotedPerceptron extends Classifier implements OptionHandler {
  
  /** The maximum number of alterations to the perceptron */
  private int m_MaxK = 10000;

  /** The number of iterations */
  private int m_NumIterations = 1;

  /** The exponent */
  private double m_Exponent = 1.0;

  /** The actual number of alterations */
  private int m_K = 0;

  /** The training instances added to the perceptron */
  private int[] m_Additions = null;

  /** Addition or subtraction? */
  private boolean[] m_IsAddition = null;

  /** The weights for each perceptron */
  private int[] m_Weights = null;
  
  /** The training instances */
  private Instances m_Train = null;

  /** Seed used for shuffling the dataset */
  private int m_Seed = 1;

  /** The filter used to make attributes numeric. */
  private NominalToBinary m_NominalToBinary;

  /** The filter used to get rid of missing values. */
  private ReplaceMissingValues m_ReplaceMissingValues;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Implementation of the voted perceptron algorithm by Freund and "
      +"Schapire. Globally replaces all missing values, and transforms "
      +"nominal attributes into binary ones. For more information, see:\n\n"
      +"Y. Freund and R. E. Schapire (1998). Large margin "
      +"classification using the perceptron algorithm.  Proc. 11th "
      +"Annu. Conf. on Comput. Learning Theory, pp. 209-217, ACM Press, New "
      +"York, NY.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option("\tThe number of iterations to be performed.\n"
				    + "\t(default 1)",
				    "I", 1, "-I <int>"));
    newVector.addElement(new Option("\tThe exponent for the polynomial kernel.\n"
				    + "\t(default 1)",
				    "E", 1, "-E <double>"));
    newVector.addElement(new Option("\tThe seed for the random number generation.\n"
				    + "\t(default 1)",
				    "S", 1, "-S <int>"));
    newVector.addElement(new Option("\tThe maximum number of alterations allowed.\n"
				    + "\t(default 10000)",
				    "M", 1, "-M <int>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -I num <br>
   * The number of iterations to be performed. (default 1)<p>
   *
   * -E num <br>
   * The exponent for the polynomial kernel. (default 1)<p>
   *
   * -S num <br>
   * The seed for the random number generator. (default 1)<p>
   *
   * -M num <br>
   * The maximum number of alterations allowed. (default 10000) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String iterationsString = Utils.getOption('I', options);
    if (iterationsString.length() != 0) {
      m_NumIterations = Integer.parseInt(iterationsString);
    } else {
      m_NumIterations = 1;
    }
    String exponentsString = Utils.getOption('E', options);
    if (exponentsString.length() != 0) {
      m_Exponent = (new Double(exponentsString)).doubleValue();
    } else {
      m_Exponent = 1.0;
    }
    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      m_Seed = Integer.parseInt(seedString);
    } else {
      m_Seed = 1;
    }
    String alterationsString = Utils.getOption('M', options);
    if (alterationsString.length() != 0) {
      m_MaxK = Integer.parseInt(alterationsString);
    } else {
      m_MaxK = 10000;
    }
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {

    String[] options = new String [8];
    int current = 0;

    options[current++] = "-I"; options[current++] = "" + m_NumIterations;
    options[current++] = "-E"; options[current++] = "" + m_Exponent;
    options[current++] = "-S"; options[current++] = "" + m_Seed;
    options[current++] = "-M"; options[current++] = "" + m_MaxK;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Builds the ensemble of perceptrons.
   *
   * @exception Exception if something goes wrong during building
   */
  public void buildClassifier(Instances insts) throws Exception {
 
    if (insts.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }
    if (insts.numClasses() > 2) {
      throw new Exception("Can only handle two-class datasets!");
    }
    if (insts.classAttribute().isNumeric()) {
      throw new UnsupportedClassTypeException("Can't handle a numeric class!");
    }

    // Filter data
    m_Train = new Instances(insts);
    m_Train.deleteWithMissingClass();
    m_ReplaceMissingValues = new ReplaceMissingValues();
    m_ReplaceMissingValues.setInputFormat(m_Train);
    m_Train = Filter.useFilter(m_Train, m_ReplaceMissingValues);
    
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(m_Train);
    m_Train = Filter.useFilter(m_Train, m_NominalToBinary);

    /** Randomize training data */
    m_Train.randomize(new Random(m_Seed));

    /** Make space to store perceptrons */
    m_Additions = new int[m_MaxK + 1];
    m_IsAddition = new boolean[m_MaxK + 1];
    m_Weights = new int[m_MaxK + 1];

    /** Compute perceptrons */
    m_K = 0;
  out:
    for (int it = 0; it < m_NumIterations; it++) {
      for (int i = 0; i < m_Train.numInstances(); i++) {
	Instance inst = m_Train.instance(i);
	if (!inst.classIsMissing()) {
	  int prediction = makePrediction(m_K, inst);
	  int classValue = (int) inst.classValue();
	  if (prediction == classValue) {
	    m_Weights[m_K]++;
	  } else {
	    m_IsAddition[m_K] = (classValue == 1);
	    m_Additions[m_K] = i;
	    m_K++;
	    m_Weights[m_K]++;
	  }
	  if (m_K == m_MaxK) {
	    break out;
	  }
	}
      }
    }
  }

  /**
   * Outputs the distribution for the given output.
   *
   * Pipes output of SVM through sigmoid function.
   * @param inst the instance for which distribution is to be computed
   * @return the distribution
   * @exception Exception if something goes wrong
   */
  public double[] distributionForInstance(Instance inst) throws Exception {

    // Filter instance
    m_ReplaceMissingValues.input(inst);
    m_ReplaceMissingValues.batchFinished();
    inst = m_ReplaceMissingValues.output();

    m_NominalToBinary.input(inst);
    m_NominalToBinary.batchFinished();
    inst = m_NominalToBinary.output();
    
    // Get probabilities
    double output = 0, sumSoFar = 0;
    if (m_K > 0) {
      for (int i = 0; i <= m_K; i++) {
	if (sumSoFar < 0) {
	  output -= m_Weights[i];
	} else {
	  output += m_Weights[i];
	}
	if (m_IsAddition[i]) {
	  sumSoFar += innerProduct(m_Train.instance(m_Additions[i]), inst);
	} else {
	  sumSoFar -= innerProduct(m_Train.instance(m_Additions[i]), inst);
	}
      }
    }
    double[] result = new double[2];
    result[1] = 1 / (1 + Math.exp(-output));
    result[0] = 1 - result[1];

    return result;
  }

  /**
   * Returns textual description of classifier.
   */
  public String toString() {

    return "VotedPerceptron: Number of perceptrons=" + m_K;
  }
 
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxKTipText() {
    return "The maximum number of alterations to the perceptron.";
  }

  /**
   * Get the value of maxK.
   *
   * @return Value of maxK.
   */
  public int getMaxK() {
    
    return m_MaxK;
  }
  
  /**
   * Set the value of maxK.
   *
   * @param v  Value to assign to maxK.
   */
  public void setMaxK(int v) {
    
    m_MaxK = v;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numIterationsTipText() {
    return "Number of iterations to be performed.";
  }

  /**
   * Get the value of NumIterations.
   *
   * @return Value of NumIterations.
   */
  public int getNumIterations() {
    
    return m_NumIterations;
  }
  
  /**
   * Set the value of NumIterations.
   *
   * @param v  Value to assign to NumIterations.
   */
  public void setNumIterations(int v) {
    
    m_NumIterations = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String exponentTipText() {
    return "Exponent for the polynomial kernel.";
  }

  /**
   * Get the value of exponent.
   *
   * @return Value of exponent.
   */
  public double getExponent() {
    
    return m_Exponent;
  }
  
  /**
   * Set the value of exponent.
   *
   * @param v  Value to assign to exponent.
   */
  public void setExponent(double v) {
    
    m_Exponent = v;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "Seed for the random number generator.";
  }

  /**
   * Get the value of Seed.
   *
   * @return Value of Seed.
   */
  public int getSeed() {
    
    return m_Seed;
  }
  
  /**
   * Set the value of Seed.
   *
   * @param v  Value to assign to Seed.
   */
  public void setSeed(int v) {
    
    m_Seed = v;
  }

  /** 
   * Computes the inner product of two instances
   */
  private double innerProduct(Instance i1, Instance i2) throws Exception {

    // we can do a fast dot product
    double result = 0;
    int n1 = i1.numValues(); int n2 = i2.numValues();
    int classIndex = m_Train.classIndex();
    for (int p1 = 0, p2 = 0; p1 < n1 && p2 < n2;) {
        int ind1 = i1.index(p1);
        int ind2 = i2.index(p2);
        if (ind1 == ind2) {
            if (ind1 != classIndex) {
                result += i1.valueSparse(p1) *
                          i2.valueSparse(p2);
            }
            p1++; p2++;
        } else if (ind1 > ind2) {
            p2++;
        } else {
            p1++;
        }
    }
    result += 1.0;
    
    if (m_Exponent != 1) {
      return Math.pow(result, m_Exponent);
    } else {
      return result;
    }
  }

  /** 
   * Compute a prediction from a perceptron
   */
  private int makePrediction(int k, Instance inst) throws Exception {

    double result = 0;
    for (int i = 0; i < k; i++) {
      if (m_IsAddition[i]) {
	result += innerProduct(m_Train.instance(m_Additions[i]), inst);
      } else {
	result -= innerProduct(m_Train.instance(m_Additions[i]), inst);
      }
    }
    if (result < 0) {
      return 0;
    } else {
      return 1;
    }
  }

  /**
   * Main method.
   */
  public static void main(String[] args) {
    try{
      String[] options = args;
      StRipShort classifier = new StRipShort();
      InstancesShort train = null, tempTrain, test = null, template = null;
      int seed = 1, folds = 10, classIndex = -1;
      String trainFileName, testFileName, sourceClass, classIndexString, seedString, foldsString, objectInputFileName, objectOutputFileName, attributeRangeString;
      boolean IRstatistics = false, noOutput = false, printClassifications = false, trainStatistics = true, printMargins = false, printComplexityStatistics = false, printGraph = false, classStatistics = false, printSource = false;
      StringBuffer text = new StringBuffer();
      BufferedReader trainReader = null, testReader = null;
      ObjectInputStream objectInputStream = null;
      CostMatrix costMatrix = null;
      StringBuffer schemeOptionsText = null;
      Range attributesToOutput = null;
      long trainTimeStart = 0, trainTimeElapsed = 0, testTimeStart = 0, testTimeElapsed = 0;

      classIndexString = Utils.getOption('c', options);
      if(classIndexString.length() != 0){
        classIndex = Integer.parseInt(classIndexString);
      }
      trainFileName = Utils.getOption('t', options);
      objectInputFileName = Utils.getOption('l', options);
      objectOutputFileName = Utils.getOption('d', options);
      testFileName = Utils.getOption('T', options);
      if(trainFileName.length() == 0){
        if(objectInputFileName.length() == 0){
          throw new Exception("No training file and no object "
              + "input file given.");
        }
        if(testFileName.length() == 0){
          throw new Exception("No training file and no test " + "file given.");
        }
      }
      try{
        if(trainFileName.length() != 0){
          trainReader = new BufferedReader(new FileReader(trainFileName));
        }
        if(testFileName.length() != 0){
          testReader = new BufferedReader(new FileReader(testFileName));
        }
        if(objectInputFileName.length() != 0){
          InputStream is = new FileInputStream(objectInputFileName);
          if(objectInputFileName.endsWith(".gz")){
            is = new GZIPInputStream(is);
          }
          objectInputStream = new ObjectInputStream(is);
        }
      } catch(Exception e){
        throw new Exception("Can't open file " + e.getMessage() + '.');
      }
      if(testFileName.length() != 0){
        template = test = new InstancesShort(testReader, 1);
        if(classIndex != -1){
          test.setClassIndex(classIndex - 1);
        } else{
          test.setClassIndex(test.numAttributes() - 1);
        }
        if(classIndex > test.numAttributes()){
          throw new Exception("Index of class attribute too large.");
        }
      }
      seedString = Utils.getOption('s', options);
      if(seedString.length() != 0){
        seed = Integer.parseInt(seedString);
      }
      foldsString = Utils.getOption('x', options);
      if(foldsString.length() != 0){
        folds = Integer.parseInt(foldsString);
      }

      classStatistics = Utils.getFlag('i', options);
      noOutput = Utils.getFlag('o', options);
      trainStatistics = !Utils.getFlag('v', options);
      printComplexityStatistics = Utils.getFlag('k', options);
      printMargins = Utils.getFlag('r', options);
      printGraph = Utils.getFlag('g', options);
      sourceClass = Utils.getOption('z', options);
      printSource = (sourceClass.length() != 0);
      for(int i = 0; i < options.length; i++){
        if(options[i].length() != 0){
          if(schemeOptionsText == null){
            schemeOptionsText = new StringBuffer();
          }
          if(options[i].indexOf(' ') != -1){
            schemeOptionsText.append('"' + options[i] + "\" ");
          } else{
            schemeOptionsText.append(options[i] + " ");
          }
        }
      }
      classifier.setOptions(options);
      Utils.checkForRemainingOptions(options);
      train = new ModifiedInstancesShort(trainReader);
      if(classIndex != -1){
        train.setClassIndex(classIndex - 1);
      } else{
        train.setClassIndex(train.numAttributes() - 1);
      }
      train.cleanUpValues();
      //System.err.println(train);
      classifier.buildClassifier(train);
    } catch(Exception e){
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }

  public static InstancesShort transitiveClosure(InstancesShort data,
      AttributeShort closureAttribute) {
    
    ModifiedInstancesShort result = (ModifiedInstancesShort)data;
    AttributeShort DOCID = result.attribute("DOCNUM");
    AttributeShort ID1 = result.attribute("ID1");
    AttributeShort ID2 = result.attribute("ID2");
    AttributeShort CLASS = closureAttribute;
    AttributeShort covered = result.attribute("covered");
    boolean otherClass = !CLASS.equals(covered);
    int positive = CLASS.indexOfValue("+");
    int startPositive = 0, endPositive = 0, startPositiveTotal = 0, endPositiveTotal = 0;
    // Compute the transitive closure of the real_class attribute
    int docnum = -1;
    int[] ptrs = null;
    int start = -1;
    for(int i = 0; i < result.numInstances(); i++){
      // read all instances for a document
      InstanceShort current = result.instance(i);
      // System.out.println("Working on: "+current);
      int currentDoc = (int)current.value(DOCID);
      int id1 = (int)current.value(ID1);
      int id2 = (int)current.value(ID2);
      boolean cl = (int)current.value(CLASS) == positive;
      short cov;
      boolean notCovered;

      if(docnum != currentDoc && docnum != -1){
        // A new document. Finish up the last document.
        // need to loop over all the entries for the document and update the
        // class value.
        for(int k = start; k < i; k++){
          InstanceShort curPair = result.instance(k);
          int curId1 = (int)curPair.value(ID1);
          int curId2 = (int)curPair.value(ID2);
          cov = curPair.value(covered);
          notCovered = cov != positive;
          notCovered = notCovered && otherClass;
          if(find(curId1, ptrs) == find(curId2, ptrs)){
            curPair.setValue(CLASS, "+");
            if(notCovered){
              endPositive += curPair.weight();
              endPositiveTotal++;
            }
          }
        }
      }
      if(docnum == -1 || docnum != currentDoc){
        // A new document document. Some setting up to be done
        int len = id2 + 1;
        ptrs = new int[len];
        // initialize pointers so each item is in it's own set
        for(int j = 0; j < len; j++){
          ptrs[j] = j;
        }
        start = i;
        docnum = currentDoc;
      }
      if(cl){
        union(id1, id2, ptrs);
        cov = current.value(covered);
        notCovered = Double.isNaN(cov) || (int)cov != positive;
        notCovered = notCovered && otherClass;
        if(notCovered){
          startPositive += current.weight();
          startPositiveTotal++;
        }
      }
    }
    for(int k = start; k < result.numInstances(); k++){
      InstanceShort curPair = result.instance(k);
      int curId1 = (int)curPair.value(ID1);
      int curId2 = (int)curPair.value(ID2);
      short cov = curPair.value(covered);
      boolean notCovered = cov != positive;
      notCovered = notCovered && otherClass;
      // System.out.println(curId1+"-"+find(curId1,ptrs)+" and
      // "+curId2+"-"+find(curId2,ptrs));
      if(find(curId1, ptrs) == find(curId2, ptrs)){
        curPair.setValue(CLASS, "+");
        if(notCovered){
          endPositive += curPair.weight();
          endPositiveTotal++;
        }
      }
    }
    //System.err.print("[TC:"+startPositive+"("+startPositiveTotal+")->"
    //		+endPositive+"("+endPositiveTotal+")]");
    return result;
  }

  private static int find(int i, int[] ptrs) {
    // find the set number for the element
    int ind = i;
    while(ind != ptrs[ind]){
      ind = ptrs[ind];
    }
    // fix the link so that it is one hop only
    // note: this doesn't implement the full union-find update
    ptrs[i] = ind;
    return ind;
  }

  private static void union(int i, int j, int[] ptrs) {
    int indI = find(i, ptrs);
    int indJ = find(j, ptrs);
    ptrs[indI] = indJ;
  }
}
    
  
