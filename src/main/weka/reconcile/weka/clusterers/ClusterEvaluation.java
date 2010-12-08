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
 *    ClusterEvaluation.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package  reconcile.weka.clusterers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;

import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Range;
import reconcile.weka.core.Utils;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.unsupervised.attribute.Remove;


/**
 * Class for evaluating clustering models.<p>
 *
 * Valid options are: <p>
 *
 * -t <name of the training file> <br>
 * Specify the training file. <p>
 *
 * -T <name of the test file> <br>
 * Specify the test file to apply clusterer to. <p>
 *
 * -d <name of file to save clustering model to> <br>
 * Specify output file. <p>
 *
 * -l <name of file to load clustering model from> <br>
 * Specifiy input file. <p>
 *
 * -p <attribute range> <br>
 * Output predictions. Predictions are for the training file if only the
 * training file is specified, otherwise they are for the test file. The range
 * specifies attribute values to be output with the predictions.
 * Use '-p 0' for none. <p>
 *
 * -x <num folds> <br>
 * Set the number of folds for a cross validation of the training data.
 * Cross validation can only be done for distribution clusterers and will
 * be performed if the test file is missing. <p>
 *
 * -c <class> <br>
 * Set the class attribute. If set, then class based evaluation of clustering
 * is performed. <p>
 *
 * @author   Mark Hall (mhall@cs.waikato.ac.nz)
 * @version  $Revision: 1.1 $
 */
public class ClusterEvaluation implements Serializable {

  /** the instances to cluster */
  private Instances m_trainInstances;
  
  /** the clusterer */
  private Clusterer m_Clusterer;

  /** holds a string describing the results of clustering the training data */
  private StringBuffer m_clusteringResults;

  /** holds the number of clusters found by the clusterer */
  private int m_numClusters;

  /** holds the assigments of instances to clusters for a particular testing
      dataset */
  private double [] m_clusterAssignments;

  /* holds the average log likelihood for a particular testing dataset
     if the clusterer is a DensityBasedClusterer */
  private double m_logL;

  /** will hold the mapping of classes to clusters (for class based 
      evaluation) */
  private int [] m_classToCluster = null;

  /**
   * set the clusterer
   * @param clusterer the clusterer to use
   */
  public void setClusterer(Clusterer clusterer) {
    m_Clusterer = clusterer;
  }

  /**
   * return the results of clustering.
   * @return a string detailing the results of clustering a data set
   */
  public String clusterResultsToString() {
    return m_clusteringResults.toString();
  }

  /**
   * Return the number of clusters found for the most recent call to
   * evaluateClusterer
   * @return the number of clusters found
   */
  public int getNumClusters() {
    return m_numClusters;
  }

  /**
   * Return an array of cluster assignments corresponding to the most
   * recent set of instances clustered.
   * @return an array of cluster assignments
   */
  public double [] getClusterAssignments() {
    return m_clusterAssignments;
  }

  /**
   * Return the array (ordered by cluster number) of minimum error class to
   * cluster mappings
   * @return an array of class to cluster mappings
   */
  public int [] getClassesToClusters() {
    return m_classToCluster;
  }

  /**
   * Return the log likelihood corresponding to the most recent
   * set of instances clustered.
   *
   * @return a <code>double</code> value
   */
  public double getLogLikelihood() {
    return m_logL;
  }

  /**
   * Constructor. Sets defaults for each member variable. Default Clusterer
   * is EM.
   */
  public ClusterEvaluation () {
    setClusterer(new EM());
    m_trainInstances = null;
    m_clusteringResults = new StringBuffer();
    m_clusterAssignments = null;
  }

