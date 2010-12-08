/*
 * JRip.java Copyright (C) 2001 Xin Xu, Eibe Frank
 */

package reconcile.weka.classifiers.functions;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import reconcile.weka.classifiers.Classifier;
import reconcile.weka.classifiers.CostMatrix;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.core.InstanceShort;
import reconcile.weka.core.Instances;
import reconcile.weka.core.InstancesShort;
import reconcile.weka.core.ModifiedInstancesShort;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Range;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.unsupervised.attribute.NominalToBinary;
import reconcile.weka.filters.unsupervised.attribute.ReplaceMissingValues;


public class PerceptronMarginMNIST extends Classifier implements OptionHandler{
  class IntermediateCalc{
    int lastUpdated;
    double prediction, current;
    public IntermediateCalc(){
      lastUpdated=-1;
      prediction=0;
    }
  }
  
  class Prediction{
    short predClass;
    double score;
    public Prediction(){
      predClass = -1;
      score = Double.MIN_VALUE;
    }
    public void update(double score, short cl){
      if(predClass==-1 || score>this.score){
        predClass = cl;
        this.score=score;
      }
    }
  }

  /** Whether to use full or short output */
  private static boolean m_FullOutput = false;

  /** Ratio of pos/neg examples */
  private static double m_PosWeight = 1;

  /** Ratio of pos/neg examples */
  private static double m_NegWeight = 2;
  
  /** The maximum number of alterations to the perceptron */
  private int m_MaxK = 300000;

  /** The number of iterations */
  private int m_NumIterations = 30;

  /** The exponent */
  private double m_Exponent = 1.0;

  /** The actual number of alterations */
  private int m_K = 0;

  /** The training instances added to the perceptron */
  private int[] m_Additions = null;

  /** Addition or subtraction? */
  private boolean[] m_IsAddition = null;

  /** Any loaded weight vector */
  private static double[] m_Weights = null;
  
  /** The weights for each perceptron */
  private static int[] m_PWeights = null;

  /** The training instances */
  private InstancesShort m_Train = null;

  /** Seed used for shuffling the dataset */
  private int m_Seed = 1;

  /** The filter used to make attributes numeric. */
  private NominalToBinary m_NominalToBinary;

  /** The filter used to get rid of missing values. */
  private ReplaceMissingValues m_ReplaceMissingValues;

  /** Use the standard perceptron update */
  private boolean m_StandardUpdate = true;

  /** Use the standard perceptron update */
  private boolean m_StandardWeight = true;

  /** The update quantity */
  private double m_delta = 1;
  
  /**Store predictions made so far for efficiency issues**/
  private IntermediateCalc[] m_TrainPredictions = null;
  private IntermediateCalc[] m_TestPredictions = null;
  
  
  /**An instance of all zeros, needed by the perceptron**/
  private InstanceShort m_ZeroInstance;
  
  
  /*Filename for saving the weight vector*/
  private static String m_SaveFilename = null;
  
  /*File from which to read initial weights */
  private static String m_InitFilename = null;
  
  /*Which version of the new negative updates to use*/
  private static boolean m_NegUpdate1 = true;

  /*Whether or not only evaluate a weight vector (no training)*/
  private static boolean m_EvaluateOnly = false;
  
  /*Whether to use last or average prediction vector */
  private static boolean m_UseAverage = false;
  
  private static float epsilon = (float)0; 
  
  private static double margin = 0;
  
  /*The kernel degree*/
  private static int m_D = 4;
  
  private static int outputFreq = 1;
  
