/*
 * AnnotationConverterTipsterToMPQA.java
 * 
 * Created on March 28, 2007
 */

package reconcile.data;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * A simple converter that takes a file in the Tipster and converts it to the bytespan format
 * 
 * @author ves
 */
public class AnnotationConverterTipsterToBytespan {

/**
 * @param args
 *          the command line arguments
 */
public static void main(String[] args)
{
  try {
    InputStream inputFilename;

    inputFilename = new FileInputStream(args[0]);
    String outputFilename = args[1];
    AnnotationReader reader = new AnnotationReaderTipster();
    AnnotationSet annots = reader.read(inputFilename, "temp");
    AnnotationWriter writer = new AnnotationWriterBytespan();

    PrintWriter out = new PrintWriter(System.out);
    writer.write(annots, out);
    out.flush();

    out = new PrintWriter(new FileWriter(outputFilename));
    writer.write(annots, out);
    out.flush();

  }
  catch (ArrayIndexOutOfBoundsException aob) {
    System.out.println("Please provide input and output filenames");
    return;
  }
  catch (IOException e) {
    e.printStackTrace();
    throw new RuntimeException(e);
  }
}

}
