package reconcile.classifiers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

import reconcile.Driver;
import reconcile.weka.classifiers.functions.WeightVectorApplier;
import reconcile.weka.core.AttributeShort;
import reconcile.weka.core.InstanceShort;
import reconcile.weka.core.ModifiedInstancesShort;


/**
 * @author ves
 * 
 */
public class PerceptronM
    extends Classifier {
public static double SCALE_A = -10;
public static double SCALE_B = 0;
private static double[] weightVector;
private static String cachedModelName = "no file";
private static long modelTimestamp = 0;

private double[] loadClassifier(String modelInputFile, int numAtts)
{
  if (modelInputFile == null) modelInputFile = mModelFile;
  if (!mModelFile.equals(modelInputFile)) {
	  readClassifier(modelInputFile, numAtts);
  }
  else {
    readClassifier(modelInputFile, numAtts);
  }
  return weightVector;
}

private void readClassifier(String modelInputFile, int numAtts)
{
  System.out.println("Reading classifier from file " + modelInputFile);
  cachedModelName = modelInputFile;
  modelTimestamp = System.currentTimeMillis();
  weightVector = WeightVectorApplier.readWeightVector(modelInputFile, numAtts, true);
}

@Override
public double[] test(File testFile, File outputFile, String modelInputFile, String[] options)
{
	try {
		return test(new FileReader(testFile),new FileWriter(outputFile), modelInputFile, options);
	} catch (FileNotFoundException e) {
		throw new RuntimeException(e);
	} catch (IOException e) {
		throw new RuntimeException(e);
	}
}
@Override
public double[] test(Reader testFile, Writer outputFile, String[] options)
{
	return test(testFile, outputFile, null, options);
}
public double[] test(Reader testFile, Writer outputFile, String modelInputFile, String[] options)
{
  double min = Double.MAX_VALUE;
  double max = Double.MIN_VALUE;
  ModifiedInstancesShort insts;
  PrintWriter out = null;
  ;
  // System.out.println("Testing "+testFilename+" -- "+outputFilename);
  try {
    insts = new ModifiedInstancesShort(new BufferedReader(testFile));
    insts.setClass(insts.attribute("class"));
    if (insts.numInstances() > 0)
 insts.cleanUpValuesAndSetWeight(0);
    out = new PrintWriter(outputFile);
    if (insts.numInstances() <= 0) return null;

  double[] w = loadClassifier(modelInputFile, insts.numAttributes());
  double[] res = WeightVectorApplier.getDistance(w, insts);

  AttributeShort docID = insts.attribute("DocNo");
  AttributeShort id1 = insts.attribute("ID1");
  AttributeShort id2 = insts.attribute("ID2");

  for (int i = 0; i < insts.numInstances(); i++) {
    InstanceShort cur = insts.instance(i);
    short curDoc = cur.value(docID);
    short curID1 = cur.value(id1);
    short curID2 = cur.value(id2);
    double value = plattScale(res[i], SCALE_A, SCALE_B);
    min = min < value ? min : value;
    max = max > value ? max : value;
    out.println(curDoc + "," + curID1 + "," + curID2 + " " + value);
  }
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
  finally {
    if (out != null) {
      out.flush();
      out.close();
    }
  }
  double[] result = new double[] { min, max };
  return result;
}

@Override
public void train(File trainFilename, File modelOutputFilename, String[] options)
{
  String[] additionalOpts = { "-t", trainFilename.getAbsolutePath(), };
  // "-T", Utils.getWorkDirectory() + Utils.SEPARATOR + Utils.getConfig().getDataset() + "tst.features.arff",
  if (modelOutputFilename != null) {
    String[] modelOpts = { "-F", modelOutputFilename.getAbsolutePath() };
    additionalOpts = Driver.joinArrays(additionalOpts, modelOpts);
  }
  String[] allOpts = Driver.joinArrays(additionalOpts, options);
  reconcile.weka.classifiers.functions.PerceptronMargin.main(allOpts);
}


public double getVector2Norm()
{
  return 0;
}

/* (non-Javadoc)
 * @see reconcile.classifiers.Classifier#test(java.io.File, java.io.File, java.lang.String[])
 */
@Override
public double[] test(File testFile, File outputFile, String[] options)
{
  return test(testFile, outputFile, null, options);
}

/* (non-Javadoc)
 * @see reconcile.classifiers.Classifier#train(java.io.File, java.lang.String[])
 */
@Override
public void train(File trainFile, String[] options)
{
  train(trainFile, null, options);
}

}