  private double[][] valueArray;

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option("\tThe number of iterations to be performed.\n" + "\t(default "
        + m_NumIterations + ")", "I", 1, "-I <int>"));
    newVector.addElement(new Option(
        "\tThe degree of the kernel.\n" + "\t(default " + m_D + ")", "D", 1, "-D <int>"));
    newVector.addElement(new Option("\tPositive class relative weight.\n" + "\t(default "
        + m_PosWeight + ")", "P", 1, "-P <int>"));
    newVector.addElement(new Option("\tNegative class relative weight.\n" + "\t(default "
            + m_NegWeight + ")", "N", 1, "-N <int>"));
    newVector.addElement(new Option("\tFilename to save the weight vector.\n" 
        + "\t(default null)", "F", 1, "-F <string>"));
    newVector.addElement(new Option("\tFilename for initializing the weight vector.\n" 
        + "\t(default null)", "L", 1, "-L <string>"));
    newVector.addElement(new Option("\tThe seed for the random number generation.\n"
        + "\t(default 1)", "S", 1, "-S <int>"));
    newVector.addElement(new Option("\tWhether or not to use new update.\n" + "\t(default "
        + m_StandardUpdate + ")", "U", 0, "-U"));
    newVector.addElement(new Option("\tWhether or not to use new negative weight update.\n"
        + "\t(default " + m_StandardUpdate + ")", "W", 0, "-W"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:
   * <p>
   * 
   * -I num <br>
   * The number of iterations to be performed. (default 1)
   * <p>
   * 
   * -E num <br>
   * The exponent for the polynomial kernel. (default 1)
   * <p>
   * 
   * -S num <br>
   * The seed for the random number generator. (default 1)
   * <p>
   * 
   * -M num <br>
   * The maximum number of alterations allowed. (default 10000)
   * <p>
   * 
   * @param options
   *          the list of options as an array of strings
   * @exception Exception
   *              if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String iterationsString = Utils.getOption('I', options);
    if(iterationsString.length() != 0){
      m_NumIterations = Integer.parseInt(iterationsString);
    }
    m_InitFilename = Utils.getOption('L', options);
    m_SaveFilename = Utils.getOption('F', options);

    String kernelPower = Utils.getOption('D', options);
    if(kernelPower.length() != 0){
      m_D = Integer.parseInt(kernelPower);
    }
    String posWeightString = Utils.getOption('P', options);
    if(posWeightString.length() != 0){
      m_PosWeight = Double.parseDouble(posWeightString);
    }
    String marginString = Utils.getOption('M', options);
    if(marginString.length() != 0){
      if(marginString.startsWith("r")){
        margin = -Float.parseFloat(marginString.substring(1,marginString.length()));
      }else
        margin = Float.parseFloat(marginString);
    }
    String seedString = Utils.getOption('S', options);
    if(seedString.length() != 0){
      m_Seed = Integer.parseInt(seedString);
    } else{
      m_Seed = 1;
    }
    String epsilonString = Utils.getOption('E', options);
    if(epsilonString.length() != 0){
      epsilon = Float.parseFloat(epsilonString);
    }
    String ofString = Utils.getOption('O', options);
    if(ofString.length() != 0){
      outputFreq = Integer.parseInt(ofString);
    }
    
    m_StandardUpdate = !Utils.getFlag('U', options);
    m_StandardWeight = !Utils.getFlag('W', options);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {

    String[] options = new String[8];
    int current = 0;

    options[current++] = "-I";
    options[current++] = "" + m_NumIterations;
    options[current++] = "-D";
    options[current++] = "" + m_D;
    options[current++] = "-P";
    options[current++] = "" + m_PosWeight;
    options[current++] = "-E";
    options[current++] = "" + m_EvaluateOnly;
    options[current++] = "-S";
    options[current++] = "" + m_Seed;
    options[current++] = "-M";
    options[current++] = "" + m_MaxK;
    options[current++] = "-F";
    options[current++] = "" + m_SaveFilename;
    options[current++] = "-L";
    options[current++] = "" + m_InitFilename;
    while(current < options.length){
      options[current++] = "";
    }
    return options;
  }
  public double evaluateClassifier(int numP, ModifiedInstancesShort insts, IntermediateCalc[] oldPreds, Prediction[] preds)
  throws Exception{
    return evaluateClassifier(numP, insts, oldPreds, preds, 1.0);
  }
  public double evaluateClassifier(int numP, ModifiedInstancesShort insts, IntermediateCalc[] oldPreds, Prediction[] preds, double predNorm)
      throws Exception {
    if(insts == null)
      return 0;
    int vecLen = insts.numAttributes();
    double[] result = {0, 0};
 

    if(vecLen == 0)
      throw new RuntimeException("No training instances");
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(4);
    nf.setMinimumFractionDigits(4);

    /*Keep track of 6 quantities*/
    int correctPos = 0, totalPos = 0, correctNeg = 0, totalNeg = 0;
    int posPred = 0, negPred = 0;

    for(int i = 0; i < insts.numInstances(); i++){
      InstanceShort inst = insts.instance(i);
      double product = m_UseAverage? makeWeightedPrediction(numP, inst, oldPreds[i]):makePrediction(numP, inst, oldPreds[i]);
      boolean posPrediction =  product  > 0;
      if(preds!=null)
        preds[i].update(product/predNorm, insts.getPositiveClass());
      boolean positive = inst.classValue() == insts.getPositiveClass();
      if(positive) totalPos++;
      if(posPrediction) posPred++; 
      if(positive&&posPrediction) correctPos++;
      if(!positive) totalNeg++;
      if(!posPrediction) negPred++;
      if(!positive&&!posPrediction) correctNeg++;
    }
    double accuracy = (double)(correctPos+correctNeg)/(double)(totalPos+totalNeg);
    double prec = (double)correctPos/(double)posPred;
    double recall = (double)correctPos/(double)totalPos;
    double f1 = (2*prec*recall)/(prec+recall);
    if(m_FullOutput)
      System.out.print("Accuracy "+nf.format(accuracy)+"; F1 " + nf.format(prec) + "|" + nf.format(recall) + "="
          + nf.format(f1));
    else
      //System.out.print(nf.format(accuracy)+";"+nf.format(prec) + "," + nf.format(recall) + "=" + nf.format(f1) + ";");
      System.out.print(nf.format((1-accuracy)*100)+";");
    return accuracy;
  }
  
  public double evaluateSimpleClassifier(double[] w, ModifiedInstancesShort insts, Prediction[] preds) throws Exception{
    return evaluateSimpleClassifier(w, insts, preds, 1);
  }
  public double evaluateSimpleClassifier(double[] w, ModifiedInstancesShort insts, Prediction[] preds, double predNorm)
      throws Exception {
    if(insts == null)
      return 0;
    int vecLen = insts.numAttributes();
    double[] result = {0, 0};
 

    if(vecLen == 0)
      throw new RuntimeException("No training instances");
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(4);
    nf.setMinimumFractionDigits(4);

    /*Keep track of 6 quantities*/
    int correctPos = 0, totalPos = 0, correctNeg = 0, totalNeg = 0;
    int posPred = 0, negPred = 0;

    for(int i = 0; i < insts.numInstances(); i++){
      InstanceShort inst = insts.instance(i);
      double product = makeSimplePrediction(w, inst);
      boolean posPrediction =  product  > 0;
      if(preds!=null)
        preds[i].update(product/predNorm, insts.getPositiveClass());
      boolean positive = inst.classValue() == insts.getPositiveClass();
      if(positive) totalPos++;
      if(posPrediction) posPred++; 
      if(positive&&posPrediction) correctPos++;
      if(!positive) totalNeg++;
      if(!posPrediction) negPred++;
      if(!positive&&!posPrediction) correctNeg++;
    }
    double accuracy = (double)(correctPos+correctNeg)/(double)(totalPos+totalNeg);
    double prec = (double)correctPos/(double)posPred;
    double recall = (double)correctPos/(double)totalPos;
    double f1 = (2*prec*recall)/(prec+recall);
    if(m_FullOutput)
      System.out.print("Accuracy "+nf.format(accuracy)+"; F1 " + nf.format(prec) + "|" + nf.format(recall) + "="
          + nf.format(f1));
    else
      //System.out.print(nf.format(accuracy)+";"+nf.format(prec) + "," + nf.format(recall) + "=" + nf.format(f1) + ";");
      System.out.print(nf.format((1-accuracy)*100)+";");
    return accuracy;
  }

  public void buildClassifier(Instances i) {
    throw new RuntimeException("Not Implemented");
  }

  public void buildClassifier(ModifiedInstancesShort insts) throws Exception {
    buildClassifier(insts, null, null);
  }

  /**
   * Builds the ensemble of perceptrons.
   * 
   * @exception Exception
   *              if something goes wrong during building
   */
  public void buildClassifier(ModifiedInstancesShort insts, ModifiedInstancesShort testInsts, Prediction[][] preds)
      throws Exception {
    if(insts == null)
      return;
    if(!m_StandardUpdate){
      System.err.println("Using novel perceptron update!");
    }
    if(!m_StandardWeight){
      System.err.println("Using the new NEGATIVE perceptron update!");
    }
    if(m_InitFilename != null && m_InitFilename.length() > 0){
      System.err.println("Using initial weights from " + m_InitFilename);
    }
    if(m_SaveFilename != null && m_SaveFilename.length() > 0){
      System.err.println("Saving weights to " + m_SaveFilename);
    }
    System.err.println("Total of " + insts.numAttributes() + " attributes.");
    System.err.println("Margin is " + margin + ". Epsilon is " + epsilon + ". Predicting class "
        + insts.m_Positive);

    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(4);
    nf.setMinimumFractionDigits(4);
    
    m_Train = insts;
    m_ZeroInstance = new InstanceShort(0, new short[insts.numAttributes()]);
    
    //cach all multiplication values for fast computation
    int maxValue=255;
    valueArray=new double[(maxValue+1)][(maxValue+1)];
    for(int i=0;i<=maxValue;i++)
      for(int j=0;j<=maxValue;j++)
        valueArray[i][j]=((double)i/(double)maxValue)*((double)j/(double)maxValue);
    int totalUpdates = 0;
    double r=0;
    if(epsilon<0 || margin<0){
      //Compute R (=max||x_i||)
      for(int i=0; i<insts.numInstances();i++){
        double norm = twoNorm(insts.instance(i)); 
        if(norm>r)
          r=norm;
      }
      System.err.println("R="+r);
    }
    if(epsilon<0){
      float newEpsilon = -epsilon*(float)r;
      System.err.println("Setting eps to "+(-epsilon)+"*R="+newEpsilon);
      epsilon=newEpsilon;
    }
    if(margin<0){
      double newMargin =-margin*r;
      System.err.println("Setting margin to "+(-margin)+"*R="+newMargin);
      margin = newMargin;
    }
    
    double maxAcc = 0, maxAcc1 = 0;
    int maxAccIter = 0, maxAccIter1 = 0;

    /** Make space to store perceptrons */
    m_Additions = new int[m_MaxK + 1];
    m_IsAddition = new boolean[m_MaxK + 1];
    m_PWeights = new int[m_MaxK + 1];
    
    /** Make space to store previous predictions **/
    m_TrainPredictions = new IntermediateCalc[insts.numInstances()];
    for(int k=0; k<insts.numInstances(); k++) m_TrainPredictions[k]=new IntermediateCalc();
    IntermediateCalc[] runningPredictions;
    if(m_UseAverage){
      runningPredictions = new IntermediateCalc[insts.numInstances()];
      for(int k=0; k<insts.numInstances(); k++) runningPredictions[k]=new IntermediateCalc();
    }else{
      runningPredictions = m_TrainPredictions;
    }
    if(testInsts!=null){
      m_TestPredictions = new IntermediateCalc[testInsts.numInstances()];
      for(int k=0; k<testInsts.numInstances(); k++) m_TestPredictions[k]=new IntermediateCalc();
    }
    /** Compute perceptrons */
    m_K = 0; 
    boolean progress = true;
    Date start, end;
    
    for (int it = 0; it < m_NumIterations; it++) {
      int positiveUpdates = 0, negativeUpdates = 0;
      int excludedPos = 0, excludedNeg=0;
      start = new Date();
      for (int i = 0; i < insts.numInstances(); i++) {
        //if(i%10000==0)
         // System.err.print(i+"...");
        InstanceShort inst = insts.instance(i); 
        if (!inst.classIsMissing()) {
          boolean positive = inst.classValue() == insts.getPositiveClass();
          float exWeight = inst.weight();
          int cl = positive?1:-1;
          double prediction = makePrediction(m_K, inst, runningPredictions[i]);
          //System.err.println(prediction+"-"+inst.classValue());
          boolean correctPred = cl*(prediction+exWeight*epsilon)>margin;
          int classValue = (int) inst.classValue();
          if(correctPred){
            m_PWeights[m_K]++;
            if(positive){
              if(prediction <= margin)
                excludedPos++;
            } else{
              if(-prediction <= margin)
                excludedNeg++;
            }
          } else{
            if(positive){
              m_IsAddition[m_K] = true;
              m_Additions[m_K] = i;
              m_K++;
              m_PWeights[m_K]++;

              positiveUpdates++;
              totalUpdates++;

              inst.setWeight(inst.weight() + epsilon);
            } else{
              m_IsAddition[m_K] = false;
              m_Additions[m_K] = i;
              m_K++;
              m_PWeights[m_K]++;

              negativeUpdates++;
              totalUpdates++;

              inst.setWeight(inst.weight() - epsilon);
            }

          }
          if(m_K == m_MaxK){
            throw new RuntimeException("Max number of corrections exceeded");
          }
        }
      }
      end = new Date();
      long milisecs = end.getTime()-start.getTime();
      //System.out.println();
      System.out.print(it + "("+milisecs/1000+"secs)\t");
      int updts = positiveUpdates + negativeUpdates;

      System.out.print(positiveUpdates + "," + negativeUpdates + "(" + excludedPos + ","
          + excludedNeg + ")=" + updts + ";\t");
      evaluateClassifier(m_K, insts, m_TrainPredictions,null);
      double predNorm = predictorNorm(m_K);
      if(testInsts != null){
        double acc = evaluateClassifier(m_K, testInsts, m_TestPredictions, preds[it], predNorm);
        if(acc > maxAcc){
          maxAcc = acc;
          maxAccIter = it;
        }
      }
      boolean outputNorm = true;
      if(outputNorm){
        System.out.println("\tnorm(v)="+nf.format(predNorm)+";Margin="+nf.format(margin>0?margin/predNorm:0));
      }else{
        System.out.println();
      }
    }
    System.out.println("Max accuracy " + nf.format(maxAcc) + " at iteration " + maxAccIter);
    //System.out.println("Max iter accuracy " + nf.format(maxAcc1) + " at iteration " + maxAccIter1);

    evaluateClassifier(m_K, insts, m_TrainPredictions, null);
  }

  /**
   * Builds the ensemble of perceptrons.
   * 
   * @exception Exception
   *              if something goes wrong during building
   */
  public double[] buildSimpleClassifier(ModifiedInstancesShort insts, ModifiedInstancesShort testInsts, Prediction[][] preds)
      throws Exception {
    if(insts == null)
      return null;
    if(!m_StandardUpdate){
      System.err.println("Using novel perceptron update!");
    }
    if(!m_StandardWeight){
      System.err.println("Using the new NEGATIVE perceptron update!");
    }
    if(m_InitFilename != null && m_InitFilename.length() > 0){
      System.err.println("Using initial weights from " + m_InitFilename);
    }
    if(m_SaveFilename != null && m_SaveFilename.length() > 0){
      System.err.println("Saving weights to " + m_SaveFilename);
    }
    System.err.println("Total of " + insts.numAttributes() + " attributes.");
    System.err.println("Margin is " + margin + ". Epsilon is " + epsilon + ". Predicting class "
        + insts.m_Positive);

    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(4);
    nf.setMinimumFractionDigits(4);
    
    m_Train = insts;
    int totalUpdates = 0;
    double r=0;
    if(epsilon<0 || margin<0){
      //Compute R (=max||x_i||)
      for(int i=0; i<insts.numInstances();i++){
        double norm = simpleTwoNorm(insts.instance(i)); 
        if(norm>r)
          r=norm;
      }
      System.err.println("R="+r);
    }
    if(epsilon<0){
      float newEpsilon = -epsilon*(float)r;
      System.err.println("Setting eps to "+(-epsilon)+"*R="+newEpsilon);
      epsilon=newEpsilon;
    }
    if(margin<0){
      double newMargin =-margin*r;
      System.err.println("Setting margin to "+(-margin)+"*R="+newMargin);
      margin = newMargin;
    }
    
    double maxAcc = 0, maxAcc1 = 0;
    int maxAccIter = 0, maxAccIter1 = 0;

    double[] w=new double[insts.numAttributes()+1];
    w[insts.numAttributes()]=1;
    double[] averageW=new double[insts.numAttributes()+1];
    averageW[insts.numAttributes()]=1;
    /** Compute perceptrons */
    m_K = 0; 
    Date start, end;
    
    for (int it = 0; it < m_NumIterations; it++) {
      int positiveUpdates = 0, negativeUpdates = 0;
      int excludedPos = 0, excludedNeg=0;
      start = new Date();
      for (int i = 0; i < insts.numInstances(); i++) {
        //if(i%10000==0)
         // System.err.print(i+"...");
        InstanceShort inst = insts.instance(i); 
        if (!inst.classIsMissing()) {
          boolean positive = inst.classValue() == insts.getPositiveClass();
          float exWeight = inst.weight();
          int cl = positive?1:-1;
          double prediction = makeSimplePrediction(w, inst);
          //System.err.println(prediction+"-"+inst.classValue());
          boolean correctPred = cl*(prediction+exWeight*epsilon)>margin;
          int classValue = (int) inst.classValue();
          if(correctPred){
            addToArray(averageW, w);
            if(positive){
              if(prediction <= margin)
                excludedPos++;
            } else{
              if(-prediction <= margin)
                excludedNeg++;
            }
          } else{
            if(positive){
              addWeight(w, inst);
              positiveUpdates++;
              totalUpdates++;
              addToArray(averageW, w);
              inst.setWeight(inst.weight() + epsilon);
            } else{
              subtractWeight(w, inst);

              negativeUpdates++;
              totalUpdates++;
              addToArray(averageW,w);
              inst.setWeight(inst.weight() - epsilon);
            }

          }
        }
      }
      end = new Date();
      long milisecs = end.getTime()-start.getTime();
      //System.out.println();
      System.out.print(it + "("+milisecs/1000+"secs)\t");
      int updts = positiveUpdates + negativeUpdates;

      System.out.print(positiveUpdates + "," + negativeUpdates + "(" + excludedPos + ","
          + excludedNeg + ")=" + updts + ";\t");
      boolean evaluateAverage = false;
      
      if(evaluateAverage)
        evaluateSimpleClassifier(averageW, insts, null);
      else
        evaluateSimpleClassifier(w, insts, null);
      double predNorm = twoNorm(w);
      
      if(testInsts != null){
        double acc = evaluateAverage?evaluateSimpleClassifier(averageW, testInsts, preds[it], predNorm):
          evaluateSimpleClassifier(w, testInsts, preds[it], predNorm);
        if(acc > maxAcc){
          maxAcc = acc;
          maxAccIter = it;
        }
      }
      boolean outputNorm = true;
      if(outputNorm){
        System.out.println("\tnorm(v)="+nf.format(predNorm)+";Margin="+nf.format(margin>0?margin/predNorm:0));
      }else{
        System.out.println();
      }
    }
    System.out.println("Max accuracy " + nf.format(maxAcc) + " at iteration " + maxAccIter);
    //System.out.println("Max iter accuracy " + nf.format(maxAcc1) + " at iteration " + maxAccIter1);

    evaluateSimpleClassifier(w, insts, null);
    return w;
  }
  
  /*This function computes the length of the current prediction vector*/
  public double predictorNorm(int k) throws Exception{
    double result = 0;
    for(int i=0; i<k; i++){
      if(m_IsAddition[i]){
        result += makePrediction(k, m_Train.instance(m_Additions[i]),m_TrainPredictions[m_Additions[i]]);
      } else{
        result -= makePrediction(k, m_Train.instance(m_Additions[i]),m_TrainPredictions[m_Additions[i]]);
      }
    }
    if(result<0)
      throw new RuntimeException("Te norm is "+result);
    return Math.sqrt(result);
  }
  public double twoNorm(double[] w){
    double result = 0;
    for(int i = 0; i < w.length; i++)
      result += w[i] * w[i];
    return Math.sqrt(result);
  }

  public double twoNorm(InstanceShort in) {
    double result = kernel(in,in);
    return Math.sqrt(result);
  }

  public double simpleTwoNorm(InstanceShort in) {
    double result = 1;
    for(int j = 0; j < in.numAttributes(); j++){
      if(in.attribute(j).isFeature()){
        result += in.value(j) * in.value(j);
      }
    }
    return Math.sqrt(result);
  }

  public void addToArray(double[] a1, double[] a2) {
    for(int i = 0; i < a1.length; i++)
      a1[i] += a2[i];
  }

  public String printWeightVector(double[] w) {
    String result = "";
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf = NumberFormat.getNumberInstance();
    nf.setMinimumFractionDigits(2);
    nf.setMaximumFractionDigits(2);
    for(int i = 0; i < w.length; i++){
      result += nf.format(w[i]) + ":";
    }
    return result;
  }


  private double kernel(InstanceShort i1, InstanceShort i2) {
    double result = Math.pow(1+simpleInnerProduct(i1,i2),m_D);
    return result;
  }
  private static double kernel(double[] w, InstanceShort i) {
    double result = Math.pow(1+simpleInnerProduct(w,i),m_D);
    return result;
  }
  
  private static double simpleInnerProduct(double[] w, InstanceShort i) {
    if(w.length != i.numAttributes())
      throw new RuntimeException("Wrong number of attributes");
    double result = 0;
    for(int j = 0; j < i.numAttributes(); j++){
      if(i.attribute(j).isFeature()){
        result += i.value(j) * w[j];
      }
    }
    //System.err.println("VAL="+result);
    return result;
  }

  /**
   * Computes the inner product of two instances
   */
  private double simpleInnerProduct(InstanceShort i1, InstanceShort i2) {


    double result = 0;
    short[] insArray1=i1.getValueArray(), insArray2=i2.getValueArray();
    int n1 = insArray1.length;
    int n2 = insArray2.length;
    if(n1!=n2)
      throw new RuntimeException("Number of attributes mismatch");
    int classIndex = m_Train.classIndex();
    if(classIndex!=n1-1)
      throw new RuntimeException("Class index in an inexpected place!");
    int numFeatures = n1-1;
    for(int i = 0; i < numFeatures; i++){
      //result += (insArray1[i]/65025.0)*insArray2[i];       
      result += valueArray[insArray1[i]][insArray2[i]];       
    }

    return result;   
  } 

  /**
   * Compute a prediction from a perceptron
   */
  private double makePrediction(int k, InstanceShort inst, IntermediateCalc ic) throws Exception {

    double result = ic.prediction;
    int startFrom = ic.lastUpdated;
    //double result = 0;
    //int startFrom = -1;
    if (startFrom ==-1){
//    result += m_PWeights[i+1]*kernel(m_ZeroInstance, inst);
      result += kernel(m_ZeroInstance, inst);
      startFrom = 0;
    }
    for(int i = startFrom; i < k; i++){
      if(m_IsAddition[i]){
        //result += m_PWeights[i+1]*kernel(m_Train.instance(m_Additions[i]), inst);
        result += kernel(m_Train.instance(m_Additions[i]), inst);
      } else{
        //result -= m_PWeights[i+1]*kernel(m_Train.instance(m_Additions[i]), inst);
        result -= kernel(m_Train.instance(m_Additions[i]), inst);
      }
    }
    ic.prediction=result;
    ic.lastUpdated=k;
    return result;
  }
  
  /**
   * Compute a prediction from a perceptron
   */
  private double makeSimplePrediction(double[]w, InstanceShort inst) throws Exception {
    double result=0;
    for(int i = 0; i < inst.numAttributes(); i++){
      result+=((double)inst.value(i)/255.0)*w[i];
    }
    result+=w[inst.numAttributes()];
    return result;
  }
  
  /**
   * Compute a prediction from a perceptron
   */
  private double makeWeightedPrediction(int k, InstanceShort inst, IntermediateCalc ic) throws Exception {

 
    double current = kernel(m_ZeroInstance, inst);
    double result = m_PWeights[0]*current;

    for(int i = 0; i < k; i++){
      if(m_IsAddition[i]){
        current += kernel(m_Train.instance(m_Additions[i]), inst);
      } else{
        current -= kernel(m_Train.instance(m_Additions[i]), inst);
      }
      result +=m_PWeights[i+1]*current;
    }

    return result; 
    
  }

 
  /**
   * Main method.
   * 
   * @param args
   *          the options for the classifier
   */
  public static void main(String[] args) {
    try{
      String[] options = args;
      PerceptronMarginMNIST classifier = new PerceptronMarginMNIST();
      ModifiedInstancesShort trainData = null, testData = null;
      ModifiedInstancesShort trainAll = null, testAll =null, tempTrain, test = null, template = null;
      int seed = 1, folds = 10, classIndex = -1;
      String trainFileName, testFileName, sourceClass, classIndexString, seedString, foldsString, objectInputFileName, objectOutputFileName, attributeRangeString;
      boolean IRstatistics = false, noOutput = false, printClassifications = false, trainStatistics = true, printMargins = false, printComplexityStatistics = false, printGraph = false, classStatistics = false, printSource = false;
      StringBuffer text = new StringBuffer();
      BufferedReader trainReader = null, testReader = null;
      DataInputStream trainFile = null, testFile = null, trainClassFile = null, testClassFile = null;
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
          System.out.println(Evaluation.makeOptionString(classifier));
          throw new Exception("No training file and no object " + "input file given.");
        }
        if(testFileName.length() == 0){
          System.out.println(Evaluation.makeOptionString(classifier));
          throw new Exception("No training file and no test " + "file given.");
        }
      }
      try{
        if(trainFileName.length() != 0){
          if(trainFileName.endsWith(".idx")){
            trainFile = new DataInputStream(new FileInputStream(trainFileName));
            String trainClassFileName = trainFileName.replaceFirst("images-idx3","labels-idx1");
            trainClassFile = new DataInputStream(new FileInputStream(trainClassFileName));
          }else
            trainReader = new BufferedReader(new FileReader(trainFileName));
        }
        if(testFileName.length() != 0){
          if(testFileName.endsWith(".idx")){
            testFile = new DataInputStream(new FileInputStream(testFileName));
            String testClassFileName = testFileName.replaceFirst("images-idx3","labels-idx1");
            testClassFile = new DataInputStream(new FileInputStream(testClassFileName));
          }else
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
      if(trainReader!=null){
        trainAll = new ModifiedInstancesShort(trainReader);
        trainAll.setClass(trainAll.attribute("class"));
        trainAll.cleanUpValuesAndSetWeight(0);
        trainAll = trainAll.binarizeValues();
        trainAll.normalizeAttrValues();
      }else{
        System.err.println("Reading idx file "+trainFileName);
        trainAll = new ModifiedInstancesShort(trainFile, trainClassFile);
        trainAll.setClass(trainAll.attribute("class"));
      }
      System.err.println("Read "+trainAll.numInstances()+" training instances.");
    
      trainAll.setClass(trainAll.attribute("class"));
      trainAll.randomize(new Random(20));

     //AttributeShort predClass =
     // trainAll.classAttribute().copy("predicted_class");
      //trainAll.insertAttributeAt(predClass, trainAll.numAttributes());
      //System.err.println(trainAll);
      if(testReader != null || testFile != null){
        if(testReader!=null){
          testAll = new ModifiedInstancesShort(testReader);
          testAll.setClass(testAll.attribute("class"));
          testAll.cleanUpValuesAndSetWeight(0);
          testAll = testAll.binarizeValues();
          testAll.normalizeAttrValues(trainAll);
        }else{
          testAll = new ModifiedInstancesShort(testFile, testClassFile);
          testAll.setClass(testAll.attribute("class"));
        }
        System.err.println("Read " + testAll.numInstances() + " test instances.");

        //testAll.insertAttributeAt(predClass, testAll.numAttributes());

        testAll.setClass(testAll.attribute("class"));
        //System.err.println(trainAll);
        trainData = trainAll;
        testData = testAll;
      } else{
        System.err.println("No test file given splitting the train file");

        //data = ModifiedInstancesShort.splitDocs(trainAll);
        //Use half of the data for training and half for evaluation
        int splits = 3;
        int split = ((splits - 1) * trainAll.numInstances()) / splits;
        //int mid = data.length / 2;
        trainData = new ModifiedInstancesShort(trainAll, split);
        testData = new ModifiedInstancesShort(trainAll, trainAll.numInstances() - split);

        for(int i = 0; i < split; i++){
          trainData.add(trainAll.instance(i));
        }
        for(int i = split; i < trainAll.numInstances(); i++){
          testData.add(trainAll.instance(i));
        }

      }

      if(m_InitFilename.length() != 0){
        ArrayList weights = new ArrayList();
        try{
          BufferedReader file = new BufferedReader(new FileReader(m_InitFilename));
          String line;
          while((line = file.readLine())!=null){
            double w = Double.parseDouble(line);
            weights.add(new Double(w));
          }
          m_Weights = new double[weights.size()];
          for(int y=0; y<weights.size();y++){
            m_Weights[y]=((Double)weights.get(y)).doubleValue();
          }
        }catch(IOException ioe){
          throw new RuntimeException(ioe);
        }
      }
      if(m_EvaluateOnly){
        classifier.evaluateClassifier(classifier.m_K, testData, classifier.m_TestPredictions,null);
      } else{
        System.out.println("Train " + trainData.numInstances() + " insts. Test "
            + testData.numInstances() + " insts.");
        
        int numTest=testData.numInstances();
        Prediction[][] overallPreds = new Prediction[classifier.m_NumIterations][numTest];
        for(int i=0; i<classifier.m_NumIterations; i++)
          for(int j=0; j<numTest; j++)
            overallPreds[i][j]=((PerceptronMarginMNIST)classifier).new Prediction();
        
        for(short clss = 0; clss<=9; clss++){
          if(epsilon!=0){
            for(int t=0;t<trainData.numInstances();t++){
              trainData.instance(t).setWeight(0);
            }
          }
          trainData.setPositiveClass(clss);
          testData.setPositiveClass(clss);
          if(m_D>1){
            classifier.buildClassifier(trainData, testData, overallPreds);
            classifier.evaluateClassifier(classifier.m_K, testData, classifier.m_TestPredictions, null);
          }else{
            double[] w = classifier.buildSimpleClassifier(trainData, testData, overallPreds);
            classifier.evaluateSimpleClassifier(w, testData, null);
          }
        }
        overallEvaluation(testData, overallPreds);
      }
    } catch(Exception e){
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
    System.out.println();
  }

  public static void overallEvaluation(InstancesShort insts, Prediction[][] preds){
    int iterNum = preds.length;
    int instNum = insts.numInstances();
    System.out.println();
    for(int it = 0; it<iterNum; it++){
      System.out.print("Iteration "+it+": ");
      int correct = 0, incorrect = 0;
      for(int j=0;j<instNum; j++){
        InstanceShort ins = insts.instance(j);
        if(preds[it][j].predClass==ins.classValue())
          correct++;
        else{
          incorrect++;
          //System.err.println("Predicted "+preds[it][j].predClass+" true "+ins.classValue());
        }
      }
      if(correct+incorrect!=instNum)
        throw new RuntimeException("Evaluation error");
      double error = ((double)incorrect/(double)instNum)*100.0;
      System.out.println(error);
    }
  }
  
  /**
   * Returns textual description of classifier.
   */
  public String toString() {

    return "VotedPerceptron: Number of perceptrons=" + m_K;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
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
   * @param v
   *          Value to assign to maxK.
   */
  public void setMaxK(int v) {

    m_MaxK = v;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
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
   * @param v
   *          Value to assign to NumIterations.
   */
  public void setNumIterations(int v) {

    m_NumIterations = v;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
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
   * @param v
   *          Value to assign to exponent.
   */
  public void setExponent(double v) {

    m_Exponent = v;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
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
   * @param v
   *          Value to assign to Seed.
   */
  public void setSeed(int v) {

    m_Seed = v;
  }
  /**
   * Returns a string describing this classifier
   * 
   * @return a description of the classifier suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Implementation of the voted perceptron algorithm by Freund and "
        + "Schapire. Globally replaces all missing values, and transforms "
        + "nominal attributes into binary ones. For more information, see:\n\n"
        + "Y. Freund and R. E. Schapire (1998). Large margin "
        + "classification using the perceptron algorithm.  Proc. 11th "
        + "Annu. Conf. on Comput. Learning Theory, pp. 209-217, ACM Press, New " + "York, NY.";
  }
  public void addWeight(double[] w, InstanceShort inst) {
    for(int j = 0; j < inst.numAttributes(); j++){
      if(inst.attribute(j).isFeature()){
        w[j] = w[j] + (double)inst.value(j)/255;
      }
    }
    w[inst.numAttributes()]+=1;
  }

  public void subtractWeight(double[] w, InstanceShort inst) {
    for(int j = 0; j < inst.numAttributes(); j++){
      if(inst.attribute(j).isFeature()){
        w[j] = w[j] - (double)inst.value(j)/255;
      }
    }
    w[inst.numAttributes()]-=1;
  }

  public double[] divideArray(double[] w, int divider) {
    double[] result = new double[w.length];
    for(int i = 0; i < w.length; i++)
      result[i] = w[i] / divider;
    return result;
  }

  public double[] multiplyArray(double[] w, int mult) {
    double[] result = new double[w.length];
    for(int i = 0; i < w.length; i++)
      result[i] = w[i] * mult;
    return result;
  }
  
}
  