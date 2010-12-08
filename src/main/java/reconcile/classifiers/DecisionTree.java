package reconcile.classifiers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.classifiers.trees.J48;
import reconcile.weka.core.Attribute;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.unsupervised.attribute.Remove;


/**
 * @author Nathan Gilbert
 * 
 */
public class DecisionTree extends Classifier {

private J48 dt = null;
private Evaluation Eval = null;

/*
* Reads in an arff file, and returns the Instances
*/
public static Instances readArffFile(File testFilename)
    throws IOException
{
  Instances data = null;
  data = new Instances(new BufferedReader(new FileReader(testFilename)));
  data.setClassIndex(data.numAttributes() - 1);
  return data;
}

/**
 * read a previously saved model from file
 * 
 * @return
 */
public static J48 readModel(String fn)
    throws IOException
{
  try {
    InputStream is = new FileInputStream(fn);
    ObjectInputStream objectInputStream = new ObjectInputStream(is);

    Object o = objectInputStream.readObject();

    J48 dt = (J48) o;
    objectInputStream.close();
    return dt;
  }
  catch (ClassNotFoundException e) {
    e.printStackTrace();
    throw new IOException(e);
  }
}

public static void saveModel(J48 model, File fn)
    throws IOException
{
  OutputStream os = new FileOutputStream(fn);
  ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);

  objectOutputStream.writeObject(model);
  objectOutputStream.flush();
  objectOutputStream.close();

  /* Write a human readable version. */
  FileWriter fw = new FileWriter(fn + "-human");
  fw.write(model.toString());
  fw.close();
}

public DecisionTree(String model) throws IOException {
  dt = readModel(model);
}

public DecisionTree() {
}

private double[] evaluate(Instances test_set) throws Exception
{
  double[] result = null;
  if (dt == null) throw new Exception("dt is null");
  Eval = new Evaluation(test_set);
  result = Eval.evaluateModel(dt, test_set);
  return result;
}

/*
 * testFilename = genFeatures file
 * outputFilename = predictions
 * modelInputFilename = the j48 model file (binary object)
 * 
 * (non-Javadoc)
 * @see Classifiers.Classifier#test(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
 */
@Override
public double[] test(File testFilename, File outputFilename, String modelInputFilename, String[] options)
{

  PrintWriter out;
  double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
  if (dt == null) {
    try {
      dt = readModel(modelInputFilename);
		System.out.println(modelInputFilename);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  Instances testData = null;
  Instances instances = null;
  double[] results;

  try {
    testData = readArffFile(testFilename);
    out = null;
    try {
    out = new PrintWriter(outputFilename);
    if (testData.numInstances() <= 0) return null;

    instances = filterAttributes(testData);
    results = evaluate(instances);

    Attribute docID = testData.attribute("DocNo");
    Attribute id1 = testData.attribute("ID1");
    Attribute id2 = testData.attribute("ID2");

    for (int j = 0; j < testData.numInstances(); j++) {
      Instance cur = testData.instance(j);
      int curDoc = (int) cur.value(docID);
      int curID1 = (int) cur.value(id1);
      int curID2 = (int) cur.value(id2);

      // it seems that the decision labels coreferent things as 0.0 and non-coreferent as 1.0
      // so for the threshold clustering to work we need to do this.
      min = min < results[j] ? min : results[j];
      max = max > results[j] ? max : results[j];
      String r = (results[j] == 1.0) ? "0.0" : "1.0";
      out.println(curDoc + "," + curID1 + "," + curID2 + " " + r);
    }
    }
    finally {
      out.flush();
      out.close();
    }
  }
  catch (Exception ex) {
    ex.printStackTrace();
  }
  double[] result = new double[] { min, max };
  return result;
}

/*
 * Removes attribues that we don't want to train/test on.
 * namely, DocNo, ID1, ID2	  
 */
public static Instances filterAttributes(Instances data)
    throws Exception
{
  Remove remove = new Remove(); // new instance of filter
  int[] indices_to_remove = new int[3];

  indices_to_remove[0] = data.attributeIndex("DocNo");
  indices_to_remove[1] = data.attributeIndex("ID1");
  indices_to_remove[2] = data.attributeIndex("ID2");

  remove.setAttributeIndicesArray(indices_to_remove);
  remove.setInvertSelection(false);
  remove.setInputFormat(data); // inform filter about dataset AFTER setting sOptions
  Instances newData = Filter.useFilter(data, remove); // apply filter

  return newData;
}

@Override
public void train(File trainFilename, File modelOutputFilename, String[] options)
{

  dt = new J48();

  // set classifier options
  try {
    dt.setOptions(options);
  }
  catch (Exception ex) {
    ex.printStackTrace();
  }

  // read in arff file
  Instances data = null;
  Instances mData = null;

  try {
    data = readArffFile(trainFilename);
    mData = filterAttributes(data);

  // train
  // remove docno, id1, id2

    // dt.buildClassifier(data);
    dt.buildClassifier(mData);

    // save model
    saveModel(dt, modelOutputFilename);
  }
  catch (Exception ex) {
    ex.printStackTrace();
    throw new RuntimeException(ex);
  }
}

/* (non-Javadoc)
 * @see reconcile.classifiers.Classifier#test(java.io.InputStream, java.io.OutputStream, java.lang.String[])
 */
@Override
public double[] test(File testFile, File outputFile, String[] options)
{
	return test(testFile, outputFile, null, options);
}

/* (non-Javadoc)
 * @see reconcile.classifiers.Classifier#train(java.io.InputStream, java.lang.String[])
 */
@Override
public void train(File trainFile, String[] options)
{
	train(trainFile, null, options);

}
}
