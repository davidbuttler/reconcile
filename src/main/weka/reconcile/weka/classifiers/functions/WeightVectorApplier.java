/*
 * JRip.java Copyright (C) 2001 Xin Xu, Eibe Frank
 */

package reconcile.weka.classifiers.functions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;
import reconcile.data.AnnotationWriterBytespan;
import reconcile.scorers.LeanDocument;
import reconcile.weka.core.AttributeShort;
import reconcile.weka.core.Instance;
import reconcile.weka.core.InstanceShort;
import reconcile.weka.core.ModifiedInstancesShort;


public class WeightVectorApplier {
	/** The similarity threshold */
	private static double THRESHOLD = 0;

	/** The weights for each perceptron */
	private static double[] m_Weights = null;

	/** The exponent */
	private double m_Exponent = 1.0;

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
	@Override
  public String toString() {

		return "WeightVectiorApplier";
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
	
	public static double[] readWeightVector(String wFilename, int numAttr, boolean check){
		ArrayList<Double> weights = new ArrayList<Double>();
		System.out.println("Reading classifier from "+wFilename);
		double[] result = new double[numAttr];
		try {
			BufferedReader file;
			try {
				file = new BufferedReader(new InputStreamReader(WeightVectorApplier.class.getResourceAsStream(wFilename)));
			}
			catch(NullPointerException npe) {
				file = new BufferedReader(new FileReader(wFilename));				
			}
			
			String line;
			while ((line = file.readLine()) != null) {
				double w = Double.parseDouble(line);
				weights.add(new Double(w));
			}
			if(weights.size()!=numAttr&&weights.size()!=numAttr+1){
				throw new RuntimeException("Weight vector is wrong length "+weights.size()+" vs "+numAttr);
			}
			for (int y = 0; y < numAttr; y++) {
				result[y] = (weights.get(y)).doubleValue();
			}
			//THRESHOLD = weights.size()==numAttr+1?weights.get(weights.size()-1):THRESHOLD;
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		return result;
	}
	
	public double[] readWeightVector(String wFilename, int numAttr){
		ArrayList<Double> weights = new ArrayList<Double>();
		try {
			BufferedReader file = new BufferedReader(new FileReader(wFilename));
			String line;
			while ((line = file.readLine()) != null) {
				double w = Double.parseDouble(line);
				weights.add(new Double(w));
			}
			if(weights.size()!=numAttr&&weights.size()!=numAttr+1){
				throw new RuntimeException("Weight vector is wrong length "+weights.size()+" vs "+numAttr);
			}
			m_Weights = new double[numAttr];
			for (int y = 0; y < numAttr; y++) {
				m_Weights[y] = (weights.get(y)).doubleValue();
			}
			THRESHOLD = weights.size()==numAttr+1?weights.get(weights.size()-1):THRESHOLD;
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		return m_Weights;
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
		WeightVectorApplier classifier = new WeightVectorApplier();
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
			int numAttr = test.numAttributes();
			classifier.readWeightVector(wFilename, numAttr);
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
	public static void apply(String testFileName, String initFilename, String outputFilename,
			AnnotationSet basenps) {
		ModifiedInstancesShort[] testData = null;
		ModifiedInstancesShort test = null;
		WeightVectorApplier classifier = new WeightVectorApplier();
		int classIndex = -1;
		BufferedReader testReader = null;

		try {
			testReader = new BufferedReader(new FileReader(testFileName));
			test = new ModifiedInstancesShort(testReader);
			test.setClass(test.attribute("class"));
			
			test.cleanUpValues();
			AttributeShort predClass = test.classAttribute().copy("predicted_class");
			test.insertAttributeAt(predClass, test.numAttributes());
			test = test.binarizeValues();
			test.setClass(test.attribute("class"));

			System.out.println("Classifying " + test.numInstances() + " instances.");

			classifier.readWeightVector(initFilename, test.numAttributes());
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		test.setUpSources();
		int[] ptrs = apply(m_Weights, test);
		LeanDocument cDoc = makeDocument(test, ptrs);
		try{
		PrintWriter out= new PrintWriter(new File(outputFilename));
		int counter = 0;
		AnnotationSet coref = new AnnotationSet("coref");
		for (Annotation np : basenps) {
			Integer cID= cDoc.getClusterNum(new Integer(counter));
      HashMap<String, String> features = new HashMap<String, String>();
			features.put("corefID", (new Integer(cID)).toString());
			coref.add(np.getStartOffset(), np.getEndOffset(), "coref", features);
			counter++;
		}
		(new AnnotationWriterBytespan()).write(coref, out);
		out.flush();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		System.out.println();
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
		int id1 = first.value(ID1);
		int id2 = first.value(ID2);
		int len = id1>id2?id1+1:id2+1;
		ptrs = new int[len];
		// initialize pointers so each item is in it's own set
		for (int j = 0; j < len; j++)
			ptrs[j] = j;

		for (int i = 0; i < data.numInstances(); i++) {
			// read all instances for a document
			InstanceShort current = data.instance(i);
			// System.out.println("Working on: "+current);
			id1 = current.value(ID1);
			id2 = current.value(ID2);

			if (innerProduct(w, current) > THRESHOLD) {
				union(id1, id2, ptrs);
				current.setValue(data.m_Prediction, data.m_Positive);
			} else {
				current.setValue(data.m_Prediction, data.m_Negative);
			}
		}
		return ptrs;
	}
	
	public static double[] getDistance(double[] w, ModifiedInstancesShort data) {

		double[] result = new double[data.numInstances()];
		for (int i = 0; i < data.numInstances(); i++) {
			// read all instances for a document
			InstanceShort current = data.instance(i);
			result[i]=innerProductNormalizeCur(w, current);
		}
		return result;
	}

	private static LeanDocument makeDocument(ModifiedInstancesShort data,
			int[] ptrs) {
		LeanDocument result = new LeanDocument();
		AttributeShort ID1 = data.m_ID1;
		AttributeShort ID2 = data.m_ID2;

		for (int k = 0; k < data.numInstances(); k++) {
			InstanceShort curPair = data.instance(k);
			int curId1 = curPair.value(ID1);
			int curId2 = curPair.value(ID2);
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
	
	private static double innerProductNormalizeCur(double[] w, InstanceShort i) {
		if (w.length != i.numAttributes())
			throw new RuntimeException("Wrong number of attributes wv:"+w.length+" vs "+i.numAttributes());
		double result = 0;
		double twoNorm = 0;
		for (int j = 0; j < i.numAttributes(); j++) {
			if (i.attribute(j).isFeature()) {
				if (i.attribute(j).isNominal()){
					result += i.value(j) * w[j];
					twoNorm += i.value(j)*i.value(j);
				}else{
					result += i.attribute(j).getOriginalValue(new Short(i.value(j)))
							* w[j];
					twoNorm += i.attribute(j).getOriginalValue(new Short(i.value(j)))
							*i.attribute(j).getOriginalValue(new Short(i.value(j)));
				}
			}
		}
		// System.err.println("VAL="+result);
		return result/Math.sqrt(twoNorm);
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

	public static LeanDocument makeDocument(ModifiedInstancesShort data) {
		if (data == null || data.numInstances() == 0)
			return null;
		LeanDocument result = new LeanDocument(data.instance(0).value(
				data.m_DOCNO));
		AttributeShort DOCID = data.m_DOCNO;
		AttributeShort ID1 = data.m_ID1;
		AttributeShort ID2 = data.m_ID2;
		AttributeShort Cl = data.classAttribute();
		short pos = data.m_Positive;

		int[] ptrs = null;
		if (data == null || data.numInstances() == 0)
			return null;
		InstanceShort first = data.instance(0);
		int id1 = first.value(ID1);
		int id2 = first.value(ID2);
		int len = id1 + 1;
		ptrs = new int[len];
		// initialize pointers so each item is in it's own set
		for (int j = 0; j < len; j++)
			ptrs[j] = j;

		if (first.value(Cl) == pos) {
			union(id1, id2, ptrs);
		}
		for (int i = 1; i < data.numInstances(); i++) {
			// read all instances for a document
			InstanceShort current = data.instance(i);
			// System.out.println("Working on: "+current);
			id1 = current.value(ID1);
			id2 = current.value(ID2);

			if (current.value(Cl) == pos) {
				union(id1, id2, ptrs);
			}
		}
		for (int k = 0; k < data.numInstances(); k++) {
			InstanceShort curPair = data.instance(k);
			int curId1 = curPair.value(ID1);
			int curId2 = curPair.value(ID2);
			if (!result.contains(new Integer(curId1))) {
				// System.err.println("Add "+curId1+"-"+find(curId1,ptrs));
				result.add(new Integer(curId1), new Integer(find(curId1, ptrs)));
			}
			if (!result.contains(new Integer(curId2)))
				result.add(new Integer(curId2), new Integer(find(curId2, ptrs)));
		}
		// System.err.print("[TC:"+startPositive+"("+startPositiveTotal+")->"
		// +endPositive+"("+endPositiveTotal+")]");
		// System.err.println(result);
		return result;
	}
}