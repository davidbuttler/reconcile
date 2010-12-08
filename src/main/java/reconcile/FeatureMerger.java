package reconcile;

import gov.llnl.text.util.FileUtils;
import gov.llnl.text.util.InputStreamLineIterable;
import gov.llnl.text.util.LineIterator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import reconcile.data.Document;
import reconcile.general.Utils;
import reconcile.util.Doc2InputStreamIterable;
import reconcile.util.FeatureFileExtractor;
import reconcile.util.String2DocIterable;

public class FeatureMerger {

public static final Pattern pData = Pattern.compile(".*\\@DATA.*");


/**
 * 
 * @param fileNames
 *          - the list of directories containing feature files
 * @param outputFileName
 *          - a string representing where to write the merged feature vector file
 * @throws IOException
 */
// public static void combine(OutputStream output, Iterable<Document> fileNames)
// {
// String featureSetName = Utils.getConfig().getFeatSetName();
// combine(output, fileNames, featureSetName, "arff");
// }

/**
 * 
 * @param fileNames
 *          - the list of directories containing feature files
 * @param outputFileName
 *          - a string representing where to write the merged feature vector file
 * @throws IOException
 */
public static void combine(OutputStream output, Iterable<Document> corpus)
{
  String featureSetName = Utils.getConfig().getFeatSetName();
  combine(output, corpus, featureSetName, "arff");
}

/**
 * 
 * @param fileNames
 *          - the list of directories containing feature files
 * @param outputFileName
 *          - a string representing where to write the merged feature vector file
 * @throws IOException
 */
public static void combine(OutputStream output, String[] fileNames)
{
  String featureSetName = Utils.getConfig().getFeatSetName();
  combine(output, new String2DocIterable(Arrays.asList(fileNames)), featureSetName, "arff");
}

/**
 * 
 * @param fileNames
 *          - the list of directories containing feature files
 * @param outputFileName
 *          - a string representing where to write the merged feature vector file
 * @throws IOException
 */
public static void combine(OutputStream output, Iterable<Document> fileNames, String featSetName)
{
  combine(output, fileNames, featSetName, "arff");
}

/**
 * 
 * @param fileNames
 *          - the list of documents containing feature files in the appropriate place
 * @param outputFileName
 *          - a string representing where to write the merged feature vector file
 * @throws IOException
 */
public static void combine(OutputStream output, Iterable<Document> fileNames, String featSetName, String featFormat)
{
  Iterable<InputStream> fi = new Doc2InputStreamIterable(fileNames, new FeatureFileExtractor());
  combine(output, fi.iterator(), featSetName, featFormat);
}

/**
 * 
 * @param fileNames
 *          - the list of feature files
 * @param outputFileName
 *          - a string representing where to write the merged feature vector file
 * @throws IOException
 */
public static void combine(OutputStream output, Iterator<InputStream> fileNames, String featSetName, String featFormat)
{
  try {
    PrintWriter out = new PrintWriter(output);

    System.out.println("merging...");
    boolean first = true;
    while (fileNames.hasNext()) {
      InputStream f = fileNames.next();
            
      if (first) {
        // write everything
        FileUtils.write(out, new InputStreamReader(f));
        out.flush();
      }
      else {
        // only append the data
        boolean skip = true;

        for (String line : InputStreamLineIterable.iterate(f)) {
          //System.out.println(line);

          if (!skip && !line.trim().equals("")) {
            out.println(line);
          }
          else if (pData.matcher(line).find()) {
            skip = false;
          }
        }
        out.flush();
      }
      first = false;
    }
    // System.out.println();
    out.flush();
    out.close();
    System.out.println("finished merging");
  }
  catch (IOException e) {
    throw new RuntimeException(e);
  }
}

static class FeatureDataLineIterator
    implements Iterator<String> {

private LineIterator mLineIterator;

public FeatureDataLineIterator(File f)
    throws IOException {
  mLineIterator = new LineIterator(f);

  while (mLineIterator.hasNext()) {
    String line = mLineIterator.next();
    if (pData.matcher(line).find()) {
      break;
    }
  }
}

/* (non-Javadoc)
 * @see java.util.Iterator#hasNext()
 */
public boolean hasNext()
{
  return mLineIterator.hasNext();
}

/* (non-Javadoc)
 * @see java.util.Iterator#next()
 */
public String next()
{
  return mLineIterator.next();
}

/* (non-Javadoc)
 * @see java.util.Iterator#remove()
 */
public void remove()
{
  mLineIterator.remove();

}

}


}
