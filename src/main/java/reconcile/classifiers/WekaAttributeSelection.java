package reconcile.classifiers;

import gov.llnl.text.util.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import reconcile.SystemConfig;
import reconcile.general.Utils;
import reconcile.weka.attributeSelection.ASEvaluation;
import reconcile.weka.attributeSelection.ASSearch;
import reconcile.weka.attributeSelection.AttributeSelection;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;

/**
 * 
 * 
 * @author d. hysom
 */

public class WekaAttributeSelection {

HashSet<String> mAttributesToKeep = null;

/**
 * Performs attribute selection. You must run filterInstances to actually alter the Instances.
 */
public void selectAttributes(Instances data)
    throws Exception
{
  System.out.println("starting selectAttributes");
  mAttributesToKeep = new HashSet<String>();

  SystemConfig cfg = Utils.getConfig();

  String selected_evaluator = cfg.getString("FEATURE_SELECTION_EVALUATOR");
  String[] opts = cfg.getString("FEATURE_SELECTION_EVALUATOR_OPTIONS").split("\\s+");
  ASEvaluation evaluator = ASEvaluation.forName("weka.attributeSelection." + selected_evaluator, opts);

  String selected_search = cfg.getString("FEATURE_SELECTION_SEARCH");
  String[] opts2 = cfg.getString("FEATURE_SELECTION_SEARCH_OPTIONS").split("\\s+");
  ASSearch search = ASSearch.forName("weka.attributeSelection." + selected_search, opts2);

  System.out.println("\neval method: " + selected_evaluator);
  System.out.println("eval opts: ");
  for (String s : opts) {
    System.out.print(s + " ");
  }
  System.out.println();
  System.out.println("\nsearch method: " + selected_search);
  System.out.println("search opts:");
  for (String s : opts2) {
    System.out.print(s + " ");
  }
  System.out.println();

  int cutoff = cfg.getInt("FEATURE_SELECTION_CUTOFF");
  AttributeSelection ass = new AttributeSelection();
  ass.setEvaluator(evaluator);
  ass.setSearch(search);

  System.out.println("numInstances: " + data.numInstances());
  System.out.println("numClasses: " + data.numClasses());
  System.out.println("numAttributes: " + data.numAttributes());

  System.out.println("\nselecting attributes ...");
  ass.SelectAttributes(data);

  HashSet<String> mAttributesToKeep = new HashSet<String>();
  String[] features_to_keep = cfg.getStringArray("FEATURE_SELECTION_KEEP");
  for (String f : features_to_keep) {
    mAttributesToKeep.add(f);
  }

  int[] selected = ass.selectedAttributes();
  if (cutoff == -1) {
    cutoff = selected.length;
  }

  Instance inst = data.instance(0);
  for (int j = 0; j < cutoff; j++) {
    mAttributesToKeep.add(inst.attribute(j).name());
  }

  mAttributesToKeep = fixDependencies(mAttributesToKeep);
  System.out.println("\nattributes to keep: " + mAttributesToKeep);

  String fn = cfg.getString("SELECTED_FEATURES_FILENAME");
  File out = new File(fn);
  StringBuffer buf = new StringBuffer();
  for (String s : mAttributesToKeep) {
    buf.append(s + "\n");
  }
  FileUtils.write(out, buf.toString());
  System.out.println("selected features written to: " + fn);
}

/**
 * deletes the attributes from 'inst' that were identified during the most recent call to selectFeatures().
 * 
 */
public void filterInstances(Instances data)
    throws Exception
{
  // if (mAttributesToKeep == null) {
  SystemConfig cfg = Utils.getConfig();
  String fn = cfg.getString("SELECTED_FEATURES_FILENAME");
  mAttributesToKeep = new HashSet<String>();
  List<String> feat = FileUtils.readFileLines(fn);
  for (String s : feat) {
    mAttributesToKeep.add(s);
  }
  // }

  Instance inst = data.instance(0);
  for (int j = inst.numAttributes() - 2; j >= 0; j--) {
    if (!mAttributesToKeep.contains(Integer.toString(j)) && !mAttributesToKeep.contains(inst.attribute(j).name())) {
      data.deleteAttributeAt(j);
    }
  }
}

private HashSet<String> fixDependencies(HashSet<String> in)
    throws Exception
{
  HashSet<String> result = new HashSet<String>();

  // build dependency map; maps:
  // feature_name => {features which must be present}
  HashMap<String, HashSet<String>> dep_map = new HashMap<String, HashSet<String>>();
  List<String> file = FileUtils.readFileLines(new File("config", "feature_dependencies"));
  for (String line : file) {
    if (line.charAt(0) != '#' && line.trim().length() > 0) {
      String[] tmp = line.split("\\s+::\\s+");
      HashSet<String> h2 = new HashSet<String>();
      dep_map.put(tmp[0], h2);
      String[] tmp2 = tmp[1].split("\\s+");
      for (String s : tmp2) {
        h2.add(s);
      }
    }
  }

  // filtering stage
  for (String s : in) {
    result.add(s);
  }
  for (String s : dep_map.keySet()) {
    if (in.contains(s)) {
      for (String d : dep_map.get(s)) {
        if (!result.contains(d)) {
          result.add(d);
          System.out.println("added dependency: " + s + " <= " + d);
        }
      }
    }
  }

  return result;
}

}
