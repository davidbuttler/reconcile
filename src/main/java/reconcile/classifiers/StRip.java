package reconcile.classifiers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import reconcile.Driver;
import reconcile.general.Utils;
import reconcile.weka.classifiers.rules.StRipShort;
import reconcile.weka.core.AttributeShort;
import reconcile.weka.core.InstanceShort;
import reconcile.weka.core.ModifiedInstancesShort;


/**
 * @author ves
 * 
 */
public class StRip
    extends Classifier {

@Override
public double[] test(File testFilename, File outputFilename, String modelInputFilename, String[] options)
{
  double min = Double.MAX_VALUE;
  double max = Double.MIN_VALUE;
  ModifiedInstancesShort insts;
  PrintWriter out = null;
  // System.out.println("Testing "+testFilename+" -- "+outputFilename);
  try {
    insts = new ModifiedInstancesShort(new BufferedReader(new FileReader(testFilename)));
    insts.setClass(insts.attribute("class"));
    insts.cleanUpValuesAndSetWeight(0);
    out = new PrintWriter(outputFilename);
    if (insts.numInstances() <= 0) return null;
    StRipShort classifier = StRipShort.readClassifier(modelInputFilename, insts);
    // System.out.println(classifier.toString());
    AttributeShort docID = insts.attribute("DOCNUM");
    AttributeShort id1 = insts.attribute("ID1");
    AttributeShort id2 = insts.attribute("ID2");
    for (int i = 0; i < insts.numInstances(); i++) {
      InstanceShort cur = insts.instance(i);
      short curDoc = cur.value(docID);
      short curID1 = cur.value(id1);
      short curID2 = cur.value(id2);
      double res = classifier.classifyInstance(cur);
      min = min < res ? min : res;
      max = max > res ? max : res;
      out.println(curDoc + "," + curID1 + "," + curID2 + " " + res);
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
  String[] additionalOpts = { "-t", trainFilename.getAbsolutePath(), "-T",
      Utils.getWorkDirectory() + Utils.SEPARATOR + Utils.getConfig().getDataset() + "tst.features.arff", };
  if (modelOutputFilename != null) {
    String[] modelOpts = { "-M", modelOutputFilename.getAbsolutePath() };
    additionalOpts = Driver.joinArrays(additionalOpts, modelOpts);
  }
  String[] allOpts = Driver.joinArrays(additionalOpts, options);
  reconcile.weka.classifiers.rules.StRipShort.main(allOpts);
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
