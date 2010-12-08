package reconcile.featureVector;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;

public class FeatureWriterARFF
    extends FeatureWriter {

public FeatureWriterARFF(List<Feature> ftrs, PrintWriter output) {
  super(ftrs, output);
}

public FeatureWriterARFF(List<Feature> ftrs, OutputStream output) {
  super(ftrs, new PrintWriter(output, true));
}

public FeatureWriterARFF(List<Feature> ftrs, String filename) {
  super(ftrs, filename);
}

public FeatureWriterARFF(List<Feature> ftrs) {
  super(ftrs);
}

@Override
public void printHeader()
{
  List<Feature> fList = featureList;
  PrintWriter out = output;

  out.println("@RELATION\tcoref");
  out.println();
  for (Feature f : fList) {
    out.print("@ATTRIBUTE\t" + f.name + "\t");
    if (f.isNumeric()) {
      out.println("NUMERIC");
    }
    else if (f.isString()) {
      out.println("STRING");
    }
    else if (f.isNominal()) {
      String[] values = ((NominalFeature) f).getValues();
      out.print("{");
      String separator = "";
      for (String val : values) {
        out.print(separator + val);
        separator = ",";
      }
    }
    if (f.isNominal()) {
      out.println("}");
    }
  }
  out.println();
  out.println("@DATA");
  out.flush();
}

public String printHeaderToString()
{

  List<Feature> fList = featureList;
  StringWriter res = new StringWriter();
  PrintWriter out = new PrintWriter(res);

  out.println("@RELATION\tcoref");
  out.println();
  for (Feature f : fList) {
    out.print("@ATTRIBUTE\t" + f.name + "\t");
    if (f.isNumeric()) {
      out.println("NUMERIC");
    }
    else if (f.isString()) {
      out.println("STRING");
    }
    else if (f.isNominal()) {
      String[] values = ((NominalFeature) f).getValues();
      out.print("{");
      String separator = "";
      for (String val : values) {
        out.print(separator + val);
        separator = ",";
      }
    }
    if (f.isNominal()) {
      out.println("}");
    }
  }
  out.println();
  out.println("@DATA");
  out.flush();
  return res.toString();
}

@Override
public void printInstanceVector(HashMap<Feature, String> vals)
{
  PrintWriter out = output;
  if (vals == null || vals.size() < 1) throw new RuntimeException("Empty feature value list");
  boolean isStr = featureList.get(0).isString();
  if (isStr) {
    out.print("\"");
  }
  out.print(vals.get(featureList.get(0)));
  if (isStr) {
    out.print("\"");
  }
  for (int i = 1; i < featureList.size(); i++) {
    Feature f = featureList.get(i);
    String s = vals.get(f);
    out.print(",");
    isStr = featureList.get(i).isString();
    if (isStr) {
      out.print("\"");
    }
    out.print(s);
    if (isStr) {
      out.print("\"");
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
  boolean isStr = featureList.get(0).isString();
  if (isStr) {
    out.print("\"");
  }
  out.print(vals.get(featureList.get(0)));
  if (isStr) {
    out.print("\"");
  }
  for (int i = 1; i < featureList.size(); i++) {
    Feature f = featureList.get(i);
    String s = vals.get(f);
    out.print(",");
    isStr = featureList.get(i).isString();
    if (isStr) {
      out.print("\"");
    }
    out.print(s);
    if (isStr) {
      out.print("\"");
    }
  }
  out.println();
  out.flush();
  return res.toString();
}
}
