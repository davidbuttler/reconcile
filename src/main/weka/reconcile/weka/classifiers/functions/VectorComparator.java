/*
 * JRip.java Copyright (C) 2001 Xin Xu, Eibe Frank
 */

package reconcile.weka.classifiers.functions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;

import reconcile.scorers.BCubedScore;
import reconcile.scorers.LeanDocument;
import reconcile.scorers.MUCScore;
import reconcile.weka.core.AttributeShort;
import reconcile.weka.core.Instance;
import reconcile.weka.core.InstanceShort;
import reconcile.weka.core.ModifiedInstancesShort;


public class VectorComparator {
	/** The similarity threshold */
	private static double THRESHOLD = 0;

	/** The weights for each perceptron */
	private static double[] m_Weights = null;

	/** The exponent */
	private double m_Exponent = 1.0;
	
	private static boolean m_FullOutput = false;

	public double[] divideArray(double[] w, int divider) {
		double[] result = new double[w.length];
		for (int i = 0; i < w.length; i++)
			result[i] = w[i] / divider;
		return result;
	}

	public double[] multiplyArray(double[] w, double mult) {
		double[] result = new double[w.length];
		for (int i = 0; i < w.length; i++)
			result[i] = w[i] * mult;
		return result;
	}

	public void addToArray(double[] a1, double[] a2) {
		for (int i = 0; i < a1.length; i++)
			a1[i] += a2[i];
	}

	/**
	 * Returns textual description of classifier.
	 */
	public String toString() {

		return "WeightVectiorApplier";
	}
	
	  //This function returns a three-element array containing the number of
	  // correctly
	  //identified positives, the number of identified positives
	  //and the total number of positives
	  public static int[] positiveIdentification(LeanDocument pred, ModifiedInstancesShort gold, boolean outputMistakes) {
	    //System.err.print(pred);
	    int[] accurate = {0, 0, 0};
	    for(int i = 0; i < gold.numInstances(); i++){
	      InstanceShort ins = gold.instance(i);
	      //System.out.print(ins);
	      int id1 = ins.value(gold.m_ID1);
	      int id2 = ins.value(gold.m_ID2);
	      boolean positive = ins.classValue() == gold.m_Positive;
	      if(positive)
	        accurate[2]++;
	      if(pred.getClusterNum(new Integer(id1)).equals(pred.getClusterNum(new Integer(id2)))&&pred.getClusterNum(new Integer(id1)).intValue()>=0){
	        accurate[1]++;
	        if(positive)
	          accurate[0]++;
	        else
	      	  if(outputMistakes)
	      		  System.out.println(ins.value(gold.m_DOCNO)+":"+id1+"-"+id2+"Predicted +; true -");
	        //System.out.println(id1+"-"+id2+"[+]");
	      }else if(positive && outputMistakes)
	      	System.out.println(ins.value(gold.m_DOCNO)+":"+id1+"-"+id2+"Predicted -; true +");
	    }
	    //System.err.println("Accurate "+accurate+"/"+gold.numInstances());
	    return accurate;
	  }
	
