package reconcile.classifiers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reconcile.general.Utils;


/**
 * @author David Golland
 * 
 */
public class SVMLight
    extends Classifier {

/**
 * @param options
 *          is a string array containing the strings specified below in the following order: 0) directory containing
 *          svm_classify module 1) any options to pass to the classifier (may be omitted)
 */
@Override
public double[] test(File testFile, File outputFile, String modelInputFile, String[] options)
{
  String testFilename = testFile.getAbsolutePath();
  String outputFilename = outputFile.getAbsolutePath();
  String modelInputFilename = modelInputFile;
  try {

    /** format the test file so that SVMlight can handle it **/
    String ScriptDir = Utils.getScriptDirectory() + Utils.SEPARATOR;
    String formattedTestFeatures = testFilename + ".svm";

    String svmFormatScript = ScriptDir + "svm_compatabalize.pl";

    System.out.println("SVM - Formatting test file: " + testFilename);
    Utils.runExternal("perl " + svmFormatScript + " " + testFilename + " " + formattedTestFeatures);

    /** run the SVMlight classifier **/
    String externalClassifierPath = options[0];
    String opts = options.length > 2 ? options[2] : "";

    System.out.println(outputFilename);

    Utils.runExternal(externalClassifierPath + Utils.SEPARATOR + "svm_classify " + opts + " " + formattedTestFeatures + " "
        + modelInputFilename + " " + outputFilename + ".svm");

    /** format the output of the SVMlight classifier **/
    // String svmOutputFormatScript = ScriptDir + "svm_formatOutput.pl";

    // Utils.runExternal("perl " + svmOutputFormatScript + " " + formattedTestFeatures + " " + outputFilename + ".svm" +
    // " " + outputFilename);
    // double[] result = new double[]{0, 1};
    return formatSVMOutput(formattedTestFeatures, outputFilename + ".svm", outputFilename);
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

/**
 * @param options
 *          is a string array containing the strings specified below in the following order: 0) directory containing
 *          svm_train module 1) any options to pass to the classifier (may be omitted)
 */
@Override
public void train(File trainFile, File modelOutputFile, String[] options)
{
  String trainFilename = trainFile.getAbsolutePath();
  String modelOutputFilename = modelOutputFile.getAbsolutePath();

  try {
    if (options.length < 2)
      throw new RuntimeException("SVMLight: string[] passed to train incorrect length (got: " + options.length
          + "; needed: 2)");

    /** format the train file so that SVMlight can handle it **/
    String ScriptDir = Utils.getScriptDirectory() + Utils.SEPARATOR;
    String formattedTrainFeatures = trainFilename + ".svm";

    String svmFormatScript = ScriptDir + "svm_compatabalize.pl";

    System.out.println("SVM - Formatting train file: " + trainFilename);
    Utils.runExternal("perl " + svmFormatScript + " " + trainFilename + " " + formattedTrainFeatures);

    /** run the SVMlight learner **/
    String externalClassifierPath = options[0];
    String opts = options.length > 1 ? options[2] : "";

    /*
    for(String s : options) {
    	System.out.println(s);	
    }
    */

    Utils.runExternal(externalClassifierPath + Utils.SEPARATOR + "svm_learn " + opts + " " + formattedTrainFeatures
        + " " + modelOutputFilename);
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

private static double[] formatSVMOutput(String testFeatures, String predictions, String outputFilename)
{
  double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
  Pattern p = Pattern.compile(".*# (.*)$");
  BufferedReader testFile = null;
  BufferedReader predFile = null;
  ;
  BufferedWriter out = null;
  try {
    testFile = new BufferedReader(new FileReader(testFeatures));
    predFile = new BufferedReader(new FileReader(predictions));
    out = new BufferedWriter(new FileWriter(outputFilename));

    String lineTest, linePred;
    while (((lineTest = testFile.readLine()) != null) && ((linePred = predFile.readLine()) != null)) {
      lineTest = lineTest.trim();
      linePred = linePred.trim();

      double value = Double.parseDouble(linePred);
      min = value < min ? value : min;
      max = value > max ? value : max;
      // combine the lines
      Matcher m = p.matcher(lineTest);
      if (m.matches()) {
        out.write(m.group(1) + " " + linePred + "\n");
      }
    }
    out.flush();
    out.close();
  }
  catch (IOException ioe) {
    throw new RuntimeException(ioe);
  }
  finally {
    try {
      if (testFile != null) testFile.close();
      if (predFile != null) predFile.close();
      if (out != null) {
        out.close();
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  return new double[] { min, max };
}

/* (non-Javadoc)
 * @see reconcile.classifiers.Classifier#test(java.io.File, java.io.File, java.lang.String[])
 */
@Override
public double[] test(File testFile, File outputFile, String[] options)
{
  throw new RuntimeException("method not supported: model file is needed");
}


/* (non-Javadoc)
 * @see reconcile.classifiers.Classifier#train(java.io.File, java.lang.String[])
 */
@Override
public void train(File trainFile, String[] options)
{
  throw new RuntimeException("method not supported: model file is needed");
}


}