  /**
   * Evaluate the clusterer on a set of instances. Calculates clustering
   * statistics and stores cluster assigments for the instances in
   * m_clusterAssignments
   * @param test the set of instances to cluster
   * @exception Exception if something goes wrong
   */
  public void evaluateClusterer(Instances test) throws Exception {
    int i = 0;
    int cnum;
    double loglk = 0.0;
    double[] dist;
    double temp;
    int cc = m_Clusterer.numberOfClusters();
    m_numClusters = cc;
    int numInstFieldWidth = (int)((Math.log(test.numInstances())/
				   Math.log(10))+1);
    double[] instanceStats = new double[cc];
    m_clusterAssignments = new double [test.numInstances()];
    Instances testCopy = test;
    boolean hasClass = (testCopy.classIndex() >= 0);
    int unclusteredInstances = 0;

    // If class is set then do class based evaluation as well
    if (hasClass) {
      if (testCopy.classAttribute().isNumeric()) {
	throw new Exception("ClusterEvaluation: Class must be nominal!");
      }
      Remove removeClass = new Remove();
      removeClass.setAttributeIndices(""+(testCopy.classIndex()+1));
      removeClass.setInvertSelection(false);
      removeClass.setInputFormat(testCopy);
      testCopy = Filter.useFilter(testCopy, removeClass);
    }

    for (i=0;i<testCopy.numInstances();i++) {
      cnum = -1;
      try {
	if (m_Clusterer instanceof DensityBasedClusterer) {
	  loglk += ((DensityBasedClusterer)m_Clusterer).
	    logDensityForInstance(testCopy.instance(i));
	  //	  temp = Utils.sum(dist);
	  
	  //	  Utils.normalize(dist);
	  cnum = m_Clusterer.clusterInstance(testCopy.instance(i)); 
	  // Utils.maxIndex(dist);
	  m_clusterAssignments[i] = (double)cnum;
	} else {
	  cnum = m_Clusterer.clusterInstance(testCopy.instance(i));
	  m_clusterAssignments[i] = (double)cnum;
	}
      }
      catch (Exception e) {
	unclusteredInstances++;
      }
      
      if (cnum != -1) {
	instanceStats[cnum]++;
      }
    }

    /* // count the actual number of used clusters
    int count = 0;
    for (i = 0; i < cc; i++) {
      if (instanceStats[i] > 0) {
	count++;
      }
    }
    if (count > 0) {
      double [] tempStats = new double [count];
      double [] map = new double [m_clusterAssignments.length];
      count=0;
      for (i=0;i<cc;i++) {
	if (instanceStats[i] > 0) {
	  tempStats[count] = instanceStats[i];
	  map[i] = count;
	  count++;
	}
      }
      instanceStats = tempStats;
      cc = instanceStats.length;
      for (i=0;i<m_clusterAssignments.length;i++) {
	m_clusterAssignments[i] = map[(int)m_clusterAssignments[i]];
      }
      } */ 

    double sum = Utils.sum(instanceStats);
    loglk /= sum;
    m_logL = loglk;
    
    m_clusteringResults.append(m_Clusterer.toString());
    m_clusteringResults.append("Clustered Instances\n\n");
    int clustFieldWidth = (int)((Math.log(cc)/Math.log(10))+1);
    for (i = 0; i < cc; i++) {
      if (instanceStats[i] > 0) {
	m_clusteringResults.append(Utils.doubleToString((double)i, 
							clustFieldWidth, 0) 
				   + "      " 
				   + Utils.doubleToString(instanceStats[i],
							  numInstFieldWidth, 0) 
				   + " (" 
				   + Utils.doubleToString((instanceStats[i] / 
							   sum * 100.0)
							  , 3, 0) + "%)\n");
      }
    }
    
    if (unclusteredInstances > 0) {
      m_clusteringResults.append("\nUnclustered instances : "
				 +unclusteredInstances);
    }

    if (m_Clusterer instanceof DensityBasedClusterer) {
      m_clusteringResults.append("\n\nLog likelihood: " 
				 + Utils.doubleToString(loglk, 1, 5) 
				 + "\n");
    }
    
    if (hasClass) {
      evaluateClustersWithRespectToClass(test);
    }
  }