	public static double[] evaluateClassifier(ModifiedInstancesShort[] insts, ModifiedInstancesShort[] origInst, boolean outputMistakes, boolean usePredClass)
     throws Exception {
   if(insts == null)
     return null;
   int vecLen = 0;
   double[] result = {0, 0};
   //get the lenght of the feature vectors
   for(int i = 0; i < insts.length && vecLen == 0; i++){
     ModifiedInstancesShort ins = insts[i];
     if(ins != null){
       vecLen = ins.numAttributes();
     }
   }
   if(vecLen == 0)
     throw new RuntimeException("No training instances");
   NumberFormat nf = NumberFormat.getInstance();
   nf.setMaximumFractionDigits(4);
   nf.setMinimumFractionDigits(4);

   LeanDocument[] truth = new LeanDocument[insts.length];
   for(int i = 0; i < truth.length; i++){
   	truth[i] = makeDocument(origInst[i]);
   	//System.err.println(truth[i]);
   }	
  

   int total = 0;
   int totalInst = 0, totalPred = 0;
   int totalPrecision = 0, totalNumNPsPrecision = 0;
   int totalNumNPsRecall = 0, totalRecall = 0;
   double mucTotalPrecision = 0, mucTotalRecall = 0;
   int mucTotalRecallNum = 0, mucTotalPrecisionNum = 0, mucTotalRecallDenom = 0, mucTotalPrecisionDenom = 0;;
   int numDocuments = insts.length;
   for(int d = 0; d < insts.length; d++){
     ModifiedInstancesShort doc = insts[d];
   	
  	
     LeanDocument tr = truth[d];
     
     LeanDocument cDoc;
     if(usePredClass)
   	  cDoc = makeDocument(doc,doc.attribute("predicted_class"));
     else
   	  cDoc = makeDocument(doc);
     int[] pred = positiveIdentification(cDoc, origInst[d], outputMistakes);
     total += pred[0];
     totalPred += pred[1];
     totalInst += pred[2];
     double recall;
     int numNPsRecall = 0, numNPsPrecision = 0;
     if(tr.numNounPhrases() > 1){
       recall = BCubedScore.bCubedScore(tr, cDoc);
       numNPsRecall = tr.numNounPhrases();
     } else{
       recall = 0;
     }
     totalRecall += recall;
     totalNumNPsRecall += numNPsRecall;
     recall = numNPsRecall == 0?0:recall / numNPsRecall;

     double precision;
     if(cDoc.numNounPhrases() > 1){
       precision = BCubedScore.bCubedScore(cDoc, tr);
       numNPsPrecision = cDoc.numNounPhrases();
     } else{
       precision = 0;
     }
     totalPrecision += precision;
     totalNumNPsPrecision += numNPsPrecision;
     precision = numNPsPrecision == 0?0:precision / numNPsPrecision;
     //System.out.println("Document "+d+" nps "+numNPsPrecision+" vs "+numNPsRecall);
     int[] mucRecall = new int[2], mucPrecision = new int[2];
     if(tr.numNounPhrases()>0){
       mucRecall = MUCScore.mucScore(tr, cDoc);
       mucPrecision = MUCScore.mucScore(cDoc, tr);
     }else
       numDocuments--;
     mucTotalRecallNum += mucRecall[0];
     mucTotalRecallDenom += mucRecall[1];
     mucTotalPrecisionNum += mucPrecision[0];
     mucTotalPrecisionDenom += mucPrecision[1];

   }
   //System.out.print("Precision " +totalPrecision +"/"+totalNumNPsPrecision+
   // " recall "+totalRecall);
   double precPos = totalPred == 0?0:total / (double)totalPred;
   double recallPos = total == 0?0:total / (double)totalInst;
   double f1Pos = precPos + recallPos == 0?0:(2 * precPos * recallPos) / (precPos + recallPos);
   if(m_FullOutput)
     System.out.print("F1 " + nf.format(precPos) + "|" + nf.format(recallPos) + "="
         + nf.format(f1Pos));
   else
     System.out.print(nf.format(precPos) + "," + nf.format(recallPos) + "=" + nf.format(f1Pos)
         + ";");

   double prec = totalNumNPsPrecision == 0?1:(double)totalPrecision / totalNumNPsPrecision;
   double rec = totalNumNPsRecall == 0?1:(double)totalRecall / totalNumNPsRecall;
   double f1, denom = prec + rec;
   f1 = denom > 0?(2 * prec * rec) / denom:0;
   if(m_FullOutput){
     System.out.print(" prec=" + nf.format(prec));
     System.out.print(" recall=" + nf.format(rec));
     System.out.println(" f1=" + nf.format(f1));
   } else{
     System.out.print(nf.format(prec) + ",");
     System.out.print(nf.format(rec) + "=");
     System.out.print(nf.format(f1) + ";");
   }
   //System.err.println("prec"+mucTotalPrecision+"rec "+mucTotalRecall+"num "+numDocuments);
   mucTotalRecall = mucTotalRecallDenom>0?(double)mucTotalRecallNum/(double)mucTotalRecallDenom:0;
   mucTotalPrecision = mucTotalPrecisionDenom>0?(double)mucTotalPrecisionNum/(double)mucTotalPrecisionDenom:0;
   double mucF1,mucDenom = mucTotalPrecision + mucTotalRecall;
   mucF1 = mucDenom >0?(2*mucTotalPrecision*mucTotalRecall)/mucDenom:0;
   if(m_FullOutput){
     System.out.print(" MUCprec=" + nf.format(mucTotalPrecision));
     System.out.print(" MUCrecall=" + nf.format(mucTotalRecall));
     System.out.println(" MUCf1=" + nf.format(mucF1));
   } else{
     System.out.print(nf.format(mucTotalPrecision) + ",");
     System.out.print(nf.format(mucTotalRecall) + "=");
     System.out.print(nf.format(mucF1) + ";");
   }
   result[0] = f1Pos;
   result[1] = f1;
   return result;
 }


