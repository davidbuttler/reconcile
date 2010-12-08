package reconcile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;

import reconcile.classifiers.Classifier;
import reconcile.clusterers.Clusterer;
import reconcile.clusterers.ThresholdClusterer;
import reconcile.data.AnnotationSet;
import reconcile.data.AnnotationWriterBytespan;
import reconcile.data.Document;
import reconcile.general.Utils;


public class Tester {
public static void classify(Iterable<Document> testFilenames)
{
	classify(testFilenames,false);
}
public static void classify(Iterable<Document> testFilenames, boolean inMemory)
{
  System.out.println("Classifying...");
  SystemConfig cfg = Utils.getConfig();
  String modelFN = cfg.getString("MODEL_NAME");
  String classifier = cfg.getClassifier();
  if (classifier == null) throw new RuntimeException("Classifier not specified");
  String[] options = cfg.getStringArray("TesterOptions." + classifier);
  boolean runClassifier = cfg.getBoolean("TESTER.RUN_CLASSIFIER", true);

  if (runClassifier) {
	System.out.println(cfg.getString("WORK_DIR"));
	String fullModelFN = Utils.getWorkDirectory()+"/"+modelFN;
	//String fullModelFN = "models"+Utils.SEPARATOR+modelFN;	
	//URL res = Tester.class.getResource(fullModelFN);
//    File modelFile;
//    try {
//		modelFile= new File(fullModelFN);
//		
//	} catch (Exception e) {
//		throw new RuntimeException(e);
//	}
	
    Classifier learner = Constructor.createClassifier(classifier, fullModelFN);
	 System.out.println(learner.getInfo(options));
	 for (Document doc : testFilenames) {
		 if(inMemory)
			 learner.test(doc.getFeatureReader(), doc.getPredictionWriter(), options);
		 else
			 learner.test(doc.getFeatureFile(), doc.getPredictionFile(), fullModelFN, options);
    }
  }
}

public static void cluster(Iterable<Document> testFilenames)
{
	cluster(testFilenames,false);
}
public static void cluster(Iterable<Document> testFilenames, boolean inMemory)
{
  System.out.println("Clustering...");
  SystemConfig cfg = Utils.getConfig();
  String clustererName = cfg.getClusterer();
  Clusterer clusterer = Constructor.createClusterer(clustererName);
  if (clusterer instanceof ThresholdClusterer) {
    String thr = cfg.getString("ClustOptions.THRESHOLD");
    if (thr != null && thr.length() > 0) {
      ((ThresholdClusterer) clusterer).setThreshold(Double.parseDouble(thr));
    }
  }
  String[] clustOptions = cfg.getStringArray("ClustOptions." + clustererName);

  System.out.println(clusterer.getInfo(clustOptions));
  for (Document doc : testFilenames) {
    AnnotationSet result = clusterer.cluster(doc, clustOptions);
    doc.writeAnnotationSet(result);
	if(!inMemory)
      try {
        File out = doc.getClusterFile();
        PrintWriter outWriter = new PrintWriter(out);
        new AnnotationWriterBytespan().write(result, outWriter);
      }
      catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
  }
}

public static void test(Iterable<Document> testFilenames)
{
  test(testFilenames, null, true);
}

public static void test(Iterable<Document> testFilenames, PrintWriter record)
{
  test(testFilenames, record, true);
}

public static void test(Iterable<Document> testFilenames, PrintWriter record, boolean printIndividualFileScores)
{
  classify(testFilenames);
  cluster(testFilenames);
  Scorer.score(printIndividualFileScores, testFilenames, record);
}


public static void resolve(Iterable<Document> testFilenames)
{
  resolve(testFilenames, false);
}
public static void resolve(Iterable<Document> testFilenames, boolean inMemory)
{
  classify(testFilenames, inMemory);
  cluster(testFilenames, inMemory);
}
}
