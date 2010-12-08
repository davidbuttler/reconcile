package reconcile.classifiers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import reconcile.weka.core.Attribute;
import reconcile.weka.core.AttributeShort;
import reconcile.weka.core.Instance;
import reconcile.weka.core.InstanceShort;
import reconcile.weka.core.Instances;
import reconcile.weka.core.ModifiedInstancesShort;


/**
 * @author ves
 * 
 */
public class AlwaysNegative
    extends Classifier {

/**
 */
@Override
public double[] test(File testFilename, File outputFilename, String model, String[] options)
{
  return test(testFilename, outputFilename, options);
}
/**
 */
@Override
public double[] test(File testFilename, File outputFilename, String[] options)
{
  try {
    Instances insts = new Instances(new BufferedReader(new FileReader(testFilename)));

    PrintWriter out = null;
    try {
      out = new PrintWriter(outputFilename);
      if (insts.numInstances() <= 0) return null;

      Attribute docID = insts.attribute("DocNo");
      Attribute id1 = insts.attribute("ID1");
      Attribute id2 = insts.attribute("ID2");
      for (int i = 0; i < insts.numInstances(); i++) {
        Instance cur = insts.instance(i);
        double curDoc = cur.value(docID);
        double curID1 = cur.value(id1);
        double curID2 = cur.value(id2);
        out.println(curDoc + "," + curID1 + "," + curID2 + " -1");

      }
    }
    finally {
      out.flush();
      out.close();
    }
    double[] result = new double[] { -1, 1 };
    return result;
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

/**
 * @param options
 *          is a string array containing the strings specified below in the following order: 0) directory containing
 *          svm_train module 1) complete filename where to save the learned model file 2) any options to pass to the
 *          classifier (may be omitted)
 */
@Override
public void train(File trainFilename, String[] options)
{
  // No training for this classifier
}

@Override
public void train(File trainFilename, File model, String[] options)
{
  // No training for this classifier
}

/**
	 */

public double[] test(String testFilename, String outputFilename, String modelInputFilename, String[] options)
{
  try {
    ModifiedInstancesShort insts = new ModifiedInstancesShort(new BufferedReader(new FileReader(testFilename)));

    System.out.println("Working on " + testFilename + " -- " + outputFilename + " : " + insts.numInstances());
    PrintWriter out = null;
    try {
      out = new PrintWriter(outputFilename);

      if (insts.numInstances() <= 0) return null;

      AttributeShort docID = insts.attribute("DocNo");
      AttributeShort id1 = insts.attribute("ID1");
      AttributeShort id2 = insts.attribute("ID2");
      for (int i = 0; i < insts.numInstances(); i++) {
        InstanceShort cur = insts.instance(i);
        short curDoc = cur.value(docID);
        short curID1 = cur.value(id1);
        short curID2 = cur.value(id2);
        out.println(curDoc + "," + curID1 + "," + curID2 + " -1");

      }
    }
    finally {
      out.flush();
      out.close();
    }
    double[] result = new double[] { -1, 1 };
    return result;
  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }
}

}