  /**
   * Evaluates cluster assignments with respect to actual class labels.
   * Assumes that m_Clusterer has been trained and tested on 
   * inst (minus the class).
   * @param inst the instances (including class) to evaluate with respect to
   * @exception Exception if something goes wrong
   */
  private void evaluateClustersWithRespectToClass(Instances inst)
    throws Exception {
    int numClasses = inst.classAttribute().numValues();
    int [][] counts = new int [m_numClusters][numClasses];
    int [] clusterTotals = new int[m_numClusters];
    double [] best = new double[m_numClusters+1];
    double [] current = new double[m_numClusters+1];

    for (int i = 0; i < inst.numInstances(); i++) {
      counts[(int)m_clusterAssignments[i]][(int)inst.instance(i).classValue()]++;
      clusterTotals[(int)m_clusterAssignments[i]]++;
    }
    
    best[m_numClusters] = Double.MAX_VALUE;
    mapClasses(0, counts, clusterTotals, current, best, 0);

    m_clusteringResults.append("\n\nClass attribute: "
			+inst.classAttribute().name()
			+"\n");
    m_clusteringResults.append("Classes to Clusters:\n");
    String matrixString = toMatrixString(counts, clusterTotals, inst);
    m_clusteringResults.append(matrixString).append("\n");

    int Cwidth = 1 + (int)(Math.log(m_numClusters) / Math.log(10));
    // add the minimum error assignment
    for (int i = 0; i < m_numClusters; i++) {
      if (clusterTotals[i] > 0) {
	m_clusteringResults.append("Cluster "
				   +Utils.doubleToString((double)i,Cwidth,0));
	m_clusteringResults.append(" <-- ");
	
	if (best[i] < 0) {
	  m_clusteringResults.append("No class\n");
	} else {
	  m_clusteringResults.
	    append(inst.classAttribute().value((int)best[i])).append("\n");
	}
      }
    }
    m_clusteringResults.append("\nIncorrectly clustered instances :\t"
			       +best[m_numClusters]+"\t"
			       +(Utils.doubleToString((best[m_numClusters] / 
						       inst.numInstances() * 
						       100.0), 8, 4))
			       +" %\n");

    // copy the class assignments
    m_classToCluster = new int [m_numClusters];
    for (int i = 0; i < m_numClusters; i++) {
      m_classToCluster[i] = (int)best[i];
    }
  }

  /**
   * Returns a "confusion" style matrix of classes to clusters assignments
   * @param counts the counts of classes for each cluster
   * @param clusterTotals total number of examples in each cluster
   * @param inst the training instances (with class)
   * @exception Exception if matrix can't be generated
   */
  private String toMatrixString(int [][] counts, int [] clusterTotals,
				Instances inst) 
    throws Exception {
    StringBuffer ms = new StringBuffer();

    int maxval = 0;
    for (int i = 0; i < m_numClusters; i++) {
      for (int j = 0; j < counts[i].length; j++) {
	if (counts[i][j] > maxval) {
	  maxval = counts[i][j];
	}
      }
    }

    int Cwidth = 1 + Math.max((int)(Math.log(maxval) / Math.log(10)),
			      (int)(Math.log(m_numClusters) / Math.log(10)));

    ms.append("\n");
    
    for (int i = 0; i < m_numClusters; i++) {
      if (clusterTotals[i] > 0) {
	ms.append(" ").append(Utils.doubleToString((double)i, Cwidth, 0));
      }
    }
    ms.append("  <-- assigned to cluster\n");
    
    for (int i = 0; i< counts[0].length; i++) {

      for (int j = 0; j < m_numClusters; j++) {
	if (clusterTotals[j] > 0) {
	  ms.append(" ").append(Utils.doubleToString((double)counts[j][i], 
						     Cwidth, 0));
	}
      }
      ms.append(" | ").append(inst.classAttribute().value(i)).append("\n");
    }

    return ms.toString();
  }

