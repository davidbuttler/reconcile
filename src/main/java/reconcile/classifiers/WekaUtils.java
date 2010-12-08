package reconcile.classifiers;

import gov.llnl.text.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import reconcile.SystemConfig;
import reconcile.general.Utils;
import reconcile.weka.core.Instances;
import reconcile.weka.filters.Filter;
import reconcile.weka.filters.unsupervised.attribute.Remove;

/**
 * Contains static utility methods for filtering attributes, reading and writing arff files, etc.
 * 
 * @author D. Hysom
 */

public class WekaUtils {

public static Set<String> getFilterAttributes() {
  SystemConfig cfg = Utils.getConfig();
  String[] exclude = cfg.getStringArray("FEATURE_NAMES_EXCLUDE");

  HashSet<String> ex = new HashSet<String>();
  ex.add("DocNo");
  ex.add("ID1");
  ex.add("ID2");
  for (String s : exclude) {
    ex.add(s);
  }

  return ex;
}
/*
 * Removes attribues that we don't want to train/test on.
 * namely, DocNo, ID1, ID2; also remove additional
 * attributes, that may be specified in the configuration file.
 */
public static Instances filterAttributes(Instances data)
    throws Exception
{

  return filterAttributes(data, getFilterAttributes());
}

/*
 * Removes attribues that we don't want to train/test on.
 * The names of the attributes to be removed are
 * contained in 'ex.'
 */
public static Instances filterAttributes(Instances data, Set<String> ex)
    throws Exception
{
  Instances newData = null;
  Remove remove = new Remove(); // new instance of filter

  int[] indices_to_remove = new int[ex.size()];

  int j = -1;
  for (String s : ex) {
    int idx = data.attribute(s).index();
    if (idx != -1) {
      ++j;
      indices_to_remove[j] = idx;
    }
    else
      throw new RuntimeException(
          "cannot remove attribute "
              + s
              + " since it doesn't exist in the arff file.  Please check your FEATURE_NAMES_EXCLUDE option, which is likely in error");
  }

  remove.setAttributeIndicesArray(indices_to_remove);
  remove.setInvertSelection(false);
  remove.setInputFormat(data); // inform filter about dataset AFTER setting sOptions
  newData = Filter.useFilter(data, remove); // apply filter

  return newData;
}

/*
* Reads in an arff file, and returns the Instances
*/
public static Instances readArffFile(InputStream fn)
    throws IOException
{
  String indata = FileUtils.readFile(fn);
//  printStartEnd(indata);
  Instances data = null;
//  data = new Instances(new BufferedReader(new InputStreamReader(fn)));
  data = new Instances(new BufferedReader(new StringReader(indata)));
  data.setClassIndex(data.numAttributes() - 1);
  return data;
}
/*
* Reads in an arff file, and returns the Instances
*/
public static Instances readArffFile(File fn)
    throws IOException
{
  String indata = FileUtils.readFile(fn);
//  printStartEnd(indata);
  Instances data = null;
//  data = new Instances(new BufferedReader(new InputStreamReader(fn)));
  data = new Instances(new BufferedReader(new StringReader(indata)));
  data.setClassIndex(data.numAttributes() - 1);
  return data;
}

//private static void printStartEnd(String indata)
//{
//  int count = 0;
//  Ring<String> r = new Ring<String>(10);
//  for (String line: StringIterable.iterate(indata)) {
//    if (count++ < 10) {
//      System.out.println(line);
//    }
//    r.add(line);
//  }
//  for (String line:r) {
//    System.out.println(line);
//  }
//}

/*
* writes an arff file to disk
*/
public static void writeArffFile(File fn, Instances data)
    throws IOException
{
  FileUtils.write(fn, data.toString());
}

}
