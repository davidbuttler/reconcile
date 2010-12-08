/*
 * @author ves
 */

package reconcile.scorers;

import reconcile.data.Document;
import reconcile.general.Constants;

/*
 * BCubed Metric for measuring noun-phrase coreference resolution.
 */
public class OfficialMUCScoreNoSingletons
    extends ExternalScorer {

// private static String[] COPY_TAGS = {"DOC", "TXT", "DOCID", "DOCNO"};
@Override
public String getName()
{
  return "OMUC_no_sing";
}

/**
 * Runs the official MUC scorer and reads in the results. Expects to find the script for the official MUC score in the
 * scripts directory. For more information see <i>Algorithms for Scoring Coreference Chains </i> by Bagga and Baldwin.
 * 
 * @param key
 *          The gold standard document for the scoring.
 * @param response
 *          The LeanDocument containing the predicted clustering
 * @return The MUC score for the response.
 */

@Override
public double[] score(boolean printIndividualFiles, Iterable<Document> files)
{
  return score(printIndividualFiles, files, Constants.CLUSTER_FILE_NAME);
}

@Override
public double[] score(boolean printIndividualFiles, Iterable<Document> files, String clusterName)
{
  boolean includeSingletons = false;
  return OfficialMUCScore.produceScore(printIndividualFiles, files, clusterName, includeSingletons);
}
// public double[] score(String[] files, String clusterName, FileStructure fs){
// SystemConfig cfg = Utils.getConfig();
// String muc_scorer_path = cfg.getMUCScorerPath();
// String workDir = Utils.getWorkDirectory();
// //Make key and response files
// PrintWriter outKey, outResp;
// try{
// outKey = new PrintWriter(workDir+"/keys");
// outResp = new PrintWriter(workDir+"/responses");
// }catch(IOException e){
// throw new RuntimeException(e);
// }
// for(String file:files){
// //System.out.println("Scoring "+file);
// AnnotationSet key = (new AnnotationReaderBytespan()).read(fs.getAnnSetFilenameByKey(file, Constants.ORIG), "orig");
// String text = Utils.getTextFromFile(file+ Utils.SEPARATOR + "orig.raw.txt");
//
// AnnotationSet newKey = new AnnotationSet("newKey");
// //remove some of the tags
// for(Annotation a:key){
// if(FeatureUtils.memberArray(a.getType(), COPY_TAGS)||a.getType().equalsIgnoreCase("COREF")){
// //a.removeAttribute("STATUS");
// newKey.add(a);
// }
// }
// (new AnnotationWriterEmbedded()).write(newKey, outKey, text);
// outKey.println();
//
// String responseName = FileStructure.getPath(file, fs.getClusterSubdir(), clusterName);
// LeanDocument response = DocumentPair.readDocument(responseName);
//
// AnnotationSet nps = OfficialMUCScore.translateNPs((new
// AnnotationReaderBytespan()).read(fs.getFeatDir(file)+Utils.SEPARATOR+Constants.PROPERTIES_FILE_NAME,"nps"),response,false);//fs.getAnnSetFilenameByKey(file,
// Constants.NP), "nps"),response);
//
// HashMap<Integer, String> representatives = new HashMap<Integer, String>();
//
// for(Annotation anot:nps){
// int num = Integer.parseInt(anot.getAttribute("ID"));
// int cluster = response.getClusterNum(num);
// if(representatives.containsKey(cluster)){
// anot.setAttribute("REF", representatives.get(cluster));
// }else{
// representatives.put(cluster, Integer.toString(num));
// }
// }
//
// //Add the non-coref document annotations
// for(Annotation a:key){
// if(FeatureUtils.memberArray(a.getType(),COPY_TAGS))
// nps.add(a);
// }
// try{
// (new AnnotationWriterEmbedded()).write(nps, outResp, text);
// outResp.println();
// }catch(Exception e){
// //System.out.println(nps);
// System.out.println("File was: "+file);
// throw new RuntimeException(e);
// }
// }
//
// outKey.flush();
// outKey.close();
// outResp.flush();
// outResp.close();
// File runDir = new File(workDir);
// String command = muc_scorer_path;
// if(cfg.getDataset().contains("muc7"))
// command += " " + Utils.getScriptDirectory() + Utils.SEPARATOR +"config_muc7";
// else
// command += " " + Utils.getScriptDirectory() + Utils.SEPARATOR +"config";
//
// try{
// Utils.runExternal(command, runDir, false);
// }catch(IOException e){
// throw new RuntimeException(e);
// }catch( InterruptedException ie){
// throw new RuntimeException(ie);
// }
// String outFile = workDir + Utils.SEPARATOR + "scores";
// Pattern p =
// Pattern.compile("TOTALS\\:\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\/\\s+\\d+\\s+(\\d+\\.\\d)\\%\\s+\\d+\\s+\\/\\s+\\d+\\s+(\\d+.\\d)\\%\\s+(\\d+\\.\\d)\\%");
// //Parse the outfile
// double[] result = new double[InternalScorer.RESULT_SIZE];
// try{
// BufferedReader sc = new BufferedReader(new FileReader(outFile));
// String line;
//
// while((line=sc.readLine())!=null){
// Matcher m = p.matcher(line);
// if (m.matches()) {
// double recall = Double.parseDouble(m.group(1));
// double prec = Double.parseDouble(m.group(2));
// double f = Double.parseDouble(m.group(3));
// result[InternalScorer.PRECISION]=prec/100;
// result[InternalScorer.RECALL]=recall/100;
// result[InternalScorer.F]=f/100;
// break;
// }
// }
// }catch(Exception e){
// throw new RuntimeException(e);
// }
//
// return result;
// }

}