  /**
   * Finds the minimum error mapping of classes to clusters. Recursively
   * considers all possible class to cluster assignments.
   * @param lev the cluster being processed
   * @param counts the counts of classes in clusters
   * @param clusterTotals the total number of examples in each cluster
   * @param current the current path through the class to cluster assignment
   * tree
   * @param best the best assignment path seen
   * @param error accumulates the error for a particular path
   */
  private void mapClasses(int lev, int [][] counts, int [] clusterTotals,
			  double [] current, double [] best, int error) {
    // leaf
    if (lev == m_numClusters) {
      if (error < best[m_numClusters]) {
	best[m_numClusters] = error;
	for (int i = 0; i < m_numClusters; i++) {
	  best[i] = current[i];
	}
      }
    } else {
      // empty cluster -- ignore
      if (clusterTotals[lev] == 0) {
	current[lev] = -1; // cluster ignored
	mapClasses(lev+1, counts, clusterTotals, current, best,
		   error);
      } else {
	// first try no class assignment to this cluster
	current[lev] = -1; // cluster assigned no class (ie all errors)
	mapClasses(lev+1, counts, clusterTotals, current, best,
		   error+clusterTotals[lev]);
	// now loop through the classes in this cluster
	for (int i = 0; i < counts[0].length; i++) {
	  if (counts[lev][i] > 0) {
	    boolean ok = true;
	    // check to see if this class has already been assigned
	    for (int j = 0; j < lev; j++) {
	      if ((int)current[j] == i) {
		ok = false;
		break;
	      }
	    }
	    if (ok) {
	      current[lev] = i;
	      mapClasses(lev+1, counts, clusterTotals, current, best, 
			 (error + (clusterTotals[lev] - counts[lev][i])));
	    }
	  }
	}
      }
    }
  }

