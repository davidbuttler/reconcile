/**
 * FeatureExtractor.java
 */
package reconcile.featureExtractor;

import java.io.BufferedReader;
import java.io.IOException;

import reconcile.data.Document;
import reconcile.general.Utils;


/**
 * @author ves
 * 
 *         An abstract class defining the interface and some behaviour of annotators that use external programs
 */
public abstract class ExternalAnnotator
    extends Annotator {

public abstract String getApplicationName();

/**
 * Runs the external annotator. This will generate an annotation file.
 * 
 * @param dirName
 *          The location in which the document is stored
 * @param annSetName
 *          The names of the annotation sets that this component produces
 */
@Override
public abstract void run(Document doc, String[] annSetName);

public abstract String runOptions();

public abstract void preprocess()
    throws IOException;

public void runApplication()
{
  try {
    preprocess();
    String command = getApplicationName() + " " + runOptions();
    System.err.println("Running " + command);
    Utils.runExternal(command);
    // Run the command
    // Process p = Runtime.getRuntime().exec(command);
    //
    // // Get the outputs of the program
    // BufferedReader stdInput = new BufferedReader(new InputStreamReader(
    // p.getInputStream()));
    //
    // BufferedReader stdError = new BufferedReader(new InputStreamReader(
    // p.getErrorStream()));
    //
    // // read and process the output if necessary
    // processOutput(stdInput);
    // // read and process errors from the attempted command
    // processError(stdError);

    postprocess();

  }
  catch (Exception e) {
    throw new RuntimeException(e);
  }

}

public abstract void postprocess()
    throws IOException;

protected void processOutput(BufferedReader stdInput)
    throws IOException
{
  String line;
  while ((line = stdInput.readLine()) != null) {
    System.out.println(line);
  }
}

protected void processError(BufferedReader stdError)
    throws IOException
{
  String line;
  while ((line = stdError.readLine()) != null) {
    System.err.println(line);
  }
}

}
