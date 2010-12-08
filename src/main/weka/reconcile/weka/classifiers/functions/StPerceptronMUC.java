/*
 * JRip.java Copyright (C) 2001 Xin Xu, Eibe Frank
 */

package reconcile.weka.classifiers.functions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import reconcile.scorers.BCubedScore;
import reconcile.scorers.LeanDocument;
import reconcile.scorers.MUCScore;
import reconcile.weka.classifiers.Classifier;
import reconcile.weka.classifiers.CostMatrix;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.core.AttributeShort;
import reconcile.weka.core.Instance;
import reconcile.weka.core.InstanceShort;
import reconcile.weka.core.Instances;
import reconcile.weka.core.ModifiedInstancesShort;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Range;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.unsupervised.attribute.NominalToBinary;
import reconcile.weka.filters.unsupervised.attribute.ReplaceMissingValues;


public class StPerceptronMUC extends Classifier implements OptionHandler{
  /** The similarity threshold */
  private static double THRESHOLD = 0;

  /** Whether to use full or short output */
  private static boolean m_FullOutput = false;

  /** Ratio of pos/neg examples */
  private static double m_PosWeight = 1;

  /** Ratio of pos/neg examples */
  private static double m_NegWeight = 2;
  
  /** The maximum number of alterations to the perceptron */
  private int m_MaxK = 10000;

  /** The number of iterations */
  private int m_NumIterations = 25;

  /** The exponent */
  private double m_Exponent = 1.0;

  /** The actual number of alterations */
  private int m_K = 0;

  /** The training instances added to the perceptron */
  private int[] m_Additions = null;

  /** Addition or subtraction? */
  private boolean[] m_IsAddition = null;

  /** The weights for each perceptron */
  private static double[] m_Weights = null;

  /** The training instances */
  private Instances m_Train = null;

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
  
  /** Whether not to output debug infor */
  private static boolean m_Debug = false;
  
  /*Filename for saving the weight vector*/
  private static String m_SaveFilename = null;
  
  /*File from which to read initial weights */
  private static String m_InitFilename = null;
  
  /*Which version of the new negative updates to use*/
  private static boolean m_NegUpdate1 = true;