  /**
   * Evaluates a clusterer with the options given in an array of
   * strings. It takes the string indicated by "-t" as training file, the
   * string indicated by "-T" as test file.
   * If the test file is missing, a stratified ten-fold
   * cross-validation is performed (distribution clusterers only).
   * Using "-x" you can change the number of
   * folds to be used, and using "-s" the random seed.
   * If the "-p" option is present it outputs the classification for
   * each test instance. If you provide the name of an object file using
   * "-l", a clusterer will be loaded from the given file. If you provide the
   * name of an object file using "-d", the clusterer built from the
   * training data will be saved to the given file.
   *
   * @param clusterer machine learning clusterer
   * @param options the array of string containing the options
   * @exception Exception if model could not be evaluated successfully
   * @return a string describing the results 
   */
  public static String evaluateClusterer (Clusterer clusterer, 
					  String[] options)
    throws Exception {
    int seed = 1, folds = 10;
    boolean doXval = false;
    Instances train = null;
    Instances test = null;
    Random random;
    String trainFileName, testFileName, seedString, foldsString, objectInputFileName, objectOutputFileName, attributeRangeString;
    String[] savedOptions = null;
    boolean printClusterAssignments = false;
    Range attributesToOutput = null;
    ObjectInputStream objectInputStream = null;
    ObjectOutputStream objectOutputStream = null;
    StringBuffer text = new StringBuffer();
    int theClass = -1; // class based evaluation of clustering

    try {
      if (Utils.getFlag('h', options)) {
        throw  new Exception("Help requested.");
      }

      // Get basic options (options the same for all clusterers
      //printClusterAssignments = Utils.getFlag('p', options);
      objectInputFileName = Utils.getOption('l', options);
      objectOutputFileName = Utils.getOption('d', options);
      trainFileName = Utils.getOption('t', options);
      testFileName = Utils.getOption('T', options);

      // Check -p option
      try {
	attributeRangeString = Utils.getOption('p', options);
      }
      catch (Exception e) {
	throw new Exception(e.getMessage() + "\nNOTE: the -p option has changed. " +
			    "It now expects a parameter specifying a range of attributes " +
			    "to list with the predictions. Use '-p 0' for none.");
      }
      if (attributeRangeString.length() != 0) {
	printClusterAssignments = true;
	if (!attributeRangeString.equals("0")) 
	  attributesToOutput = new Range(attributeRangeString);
      }

      if (trainFileName.length() == 0) {
        if (objectInputFileName.length() == 0) {
          throw  new Exception("No training file and no object " 
			       + "input file given.");
        }

        if (testFileName.length() == 0) {
          throw  new Exception("No training file and no test file given.");
        }
      }
      else {
	if ((objectInputFileName.length() != 0) 
	    && (printClusterAssignments == false)) {
	  throw  new Exception("Can't use both train and model file " 
			       + "unless -p specified.");
	}
      }

      seedString = Utils.getOption('s', options);

      if (seedString.length() != 0) {
	seed = Integer.parseInt(seedString);
      }

      foldsString = Utils.getOption('x', options);

      if (foldsString.length() != 0) {
	folds = Integer.parseInt(foldsString);
	doXval = true;
      }
    }
    catch (Exception e) {
      throw  new Exception('\n' + e.getMessage() 
			   + makeOptionString(clusterer));
    }

    try {
      if (trainFileName.length() != 0) {
	train = new Instances(new BufferedReader(new FileReader(trainFileName)));

	String classString = Utils.getOption('c',options);
	if (classString.length() != 0) {
	  if (classString.compareTo("last") == 0) {
	    theClass = train.numAttributes();
	  } else if (classString.compareTo("first") == 0) {
	    theClass = 1;
	  } else {
	    theClass = Integer.parseInt(classString);
	  }
	  if (doXval || testFileName.length() != 0) {
	    throw new Exception("Can only do class based evaluation on the "
				+"training data");
	  }
	  
	  if (objectInputFileName.length() != 0) {
	    throw new Exception("Can't load a clusterer and do class based "
				+"evaluation");
	  }
	}

	if (theClass != -1) {
	  if (theClass < 1 
	      || theClass > train.numAttributes()) {
	    throw new Exception("Class is out of range!");
	  }
	  if (!train.attribute(theClass-1).isNominal()) {
	    throw new Exception("Class must be nominal!");
	  }
	  train.setClassIndex(theClass-1);
	}
      }

      if (objectInputFileName.length() != 0) {
	objectInputStream = new ObjectInputStream(new FileInputStream(objectInputFileName));
      }

      if (objectOutputFileName.length() != 0) {
	objectOutputStream = new 
	  ObjectOutputStream(new FileOutputStream(objectOutputFileName));
      }
    }
    catch (Exception e) {
      throw  new Exception("ClusterEvaluation: " + e.getMessage() + '.');
    }

    // Save options
    if (options != null) {
      savedOptions = new String[options.length];
      System.arraycopy(options, 0, savedOptions, 0, options.length);
    }

    if (objectInputFileName.length() != 0) {
      Utils.checkForRemainingOptions(options);
    }

    // Set options for clusterer
    if (clusterer instanceof OptionHandler) {
      ((OptionHandler)clusterer).setOptions(options);
    }

    Utils.checkForRemainingOptions(options);

    if (objectInputFileName.length() != 0) {
      // Load the clusterer from file
      clusterer = (Clusterer)objectInputStream.readObject();
      objectInputStream.close();
    }
    else {
      // Build the clusterer if no object file provided
      if (theClass == -1) {
	clusterer.buildClusterer(train);
      } else {
	Remove removeClass = new Remove();
	removeClass.setAttributeIndices(""+theClass);
	removeClass.setInvertSelection(false);
	removeClass.setInputFormat(train);
	Instances clusterTrain = Filter.useFilter(train, removeClass);
	clusterer.buildClusterer(clusterTrain);
	ClusterEvaluation ce = new ClusterEvaluation();
	ce.setClusterer(clusterer);
	ce.evaluateClusterer(train);
	
	return "\n\n=== Clustering stats for training data ===\n\n" +
	  ce.clusterResultsToString();
      }
    }

    /* Output cluster predictions only (for the test data if specified,
       otherwise for the training data */
    if (printClusterAssignments) {
      return  printClusterings(clusterer, train, testFileName, attributesToOutput);
    }

    text.append(clusterer.toString());
    text.append("\n\n=== Clustering stats for training data ===\n\n" 
		+ printClusterStats(clusterer, trainFileName));

    if (testFileName.length() != 0) {
      text.append("\n\n=== Clustering stats for testing data ===\n\n" 
		  + printClusterStats(clusterer, testFileName));
    }

    if ((clusterer instanceof DensityBasedClusterer) && 
	(doXval == true) && 
	(testFileName.length() == 0) && 
	(objectInputFileName.length() == 0)) {
      // cross validate the log likelihood on the training data
      random = new Random(seed);
      random.setSeed(seed);
      train.randomize(random);
      text.append(crossValidateModel(clusterer.getClass().getName()
				     , train, folds, savedOptions, random));
    }

    // Save the clusterer if an object output file is provided
    if (objectOutputFileName.length() != 0) {
      objectOutputStream.writeObject(clusterer);
      objectOutputStream.flush();
      objectOutputStream.close();
    }

    return  text.toString();
  }

