package reconcile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import reconcile.general.Utils;


public class ExperimentRecord {

private String outFile;
private SystemConfig sysConf;

private PrintWriter output;
private String experimentDir;

public ExperimentRecord() {

  SimpleDateFormat nameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
  Date date = new Date();
  sysConf = Utils.getConfig();

  if (sysConf.getString("RNAME") != null) {
    outFile = sysConf.getString("RNAME");
  }
  else {
    outFile = "Experiment-" + nameFormat.format(date) + ".rec";
  }

  System.out.println("experimentDir = " + sysConf.getString("RDIR"));
  experimentDir = sysConf.getString("RDIR");

  try {
    output = new PrintWriter(new File(experimentDir, outFile));
  }
  catch (IOException fnf) {
    throw new RuntimeException(fnf);
  }

  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  output.write("############ RECONCILE ############\n");
  output.write("Experiment started: " + dateFormat.format(date) + "\n");
  output.write("###################################\n");

  String classifier = sysConf.getClassifier();
  output.write("Classifier: " + classifier + "\n");
  String[] options = sysConf.getStringArray("ClOptions." + classifier);
  output.write("Learn options: " + makeString(options) + "\n");
  options = sysConf.getStringArray("TesterOptions." + classifier);
  output.write("Test options: " + makeString(options) + "\n");
  String clustererName = sysConf.getClusterer();
  output.write("Clusterer: " + clustererName + "\n");
  String[] clustOptions = sysConf.getStringArray("ClustOptions." + clustererName);
  output.write("Clusterer options: " + makeString(clustOptions) + "\n");
  output.write("Train directory: " + sysConf.getTrDir() + "\n");
  output.write("Train filenames: " + sysConf.getTrLst() + "\n");
  output.write("Test directory: " + sysConf.getTestDir() + "\n");
  output.write("Test filenames: " + sysConf.getTestLst() + "\n");
  output.write("Dataset: " + sysConf.getDataset() + "\n");

  if (sysConf.getCrossValidate()) {
    output.write("===================== Using cross validation ======================\n");
    output.write("Validator: " + sysConf.getCrossValidator() + "\n");
    output.write("Num folds: " + sysConf.getNumFolds() + "\n");
    output.write("MaximizingScore: " + sysConf.getOptimizeScorer() + "\n");
  }
  if (sysConf.getValidate()) {
    output.write("===================== Validating results ======================\n");
    output.write("Validator: " + sysConf.getValidator() + "\n");
    output.write("Num. folds: " + sysConf.getNumFolds() + "\n");
    output.write("MaximizingScore: " + sysConf.getOptimizeScorer() + "\n");
  }

  output.write("Feature set = [");

  String[] features = sysConf.getFeatureNames();
  int counter = 0;
  for (String f : features) {
    if ((counter++) % 5 == 0) {
      output.write("\n\t");
    }
    else {
      output.write(", ");
    }
    output.write(f);
  }

    output.write("\n]\n");
    output.flush();
}

public void commitRecord()
{

  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  Date date = new Date();

  output.write("\nExperiment ended: " + dateFormat.format(date) + "\n");
  output.write("\n");

  // if (test) {
  // output.write("############ RECONCILE ############\n");
  // output.write("Results: \n");
  // Scorer.scoreToFile(testNames, fs, output);
  // }

  output.flush();
  output.close();
}

public static String makeString(String[] options)
{
  StringBuilder result = new StringBuilder();
  for (String op : options) {
    result.append(op).append(" ");
  }
  return result.toString();
}

public PrintWriter getOutput()
{
  return output;
}

public void setOutput(PrintWriter output)
{
  this.output = output;
}

}
