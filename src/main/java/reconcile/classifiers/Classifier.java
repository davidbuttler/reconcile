/**
 * 
 * @author David Golland
 * 
 */

package reconcile.classifiers;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;

import reconcile.data.Document;
import reconcile.general.Utils;

public abstract class Classifier {



public double plattScale(double prediction, double A, double B)
{
  return 1.0 / (1.0 + Math.exp(A * prediction + B));
}

protected String[] mOptions;
protected String mModelFile;

/**
 * If you want to use the same options every time you call this object, just set them once. If you don't set them, they
 * will be loaded from the config file
 * 
 * @param options
 */
public void setOptions(String[] options)
{
  mOptions = options;
}

public String[] getOptions()
{
  return mOptions;
}

/**
 * get the name of this class
 * 
 * @return
 */
public String getName()
{
  return this.getClass().getName();
}

public void setModelFile(String modelFile)
{
  mModelFile = modelFile;
}

/**
 * Trains a classifier using the feature files housed in the training directories in the Config.
 * 
 * @param options
 *          - a string array of various options used in training (e.g. - where to save the model file, training
 *          parameters, etc.)
 */
public void train(File trainFile)
{
  if (mOptions == null) {
    mOptions = Utils.getConfig().getStringArray("ClOptions." + getName());
  }
  train(trainFile, mOptions);
}

/**
 * Trains a classifier using the feature files housed in the training directories in the Config.
 * 
 * @param options
 *          - a string array of various options used in training (e.g. - where to save the model file, training
 *          parameters, etc.)
 */
public void train(File trainFile, File outputModelFile)
{
  if (mOptions == null) {
    mOptions = Utils.getConfig().getStringArray("ClOptions." + getName());
  }
  train(trainFile, outputModelFile, mOptions);
}

/**
 * Trains a classifier using the feature files housed in the training directories in the Config.
 * 
 * @param options
 *          - a string array of various options used in training (e.g. - where to save the model file, training
 *          parameters, etc.)
 */
public abstract void train(File trainFile, String[] options);

/**
 * Trains a classifier using the feature files housed in the training directories in the Config.
 * 
 * @param options
 *          - a string array of various options used in training (e.g. - where to save the model file, training
 *          parameters, etc.)
 */
public abstract void train(File trainFile, File model, String[] options);

/**
 * Classifies the instances located in the test directories in the Config. Uses the model specifided in classifier
 * creation for the testing
 * 
 * @param options
 *          - a string array of various options used in testing (e.g. - where to load the model file, testing
 *          parameters, etc.)
 * @return the minimum and maximum numerical values of the classified instances
 */
public abstract double[] test(File testFile, File outputFile, String[] options);

/**
 * Classifies instances using a Reader. This way instances can be kept in memory.
 * 
 * @param options
 *          - a string array of various options used in testing (e.g. - where to load the model file, testing
 *          parameters, etc.)
 * @return the minimum and maximum numerical values of the classified instances
 */
public double[] test(Reader testFile, Writer outputFile, String[] options){
	throw new RuntimeException("Not implemented yet");
}

/**
 * Classifies the instances located in the test directories in the Config. Uses the model specifided in classifier
 * creation for the testing
 * 
 * @param options
 *          - a string array of various options used in testing (e.g. - where to load the model file, testing
 *          parameters, etc.)
 * @return the minimum and maximum numerical values of the classified instances
 */
public abstract double[] test(File testFile, File outputFile, String model, String[] options);

/**
 * Classifies the instances located in the test directories in the Config.
 * 
 * @param options
 *          - a string array of various options used in testing (e.g. - where to load the model file, testing
 *          parameters, etc.)
 * @return the minimum and maximum numerical values of the classified instances
 */
public double[] test(File testFile, File outputFile)
{
  if (mOptions == null) {
    mOptions = Utils.getConfig().getStringArray("ClOptions." + getName());
  }
  return test(testFile, outputFile, mOptions);

}

/**
 * Classifies the instances located in the test directories in the Config.
 * 
 * @param options
 *          - a string array of various options used in testing (e.g. - where to load the model file, testing
 *          parameters, etc.)
 * @return the minimum and maximum numerical values of the classified instances
 */
public double[] test(Reader testFile, Writer outputFile)
{
  if (mOptions == null) {
    mOptions = Utils.getConfig().getStringArray("ClOptions." + getName());
  }
  return test(testFile, outputFile, mOptions);

}
/**
 * Classifies the instances located in default feature file for the document. Uses the default classifier options
 * specified in the config file
 * 
 * @param doc
 * @return the minimum and maximum numerical values of the classified instances
 */
public double[] test(Document doc)
{
  double[] score = test(doc.getFeatureFile(), doc.getPredictionFile());

  return score;

}

/**
 * Classifies the instances located in default feature file for the document. Uses the default classifier options
 * specified in the config file
 * 
 * @param doc
 * @return the minimum and maximum numerical values of the classified instances
 */
public double[] test(Document doc, String[] options)
{
  double[] score = test(doc.getFeatureFile(), doc.getPredictionFile(), options);
  return score;

}

public String getInfo(String[] options)
{
  String result = "Classifier " + getClass().getSimpleName() + ".";

  result += " Options " + Arrays.toString(options);
  return result;
}

}