  /**
   * Perform a cross-validation for DensityBasedClusterer on a set of instances.
   *
   * @param clusterer the clusterer to use
   * @param data the training data
   * @param numFolds number of folds of cross validation to perform
   * @param random random number seed for cross-validation
   * @return the cross-validated log-likelihood
   * @exception Exception if an error occurs
   */
  public static double crossValidateModel(DensityBasedClusterer clusterer,
					  Instances data,
					  int numFolds,
					  Random random) throws Exception {
    Instances train, test;
    double foldAv = 0;;
    double[] tempDist;
    data = new Instances(data);
    data.randomize(random);
    //    double sumOW = 0;
    for (int i = 0; i < numFolds; i++) {
      // Build and test clusterer
      train = data.trainCV(numFolds, i, random);

      clusterer.buildClusterer(train);

      test = data.testCV(numFolds, i);
      
      for (int j = 0; j < test.numInstances(); j++) {
	try {
	  foldAv += ((DensityBasedClusterer)clusterer).
	    logDensityForInstance(test.instance(j));
	  //	  sumOW += test.instance(j).weight();
	  //	double temp = Utils.sum(tempDist);
	} catch (Exception ex) {
	  // unclustered instances
	}
      }
    }
   
    //    return foldAv / sumOW;
    return foldAv / data.numInstances();
  }

  /**
   * Performs a cross-validation 
   * for a DensityBasedClusterer clusterer on a set of instances.
   *
   * @param clustererString a string naming the class of the clusterer
   * @param data the data on which the cross-validation is to be 
   * performed 
   * @param numFolds the number of folds for the cross-validation
   * @param options the options to the clusterer
   * @param random a random number generator
   * @return a string containing the cross validated log likelihood
   * @exception Exception if a clusterer could not be generated 
   */
  public static String crossValidateModel (String clustererString, 
					   Instances data, 
					   int numFolds, 
					   String[] options,
					   Random random)
    throws Exception {
    Clusterer clusterer = null;
    Instances train, test;
    String[] savedOptions = null;
    double foldAv;
    double CvAv = 0.0;
    double[] tempDist;
    StringBuffer CvString = new StringBuffer();

    if (options != null) {
      savedOptions = new String[options.length];
    }

    data = new Instances(data);

    // create clusterer
    try {
      clusterer = (Clusterer)Class.forName(clustererString).newInstance();
    }
    catch (Exception e) {
      throw  new Exception("Can't find class with name " 
			   + clustererString + '.');
    }

    if (!(clusterer instanceof DensityBasedClusterer)) {
      throw  new Exception(clustererString 
			   + " must be a distrinbution " 
			   + "clusterer.");
    }

    // Save options
    if (options != null) {
      System.arraycopy(options, 0, savedOptions, 0, options.length);
    }

    // Parse options
    if (clusterer instanceof OptionHandler) {
      try {
	((OptionHandler)clusterer).setOptions(savedOptions);
	Utils.checkForRemainingOptions(savedOptions);
      }
      catch (Exception e) {
	throw  new Exception("Can't parse given options in " 
			     + "cross-validation!");
      }
    }
    CvAv = crossValidateModel((DensityBasedClusterer)clusterer, data, numFolds, random);

    CvString.append("\n" + numFolds 
		    + " fold CV Log Likelihood: " 
		    + Utils.doubleToString(CvAv, 6, 4) 
		    + "\n");
    return  CvString.toString();
  }


