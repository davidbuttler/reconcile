package reconcile.classifiers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import reconcile.SystemConfig;
import reconcile.general.Utils;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.core.Attribute;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;

/**
 * @author D. Hysom
 * 
 */
public class GeneralizedWekaClassifier
    extends Classifier {

private static reconcile.weka.classifiers.Classifier mClassifier = null;

private static WekaAttributeSelection mSelector = new WekaAttributeSelection();

private static Evaluation mEval = null;

/*
 * testFilename = genFeatures file
 * outputFilename = predictions
 * modelInputFilename = the j48 model file (binary object)
 * 
 * (non-Javadoc)
 * @see Classifiers.Classifier#test(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
 */
@Override
public double[] test(File testFilename, File outputFilename, String[] options)
{
	throw new RuntimeException("Not implemented");//return test(testFilename, outputFilename, null, options);
}

@Override
public double[] test(File testFilename, File outputFilename, String model, String[] options)
{
  if (model != null) {
    readModel(model, options);
  }
  else if (mClassifier == null) {
    readModel(mModelFile, options);
  }
  double[] results = null;

  Instances data = null;

  try {
    data = WekaUtils.readArffFile(testFilename);
    if (data.numInstances() <= 0) return null;

    Instances mData = WekaUtils.filterAttributes(data);

    SystemConfig cfg = Utils.getConfig();

    if (cfg.getBoolean("FEATURE_SELECTION", false)) {
      mSelector.filterInstances(mData);
    }

    mEval = new Evaluation(mData);

    results = mEval.evaluateModel(mClassifier, mData);

    // for (double b : results) System.out.print(b + "   ");

    double max = Double.MIN_VALUE, min = Double.MAX_VALUE;

    PrintWriter out = new PrintWriter(outputFilename);

    Attribute docID = data.attribute("DocNo");
    Attribute id1 = data.attribute("ID1");
    Attribute id2 = data.attribute("ID2");
    // Attribute c1 = data.attribute("Category1");
    // Attribute c2 = data.attribute("Category2");

    for (int j = 0; j < data.numInstances(); j++) {
      Instance cur = data.instance(j);
      int curDoc = (int) cur.value(docID);
      int curID1 = (int) cur.value(id1);
      int curID2 = (int) cur.value(id2);
      // String cat1 = cur.stringValue(c1);
      // String cat2 = cur.stringValue(c2);

      // it seems that the decision labels coreferent things as 0.0 and non-coreferent as 1.0
      // so for the threshold clustering to work we need to do this.
      min = min < results[j] ? min : results[j];
      max = max > results[j] ? max : results[j];
      String r = (results[j] == 1.0) ? "0.0" : "1.0";

      // System.out.println(cat1 + " " + cat2);

      /*
              if (cat2.equals("PRO") && cat1.equals("NAM")) {
                System.out.println("flip");
                r = "0.0";
              }
              */

      out.println(curDoc + "," + curID1 + "," + curID2 + " " + r);
    }

    out.flush();
    out.close();

  }
  catch (Exception ex) {
    throw new RuntimeException(ex);
  }

  return results;
}

private void readModel(String modelInputFilename, String[] options)
{
  try {
    if (mClassifier == null) {
      System.out.println(modelInputFilename);
      InputStream is = new FileInputStream(modelInputFilename);
      System.out.println("1");
      ObjectInputStream objectInputStream = new ObjectInputStream(is);
      Object o = objectInputStream.readObject();

      mClassifier = (reconcile.weka.classifiers.Classifier) o;
      objectInputStream.close();
    }
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

@Override
public void train(File trainFilename, String[] options)
{
	train(trainFilename, null, options);
}

@Override
public void train(File trainFilename, File model, String[] options)
{

  try {
    String classifierName = options[0];
    options[0] = "";

    mClassifier = reconcile.weka.classifiers.Classifier.forName(classifierName, options);

    Instances data = WekaUtils.readArffFile(trainFilename);
    Instances mData = WekaUtils.filterAttributes(data);

    SystemConfig cfg = Utils.getConfig();
    if (cfg.getBoolean("FEATURE_SELECTION", false)) {
      mSelector.selectAttributes(data);
      mSelector.filterInstances(mData);
    }

    mClassifier.buildClassifier(mData);
    if (model == null) {
      System.out.println("saving to: " + mModelFile);
      saveModel(mModelFile);
    }
    else {
      System.out.println("saving to: " + model.getAbsolutePath());
      saveModel(model.getAbsolutePath());
    }
  }
  catch (Exception e) {
    e.printStackTrace();
    throw new RuntimeException();
  }
}

private void saveModel(String fn)
    throws Exception
{
  OutputStream os = new FileOutputStream(fn);
  ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);

  objectOutputStream.writeObject(mClassifier);
  objectOutputStream.flush();
  objectOutputStream.close();
}

}
