package reconcile.featureVector;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FeatureWriterARFFBinarized
    extends FeatureWriter {

private HashMap<Feature, ArrayList<String>> valueMap;

// private PrintWriter output;

public FeatureWriterARFFBinarized(List<Feature> ftrs) {
  super(ftrs);
  valueMap = null;
}

public FeatureWriterARFFBinarized(List<Feature> ftrs, OutputStream output) {
  super(ftrs, new PrintWriter(output, true));
  valueMap = null;
}
public FeatureWriterARFFBinarized(List<Feature> ftrs, PrintWriter output) {
  super(ftrs, output);
  valueMap = null;
}

public FeatureWriterARFFBinarized(List<Feature> ftrs, String filename) {
  super(ftrs, filename);
  valueMap = null;
}

@Override
public void printHeader()
{
  printHeader(output);
}

public void printHeader(PrintWriter out)
{
  List<Feature> fList = featureList;
  // out.println("Printing header "+out);
  out.println("@RELATION\tcoref");
  out.println();
  valueMap = new HashMap<Feature, ArrayList<String>>();
  for (Feature f : fList) {
    if (f.getName().equalsIgnoreCase("class") && f.isNominal() && ((NominalFeature) f).getValues().length == 2) {
      out.print("@ATTRIBUTE\tclass\t");
      String[] vals = ((NominalFeature) f).getValues();
      out.println("{" + vals[0] + "," + vals[1] + "}");
    }
    else if (f.isNumeric()) {
      out.print("@ATTRIBUTE\t" + f.name + "\t");
      out.println("NUMERIC");
    }
    else if (f.isString()) {
      out.print("@ATTRIBUTE\t" + f.name + "\t");
      out.println("STRING");
    }
    else if (f.isNominal()) {
      String[] values = ((NominalFeature) f).getValues();
      ArrayList<String> vals = new ArrayList<String>();
      if (values.length <= 2) {
        out.println("@ATTRIBUTE\t" + f.name + "\t{0,1}");
        vals.add(values[0]);
      }
      else {
        for (String val : values) {
          out.println("@ATTRIBUTE\t" + f.name + "_" + val + "\t{0,1}");
          vals.add(val);
        }
      }
      valueMap.put(f, vals);
    }
  }
  out.println();
  out.println("@DATA");
  out.flush();
}

public String printHeaderToString()
{
  StringWriter sw = new StringWriter(1024);
  PrintWriter out = new PrintWriter(sw);
  printHeader(out);
  out.flush();
  return sw.toString();
}

@Override
public void printInstanceVector(HashMap<Feature, String> vals)
{
  PrintWriter out = output;
  if (vals == null || vals.size() < 1) throw new RuntimeException("Empty feature value list");
  boolean first = true;

  for (int i = 0; i < featureList.size(); i++) {
    Feature f = featureList.get(i);
    String s = vals.get(f);
    if (f.getName().equalsIgnoreCase("class") && f.isNominal() && ((NominalFeature) f).getValues().length == 2) {
      out.print("," + s);
    }
    else if (featureList.get(i).isNominal()) {
      ArrayList<String> nomValues = valueMap.get(featureList.get(i));
      for (String nVal : nomValues) {
        if (first) {
          first = false;
        }
        else {
          out.print(",");
        }
        if (s.equalsIgnoreCase(nVal)) {
          out.print("1");
        }
        else {
          out.print("0");
        }
      }
    }
    else {
      if (first) {
        first = false;
      }
      else {
        out.print(",");
      }
      boolean isStr = featureList.get(i).isString();
      if (isStr) {
        out.print("\"");
      }
      out.print(s);
      if (isStr) {
        out.print("\"");
      }
    }
  }
  out.println();
  out.flush();
}

public String printInstanceVectorToString(HashMap<Feature, String> vals)
{
  StringWriter res = new StringWriter();
  PrintWriter out = new PrintWriter(res);

  if (vals == null || vals.size() < 1) throw new RuntimeException("Empty feature value list");
  boolean first = true;

  for (int i = 0; i < featureList.size(); i++) {
    Feature f = featureList.get(i);
    String s = vals.get(f);
    if (f.getName().equalsIgnoreCase("class") && f.isNominal() && ((NominalFeature) f).getValues().length == 2) {
      out.print("," + s);
    }
    else if (featureList.get(i).isNominal()) {
      ArrayList<String> nomValues = valueMap.get(featureList.get(i));
      for (String nVal : nomValues) {
        if (first) {
          first = false;
        }
        else {
          out.print(",");
        }
        if (s.equalsIgnoreCase(nVal)) {
          out.print("1");
        }
        else {
          out.print("0");
        }
      }
    }
    else {
      if (first) {
        first = false;
      }
      else {
        out.print(",");
      }
      boolean isStr = featureList.get(i).isString();
      if (isStr) {
        out.print("\"");
      }
      out.print(s);
      if (isStr) {
        out.print("\"");
      }
    }
  }
  out.println();
  out.flush();

  return res.toString();
}
}