  // ===============
  // Private methods
  // ===============
  /**
   * Print the cluster statistics for either the training
   * or the testing data.
   *
   * @param clusterer the clusterer to use for generating statistics.
   * @return a string containing cluster statistics.
   * @exception if statistics can't be generated.
   */
  private static String printClusterStats (Clusterer clusterer, 
					   String fileName)
    throws Exception {
    StringBuffer text = new StringBuffer();
    int i = 0;
    int cnum;
    double loglk = 0.0;
    double[] dist;
    double temp;
    int cc = clusterer.numberOfClusters();
    double[] instanceStats = new double[cc];
    int unclusteredInstances = 0;

    if (fileName.length() != 0) {
      BufferedReader inStream = null;

      try {
	inStream = new BufferedReader(new FileReader(fileName));
      }
      catch (Exception e) {
	throw  new Exception("Can't open file " + e.getMessage() + '.');
      }

      Instances inst = new Instances(inStream, 1);

      while (inst.readInstance(inStream)) {
	try {
	  cnum = clusterer.clusterInstance(inst.instance(0));

	  if (clusterer instanceof DensityBasedClusterer) {
	    loglk += ((DensityBasedClusterer)clusterer).
	      logDensityForInstance(inst.instance(0));
	    //	    temp = Utils.sum(dist);
	  }
	  instanceStats[cnum]++;
	}
	catch (Exception e) {
	  unclusteredInstances++;
	}
	inst.delete(0);
	i++;
      }

      /*
      // count the actual number of used clusters
      int count = 0;
      for (i = 0; i < cc; i++) {
	if (instanceStats[i] > 0) {
	  count++;
	}
      }
      if (count > 0) {
	double [] tempStats = new double [count];
	count=0;
	for (i=0;i<cc;i++) {
	  if (instanceStats[i] > 0) {
	    tempStats[count++] = instanceStats[i];
	}
	}
	instanceStats = tempStats;
	cc = instanceStats.length;
	} */

      int clustFieldWidth = (int)((Math.log(cc)/Math.log(10))+1);
      int numInstFieldWidth = (int)((Math.log(i)/Math.log(10))+1);
      double sum = Utils.sum(instanceStats);
      loglk /= sum;
      text.append("Clustered Instances\n");

      for (i = 0; i < cc; i++) {
	if (instanceStats[i] > 0) {
	  text.append(Utils.doubleToString((double)i, 
					   clustFieldWidth, 0) 
		      + "      " 
		      + Utils.doubleToString(instanceStats[i], 
					     numInstFieldWidth, 0) 
		      + " (" 
		    + Utils.doubleToString((instanceStats[i]/sum*100.0)
					   , 3, 0) + "%)\n");
	}
      }
      if (unclusteredInstances > 0) {
	text.append("\nUnclustered Instances : "+unclusteredInstances);
      }

      if (clusterer instanceof DensityBasedClusterer) {
	text.append("\n\nLog likelihood: " 
		    + Utils.doubleToString(loglk, 1, 5) 
		    + "\n");
      }
    }

    return  text.toString();
  }


  /**
   * Print the cluster assignments for either the training
   * or the testing data.
   *
   * @param clusterer the clusterer to use for cluster assignments
   * @return a string containing the instance indexes and cluster assigns.
   * @exception if cluster assignments can't be printed
   */
  private static String printClusterings (Clusterer clusterer, Instances train,
					  String testFileName, Range attributesToOutput)
    throws Exception {
    StringBuffer text = new StringBuffer();
    int i = 0;
    int cnum;

    if (testFileName.length() != 0) {
      BufferedReader testStream = null;

      try {
	testStream = new BufferedReader(new FileReader(testFileName));
      }
      catch (Exception e) {
	throw  new Exception("Can't open file " + e.getMessage() + '.');
      }

      Instances test = new Instances(testStream, 1);

      while (test.readInstance(testStream)) {
	try {
	  cnum = clusterer.clusterInstance(test.instance(0));
	
	  text.append(i + " " + cnum + " "
		      + attributeValuesString(test.instance(0), attributesToOutput) + "\n");
	}
	catch (Exception e) {
	  /*	  throw  new Exception('\n' + "Unable to cluster instance\n" 
		  + e.getMessage()); */
	  text.append(i + " Unclustered "
		      + attributeValuesString(test.instance(0), attributesToOutput) + "\n");
	}
	test.delete(0);
	i++;
      }
    }
    else// output for training data
      {
	for (i = 0; i < train.numInstances(); i++) {
	  try {
	    cnum = clusterer.clusterInstance(train.instance(i));
	 
	    text.append(i + " " + cnum + " "
			+ attributeValuesString(train.instance(i), attributesToOutput)
			+ "\n");
	  }
	  catch (Exception e) {
	    /*  throw  new Exception('\n' 
				 + "Unable to cluster instance\n" 
				 + e.getMessage()); */
	    text.append(i + " Unclustered "
			+ attributeValuesString(train.instance(i), attributesToOutput)
			+ "\n");
	  }
	}
      }

    return  text.toString();
  }

