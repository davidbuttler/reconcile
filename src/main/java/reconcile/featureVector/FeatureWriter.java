package reconcile.featureVector;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

public abstract class FeatureWriter {

protected List<Feature> featureList;
protected PrintWriter output;
private FileWriter fileWriter;
private BufferedWriter bufferedWriter;

public FeatureWriter(List<Feature> ftrs, String filename) {
  try {
    fileWriter = new FileWriter(filename);
    bufferedWriter = new BufferedWriter(fileWriter);
    output = new PrintWriter(bufferedWriter, false);
  }
  catch (FileNotFoundException fnf) {
    throw new RuntimeException(fnf);
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }

  featureList = ftrs;
}

public FeatureWriter(List<Feature> ftrs) {
  output = null;
  featureList = ftrs;
}

public FeatureWriter(List<Feature> ftrs, Writer outputStream) {
  bufferedWriter = new BufferedWriter(outputStream);
  output = new PrintWriter(bufferedWriter, false);
  featureList = ftrs;
}

public FeatureWriter() {
  throw new RuntimeException("Must provide a list of the features and output file or stream.");
}

public void close()
{
  try {
    output.flush();
    bufferedWriter.flush();
    if (fileWriter != null) {
      fileWriter.flush();
    }

    output.close();
    bufferedWriter.close();
    if (fileWriter != null) {
      fileWriter.close();
    }
  }
  catch (IOException e) {
    e.printStackTrace();
  }
}

public abstract void printHeader();

public abstract void printInstanceVector(HashMap<Feature, String> vals);
}