  /*Whether or not only evaluate a weight vector (no training)*/
  private static boolean m_EvaluateOnly = false;
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
        "\tThe decision threshold.\n" + "\t(default " + THRESHOLD + ")", "D", 1, "-D <int>"));
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

    String thresholdString = Utils.getOption('D', options);
    if(thresholdString.length() != 0){
      THRESHOLD = Double.parseDouble(thresholdString);
    }
    String posWeightString = Utils.getOption('P', options);
    if(posWeightString.length() != 0){
      m_PosWeight = Double.parseDouble(posWeightString);
    }
    String negWeightString = Utils.getOption('N', options);
    if(negWeightString.length() != 0){
        m_NegWeight = Double.parseDouble(negWeightString);
    }
    String seedString = Utils.getOption('S', options);
    if(seedString.length() != 0){
      m_Seed = Integer.parseInt(seedString);
    } else{
      m_Seed = 1;
    }
    String alterationsString = Utils.getOption('M', options);
    if(alterationsString.length() != 0){
      m_MaxK = Integer.parseInt(alterationsString);
    } else{
      m_MaxK = 10000;
    }
    
    m_StandardUpdate = !Utils.getFlag('U', options);
    m_StandardWeight = !Utils.getFlag('W', options);
    m_EvaluateOnly = Utils.getFlag('E', options);
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
    options[current++] = "" + THRESHOLD;
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

  public static double[] evaluateClassifier(ModifiedInstancesShort[] insts, double[] w, LeanDocument[] truth)
      throws Exception {
    if(insts == null)
      return null;
    int vecLen = 0;
    double[] result = {0, 0};
    //get the lenght of the feature vectors
    for(int i = 0; i < insts.length && vecLen == 0; i++){
      ModifiedInstancesShort ins = insts[i];
      if(ins != null){
        vecLen = ins.numAttributes();
      }
    }
    if(vecLen == 0)
      throw new RuntimeException("No training instances");
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(4);
    nf.setMinimumFractionDigits(4);

    if(truth == null){
      truth = new LeanDocument[insts.length];
      for(int i = 0; i < truth.length; i++){
        truth[i] = makeDocument(insts[i]);
        //System.err.println(truth[i]);
      }
    }

    int total = 0;
    int totalInst = 0, totalPred = 0;
    int totalPrecision = 0, totalNumNPsPrecision = 0;
    int totalNumNPsRecall = 0, totalRecall = 0;
    double mucTotalPrecision = 0, mucTotalRecall = 0;
    int mucTotalRecallNum = 0, mucTotalPrecisionNum = 0, mucTotalRecallDenom = 0, mucTotalPrecisionDenom = 0;;
    int numDocuments = insts.length;
    for(int d = 0; d < insts.length; d++){
      ModifiedInstancesShort doc = insts[d];
      LeanDocument tr = truth[d];
      int[] ptrs = apply(w, doc);
      int[] pred = positiveIdentification(ptrs, doc);
      LeanDocument cDoc = makeDocument(doc, ptrs);
      total += pred[0];
      totalPred += pred[1];
      totalInst += pred[2];
      double recall;
      int numNPsRecall = 0, numNPsPrecision = 0;
      if(tr.numNounPhrases() > 1){
        recall = BCubedScore.bCubedScore(tr, cDoc);
        numNPsRecall = tr.numNounPhrases();
      } else{
        recall = 0;
      }
      totalRecall += recall;
      totalNumNPsRecall += numNPsRecall;
      recall = numNPsRecall == 0?0:recall / numNPsRecall;

      double precision;
      if(cDoc.numNounPhrases() > 1){
        precision = BCubedScore.bCubedScore(cDoc, tr);
        numNPsPrecision = cDoc.numNounPhrases();
      } else{
        precision = 0;
      }
      totalPrecision += precision;
      totalNumNPsPrecision += numNPsPrecision;
      precision = numNPsPrecision == 0?0:precision / numNPsPrecision;
      
      int[] mucRecall = new int[2], mucPrecision = new int[2];
      if(tr.numNounPhrases()>0){
        mucRecall = MUCScore.mucScore(tr, cDoc);
        mucPrecision = MUCScore.mucScore(cDoc, tr);
      }else
        numDocuments--;
      mucTotalRecallNum += mucRecall[0];
      mucTotalRecallDenom += mucRecall[1];
      mucTotalPrecisionNum += mucPrecision[0];
      mucTotalPrecisionDenom += mucPrecision[1];

    }
    //System.out.print("Precision " +totalPrecision +"/"+totalNumNPsPrecision+
    // " recall "+totalRecall);
    double precPos = totalPred == 0?0:total / (double)totalPred;
    double recallPos = total == 0?0:total / (double)totalInst;
    double f1Pos = precPos + recallPos == 0?0:(2 * precPos * recallPos) / (precPos + recallPos);
    if(m_FullOutput)
      System.out.print("F1 " + nf.format(precPos) + "|" + nf.format(recallPos) + "="
          + nf.format(f1Pos));
    else
      System.out.print(nf.format(precPos) + "," + nf.format(recallPos) + "=" + nf.format(f1Pos)
          + ";");

    double prec = totalNumNPsPrecision == 0?1:(double)totalPrecision / totalNumNPsPrecision;
    double rec = totalNumNPsRecall == 0?1:(double)totalRecall / totalNumNPsRecall;
    double f1, denom = prec + rec;
    f1 = denom > 0?(2 * prec * rec) / denom:0;
    if(m_FullOutput){
      System.out.print(" prec=" + nf.format(prec));
      System.out.print(" recall=" + nf.format(rec));
      System.out.println(" f1=" + nf.format(f1));
    } else{
      System.out.print(nf.format(prec) + ",");
      System.out.print(nf.format(rec) + "=");
      System.out.print(nf.format(f1) + ";");
    }
    //System.err.println("prec"+mucTotalPrecision+"rec "+mucTotalRecall+"num "+numDocuments);
    mucTotalRecall = mucTotalRecallDenom>0?(double)mucTotalRecallNum/(double)mucTotalRecallDenom:0;
    mucTotalPrecision = mucTotalPrecisionDenom>0?(double)mucTotalPrecisionNum/(double)mucTotalPrecisionDenom:0;
    double mucF1,mucDenom = mucTotalPrecision + mucTotalRecall;
    mucF1 = mucDenom >0?(2*mucTotalPrecision*mucTotalRecall)/mucDenom:0;
    if(m_FullOutput){
      System.out.print(" MUCprec=" + nf.format(mucTotalPrecision));
      System.out.print(" MUCrecall=" + nf.format(mucTotalRecall));
      System.out.println(" MUCf1=" + nf.format(mucF1));
    } else{
      System.out.print(nf.format(mucTotalPrecision) + ",");
      System.out.print(nf.format(mucTotalRecall) + "=");
      System.out.print(nf.format(mucF1) + ";");
    }
    result[0] = f1Pos;
    result[1] = f1;
    return result;
  }

  public void buildClassifier(Instances i) {
    throw new RuntimeException("Not Implemented");
  }

  public double[] buildClassifier(ModifiedInstancesShort[] insts) throws Exception {
    return buildClassifier(insts, null);
  }

  /**
   * Builds the ensemble of perceptrons.
   * 
   * @exception Exception
   *              if something goes wrong during building
   */
  public double[] buildClassifier(ModifiedInstancesShort[] insts, ModifiedInstancesShort[] testInsts)
      throws Exception {
    if(insts == null)
      return null;
    if(!m_StandardUpdate){
      System.err.println("Using novel perceptron update!");
    }
    if(!m_StandardWeight){
      System.err.println("Using the new NEGATIVE perceptron update!");
    }
    if(m_InitFilename!=null && m_InitFilename.length()>0){
      System.err.println("Using initial weights from "+m_InitFilename);
    }
    if(m_SaveFilename!=null && m_SaveFilename.length()>0){
      System.err.println("Saving weights to "+m_SaveFilename);
    }    
    System.err.println("Total of " + insts[0].numAttributes() + " attributes.");
    System.err.println("Delta is " + m_delta + ". Threshold is " + THRESHOLD + ". Pos weight is "
        + m_PosWeight + ". Neg weight "+m_NegWeight);
    int vecLen = 0;
    //get the lenght of the feature vectors
    for(int i = 0; i < insts.length && vecLen == 0; i++){
      ModifiedInstancesShort ins = insts[i];
      if(ins != null){
        vecLen = ins.numAttributes();
      }
    }
    if(vecLen == 0)
      throw new RuntimeException("No training instances");
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(4);
    nf.setMinimumFractionDigits(4);

    LeanDocument[] truth = new LeanDocument[insts.length];
    for(int i = 0; i < truth.length; i++){
      truth[i] = makeDocument(insts[i]);
      //System.err.println(truth[i]);
    }

    LeanDocument[] testTruth = null;
    if(testInsts != null){
      testTruth = new LeanDocument[testInsts.length];
      for(int i = 0; i < testTruth.length; i++){
        testTruth[i] = makeDocument(testInsts[i]);
        //System.err.println(truth[i]);
      }
    }
    //The weight vector, w
    double[] w = new double[vecLen], totalW = new double[vecLen];
    int totalUpdates = 0;
    if(m_Weights == null){
      for(int i = 0; i < vecLen; i++)
        totalW[i] = w[i] = 0;
    } else{
      if(vecLen!=m_Weights.length)
        throw new RuntimeException("Invalid initialization");
      for(int i = 0; i < vecLen; i++){
        totalW[i] = 0;
        w[i] = m_Weights[i];
      }
      System.err.println("Loaded weight vector");
    }
    //Compute the perceptron weights
    double delta = m_delta;
    int numIterations = m_NumIterations;
    double maxAcc = 0, maxBCubbed = 0;
    int maxAccIter = 0, maxBCubbedIter = 0;
    for(int iter = 0; iter < numIterations; iter++){
      double[] iterW = new double[vecLen];
      int positiveUpdates = 0, negativeUpdates = 0;
      for(int d = 0; d < insts.length; d++){
        //if(d%10==0)
        //  System.out.print(d+"...");
        ModifiedInstancesShort doc = insts[d];
        int muc[][] = {{0, 0},{0,0}};
        LeanDocument tr = truth[d];
        if(m_Debug)
          System.out.println("Gold standard "+tr);
        LeanDocument cDoc = applyAndSetUpMUC(doc, tr, w, muc);
        
        for(int i = 0; i < doc.numInstances(); i++){
          InstanceShort inst = doc.instance(i);
          boolean changed = false;
          boolean positive = inst.classValue() == doc.m_Positive;
          short id1 = inst.value(doc.m_ID1);
          short id2 = inst.value(doc.m_ID2);
          if(positive){
            //The gold standard says that this instance should be positive
            if(cDoc.getClusterNum(new Integer(id1)).intValue() != cDoc.getClusterNum(new Integer(id2)).intValue()){
              //if(inst.value(doc.m_Prediction) == doc.m_Positive){
              if(m_Debug)
                System.err.print(cDoc.getClusterNum(new Integer(id1)) + "-" + cDoc.getClusterNum(new Integer(id2))
                    + " ");
              int[] recall=muc[0],precision = muc[1];
              int nom = recall[0], denom=recall[1];
              double rCor = ((double)nom+1)/((double)denom);
              double p;
              if(precision[0]==0&&precision[1]==0)
                p=1;
              else
                p = ((double)precision[0])/((double)precision[1]);
              double r;
              if(recall[0]==0&&recall[1]==0)
                r =0;
              else
                r = ((double)recall[0])/((double)recall[1]);
              double correction = computeCorection(p, r, 0, rCor, tr.numNounPhrases());
              if(m_Debug)
                System.err.println(iter + ": " + id1 + "," + id2 + "\tpos: " + correction);
              addWeight(w, inst, correction);
              positiveUpdates++;
              cDoc = applyAndSetUpMUC(doc, tr, w, muc);
            }
          } else{
            if(cDoc.getClusterNum(new Integer(id1)).intValue() == cDoc.getClusterNum(new Integer(id2)).intValue()){
              if(inst.value(doc.m_Prediction) == doc.m_Positive){
                int[] recall=muc[0],precision = muc[1];
                int nom = precision[0], denom=precision[1];
                double pCor = ((double)nom)/((double)denom-1);
                double p;
                if(precision[0]==0&&precision[1]==0)
                  p=1;
                else
                  p = ((double)precision[0])/((double)precision[1]);
                double r;
                if(recall[0]==0&&recall[1]==0)
                  r =0;
                else
                  r = ((double)recall[0])/((double)recall[1]);
                TreeMap goldItems1 = tr.getElChain(new Integer(id1));
                TreeMap goldItems2 = tr.getElChain(new Integer(id2));
                int trId1 = tr.getClusterNum(new Integer(id1)).intValue();
                int trId2 = tr.getClusterNum(new Integer(id2)).intValue();
                TreeMap items = cDoc.getElChain(new Integer(id1));
                HashSet outside = new HashSet(items.keySet());
                outside.removeAll(goldItems1.keySet());
                outside.removeAll(goldItems2.keySet());

                int numItemsGold1 = goldItems1.size(), numItemsGold2 = goldItems2.size();
                int numItems = items.size();
                int numCrossing1 = 0, numCrossing2 = 0;

                //Count the number of crossing links out of gold standard
                // cluster 1
                Iterator cl1Iter = goldItems1.keySet().iterator();
                while(cl1Iter.hasNext()){
                  Integer el1 = (Integer)cl1Iter.next();
                  Iterator cl2Iter = goldItems2.keySet().iterator();
                  while(cl2Iter.hasNext()){
                    Integer el2 = (Integer)cl2Iter.next();
                    InstanceShort curItem = getItem(doc, el1.intValue(), el2.intValue());
                    if(curItem.value(doc.m_Prediction) == doc.m_Positive){
                      numCrossing1++;
                      numCrossing2++;
                    }
                  }
                }
                Iterator outIterator = outside.iterator();
                while(outIterator.hasNext()){
                  Integer el = (Integer)outIterator.next();
                  Iterator clIter1 = goldItems1.keySet().iterator();
                  while(clIter1.hasNext()){
                    Integer el1 = (Integer)clIter1.next();
                    InstanceShort curItem = getItem(doc, el.intValue(), el1.intValue());
                    if(curItem.value(doc.m_Prediction) == doc.m_Positive){
                      numCrossing1++;
                    }
                  }
                  Iterator clIter2 = goldItems2.keySet().iterator();
                  while(clIter2.hasNext()){
                    Integer el2 = (Integer)clIter2.next();
                    InstanceShort curItem = getItem(doc, el.intValue(), el2.intValue());
                    if(curItem.value(doc.m_Prediction) == doc.m_Positive){
                      numCrossing2++;
                    }
                  }
                }
                int numCrossing = numCrossing1>numCrossing2?numCrossing1:numCrossing2;
                double correction = computeCorection(p, r, pCor, 0, tr.numNounPhrases())/ (double)numCrossing;
                if(m_Debug)
                  System.err.println(iter + ": " + id1 + "," + id2 + "\tneg: " + correction);
                subtractWeight(w, inst, correction);
                negativeUpdates++;
                cDoc = applyAndSetUpMUC(doc, tr, w, muc);
              }
            }
          }

          totalUpdates++;
          double prec = (double)muc[1][0]/(double)muc[1][1], recall = (double)muc[0][0]/(double)muc[0][1];
          int numItems = tr.numNounPhrases();
          prec /= numItems;
          recall /= numItems;
          double score = 2 * (prec * recall) / ((prec + recall));
          //double[] newW = w;
          //double[] newW = multiplyArray(w, score*score); (cfs39)
          double[] newW = multiplyArray(w, score);
          addToArray(iterW, newW);
          addToArray(totalW, newW);

        }
      }
      //double[] averageW = divideArray(totalW, totalUpdates);
      double[] averageW = totalW;
      if(m_FullOutput)
        System.out.print("After iter " + iter + ": ");
      else
        System.out.print(iter + "\t");
      int updts = positiveUpdates + negativeUpdates;
      if(m_FullOutput)
        System.out.print("Number of updates is " + positiveUpdates + "|" + negativeUpdates + "="
            + updts + ". ");
      else
        System.out.print(positiveUpdates + "," + negativeUpdates + "=" + updts + ";");
      evaluateClassifier(insts, averageW, truth);

      //System.out.println("After iter "+iter+" w is "+printWeightVector(w));
      if(testInsts != null){
        double metrics[] = evaluateClassifier(testInsts, averageW, testTruth);
        if(maxAcc < metrics[0]){
          maxAcc = metrics[0];
          maxAccIter = iter;
        }
        if(maxBCubbed < metrics[1]){
          maxBCubbed = metrics[1];
          maxBCubbedIter = iter;
        }
        System.out.println();
        evaluateClassifier(testInsts, iterW, testTruth);
      }
      if(!m_FullOutput)
        System.out.println();
    }
    totalW = divideArray(totalW, totalUpdates);
    if(m_FullOutput){
      System.out.println("Max f1 " + nf.format(maxAcc) + " at iteration " + maxAccIter);
      System.out.println("Max B-cubed  " + nf.format(maxBCubbed) + " at iteration "
          + maxBCubbedIter);

      
      System.out.println("The final w is " + printWeightVector(totalW));
      System.out.print("Final on test data: ");

      evaluateClassifier(insts, totalW, truth);
    }
    return totalW;
  }

  public double computeCorection(double prec, double recall, double pCor, double rCor, int numItems){
    if(m_Debug)
      System.err.println("Correction with "+prec+" + "+pCor+" : "+recall+" + "+rCor);

    double score = 2*(prec*recall)/((prec+recall));
    double newScore = 2*((prec+pCor)*(recall+rCor))/((prec+pCor+recall+rCor));
    double correction = newScore - score;
    if(m_Debug)
      System.err.println("Prec "+prec+" Recall "+recall+" --- "+newScore+"-"+score);
    return correction;
  }
  
  public InstanceShort getItem(ModifiedInstancesShort doc, int id1, int id2){
    int bigger =id1>id2?id1:id2;
    int smaller = id1>id2?id2:id1;
    int base = ((bigger-2)*(bigger-1))/2;
    int offset = base+smaller;
    InstanceShort result = doc.instance(doc.numInstances()-offset);
    short id1new = result.value(doc.m_ID1);
    short id2new = result.value(doc.m_ID2);
    if(smaller!=id1new || bigger!=id2new)
      throw new RuntimeException("Id mismatch "+smaller+" vs. "+id1new+" and "+bigger+" vs. "+id2new);
    return result;
  }
  
  public void addWeight(double[] w, InstanceShort inst, double delta) {
    for(int j = 0; j < inst.numAttributes(); j++){
      if(inst.attribute(j).isFeature()){
        if(inst.attribute(j).isNominal())
          w[j] = w[j] + delta * inst.value(j);
        else
          w[j] = w[j] + delta * inst.attribute(j).getOriginalValue(new Short(inst.value(j)));
      }
    }
  }

  public void subtractWeight(double[] w, InstanceShort inst, double delta) {
    for(int j = 0; j < inst.numAttributes(); j++){
      if(inst.attribute(j).isFeature()){
        if(inst.attribute(j).isNominal())
          w[j] = w[j] - delta * inst.value(j);
        else
          w[j] = w[j] - delta * inst.attribute(j).getOriginalValue(new Short(inst.value(j)));
      }
    }
  }

  public double[] divideArray(double[] w, int divider) {
    double[] result = new double[w.length];
    for(int i = 0; i < w.length; i++)
      result[i] = w[i] / divider;
    return result;
  }

  public double[] multiplyArray(double[] w, double mult) {
    double[] result = new double[w.length];
    for(int i = 0; i < w.length; i++)
      result[i] = w[i] * mult;
    return result;
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

  //This function returns a three-element array containing the number of
  // correctly
  //identified positives, the number of identified positives
  //and the total number of positives
  public static int[] positiveIdentification(int[] pred, ModifiedInstancesShort gold) {
    //System.err.print(pred);
    int[] accurate = {0, 0, 0};
    for(int i = 0; i < gold.numInstances(); i++){
      InstanceShort ins = gold.instance(i);
      //System.out.print(ins);
      int id1 = ins.value(gold.m_ID1);
      int id2 = ins.value(gold.m_ID2);
      boolean positive = ins.classValue() == gold.m_Positive;
      if(positive)
        accurate[2]++;
      if(find(id1, pred) == find(id2, pred)){
        accurate[1]++;
        if(positive)
          accurate[0]++;
        //System.out.println(id1+"-"+id2+"[+]");
      }
    }
    //System.err.println("Accurate "+accurate+"/"+gold.numInstances());
    return accurate;
  }

  /**
   * Outputs the distribution for the given output.
   * 
   * Pipes output of SVM through sigmoid function.
   * 
   * @param inst
   *          the instance for which distribution is to be computed
   * @return the distribution
   * @exception Exception
   *              if something goes wrong
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
    if(m_K > 0){
      for(int i = 0; i <= m_K; i++){
        if(sumSoFar < 0){
          output -= m_Weights[i];
        } else{
          output += m_Weights[i];
        }
        if(m_IsAddition[i]){
          sumSoFar += innerProduct(m_Train.instance(m_Additions[i]), inst);
        } else{
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

  private static double innerProduct(double[] w, InstanceShort i) {
    if(w.length != i.numAttributes())
      throw new RuntimeException("Wrong number of attributes");
    double result = 0;
    for(int j = 0; j < i.numAttributes(); j++){
      if(i.attribute(j).isFeature()){
        if(i.attribute(j).isNominal())
          result += i.value(j) * w[j];
        else
          result += i.attribute(j).getOriginalValue(new Short(i.value(j))) * w[j];
      }
    }
    //System.err.println("VAL="+result);
    return result;
  }

  /**
   * Computes the inner product of two instances
   */
  private double innerProduct(Instance i1, Instance i2) throws Exception {

    // we can do a fast dot product
    double result = 0;
    int n1 = i1.numValues();
    int n2 = i2.numValues();
    int classIndex = m_Train.classIndex();
    for(int p1 = 0, p2 = 0; p1 < n1 && p2 < n2;){
      int ind1 = i1.index(p1);
      int ind2 = i2.index(p2);
      if(ind1 == ind2){
        if(ind1 != classIndex){
          result += i1.valueSparse(p1) * i2.valueSparse(p2);
        }
        p1++;
        p2++;
      } else if(ind1 > ind2){
        p2++;
      } else{
        p1++;
      }
    }
    result += 1.0;

    if(m_Exponent != 1){
      return Math.pow(result, m_Exponent);
    } else{
      return result;
    }
  }

  /**
   * Compute a prediction from a perceptron
   */
  private int makePrediction(int k, Instance inst) throws Exception {

    double result = 0;
    for(int i = 0; i < k; i++){
      if(m_IsAddition[i]){
        result += innerProduct(m_Train.instance(m_Additions[i]), inst);
      } else{
        result -= innerProduct(m_Train.instance(m_Additions[i]), inst);
      }
    }
    if(result < 0){
      return 0;
    } else{
      return 1;
    }
  }

  //public class StPerceptron implements OptionHandler,
  // AdditionalMeasureProducer,
  //    WeightedInstancesHandler{
  //
  //  /** The limit of description length surplus in ruleset generation */
  //  private static double MAX_DL_SURPLUS = 64.0;
  //
  //  /** A constant that controls how the algorithm works. In several places,
  // the JRep
  //   * implementation differs from ripper, such as the loss ratio, and prunning.
  // When
  //   * this variable is set to true, this places are handled like RIPPER.
  //   */
  //  private boolean m_RipperStyle = false;
  //  
  //  /** Whether to use positive correlation (accuracy) instead information gain
  // when growing rules*/
  //  private boolean m_UsePosCor = false;
  //  /** The class attribute of the data */
  //  private AttributeShort m_Class;
  //
  //  /** The class attribute of the data */
  //  private AttributeShort m_Docid;
  //
  //  /** The class attribute of the data */
  //  private AttributeShort m_Id1;
  //
  //  /** The class attribute of the data */
  //  private AttributeShort m_Id2;
  //  
  //  /** The real class attribute of the data needed for processing */
  //  private AttributeShort m_RealClass;
  //
  //  /** The predicted class attribute of the data needed for processing */
  //  private AttributeShort m_PredictedClass;
  //
  //  /** The covered attribute of the data needed for processing */
  //  private AttributeShort m_Covered;
  //
  //  /** The ruleset */
  //  private FastVector m_Ruleset;
  //
  //  /** The predicted class distribution */
  //  private FastVector m_Distributions;
  //
  //  /** Runs of optimizations */
  //  private int m_Optimizations = 2;
  //
  //  /** Ratio of weight pos/neg for examles */
  //  private float m_Ratio = 1;
  //  private float m_RatioOrig = 1;
  //  
  //  /** Cost for positive examples **/
  //  private float m_FP_cost = 1;
  //  
  //  /** Cost for negative examples **/
  //  private float m_FN_cost = 1;
  //
  //  /** Random object used in this class */
  //  private Random m_Random = null;
  //
  //  /** # of all the possible conditions in a rule */
  //  private double m_Total = 0;
  //
  //  /** The seed to perform randomization */
  //  private long m_Seed = 1;
  //
  //  /** The number of folds to split data into Grow and Prune for IREP */
  //  private int m_Folds = 3;
  //
  //  /** The minimal number of instance weights within a split */
  //  private double m_MinNo = 2.0;
  //
  //  /** Whether in a debug mode */
  //  private boolean m_Debug = true;
  //
  //  /** An additional debug mode (including even more information */
  //  private boolean ves_Debug = false;
  //
  //  /** Whether check the error rate >= 0.5 in stopping criteria */
  //  private boolean m_CheckErr = true;
  //
  //  /** Whether use pruning, i.e. the data is clean or not */
  //  private boolean m_UsePruning = true;
  //
  //  /** The filter used to randomize the class order */
  //  private Filter m_Filter = null;
  //
  //  /** Used to store the instances */
  //  private InstancesShort m_Instances = null;
  //
  //  /** The RuleStats for the ruleset of each class value */
  //  private FastVector m_RulesetStats;
  //
  //  /** The number of distinct values allowed for numeric attributes */
  //  private int m_MaxNumValues = 100;
  //  
  //  /** Controls how replace rule is grown in the optimization phase */
  //  private boolean m_DefaultOpt = true;
  //  
  //  /** A rulestat instance */
  //  private StRuleStatsShort defaultRuleStat = null;
  //
  //  /**
  //   * Gets the current settings of the Classifier.
  //   *
  //   * @return an array of strings suitable for passing to setOptions
  //   */
  //  public String[] getOptions() {
  //
  //    String[] options = new String[18];
  //    int current = 0;
  //    options[current++] = "-F";
  //    options[current++] = "" + m_Folds;
  //    options[current++] = "-N";
  //    options[current++] = "" + m_MinNo;
  //    options[current++] = "-O";
  //    options[current++] = "" + m_Optimizations;
  //    options[current++] = m_RipperStyle?"-LRip":"-L";
  //    options[current++] = "" + m_RatioOrig;
  //    options[current++] = "-V";
  //    options[current++] = "" + m_MaxNumValues;
  //    options[current++] = "-S";
  //    options[current++] = "" + m_Seed;
  //
  //    if(m_Debug)
  //      options[current++] = "-D";
  //
  //    if(!m_CheckErr)
  //      options[current++] = "-E";
  //
  //    if(!m_UsePruning)
  //      options[current++] = "-P";
  //    
  //    if(!m_DefaultOpt)
  //      options[current++] = "-R";
  //    
  //    if(m_UsePosCor)
  //      options[current++] = "posCorr";
  //
  //    while(current < options.length)
  //      options[current++] = "";
  //
  //    return options;
  //  }
  //    /**
  //     * Compute the best information gain for the specified antecedent
  //     *
  //     * @param instances
  //     * the data based on which the infoGain is computed
  //     * @param defAcRt
  //     * the default accuracy rate of data
  //     * @param antd
  //     * the specific antecedent
  //     * @param numConds
  //     * the number of antecedents in the rule so far
  //     * @return the info gain for the antecedent
  //     */
  //    private double computeInfoGain(InstancesShort instances, double defAcRt,
  // Antd antd) {
  //      if(antd.getAttr().isNominal())
  //        return computeInfoGainNominal(instances, defAcRt, antd);
  //      if(antd.getAttr().isNumeric())
  //        return computeInfoGainNumeric(instances, defAcRt, antd);
  //      throw new RuntimeException("Anteceedent type not yet implemented");
  //          coverage[v] = 0;
  //          continue;
  //        }
  //        data = transitiveClosure(data, currentClass);
  //        if(m_Debug && ves_Debug)
  //          System.err.println("Trying " + antd + ". After transitive closure:"
  //              + data.toString(false));
  //        for(int i = 0; i < data.numInstances(); i++){
  //          InstanceShort current = data.instance(i);
  //          // if this instance is not covered by a previous rule
  //          short cov = current.value(covered);
  //
  //          int predicted = (int)current.value(currentClass);
  //          int trueCl = (int)current.value(trueClass);
  //          boolean positive = predicted == indexOfPositive;
  //          boolean correct = predicted == trueCl;
  //          if(positive){
  //            if(cov != indexOfPositive){
  //              coverage[v] += current.weight();
  //              if(correct)
  //                accurate[v] += current.weight();
  //            }
  //          }
  //        }
  //        //if(m_Debug){
  //        // System.err.print((int)coverage[v]+"("+(int)accurate[v]+") ");
  //        //}
  //      }
  //
  //      double cover = 0, accu = Double.NaN, accuRate = Double.NaN;
  //      short val = 0;
  //      int ind = -1;
  //      for(int x = 0; x < bag; x++){
  //        double t = coverage[x] + 1.0;
  //        double p = accurate[x] + 1.0;
  //        double infoGain = 0;
  //        if(m_UsePosCor){
  //          infoGain = (accurate[x]+1.0)/(coverage[x]+2.0);
  //        }else{
  //          infoGain = accurate[x] * (Utils.log2(p / t) - Utils.log2(defAcRt));
  //        }
  //        // if (m_Debug)
  //        // System.err.println(antd+" => " + infoGain + "=" + accurate[x]
  //        // + "(*" + Utils.log2(p / t) + "-" + Utils.log2(defAcRt) + ")");
  //        if(infoGain > maxInfoGain){
  //          maxInfoGain = infoGain;
  //          cover = coverage[x];
  //          accu = accurate[x];
  //          accuRate = p / t;
  //          val = (short)x;
  //          ind = x;
  //        }
  //      }
  //      // Store all the necessary data with the antecedent.
  //      if(m_Debug && ves_Debug)
  //        System.err.println("Cover " + cover + "; Accurate " + accu
  //            + ";InfoGain " + maxInfoGain);
  //      antd.maxInfoGain = maxInfoGain;
  //      antd.cover = cover;
  //      antd.accu = accu;
  //      antd.accuRate = accuRate;
  //      antd.value = val;
  //      antd.bestValue = val;
  //      if(ind < 0)
  //        return 0;
  //      return maxInfoGain;
  //    }
  //    
  //    /**
  //     * An encapsulating class needed for computing info gain of numeric
  // antecedents
  //     * @author ves
  //     *
  //     * TODO To change the template for this generated type comment go to
  //     * Window - Preferences - Java - Code Style - Code Templates
  //     */
  //    private class SmallInstance{
  //      short docid = -1;
  //      short id1 = -1;
  //      short id2 = -1;
  //      boolean positive = false;
  //      boolean covered = false;
  //      public SmallInstance(short doc, short np1, short np2, boolean pos, boolean
  // cov){
  //        this.docid = doc;
  //        this.id1 = np1;
  //        this.id2 = np2;
  //        this.positive = pos;
  //        this.covered = cov;
  //      }
  //    }
  //    
  //    /**
  //     * Compute the best information gain for the specified numeric antecedent
  //     *
  //     * @param instances
  //     * the data based on which the infoGain is computed
  //     * @param defAcRt
  //     * the default accuracy rate of data
  //     * @param antd
  //     * the specific antecedent
  //     * @param numConds
  //     * the number of antecedents in the rule so far
  //     * @return the info gain for the antecedent
  //     */
  //    private double computeInfoGainNumeric(InstancesShort instances, double
  // defAcRt, Antd antd) {
  //      InstancesShort data = instances;
  //      double maxInfoGain = 0;
  //      AttributeShort currentClass = m_RealClass;
  //      AttributeShort covered = m_Covered;
  //      int indexOfPositive = currentClass.indexOfValue("+");
  //      AttributeShort trueClass = m_Class;
  //      
  //      Vector[][] docClusters = new Vector[instances.getNumDocuments()][];
  //      int bag = antd.getAttr().numValues();
  //      Vector[] splits = new Vector[bag];
  //      float[] coverage = new float[bag];
  //      float[] accurate = new float[bag];
  //      float[][][] positive = new float[instances.getNumDocuments()][][];
  //      if(m_Debug)
  //        System.err.print("\t*Numeric antd " + antd + " with " + bag + " values. ");
  //
  //      short docid = -1;
  //      short currentDoc = -1;
  //      //First, split the instances into bags
  //      //while doing that, set up data structures for the documents
  //      for(int i = 0; i < data.numInstances(); i++){
  //        InstanceShort current = data.instance(i);
  //        // if this instance is not covered by a previous rule
  //        docid = current.value(m_Docid);
  //        short id1 = current.value(m_Id1);
  //        short id2 = current.value(m_Id2);
  //        boolean pos = current.value(trueClass) == indexOfPositive;
  //        short cov = current.value(covered);
  //        boolean isCovered = cov == indexOfPositive;
  //        
  //        float weight = current.weight();
  //        SmallInstance cur = new SmallInstance(docid,id1,id2,pos,isCovered);
  //        short valNum = current.value(antd.getAttr());
  //        if(!isCovered && covers(current)){//if the current instance is covered by
  // the rule so far
  //          if(splits[valNum]==null){
  //            splits[valNum]=new Vector();
  //          }
  //          splits[valNum].add(cur);
  //        }
  //        if(currentDoc == -1 || docid != currentDoc){
  //          // A new document document. Some setting up to be done
  //          int len = id2 + 1;
  //          docClusters[docid] = new Vector[len];
  //          positive[docid] = new float[len][];
  //          currentDoc = docid;
  //        }
  //        if(positive[docid][id1]==null)
  //          positive[docid][id1] = new float[positive[docid].length];
  //        if(positive[docid][id2]==null)
  //          positive[docid][id2] = new float[positive[docid].length];
  //        positive[docid][id1][id2] = positive[docid][id2][id1] =
  // isCovered?0:(pos?weight:(-1*weight));
  // 
  //        //need to join covered instances
  //        if(isCovered){
  //          Vector cl1 = docClusters[docid][id1];
  //          Vector cl2 = docClusters[docid][id2];
  //          if(cl1==null){
  //            cl1 = new Vector();
  //            cl1.add(new Integer(id1));
  //            docClusters[docid][id1]=cl1;
  //          }
  //          if(cl2==null){
  //            cl2 = new Vector();
  //            cl2.add(new Integer(id2));
  //            docClusters[docid][id2]=cl2;
  //          }
  //          if(cl1!=cl2){
  //            int size1 = cl1.size();
  //            int size2 = cl2.size();
  //            for(int k=0; k<size1; k++){
  //              int el1 = ((Integer)cl1.get(k)).intValue();
  //              for(int l=0; l<size2; l++){
  //                int el2 = ((Integer)cl2.get(l)).intValue();
  //                //System.err.print(" joining "+docid+":"+el1+"+"+el2);
  //                if(positive[docid][el1][el2]>0){
  //                  accurate[i]+=positive[docid][el1][el2];
  //                  coverage[i]+=positive[docid][el1][el2];
  //                }else
  //                  if(positive[docid][el1][el2]<0)
  //                    coverage[i]-=positive[docid][el1][el2];
  //              }
  //              cl2.add(new Integer(el1));
  //              docClusters[docid][el1]=cl2;
  //            }
  //            cl1 = null;
  //          }
  //        }
  //      }
  //      
  //      //Now, we need a forward and a backward run
  //      
  //      for(int i = 0; i < bag; i++){
  //        if(splits[i]!=null){
  //          Vector insts = splits[i];
  //          for(int j=0; j<insts.size(); j++){
  //            SmallInstance cur = (SmallInstance)insts.get(j);
  //            Vector[] clusters = docClusters[cur.docid];
  //            short id1 = cur.id1;
  //            short id2 = cur.id2;
  //            docid = cur.docid;
  //            Vector cl1 = clusters[id1];
  //            Vector cl2 = clusters[id2];
  //            
  //            if(cl1==null){
  //              cl1 = new Vector();
  //              cl1.add(new Integer(id1));
  //              clusters[id1]=cl1;
  //            }
  //            if(cl2==null){
  //              cl2 = new Vector();
  //              cl2.add(new Integer(id2));
  //              clusters[id2]=cl2;
  //            }
  //            if(cl1!=cl2){
  //              int size1 = cl1.size();
  //              int size2 = cl2.size();
  //              for(int k=0; k<size1; k++){
  //                int el1 = ((Integer)cl1.get(k)).intValue();
  //                for(int l=0; l<size2; l++){
  //                  int el2 = ((Integer)cl2.get(l)).intValue();
  //                  if(positive[docid][el1][el2]>0){
  //                    accurate[i]+=positive[docid][el1][el2];
  //                    coverage[i]+=positive[docid][el1][el2];
  //                  }else
  //                    if(positive[docid][el1][el2]<0)
  //                      coverage[i]-=positive[docid][el1][el2];
  //                }
  //                cl2.add(new Integer(el1));
  //                clusters[el1]=cl2;
  //              }
  //              cl1 = null;
  //            }
  //          }
  //        }
  //      }
  //      
  //      
  //      //Now accumulate the counts
  //      //System.err.println();
  //      for(int i = 1; i < splits.length; i++){
  //        //System.err.print("|"+antd.getAttr().getOriginalValue(new
  // Short((short)i))+"c-"+coverage[i]);
  //        //System.err.print("a-"+accurate[i]);
  //        coverage[i]+=coverage[i-1];
  //        accurate[i]+=accurate[i-1];
  //      }
  //      //System.err.println();
  //      double cover = 0, accu = Double.NaN, accuRate = Double.NaN;
  //
  //      short val = 0;
  //      int ind = -1;
  //      for(short b = 0; b < bag; b++){
  //        double t = coverage[b] + 1.0;
  //        double p = accurate[b] + 1.0;
  //        double infoGain = 0;
  //        if(m_UsePosCor){
  //          infoGain = (accurate[b]+1.0)/(coverage[b]+2.0);
  //        }else{
  //          infoGain = accurate[b] * (Utils.log2(p / t) - Utils.log2(defAcRt));
  //        }
  //        //double infoGain = accurate[b] * (Utils.log2(p / t) -
  // Utils.log2(defAcRt));
  //        // if (m_Debug)
  //        // System.err.println(antd+" => " + infoGain + "=" + accurate[x]
  //        // + "(*" + Utils.log2(p / t) + "-" + Utils.log2(defAcRt) + ")");
  //        if(infoGain > maxInfoGain){
  //          maxInfoGain = infoGain;
  //          cover = coverage[b];
  //          accu = accurate[b];
  //          accuRate = p / t;
  //          val = NumericAntd.LE;
  //          ((NumericAntd)antd).setSplitPoint((short)(b));
  //          ind = b;
  //        }
  //      }
  //      
  //      //The same run as above, but backward
  //      docid = -1;
  //      currentDoc = -1;
  //      docClusters = new Vector[instances.getNumDocuments()][];
  //      //First, split the instances into bags
  //      //while doing that, set up data structures for the documents
  //      for(int i = 0; i < data.numInstances(); i++){
  //        InstanceShort current = data.instance(i);
  //        docid = current.value(m_Docid);
  //        docid = current.value(m_Docid);
  //        short id1 = current.value(m_Id1);
  //        short id2 = current.value(m_Id2);
  //        short cov = current.value(covered);
  //        boolean isCovered = cov == indexOfPositive;
  //
  //        if(currentDoc == -1 || docid != currentDoc){
  //          // A new document document. Some setting up to be done
  //          int len = id2 + 1;
  //          docClusters[docid] = new Vector[len];
  //          currentDoc = docid;
  //        }
  //        if(isCovered){
  //          Vector cl1 = docClusters[docid][id1];
  //          Vector cl2 = docClusters[docid][id2];
  //          if(cl1==null){
  //            cl1 = new Vector();
  //            cl1.add(new Integer(id1));
  //            docClusters[docid][id1]=cl1;
  //          }
  //          if(cl2==null){
  //            cl2 = new Vector();
  //            cl2.add(new Integer(id2));
  //            docClusters[docid][id2]=cl2;
  //          }
  //          if(cl1!=cl2){
  //            int size1 = cl1.size();
  //            int size2 = cl2.size();
  //            for(int k=0; k<size1; k++){
  //              int el1 = ((Integer)cl1.get(k)).intValue();
  //              for(int l=0; l<size2; l++){
  //                int el2 = ((Integer)cl2.get(l)).intValue();
  //                if(positive[docid][el1][el2]>0){
  //                  accurate[i]+=positive[docid][el1][el2];
  //                  coverage[i]+=positive[docid][el1][el2];
  //                }else
  //                  if(positive[docid][el1][el2]<0)
  //                    coverage[i]-=positive[docid][el1][el2];
  //              }
  //              cl2.add(new Integer(el1));
  //              docClusters[docid][el1]=cl2;
  //            }
  //            cl1 = null;
  //          }
  //        }
  //        
  //      }
  // 
  //      for(int i = bag-1; i >= 0; i--){
  //        //System.err.print("bag " + i + " - ");
  //        accurate[i]=coverage[i]=0;
  //        if(splits[i]!=null){
  //          Vector insts = splits[i];
  //          for(int j=0; j<insts.size(); j++){
  //            SmallInstance cur = (SmallInstance)insts.get(j);
  //            Vector[] clusters = docClusters[cur.docid];
  //            short id1 = cur.id1;
  //            short id2 = cur.id2;
  //            docid = cur.docid;
  //            Vector cl1 = clusters[id1];
  //            Vector cl2 = clusters[id2];
  //            
  //            if(cl1==null){
  //              cl1 = new Vector();
  //              cl1.add(new Integer(id1));
  //              clusters[id1]=cl1;
  //            }
  //            if(cl2==null){
  //              cl2 = new Vector();
  //              cl2.add(new Integer(id2));
  //              clusters[id2]=cl2;
  //            }
  //            if(cl1!=cl2){
  //              //System.err.print(" joining "+id1+"+"+id2);
  //              int size1 = cl1.size();
  //              int size2 = cl2.size();
  //              for(int k=0; k<size1; k++){
  //                int el1 = ((Integer)cl1.get(k)).intValue();
  //                for(int l=0; l<size2; l++){
  //                  int el2 = ((Integer)cl2.get(l)).intValue();
  //                  if(positive[docid][el1][el2]>0){
  //                    accurate[i]+=positive[docid][el1][el2];
  //                    coverage[i]+=positive[docid][el1][el2];
  //                  }else
  //                    if(positive[docid][el1][el2]<0)
  //                      coverage[i]-=positive[docid][el1][el2];
  //                }
  //                cl2.add(new Integer(el1));
  //                clusters[el1]=cl2;
  //              }
  //              cl1 = null;
  //            }
  //          }
  //        }
  //        //System.err.println();
  //      }
  //      
  //      
  //      //Now accumulate the counts
  //      for(int i = bag-2; i >=0; i--){
  //        //System.err.print("|"+antd.getAttr().getOriginalValue(new
  // Short((short)i))+"c-"+coverage[i]);
  //        //System.err.print("a-"+accurate[i]);
  //        coverage[i]+=coverage[i+1];
  //        accurate[i]+=accurate[i+1];
  //      }
  //
  //      for(short b = 0; b < bag; b++){
  //        double t = coverage[b] + 1.0;
  //        double p = accurate[b] + 1.0;
  //        double infoGain = 0;
  //        if(m_UsePosCor){
  //          infoGain = (accurate[b]+1.0)/(coverage[b]+2.0);
  //        }else{
  //          infoGain = accurate[b] * (Utils.log2(p / t) - Utils.log2(defAcRt));
  //        }
  //        //double infoGain = accurate[b] * (Utils.log2(p / t) -
  // Utils.log2(defAcRt));
  //        // if (m_Debug)
  //        // System.err.println(antd+" => " + infoGain + "=" + accurate[x]
  //        // + "(*" + Utils.log2(p / t) + "-" + Utils.log2(defAcRt) + ")");
  //        if(infoGain > maxInfoGain){
  //          maxInfoGain = infoGain;
  //          cover = coverage[b];
  //          accu = accurate[b];
  //          accuRate = p / t;
  //          val = NumericAntd.GE;
  //          ((NumericAntd)antd).setSplitPoint((short)(b));
  //          ind = b;
  //        }
  //      }
  //      //Store all the necessary data with the antecedent.
  //      antd.maxInfoGain = maxInfoGain;
  //      antd.cover = cover;
  //      antd.accu = accu;
  //      antd.accuRate = accuRate;
  //      antd.value = val;
  //      antd.bestValue = val;
  //      
  //      /*
  //      if(m_Debug){
  //        double trueCover = 0, trueAccu = 0;
  //        applyWithAntd(data, antd);
  //        data = transitiveClosure(data, currentClass);
  //        for(int i = 0; i < data.numInstances(); i++){
  //          InstanceShort current = data.instance(i);
  //          // if this instance is not covered by a previous rule
  //          double cov = current.value(covered);
  //
  //          int predicted = (int)current.value(currentClass);
  //          int trueCl = (int)current.value(trueClass);
  //          boolean pos = predicted == indexOfPositive;
  //          boolean correct = predicted == trueCl;
  //          if(pos){
  //            if(Double.isNaN(cov) || (int)cov != indexOfPositive){
  //              trueCover += current.weight();
  //              if(correct)
  //                trueAccu += current.weight();
  //            }
  //          }
  //        }
  //        System.err.println("Computed " + accu + "/" + cover + ";True " + trueAccu +
  // "/" + trueCover);
  //      }*/
  //      if(m_Debug)
  //        System.err.println("Best " + accu + "/" + cover + " for " + antd);
  //      
  //      if(ind < 0)
  //        return 0;
  //      return maxInfoGain;
  //
  //    }
  //
  //    private InstancesShort getCoveredData(InstancesShort data, Antd antd) {
  //      AttributeShort realClass = m_RealClass;
  //      AttributeShort covered = m_Covered;
  //      short indexOfPositive = realClass.indexOfValue("+");
  //      InstancesShort result = new ModifiedInstancesShort(data,
  // data.numInstances());
  //
  //      applyWithAntd(data, antd);
  //      data = transitiveClosure(data, realClass);
  //
  //      for(int i = 0; i < data.numInstances(); i++){
  //        InstanceShort current = data.instance(i);
  //        // if this instance is not covered by a previous rule
  //        short val = current.value(covered);
  //
  //        int predicted = (int)current.value(realClass);
  //        boolean positive = predicted == indexOfPositive;
  //        if(positive){
  //          result.add(current);
  //        } else if(val == indexOfPositive)
  //          result.add(current);
  //      }
  //      return result;
  //    }
  //
  //    private InstancesShort getCovered(InstancesShort data) {
  //      AttributeShort predicted = m_PredictedClass;
  //      short indexOfPositive = predicted.indexOfValue("+");
  //      InstancesShort result = new ModifiedInstancesShort(data,
  // data.numInstances());
  //      for(int i = 0; i < data.numInstances(); i++){
  //        InstanceShort current = data.instance(i);
  //        // if this instance is not covered by a previous rule
  //
  //        short predictedValue = current.value(predicted);
  //        boolean positive = predictedValue == indexOfPositive;
  //        if(positive)
  //          result.add(current);
  //      }
  //      return result;
  //    }
  //
  //    /**
  //     * Prune all the possible final sequences of the rule using the pruning
  //     * data. The measure used to prune the rule is based on flag given.
  //     *
  //     * @param pruneData
  //     * the pruning data used to prune the rule
  //     * @param useWhole
  //     * flag to indicate whether use the error rate of the whole pruning
  //     * data instead of the data covered
  //     */
  //    public void prune(InstancesShort pruneData, boolean useWhole) {
  //      InstancesShort data = pruneData;
  //      AttributeShort predictedClass = m_PredictedClass;
  //
  //      short positive = predictedClass.indexOfValue("+");
  //      double total = data.sumOfUncovWeights(m_Covered);
  //      if(!Utils.gr(total, 0.0))
  //        return;
  //
  //      /* The default accurate # and rate on pruning data */
  //      double defAccu = computeDefAccu(data);
  //
  //      if(m_Debug)
  //        System.err.println("Pruning with " + defAccu + " positive data out of "
  //            + total + " instances");
  //
  //      int size = m_Antds.size();
  //      if(size == 0)
  //        return; // Default rule before pruning
  //
  //      double[] worthRt = new double[size];
  //      double[] coverage = new double[size];
  //      double[] worthValue = new double[size];
  //      double[] tn = new double[size];
  //      for(int w = 0; w < size; w++){
  //        tn[w] = worthRt[w] = coverage[w] = worthValue[w] = 0.0;
  //      }
  //
  //      /* Calculate accuracy parameters for all the antecedents in this rule */
  //      for(int x = 0; x < size; x++){
  //        //this.apply(data, x - 1, x);
  //        this.apply(data, x);
  //        transitiveClosure(data, predictedClass);
  //
  //        for(int y = 0; y < data.numInstances(); y++){
  //          InstanceShort ins = data.instance(y);
  //
  //          short previouslyCovered = ins.value(m_Covered);
  //          boolean notPrevCovered = previouslyCovered != positive;
  //          boolean covered = ins.value(predictedClass) == positive;
  //
  //          if(!m_RipperStyle){
  //            if(notPrevCovered){
  //              if(covered){
  //                coverage[x] += ins.weight();
  //                if(ins.classValue() == positive) // Accurate prediction
  //                  worthValue[x] += ins.weight();
  //              } else if(useWhole){ // Not covered
  //                if(ins.classValue() != positive)
  //                  tn[x] += ins.weight();
  //              }
  //            }
  //          }else{
  //            
  //            if(notPrevCovered){
  //              
  //              if(covered){
  //                coverage[x] += ins.weight();
  //                if(ins.classValue() == positive) // Accurate prediction
  //                  worthValue[x] += ins.weight();
  //              } else if(useWhole){ // Not covered
  //                //In this case tn is really the false negative
  //                if(ins.classValue() == positive)
  //                  tn[x] += ins.weight();
  //              }
  //            }else{
  //              
  //              if(useWhole){
  //                coverage[x]+=ins.weight();
  //                if(ins.classValue()==positive)
  //                  worthValue[x]+=ins.weight();
  //              }
  //            }
  //          }
  //        }
  //        
  //        if(m_RipperStyle){
  //          //in this case v = (p-n)/(p+n)
  //          double p = worthValue[x];
  //          double n = coverage[x] - p;
  //          //System.err.println("P="+p+" N="+n);
  //          if(!useWhole)
  //            worthRt[x]= ((p+1)*m_FN_cost - (n+1)*m_FP_cost)/(p*m_FN_cost +
  // (n+1)*m_FP_cost + 4.0);
  //          else{
  //            double fp = n;
  //            double fn = tn[x];
  //            worthRt[x] = 1.0 - (fp*m_FP_cost+fn*m_FN_cost)/total;
  //          }
  //        }else{
  //          if(useWhole){
  //            worthValue[x] += tn[x];
  //            worthRt[x] = worthValue[x] / total;
  //          } else
  //            // Note if coverage is 0, accuracy is 0.5
  //            worthRt[x] = (worthValue[x] + 1.0) / (coverage[x] + 2.0);
  //        }
  //      }
  //
  //      double maxValue = (defAccu + 1.0) / (total + 2.0);
  //      
  //      int maxIndex = -1;
  //      for(int i = 0; i < worthValue.length; i++){
  //        if(m_Debug){
  //          double denom = useWhole?total:coverage[i];
  //          String computation = m_RipperStyle?"(RIPPERStyle): ":"(useAccuray? " +
  // !useWhole + "): ";
  //          System.err.println(i + computation + worthRt[i] + "=" + worthValue[i] + "/"
  // + denom);
  //        }
  //        if(worthRt[i] > maxValue){ // Prefer to the
  //          maxValue = worthRt[i]; // shorter rule
  //          maxIndex = i;
  //        }
  //      }
  //
  //      /* Prune the antecedents according to the accuracy parameters */
  //      for(int z = size - 1; z > maxIndex; z--)
  //        m_Antds.removeElementAt(z);
  //    }
  //
  //    /**
  //     * Prune all the possible final sequences of the rule using the pruning
  //     * data. The measure used to prune the rule is based on flag given. This
  //     * particular function measures the performance in the context of the entire
  //     * ruleset, not just the particular rule.
  //     *
  //     * Currently the same as prune();
  //     *
  //     * @param pruneData
  //     * the pruning data used to prune the rule
  //     * @param useWhole
  //     * flag to indicate whether use the error rate of the whole pruning
  //     * data instead of the data covered
  //     */
  //    public void pruneWithRuleset(InstancesShort pruneData, boolean useWhole) {
  //      prune(pruneData, useWhole);
  //    }
  //
  //    /*
  //     * InstancesShort data = pruneData; AttributeShort predictedClass =
  //     * m_PredictedClass; if (m_Debug && ves_Debug) System.err.println("Pruning "
  // +
  //     * data.toString(false));
  //     *
  //     * int positive = predictedClass.indexOfValue("+"); double total =
  //     * data.sumOfWeights(); if (!Utils.gr(total, 0.0)) return;
  //     * // The default accurate # and rate on pruning data double defAccu =
  //     * computeDefAccu(data);
  //     *
  //     * if (m_Debug) System.err.println("Pruning with " + defAccu + " positive
  //     * data out of " + total + " InstancesShort");
  //     *
  //     * int size = m_Antds.size(); if (size == 0) return; // Default rule before
  //     * pruning
  //     *
  //     * double[] worthRt = new double[size]; double[] coverage = new
  //     * double[size]; double[] worthValue = new double[size]; for (int w = 0; w <
  //     * size; w++) { worthRt[w] = coverage[w] = worthValue[w] = 0.0; }
  //     * // Calculate accuracy parameters for all the antecedents in this rule
  //     * double tn = 0.0; // True negative if useWhole for (int x = 0; x < size;
  //     * x++) { this.apply(data, x - 1, x); transitiveClosure(data,
  //     * predictedClass); if (m_Debug && ves_Debug)
  //     * System.err.println(data.toString(false)); // Antd antd = (Antd)
  //     * m_Antds.elementAt(x); // AttributeShort attr = antd.getAttr(); //
  //     * InstancesShort newData = data; // data = new
  //     * ModifiedInstancesShort(newData, 0); // Make data empty
  //     *
  //     * for (int y = 0; y < data.numInstances(); y++) { InstanceShort ins =
  //     * data.instance(y);
  //     *
  //     * boolean covered = (int) ins.value(predictedClass) == positive; if
  //     * (covered) { coverage[x] += ins.weight(); if ((int) ins.classValue() ==
  //     * positive) // Accurate prediction worthValue[x] += ins.weight(); } else if
  //     * (useWhole) { // Not covered if ((int) ins.classValue() != positive) tn +=
  //     * ins.weight(); } } // for (int y = 0; y < newData.numInstances(); y++) {
  // //
  //     * InstanceShort ins = newData.instance(y); // // if (antd.covers(ins)) { //
  //     * Covered by this antecedent // coverage[x] += ins.weight(); //
  //     * data.add(ins); // Add to data for further pruning // if ((int)
  //     * ins.classValue() == (int) m_Consequent) // Accurate // // prediction //
  //     * worthValue[x] += ins.weight(); // } else if (useWhole) { // Not covered
  // //
  //     * if ((int) ins.classValue() != (int) m_Consequent) // tn += ins.weight();
  // // } // }
  //     *
  //     * if (useWhole) { worthValue[x] += tn; worthRt[x] = worthValue[x] / total;
  // }
  //     * else // Note if coverage is 0, accuracy is 0.5 worthRt[x] =
  //     * (worthValue[x] + 1.0) / (coverage[x] + 2.0); }
  //     *
  //     * double maxValue = (defAccu + 1.0) / (total + 2.0); int maxIndex = -1; for
  //     * (int i = 0; i < worthValue.length; i++) { if (m_Debug) { double denom =
  //     * useWhole ? total : coverage[i]; System.err.println(i + "(useAccuray? " +
  //     * !useWhole + "): " + worthRt[i] + "=" + worthValue[i] + "/" + denom); } if
  //     * (worthRt[i] > maxValue) { // Prefer to the maxValue = worthRt[i]; //
  //     * shorter rule maxIndex = i; } }
  //     * // Prune the antecedents according to the accuracy parameters for (int z
  // =
  //     * size - 1; z > maxIndex; z--) m_Antds.removeElementAt(z); }
  //     */
  //
  //    /**
  //     * Prints this rule
  //     *
  //     * @param classAttr
  //     * the class attribute in the data
  //     * @return a textual description of this rule
  //     */
  //    public String toString(AttributeShort classAttr) {
  //      if(classAttr == null)
  //        classAttr = m_Class;
  //      StringBuffer text = new StringBuffer();
  //      if(m_Antds.size() > 0){
  //        for(int j = 0; j < (m_Antds.size() - 1); j++)
  //          text.append("(" + ((Antd)(m_Antds.elementAt(j))).toString()
  //              + ") and ");
  //        text.append("(" + ((Antd)(m_Antds.lastElement())).toString() + ")");
  //      }
  //      text.append(" => " + classAttr.name() + "="
  //          + classAttr.value((int)m_Consequent));
  //
  //      return text.toString();
  //    }
  //
  //    /*
  //     * Print the rule
  //     */
  //
  //    public String toString() {
  //      return toString(m_Class);
  //    }
  //
  //    /**
  //     * Prints this rule in the RIPPER traditional representation
  //     *
  //     * @return a textual description of this rule
  //     */
  //    public String toRIPPERString() {
  //      StringBuffer text = new StringBuffer();
  //      if(m_Antds.size() > 0){
  //        for(int j = 0; j < m_Antds.size(); j++)
  //          text.append(((Antd)(m_Antds.elementAt(j))).toString() + " ");
  //      }
  //      text.append(".");
  //      return text.toString();
  //    }
  //  }
  //
  //  /**
  //   * Builds Ripper in the order of class frequencies. For each class it's
  // built
  //   * in two stages: building and optimization
  //   *
  //   * @param instances
  //   * the training data
  //   * @exception Exception
  //   * if classifier can't be built successfully
  //   */
  //  public void buildClassifier(InstancesShort instances) throws Exception {
  //    System.err.println("StPerceptron with options
  // "+Utils.joinOptions(getOptions())+" docs="
  //        +instances.getNumDocuments()+" features="+instances.numAttributes());
  //    // System.err.println("Called builClassifier:"+instances);
  //    if(instances.numInstances() == 0)
  //      throw new Exception(" No instances with a class value!");
  //
  //    if(instances.checkForStringAttributes())
  //      throw new UnsupportedAttributeTypeException(
  //          " Cannot handle string attributes!");
  //
  //    if(!instances.classAttribute().isNominal())
  //      throw new UnsupportedClassTypeException(" Only nominal class, please.");
  //
  //    m_Class = instances.classAttribute();
  //    // A hack to use the modified class for instances
  //    if(m_Instances == null){
  //      //instances = new ModifiedInstancesShort(instances);
  //      // Add a new attribute to the instances, needed in the algorithm
  //      //System.err.println("*** L = "+m_Ratio +" ****");
  //      if(m_Ratio != 1){
  //        ((ModifiedInstancesShort)instances).setInstWeights(m_Class, m_Ratio);
  //        System.err.println("Setting ratio to " + m_Ratio);
  //      }
  //      AttributeShort realClass = m_Class.copy("real_class");
  //      AttributeShort predClass = realClass.copy("predicted_class");
  //      AttributeShort covered = realClass.copy("covered");
  //      instances.insertAttributeAt(realClass, instances.numAttributes());
  //      instances.insertAttributeAt(predClass, instances.numAttributes());
  //      instances.insertAttributeAt(covered, instances.numAttributes());
  //      m_RealClass = instances.attribute("real_class");
  //      m_PredictedClass = instances.attribute("predicted_class");
  //      m_Covered = instances.attribute("covered");
  //      m_Docid = instances.attribute("DOCNUM");
  //      m_Id1 = instances.attribute("ID1");
  //      m_Id2 = instances.attribute("ID2");
  //      m_Instances = instances;
  //      
  //    } else
  //      // only run once
  //      return;
  //
  //    m_Random = instances.getRandomNumberGenerator(m_Seed);
  //    instances = m_Instances;
  //
  //    m_Total = StRuleStatsShort.numAllConditions(instances);
  //    if(m_Debug)
  //      System.err.println("Number of all possible conditions = " + m_Total);
  //
  //    /*
  //     * Removed. I don't want to change the order of the examples!!! m_Filter =
  //     * new ClassOrder(); ((ClassOrder)m_Filter).setSeed(m_Random.nextInt());
  //     * ((ClassOrder)m_Filter).setClassOrder(ClassOrder.FREQ_ASCEND);
  //     * m_Filter.setInputFormat(instances); Instances modData = new
  //     * ModifiedInstances(Filter.useFilter(instances, m_Filter));
  //     */
  //    if(m_Debug && ves_Debug){
  //      System.err.println(instances.toString(false));
  //    }
  //
  //    if(instances == null)
  //      throw new Exception(" Unable to randomize the class orders.");
  //    instances.deleteWithMissingClass();
  //    if(instances.numInstances() == 0)
  //      throw new Exception(" No instances with a class value!");
  //
  //    if(instances.numInstances() < m_Folds)
  //      throw new Exception(" Not enough data for REP.");
  //
  //    m_Ruleset = new FastVector();
  //    m_RulesetStats = new FastVector();
  //    m_Distributions = new FastVector();
  //
  //    // Get the class frequencies
  //    double[] orderedClasses = ((ModifiedInstancesShort)instances)
  //        .getClassCounts();
  //    if(m_Debug){
  //      System.err.println("Sorted classes:");
  //      for(int x = 0; x < m_Class.numValues(); x++)
  //        System.err.println(x + ": " + m_Class.value(x) + " has "
  //            + orderedClasses[x] + " instances.");
  //    }
  //
  //    // Iterate from less prevalent class to more frequent one
  //    oneClass: for(int y = 0; y < instances.numClasses() - 1; y++){ // For each
  //      // class
  //      double classIndex = (double)y;
  //      if(m_Debug){
  //        int ci = (int)classIndex;
  //        System.err.println("\n\nClass " + m_Class.value(ci) + "(" + ci + "): "
  //            + orderedClasses[y]
  //            + "instances\n=====================================\n");
  //      }
  //
  //      if(Utils.eq(orderedClasses[y], 0.0)) // No data for this class
  //        continue oneClass;
  //
  //      // The expected FP/err is the proportion of the class
  //      double all = 0;
  //      for(int i = y; i < orderedClasses.length; i++)
  //        all += orderedClasses[i];
  //      double expFPRate = orderedClasses[y] / all;
  //
  //      double classYWeights = 0, totalWeights = 0;
  //      for(int j = 0; j < instances.numInstances(); j++){
  //        InstanceShort datum = instances.instance(j);
  //        // if(m_Debug)
  //        // System.err.println(datum + " w = "+datum.weight());
  //        totalWeights += datum.weight();
  //        if((int)datum.classValue() == y){
  //          classYWeights += datum.weight();
  //        }
  //      }
  //
  //      // DL of default rule, no theory DL, only data DL
  //      double defDL;
  //      if(defaultRuleStat==null)
  //        defaultRuleStat = new StRuleStatsShort(m_Debug&&ves_Debug, m_FP_cost,
  // m_FN_cost);
  //      if(classYWeights > 0)
  //        defDL = defaultRuleStat.dataDL(expFPRate, 0.0, totalWeights, 0.0,
  // classYWeights);
  //      else
  //        continue oneClass; // Subsumed by previous rules
  //
  //      if(Double.isNaN(defDL) || Double.isInfinite(defDL))
  //        throw new Exception("Should never happen: defDL NaN or infinite!");
  //      if(m_Debug)
  //        System.err.println("The default DL = " + defDL);
  //
  //      instances = rulesetForOneClass(expFPRate, instances, classIndex, defDL);
  //    }
  //
  //    // Set the default rule
  //    RipperRule defRule = new RipperRule();
  //    defRule.setConsequent((double)(instances.numClasses() - 1));
  //    m_Ruleset.addElement(defRule);
  //
  //    StRuleStatsShort defRuleStat = new StRuleStatsShort(m_Debug && ves_Debug,
  // m_FP_cost, m_FN_cost);
  //    //defRuleStat.setData(instances);
  //    defRuleStat.setNumAllConds(m_Total);
  //    defRuleStat.addAndUpdate(defRule, instances);
  //    m_RulesetStats.addElement(defRuleStat);
  //
  //    for(int z = 0; z < m_RulesetStats.size(); z++){
  //      StRuleStatsShort oneClass = (StRuleStatsShort)m_RulesetStats.elementAt(z);
  //      for(int xyz = 0; xyz < oneClass.getRulesetSize(); xyz++){
  //        double[] classDist = oneClass.getDistributions(xyz);
  //        Utils.normalize(classDist);
  //        if(classDist != null)
  //          m_Distributions.addElement(classDist);// ((ClassOrder)
  //        // m_Filter).distributionsByOriginalIndex(classDist));
  //      }
  //    }
  //  }
  //
  //  /**
  //   * Classify the test instance with the rule learner and provide the class
  //   * distributions
  //   *
  //   * @param datum
  //   * the instance to be classified
  //   * @return the distribution
  //   */
  //  public double[] distributionForInstance(InstanceShort datum) {
  //    try{
  //      for(int i = 0; i < m_Ruleset.size(); i++){
  //        RipperRule rule = (RipperRule)m_Ruleset.elementAt(i);
  //        if(rule.covers(datum))
  //          return (double[])m_Distributions.elementAt(i);
  //      }
  //    } catch(Exception e){
  //      System.err.println(e.getMessage());
  //      e.printStackTrace();
  //    }
  //
  //    System.err.println("Should never happen!");
  //    return new double[datum.classAttribute().numValues()];
  //  }
  //
  //  /**
  //   * Build a ruleset for the given class according to the given data
  //   *
  //   * @param expFPRate
  //   * the expected FP/(FP+FN) used in DL calculation
  //   * @param data
  //   * the given data
  //   * @param classIndex
  //   * the given class index
  //   * @param defDL
  //   * the default DL in the data
  //   * @exception if
  //   * the ruleset can be built properly
  //   */
  //  protected InstancesShort rulesetForOneClass(double expFPRate,
  //      InstancesShort data, double classIndex, double defDL) throws Exception {
  //
  //    InstancesShort growData, pruneData;
  //    boolean stop = false;
  //    FastVector ruleset = new FastVector();
  //
  //    double dl = defDL, minDL = defDL;
  //    StRuleStatsShort rstats = null;
  //    double[] rst;
  //
  //    // Check whether data have positive examples
  //    boolean defHasPositive = true; // No longer used
  //    boolean hasPositive = true;
  //
  //    /** ******************** Building stage ********************** */
  //    if(m_Debug)
  //      System.err.println("\n*** Building stage ***");
  //    //data = StRuleStats.randomizeByDoc(data, m_Folds, m_Random);
  //    InstancesShort[] part;
  //    while((!stop) && hasPositive){ // Generate new rules until
  //      // stopping criteria met
  //      RipperRule oneRule;
  //      if(m_UsePruning){
  //        /* Split data into Grow and Prune */
  //
  //        // We should have stratified the data, but ripper seems
  //        // to have a bug that makes it not to do so. In order
  //        // to simulate it more precisely, we do the same thing.
  //        // newData.randomize(m_Random);
  //        /*
  //         *
  //         * newData = StRuleStats.stratify(newData, m_Folds, m_Random);
  //         * Instances[] part = StRuleStats.partition(newData, m_Folds); growData =
  //         * part[0]; pruneData = part[1];
  //         */
  //        //data = StRuleStats.randomizeByDoc(data, m_Folds, m_Random);
  //        //part = StRuleStats.partitionByDocument(data, m_Folds);
  //        part = StRuleStatsShort.randomizeAndPartitionByDoc(data, m_Folds,
  //            m_Random);
  //        growData = part[0];
  //        pruneData = part[1];
  //        // growData=newData.trainCV(m_Folds, m_Folds-1);
  //        // pruneData=newData.testCV(m_Folds, m_Folds-1);
  //
  //        oneRule = new RipperRule();
  //        oneRule.setConsequent(classIndex); // Must set first
  //
  //        if(m_Debug)
  //          System.err.println("\nGrowing a rule ...");
  //        oneRule.grow(growData); // Build the rule
  //        if(m_Debug){
  //          System.err.println("One rule found before pruning:"
  //              + oneRule.toString(m_Class));
  //          // throw new RuntimeException("breaking now");
  //        }
  //        if(m_Debug)
  //          System.err.println("\nPruning the rule ...");
  //        oneRule.prune(pruneData, false); // Prune the rule
  //        if(m_Debug)
  //          System.err.println("One rule found after pruning:"
  //              + oneRule.toString(m_Class));
  //      } else{
  //        oneRule = new RipperRule();
  //        oneRule.setConsequent(classIndex); // Must set first
  //        if(m_Debug)
  //          System.err.println("\nNo pruning: growing a rule ...");
  //        oneRule.grow(data); // Build the rule
  //        if(m_Debug)
  //          System.err.println("No pruning: one rule found:\n"
  //              + oneRule.toString(m_Class));
  //      }
  //
  //      // Compute the DL of this ruleset
  //      if(rstats == null){ // First rule
  //        rstats = new StRuleStatsShort(m_Debug && ves_Debug, m_FP_cost, m_FN_cost);
  //        rstats.setNumAllConds(m_Total);
  //        //rstats.setData(newData);
  //      }
  //      rstats.addAndUpdate(oneRule, data);
  //      int last = rstats.getRuleset().size() - 1; // Index of last rule
  //      dl += rstats.relativeDL(data, last, expFPRate, m_CheckErr);
  //
  //      if(Double.isNaN(dl) || Double.isInfinite(dl))
  //        throw new Exception(
  //            "Should never happen: dl in building stage NaN or infinite!");
  //      if(m_Debug)
  //        System.err.println("Before optimization(" + last + "): the dl = " + dl
  //            + " | best: " + minDL);
  //
  //      if(dl < minDL)
  //        minDL = dl; // The best dl so far
  //
  //      rst = rstats.getSimpleStats(last);
  //      if(m_Debug)
  //        System.err.println("The rule covers: " + rst[0] + " | pos = " + rst[2]
  //            + " | neg = " + rst[4] + "\nThe rule doesn't cover: " + rst[1]
  //            + " | pos = " + rst[5]);
  //
  //      stop = checkStop(rst, minDL, dl);
  //
  //      if(!stop){
  //        ruleset.addElement(oneRule); // Accepted
  //        // newData = rstats.getFiltered(last)[1];// Data not covered
  //        data = oneRule.apply(ruleset, data);
  //        hasPositive = Utils.gr(rst[5], 0.0); // Positives remaining?
  //        if(m_Debug)
  //          System.err.println("One rule added: has positive? " + hasPositive);
  //      } else{
  //        if(m_Debug)
  //          System.err.println("Quit rule");
  //        rstats.removeLast(); // Remove last to be re-used
  //      }
  //    }// while !stop
  //
  //    System.out.println("Ruleset (in Ripper format) after building stage \n"
  //        + rstats.toRIPPERString());
  //    /** ****************** Optimization stage ****************** */
  //    StRuleStatsShort finalRulesetStat = null;
  //    if(m_UsePruning){
  //      for(int z = 0; z < m_Optimizations; z++){
  //        if(m_Debug)
  //          System.err.println("\n*** Optimization: run #" + z + " ***");
  //
  //        // newData = data;
  //        finalRulesetStat = new StRuleStatsShort(m_Debug && ves_Debug, m_FP_cost,
  // m_FN_cost);
  //        //finalRulesetStat.setData(newData);
  //        finalRulesetStat.setNumAllConds(m_Total);
  //        (new RipperRule()).clear(data, false);
  //        int position = 0;
  //        stop = false;
  //        boolean isResidual = false;
  //        hasPositive = defHasPositive;
  //        dl = minDL = defDL;
  //
  //        oneRule: while(!stop && hasPositive){
  //
  //          isResidual = (position >= ruleset.size()); // Cover residual positive
  //          // examples
  //          // Re-do shuffling and stratification
  //          // newData.randomize(m_Random);
  //          //data = StRuleStats.randomizeByDoc(data, m_Folds, m_Random);
  //          //Instances[] part = StRuleStats.partitionByDocument(data, m_Folds);
  //          /* Instances[] */
  //          part = StRuleStatsShort.randomizeAndPartitionByDoc(data, m_Folds,
  //              m_Random);
  //          growData = part[0];
  //          pruneData = part[1];
  //          // growData=newData.trainCV(m_Folds, m_Folds-1);
  //          // pruneData=newData.testCV(m_Folds, m_Folds-1);
  //          RipperRule finalRule;
  //
  //          if(m_Debug)
  //            System.err.println("\nRule #" + position + "| isResidual?"
  //                + isResidual + "| data size: " + data.sumOfWeights());
  //
  //          if(isResidual){
  //            RipperRule newRule = new RipperRule();
  //            newRule.setConsequent(classIndex);
  //            if(m_Debug)
  //              System.err.println("\nGrowing and pruning" + " a new rule ...");
  //            newRule.grow(growData);
  //            newRule.prune(pruneData, false);
  //            finalRule = newRule;
  //            if(m_Debug)
  //              System.err.println("\nNew rule found: "
  //                  + newRule.toString(m_Class));
  //          } else{
  //            RipperRule oldRule = (RipperRule)ruleset.elementAt(position);
  //            boolean covers = false;
  //            // Test coverage of the next old rule
  //            for(int i = 0; i < data.numInstances() && !covers; i++)
  //              if(oldRule.covers(data.instance(i))){
  //                covers = true;
  //              }
  //
  //            if(!covers){// Null coverage, no variants can be generated
  //              if(m_Debug)
  //                System.err.println("Rule " + oldRule
  //                    + " has 0 coverage. Adding.");
  //              finalRulesetStat.addAndUpdate(oldRule, data);
  //              position++;
  //              continue oneRule;
  //            }
  //
  //            // 2 variants
  //            if(m_Debug)
  //              System.err.println("\nGrowing and pruning" + " Replace ...");
  //            RipperRule replace = new RipperRule();
  //            replace.setConsequent(classIndex);
  //            if(m_DefaultOpt){
  //              replace.applyWhileSkipping(ruleset, growData, position);
  //            }else{
  //              replace.apply(ruleset,growData,position);
  //            }
  //            transitiveClosure(growData, m_Covered);
  //            replace.grow(growData);
  //            if(m_Debug)
  //              System.err.println("\nReplace rule " + replace);
  //
  //            // Remove the pruning data covered by the following
  //            // rules, then simply compute the error rate of the
  //            // current rule to prune it. According to Ripper,
  //            // it's equivalent to computing the error of the
  //            // whole ruleset -- is it true?
  //            // TO DO: This has to be modified
  //            //pruneData = StRuleStats.rmCoveredBySuccessives(pruneData,
  //            // ruleset, position);
  //            //Apply and transitive close all rules in the ruleset except for
  //            // the current rule
  //            replace.applyWhileSkipping(ruleset, pruneData, position);
  //            transitiveClosure(pruneData, m_Covered);
  //
  //            replace.pruneWithRuleset(pruneData, true);
  //
  //            if(m_Debug)
  //              System.err.println("\nGrowing and pruning" + " Revision ...");
  //            RipperRule revision = (RipperRule)oldRule.copy();
  //
  //            // For revision, first rm the data covered by the old rule
  //            /* We use transitive closure here */
  //            //ModifiedInstances newGrowData = new ModifiedInstances(growData,
  //            // 0);
  //            //revision.apply(growData, false);
  //            //transitiveClosure(growData, m_PredictedClass);
  //            //int positive = m_PredictedClass.indexOfValue("+");
  //            //for (int b = 0; b < growData.numInstances(); b++) {
  //            // InstanceShort inst = growData.instance(b);
  //            // if ((int)inst.value(m_PredictedClass)==positive)
  //            // newGrowData.add(inst);
  //            //}
  //            revision.apply(ruleset, growData, position);
  //            revision.grow(growData);
  //            if(m_Debug)
  //              System.err.println("\nRevision rule " + revision);
  //            revision.prune(pruneData, true);
  //
  //            double[][] prevRuleStats = new double[position][6];
  //            for(int c = 0; c < position; c++)
  //              prevRuleStats[c] = finalRulesetStat.getSimpleStats(c);
  //
  //            // Now compare the relative DL of variants
  //            FastVector tempRules = (FastVector)ruleset.copyElements();
  //            tempRules.setElementAt(replace, position);
  //            //revision.clear(data,false);
  //
  //            StRuleStatsShort repStat = new StRuleStatsShort(data, tempRules,
  //                m_Debug && ves_Debug, m_FP_cost, m_FN_cost);
  //            repStat.setNumAllConds(m_Total);
  //            repStat.countData(position, data, prevRuleStats);
  //            // repStat.countData();
  //            rst = repStat.getSimpleStats(position);
  //            if(m_Debug)
  //              System.err.println("Replace rule covers: " + rst[0] + " | pos = "
  //                  + rst[2] + " | neg = " + rst[4]
  //                  + "\nThe rule doesn't cover: " + rst[1] + " | pos = "
  //                  + rst[5]);
  //
  //            double repDL = repStat.relativeDL(data, position, expFPRate,
  //                m_CheckErr);
  //            if(m_Debug)
  //              System.err.println("\nReplace: " + replace.toString(m_Class)
  //                  + " |dl = " + repDL);
  //
  //            if(Double.isNaN(repDL) || Double.isInfinite(repDL))
  //              throw new Exception(
  //                  "Should never happen: repDL in optmz. stage NaN or infinite!");
  //
  //            tempRules.setElementAt(revision, position);
  //            StRuleStatsShort revStat = new StRuleStatsShort(data, tempRules,
  //                m_Debug && ves_Debug, m_FP_cost, m_FN_cost);
  //            revStat.setNumAllConds(m_Total);
  //            revStat.countData(position, data, prevRuleStats);
  //            // revStat.countData();
  //            double revDL = revStat.relativeDL(data, position, expFPRate,
  //                m_CheckErr);
  //
  //            if(m_Debug)
  //              System.err.println("Revision: " + revision.toString(m_Class)
  //                  + " |dl = " + revDL);
  //
  //            if(Double.isNaN(revDL) || Double.isInfinite(revDL))
  //              throw new Exception(
  //                  "Should never happen: revDL in optmz. stage NaN or infinite!");
  //
  //            rstats = new StRuleStatsShort(data, ruleset, m_Debug && ves_Debug,
  // m_FP_cost, m_FN_cost);
  //            rstats.setNumAllConds(m_Total);
  //            rstats.countData(position, data, prevRuleStats);
  //            // rstats.countData();
  //            double oldDL = rstats.relativeDL(data, position, expFPRate,
  //                m_CheckErr);
  //
  //            if(Double.isNaN(oldDL) || Double.isInfinite(oldDL))
  //              throw new Exception(
  //                  "Should never happen: oldDL in optmz. stage NaN or infinite!");
  //            if(m_Debug)
  //              System.err.println("Old rule: " + oldRule.toString(m_Class)
  //                  + " |dl = " + oldDL);
  //
  //            if(m_Debug)
  //              System.err.println("\nrepDL: " + repDL + "\nrevDL: " + revDL
  //                  + "\noldDL: " + oldDL);
  //
  //            if((oldDL <= revDL) && (oldDL <= repDL))
  //              finalRule = oldRule; // Old the best
  //            else if(revDL <= repDL)
  //              finalRule = revision; // Revision the best
  //            else
  //              finalRule = replace; // Replace the best
  //          }
  //
  //          finalRulesetStat.addAndUpdate(finalRule, data);
  //          rst = finalRulesetStat.getSimpleStats(position);
  //
  //          if(isResidual){
  //
  //            dl += finalRulesetStat.relativeDL(data, position, expFPRate,
  //                m_CheckErr);
  //            if(m_Debug)
  //              System.err.println("After optimization: the dl" + "=" + dl
  //                  + " | best: " + minDL);
  //
  //            if(dl < minDL)
  //              minDL = dl; // The best dl so far
  //
  //            stop = checkStop(rst, minDL, dl);
  //            if(!stop){
  //              ruleset.addElement(finalRule); // Accepted
  //            } else{
  //              finalRulesetStat.removeLast(); // Remove last to be re-used
  //              position--;
  //            }
  //          } else
  //            ruleset.setElementAt(finalRule, position); // Accepted
  //          //(new RipperRule()).apply(ruleset,data,position+1);
  //          //transitiveClosure(data, m_Covered);
  //          if(m_Debug){
  //            System.err.println("The rule covers: " + rst[0] + " | pos = "
  //                + rst[2] + " | neg = " + rst[4] + "\nThe rule doesn't cover: "
  //                + rst[1] + " | pos = " + rst[5]);
  //            System.err.println("\nRuleset so far: ");
  //            for(int x = 0; x < ruleset.size(); x++)
  //              System.err.println(x + ": "
  //                  + ((RipperRule)ruleset.elementAt(x)).toString(m_Class));
  //            System.err.println();
  //          }
  //
  //          // Data not covered
  //          //if (finalRulesetStat.getRulesetSize() > 0)// If any rules
  //          // newData = finalRulesetStat.getFiltered(position)[1];
  //
  //          hasPositive = Utils.gr(rst[5], 0.0); // Positives remaining?
  //          position++;
  //        } // while !stop && hasPositive
  //
  //        if(ruleset.size() > (position + 1)){ // Hasn't gone through yet
  //          for(int k = position + 1; k < ruleset.size(); k++)
  //            finalRulesetStat.addAndUpdate((RipperRule)ruleset.elementAt(k),
  //                data);
  //        }
  //        if(m_Debug)
  //          System.err
  //              .println("\nDeleting rules to decrease DL of the whole ruleset ...");
  //        if(m_Debug){
  //          System.err.println("Reducing length of " + finalRulesetStat);
  //        }
  //
  //        finalRulesetStat.reduceDL(data, expFPRate, m_CheckErr);
  //        if(m_Debug){
  //          int del = ruleset.size() - finalRulesetStat.getRulesetSize();
  //          System.err.println(del
  //              + " rules are deleted after DL reduction procedure");
  //        }
  //        ruleset = finalRulesetStat.getRuleset();
  //        rstats = finalRulesetStat;
  //        System.out.println("Ruleset (in Ripper format) after optimization run "
  //            + z + "\n" + rstats.toRIPPERString());
  //      } // For each run of optimization
  //    } // if pruning is used
  //
  //    // Concatenate the ruleset for this class to the whole ruleset
  //    if(m_Debug){
  //      System.err.println("\nFinal ruleset (size " + ruleset.size() + "): ");
  //      for(int x = 0; x < ruleset.size(); x++)
  //        System.err.println(x + ": "
  //            + ((RipperRule)ruleset.elementAt(x)).toString(m_Class));
  //      System.err.println();
  //    }
  //
  //    m_Ruleset.appendElements(ruleset);
  //    m_RulesetStats.addElement(rstats);
  //
  //    //if (ruleset.size() > 0)// If any rules for this class
  //    // return rstats.getFiltered(ruleset.size() - 1)[1]; // Data not covered
  //    //else
  //    return data;
  //  }
  //
  //  /**
  //   * Check whether the stopping criterion meets
  //   *
  //   * @param rst
  //   * the statistic of the ruleset
  //   * @param minDL
  //   * the min description length so far
  //   * @param dl
  //   * the current description length of the ruleset
  //   * @return true if stop criterion meets, false otherwise
  //   */
  //  private boolean checkStop(double[] rst, double minDL, double dl) {
  //
  //    if(dl > minDL + MAX_DL_SURPLUS){
  //      if(m_Debug)
  //        System.err.println("DL too large: " + dl + " | " + minDL);
  //      return true;
  //    } else if(!Utils.gr(rst[2], 0.0)){// Covered positives
  //      if(m_Debug)
  //        System.err.println("Too few positives.");
  //      return true;
  //    } else if((rst[4] / rst[0]) >= 0.5){// Err rate
  //      if(m_CheckErr){
  //        if(m_Debug)
  //          System.err.println("Error too large: " + rst[4] + "/" + rst[0]);
  //        return true;
  //      } else
  //        return false;
  //    } else{// Not stops
  //      if(m_Debug)
  //        System.err.println("Continue.");
  //      return false;
  //    }
  //  }
  //
  //  /**
  //   * Prints the all the rules of the rule learner.
  //   *
  //   * @return a textual description of the classifier
  //   */
  //  public String toString() {
  //    if(m_Ruleset == null)
  //      return "StRIP: No model built yet.";
  //
  //    StringBuffer sb = new StringBuffer("StRip rules:\n" + "===========\n\n");
  //    for(int j = 0; j < m_RulesetStats.size(); j++){
  //      StRuleStatsShort rs = (StRuleStatsShort)m_RulesetStats.elementAt(j);
  //      FastVector rules = rs.getRuleset();
  //      for(int k = 0; k < rules.size(); k++){
  //        double[] simStats = rs.getSimpleStats(k);
  //        sb.append(((RipperRule)rules.elementAt(k)).toString(m_Class) + " ("
  //            + simStats[0] + "/" + simStats[4] + ")\n");
  //      }
  //    }
  //    if(m_Debug){
  //      System.err.println("Inside m_Ruleset");
  //      for(int i = 0; i < m_Ruleset.size(); i++)
  //        System.err.println(((RipperRule)m_Ruleset.elementAt(i))
  //            .toString(m_Class));
  //    }
  //    sb.append("\nNumber of Rules : " + m_Ruleset.size() + "\n");
  //    return sb.toString();
  //  }
  //
  /**
   * Main method.
   * 
   * @param args
   *          the options for the classifier
   */
  public static void main(String[] args) {
    try{
      String[] options = args;
      StPerceptronMUC classifier = new StPerceptronMUC();
      ModifiedInstancesShort[] data = null, trainData = null, testData = null;
      ModifiedInstancesShort trainAll = null, tempTrain, test = null, template = null;
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
        template = test = new ModifiedInstancesShort(testReader, 1);
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
      trainAll = new ModifiedInstancesShort(trainReader);
      trainAll.setClass(trainAll.attribute("class"));
      trainAll.cleanUpValues();
      AttributeShort predClass = trainAll.classAttribute().copy("predicted_class");
      trainAll.insertAttributeAt(predClass, trainAll.numAttributes());
      //System.err.println(trainAll);
      int numDocs = trainAll.getNumDocuments();
      trainAll = trainAll.binarizeValues();
      trainAll.normalizeAttrValues();
      trainAll.setNumDocuments(numDocs);
      trainAll.setClass(trainAll.attribute("class"));
      //System.err.println(trainAll);

      data = ModifiedInstancesShort.splitDocs(trainAll);
      //Use half of the data for training and half for evaluation
      int mid = data.length / 2;
      trainData = new ModifiedInstancesShort[mid];
      testData = new ModifiedInstancesShort[data.length - mid];
      int trainInst = 0, testInst = 0;
      for(int i = 0; i < mid; i++){
        trainData[i] = data[i];
        trainInst += data[i].numInstances();
      }
      for(int i = mid; i < data.length; i++){
        testData[i - mid] = data[i];
        testInst += data[i].numInstances();
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
        LeanDocument[] truth = new LeanDocument[testData.length];
        for(int i = 0; i < truth.length; i++){
          truth[i] = makeDocument(testData[i]);
          //System.err.println(truth[i]);
        }
        evaluateClassifier(testData, m_Weights, truth);
      } else{
        System.out.println("Split at document " + mid + ". Train " + trainInst + " insts. Test "
            + testInst + " insts.");
        double[] w = classifier.buildClassifier(trainData, testData);
        if(m_SaveFilename.length() != 0){
          try{
            PrintStream file = new PrintStream(new FileOutputStream(m_SaveFilename));
            for(int y = 0; y < w.length; y++)
              file.println(w[y]);
          } catch(IOException ioe){
            throw new RuntimeException(ioe);
          }
        }
        //classifier.evaluateClassifier(trainData,w);
        evaluateClassifier(testData, w, null);
      }
    } catch(Exception e){
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
    System.out.println();
  }
  
  public static LeanDocument applyAndSetUpMUC(ModifiedInstancesShort doc, LeanDocument truth, double w[], int muc[][]){
    int[] clusterPtrs = apply(w, doc);
    LeanDocument cDoc = makeDocument(doc, clusterPtrs);
    
    muc[1] = MUCScore.mucScore(cDoc, truth);
    muc[0] = MUCScore.mucScore(truth, cDoc);
    if(m_Debug)
      System.err.println(cDoc);
    return cDoc;
  }

  public static int[] apply(double[] w, ModifiedInstancesShort data) {

    //LeanDocument result = new LeanDocument();
    AttributeShort DOCID = data.m_DOCNO;
    AttributeShort ID1 = data.m_ID1;
    AttributeShort ID2 = data.m_ID2;

    int[] ptrs = null;
    if(data == null || data.numInstances() == 0)
      return null;
    InstanceShort first = data.instance(0);
    int id1 = (int)first.value(ID1);
    int id2 = (int)first.value(ID2);
    int len = id2 + 1;
    ptrs = new int[len];
    // initialize pointers so each item is in it's own set
    for(int j = 0; j < len; j++)
      ptrs[j] = j;

    for(int i = 0; i < data.numInstances(); i++){
      // read all instances for a document
      InstanceShort current = data.instance(i);
      // System.out.println("Working on: "+current);
      id1 = (int)current.value(ID1);
      id2 = (int)current.value(ID2);

      if(innerProduct(w, current) > THRESHOLD){
        union(id1, id2, ptrs);
        current.setValue(data.m_Prediction, data.m_Positive);
      } else{
        current.setValue(data.m_Prediction, data.m_Negative);
      }
    }
    /*
     * for(int k = 0; k < data.numInstances(); k++){ InstanceShort curPair =
     * data.instance(k); int curId1 = (int)curPair.value(ID1); int curId2 =
     * (int)curPair.value(ID2); if(!result.contains(new Integer(curId1))){
     * //System.err.println("Add "+curId1+"-"+find(curId1,ptrs)); result.add(new
     * Integer(curId1),new Integer(find(curId1,ptrs))); }
     * if(!result.contains(new Integer(curId2))) result.add(new
     * Integer(curId2),new Integer(find(curId2,ptrs))); }
     * //System.err.print("[TC:"+startPositive+"("+startPositiveTotal+")->" //
     * +endPositive+"("+endPositiveTotal+")]"); //System.err.println(result);
     *  
     */
    return ptrs;
  }

  private static LeanDocument makeDocument(ModifiedInstancesShort data, int[] ptrs) {
    LeanDocument result = new LeanDocument();
    AttributeShort ID1 = data.m_ID1;
    AttributeShort ID2 = data.m_ID2;

    for(int k = 0; k < data.numInstances(); k++){
      InstanceShort curPair = data.instance(k);
      int curId1 = (int)curPair.value(ID1);
      int curId2 = (int)curPair.value(ID2);
      if(!result.contains(new Integer(curId1))){
        //System.err.println("Add "+curId1+"-"+find(curId1,ptrs));
        result.add(new Integer(curId1), new Integer(find(curId1, ptrs)));
      }
      if(!result.contains(new Integer(curId2)))
        result.add(new Integer(curId2), new Integer(find(curId2, ptrs)));
    }
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

  private int getChainSize(int id1, int[] clusterPtrs) {
    int cl = find(id1, clusterPtrs);
    int num = 0;
    for(int i = 0; i < clusterPtrs.length; i++)
      if(find(i, clusterPtrs) == cl)
        num++;
    return num;
  }

  public static LeanDocument makeDocument(ModifiedInstancesShort data) {
    if(data == null || data.numInstances() == 0)
      return null;
    LeanDocument result = new LeanDocument(data.instance(0).value(data.m_DOCNO));
    AttributeShort DOCID = data.m_DOCNO;
    AttributeShort ID1 = data.m_ID1;
    AttributeShort ID2 = data.m_ID2;
    AttributeShort Cl = data.classAttribute();
    short pos = data.m_Positive;

    int[] ptrs = null;
    if(data == null || data.numInstances() == 0)
      return null;
    InstanceShort first = data.instance(0);
    int id1 = (int)first.value(ID1);
    int id2 = (int)first.value(ID2);
    int len = id2 + 1;
    ptrs = new int[len];
    // initialize pointers so each item is in it's own set
    for(int j = 0; j < len; j++)
      ptrs[j] = j;

    if(first.value(Cl) == pos){
      union(id1, id2, ptrs);
    }
    for(int i = 1; i < data.numInstances(); i++){
      // read all instances for a document
      InstanceShort current = data.instance(i);
      // System.out.println("Working on: "+current);
      id1 = (int)current.value(ID1);
      id2 = (int)current.value(ID2);

      if(current.value(Cl) == pos){
        union(id1, id2, ptrs);
      }
    }
    for(int k = 0; k < data.numInstances(); k++){
      InstanceShort curPair = data.instance(k);
      int curId1 = (int)curPair.value(ID1);
      int curId2 = (int)curPair.value(ID2);
      if(!result.contains(new Integer(curId1))){
        //System.err.println("Add "+curId1+"-"+find(curId1,ptrs));
        result.add(new Integer(curId1), new Integer(find(curId1, ptrs)));
      }
      if(!result.contains(new Integer(curId2)))
        result.add(new Integer(curId2), new Integer(find(curId2, ptrs)));
    }
    //System.err.print("[TC:"+startPositive+"("+startPositiveTotal+")->"
    //		+endPositive+"("+endPositiveTotal+")]");
    //System.err.println(result);
    return result;
  }
}