  /**
   * Builds a string listing the attribute values in a specified range of indices,
   * separated by commas and enclosed in brackets.
   *
   * @param instance the instance to print the values from
   * @param attributes the range of the attributes to list
   * @return a string listing values of the attributes in the range
   */
  private static String attributeValuesString(Instance instance, Range attRange) {
    StringBuffer text = new StringBuffer();
    if (attRange != null) {
      boolean firstOutput = true;
      attRange.setUpper(instance.numAttributes() - 1);
      for (int i=0; i<instance.numAttributes(); i++)
	if (attRange.isInRange(i)) {
	  if (firstOutput) text.append("(");
	  else text.append(",");
	  text.append(instance.toString(i));
	  firstOutput = false;
	}
      if (!firstOutput) text.append(")");
    }
    return text.toString();
  }

  /**
   * Make up the help string giving all the command line options
   *
   * @param clusterer the clusterer to include options for
   * @return a string detailing the valid command line options
   */
  private static String makeOptionString (Clusterer clusterer) {
    StringBuffer optionsText = new StringBuffer("");
    // General options
    optionsText.append("\n\nGeneral options:\n\n");
    optionsText.append("-t <name of training file>\n");
    optionsText.append("\tSets training file.\n");
    optionsText.append("-T <name of test file>\n");
    optionsText.append("-l <name of input file>\n");
    optionsText.append("\tSets model input file.\n");
    optionsText.append("-d <name of output file>\n");
    optionsText.append("\tSets model output file.\n");
    optionsText.append("-p <attribute range>\n");
    optionsText.append("\tOutput predictions. Predictions are for " 
		       + "training file" 
		       + "\n\tif only training file is specified," 
		       + "\n\totherwise predictions are for the test file."
		       + "\n\tThe range specifies attribute values to be output"
		       + "\n\twith the predictions. Use '-p 0' for none.\n");
    optionsText.append("-x <number of folds>\n");
    optionsText.append("\tOnly Distribution Clusterers can be cross " 
		       + "validated.\n");
    optionsText.append("-s <random number seed>\n");
    optionsText.append("-c <class index>\n");
    optionsText.append("\tSet class attribute. If supplied, class is ignored");
    optionsText.append("\n\tduring clustering but is used in a classes to");
    optionsText.append("\n\tclusters evaluation.\n");

    // Get scheme-specific options
    if (clusterer instanceof OptionHandler) {
      optionsText.append("\nOptions specific to " 
			 + clusterer.getClass().getName() + ":\n\n");
      Enumeration enu = ((OptionHandler)clusterer).listOptions();

      while (enu.hasMoreElements()) {
	Option option = (Option)enu.nextElement();
	optionsText.append(option.synopsis() + '\n');
	optionsText.append(option.description() + "\n");
      }
    }

    return  optionsText.toString();
  }


  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {
    try {
      if (args.length == 0) {
	throw  new Exception("The first argument must be the name of a " 
			     + "clusterer");
      }

      String ClustererString = args[0];
      args[0] = "";
      Clusterer newClusterer = Clusterer.forName(ClustererString, null);
      System.out.println(evaluateClusterer(newClusterer, args));
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

}