	/**
	 * Computes the inner product of two instances
	 */
	private double innerProduct(Instance i1, Instance i2) throws Exception {

		// we can do a fast dot product
		double result = 0;
		int n1 = i1.numValues();
		int n2 = i2.numValues();
		int classIndex = i1.classIndex();
		for (int p1 = 0, p2 = 0; p1 < n1 && p2 < n2;) {
			int ind1 = i1.index(p1);
			int ind2 = i2.index(p2);
			if (ind1 == ind2) {
				if (ind1 != classIndex) {
					result += i1.valueSparse(p1) * i2.valueSparse(p2);
				}
				p1++;
				p2++;
			} else if (ind1 > ind2) {
				p2++;
			} else {
				p1++;
			}
		}
		result += 1.0;

		if (m_Exponent != 1) {
			return Math.pow(result, m_Exponent);
		} else {
			return result;
		}
	}

	/**
	 * Main method.
	 * 
	 * @param args
	 *           the options for the classifier
	 */
	public static int[] apply(BufferedReader testReader, String wFilename) {
		ModifiedInstancesShort[] testData = null;
		ModifiedInstancesShort test = null;
		VectorComparator classifier = new VectorComparator();
		int classIndex = -1;

		try {
			test = new ModifiedInstancesShort(testReader);
			test.setClass(test.attribute("class"));
			
			test.cleanUpValues();
			AttributeShort predClass = test.classAttribute().copy("predicted_class");
			test.insertAttributeAt(predClass, test.numAttributes());
			test = test.binarizeValues();
			test.setClass(test.attribute("class"));

			System.out.println("Classifying " + test.numInstances() + " instances.");

			ArrayList<Double> weights = new ArrayList<Double>();
			if (wFilename == null || wFilename.length() < 1)
				throw new RuntimeException(
						"Name of file containing weights not specified");
			BufferedReader file = new BufferedReader(new FileReader(wFilename));
			String line;
			while ((line = file.readLine()) != null) {
				double w = Double.parseDouble(line);
				weights.add(new Double(w));
			}
			m_Weights = new double[weights.size()];
			for (int y = 0; y < weights.size(); y++) {
				m_Weights[y] = ((Double) weights.get(y)).doubleValue();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		test.setUpSources();
		int[] ptrs = apply(m_Weights, test);
		/*
		LeanDocument cDoc = makeDocument(test, ptrs);
		try{
		PrintStream out= new PrintStream(new File(outputFilename));
		int counter = 0;
		AnnotationSet coref = new AnnotationSet("coref");
		for (Annotation np : basenps) {
			Integer cID= cDoc.get(new Integer(counter));
			HashMap<String, String> features = Maps.newTreeMap();
			features.put("corefID", (new Integer(cID)).toString());
			coref.add(np.getStartOffset(), np.getEndOffset(), "coref", features);
			counter++;
		}
		(new AnnotationWriterBytespan()).write(coref, out);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		System.out.println();*/
		return ptrs;
	}
	/**
	 * Main method.
	 * 
	 * @param args
	 *           the options for the classifier
	 */
	public static void apply(String newFilename, String origFilename) {
		ModifiedInstancesShort[] newData = null;
		ModifiedInstancesShort newIns = null;
		ModifiedInstancesShort[] origData = null;
		ModifiedInstancesShort origIns = null;
		VectorComparator classifier = new VectorComparator();
		int classIndex = -1;
		BufferedReader newReader = null;
		BufferedReader origReader = null;

		try {
			newReader = new BufferedReader(new FileReader(newFilename));
			newIns = new ModifiedInstancesShort(newReader);
			newIns.setClass(newIns.attribute("class"));
			int num = newIns.getNumDocuments();
			newIns.cleanUpValues();
			newIns.setClass(newIns.attribute("class"));
			newIns.setPositiveClass(newIns.attribute("class").indexOfValue("+"));
			AttributeShort predClass = newIns.classAttribute().copy("predicted_class");
			newIns.insertAttributeAt(predClass, newIns.numAttributes());
			
         newIns.setNumDocuments(num);
      	newData = ModifiedInstancesShort.splitDocs(newIns);


			System.out.println("Classifying " + newIns.numInstances() + " instances.");

			origReader = new BufferedReader(new FileReader(origFilename));
			origIns = new ModifiedInstancesShort(origReader);
			origIns.setClass(origIns.attribute("class"));
			
			origIns.cleanUpValues();
			origIns.insertAttributeAt(predClass, origIns.numAttributes());
			origIns.setClass(origIns.attribute("class"));
			origIns.setPositiveClass(origIns.attribute("class").indexOfValue("+"));
			System.out.println("Classifying " + origIns.numInstances() + " instances.");
         origIns.setNumDocuments(num);
      	origData = ModifiedInstancesShort.splitDocs(origIns);
      	
      	evaluateClassifier(newData, origData, true, false);
		} catch (Exception ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public static int[] apply(double[] w, ModifiedInstancesShort data) {

		// LeanDocument result = new LeanDocument();
		AttributeShort DOCID = data.m_DOCNO;
		AttributeShort ID1 = data.m_ID1;
		AttributeShort ID2 = data.m_ID2;

		int[] ptrs = null;
		if (data == null || data.numInstances() == 0)
			return null;
		InstanceShort first = data.instance(0);
		//System.err.println(first);
		int id1 = (int) first.value(ID1);
		int id2 = (int) first.value(ID2);
		int len = id1>id2?id1+1:id2+1;
		ptrs = new int[len];
		// initialize pointers so each item is in it's own set
		for (int j = 0; j < len; j++)
			ptrs[j] = j;

		for (int i = 0; i < data.numInstances(); i++) {
			// read all instances for a document
			InstanceShort current = data.instance(i);
			// System.out.println("Working on: "+current);
			id1 = (int) current.value(ID1);
			id2 = (int) current.value(ID2);

			if (innerProduct(w, current) > THRESHOLD) {
				union(id1, id2, ptrs);
				current.setValue(data.m_Prediction, data.m_Positive);
			} else {
				current.setValue(data.m_Prediction, data.m_Negative);
			}
		}
		return ptrs;
	}

	private static LeanDocument makeDocument(ModifiedInstancesShort data,
			int[] ptrs) {
		LeanDocument result = new LeanDocument();
		AttributeShort ID1 = data.m_ID1;
		AttributeShort ID2 = data.m_ID2;

		for (int k = 0; k < data.numInstances(); k++) {
			InstanceShort curPair = data.instance(k);
			int curId1 = (int) curPair.value(ID1);
			int curId2 = (int) curPair.value(ID2);
			if (!result.contains(new Integer(curId1))) {
				// System.err.println("Add "+curId1+"-"+find(curId1,ptrs));
				result.add(new Integer(curId1), new Integer(find(curId1, ptrs)));
			}
			if (!result.contains(new Integer(curId2)))
				result.add(new Integer(curId2), new Integer(find(curId2, ptrs)));
		}
		return result;
	}

	private static double innerProduct(double[] w, InstanceShort i) {
		if (w.length != i.numAttributes())
			throw new RuntimeException("Wrong number of attributes wv:"+w.length+" vs "+i.numAttributes());
		double result = 0;
		for (int j = 0; j < i.numAttributes(); j++) {
			if (i.attribute(j).isFeature()) {
				if (i.attribute(j).isNominal())
					result += i.value(j) * w[j];
				else
					result += i.attribute(j).getOriginalValue(new Short(i.value(j)))
							* w[j];
			}
		}
		// System.err.println("VAL="+result);
		return result;
	}

	private static int find(int i, int[] ptrs) {
		// find the set number for the element
		int ind = i;
		while (ind != ptrs[ind]) {
			ind = ptrs[ind];
		}
		// fix the link so that it is one hop only
		// note: this doesn't implement the full union-find update

		ptrs[i] = ind;
		return ind;
	}

	private static void union(int i, int j, int[] ptrs) {
		int indI = find(i, ptrs);
		int indJ = find(j, ptrs);
		ptrs[indI] = indJ;
	}

	private int getChainSize(int id1, int[] clusterPtrs) {
		int cl = find(id1, clusterPtrs);
		int num = 0;
		for (int i = 0; i < clusterPtrs.length; i++)
			if (find(i, clusterPtrs) == cl)
				num++;
		return num;
	}
	public static LeanDocument makeDocument(ModifiedInstancesShort data){
		return makeDocument(data, data.classAttribute());
	}
	public static LeanDocument makeDocument(ModifiedInstancesShort data, AttributeShort Cl) {
		if (data == null || data.numInstances() == 0)
			return null;
		LeanDocument result = new LeanDocument(data.instance(0).value(
				data.m_DOCNO));
		int positive = 0;
		AttributeShort DOCID = data.m_DOCNO;
		AttributeShort ID1 = data.m_ID1;
		AttributeShort ID2 = data.m_ID2;
		short pos = data.m_Positive;
		//System.out.println(Cl+" --- "+pos);
		int[] ptrs = null;
		if (data == null || data.numInstances() == 0)
			return null;
		InstanceShort first = data.instance(0);
		int id1 = (int) first.value(ID1);
		int id2 = (int) first.value(ID2);
		int len = id1>id2 ?id1 + 1:id2+1;
		ptrs = new int[len];
		// initialize pointers so each item is in it's own set
		for (int j = 0; j < len; j++)
			ptrs[j] = j;
		//System.out.println(first);
		if (first.value(Cl) == pos) {
			union(id1, id2, ptrs);
			positive++;
		}
		for (int i = 1; i < data.numInstances(); i++) {
			// read all instances for a document
			InstanceShort current = data.instance(i);
			// System.out.println("Working on: "+current);
			id1 = (int) current.value(ID1);
			id2 = (int) current.value(ID2);

			if (current.value(Cl) == pos) {
				union(id1, id2, ptrs);
				positive++;
			}
		}
		for (int k = 0; k < data.numInstances(); k++) {
			InstanceShort curPair = data.instance(k);
			int curId1 = (int) curPair.value(ID1);
			int curId2 = (int) curPair.value(ID2);
			if (!result.contains(new Integer(curId1))) {
				// System.err.println("Add "+curId1+"-"+find(curId1,ptrs));
				result.add(new Integer(curId1), new Integer(find(curId1, ptrs)));
			}
			if (!result.contains(new Integer(curId2)))
				result.add(new Integer(curId2), new Integer(find(curId2, ptrs)));
		}
		// System.err.print("[TC:"+startPositive+"("+startPositiveTotal+")->"
		// +endPositive+"("+endPositiveTotal+")]");
		//System.out.println("Positive "+positive);
		return result;
	}
}