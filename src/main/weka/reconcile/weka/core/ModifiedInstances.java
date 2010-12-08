/*
 * Created on Dec 8, 2005
 *
 */
package reconcile.weka.core;

import java.io.IOException;
import java.io.Reader;
import java.util.Enumeration;


/**
 * This class wraps around the Instances class. The problem is that
 * for this classifier we have three special attributes -- DOCNUM,
 * ID1 and ID2. This wrapper takes care of the extra attributes.
 *  
 * @author ves
 *
 */
public class ModifiedInstances extends Instances{
	//this parameter is used for processing convinience. Simply stores the
	//highest np_id found in the data
	private int maxId = -1;
	
	public Attribute m_ID1;
	public Attribute m_ID2;
	public Attribute m_DOCNO;
	public Attribute m_Prediction;
	public int m_Positive;
	public int m_Negative;
	/**
	 * @param dataset
	 */
	public ModifiedInstances(Instances dataset) {
		super(dataset);
		setUpSources();
	}
	/**
	 * @param dataset
	 * @param capacity
	 */
	public ModifiedInstances(Instances dataset, int capacity) {
		super(dataset, capacity);
		setUpSources();
	}
	/**
	 * @param source
	 * @param first
	 * @param toCopy
	 */
	public ModifiedInstances(Instances source, int first, int toCopy) {
	  super(source, first, toCopy);
	  setUpSources();
	}
	
	/**
	 * @param reader
	 * @throws IOException
	 */
	public ModifiedInstances(Reader reader) throws IOException {
		super(reader);
		setUpSources();
	}
	
	/*
	public ModifiedInstances(String header) throws IOException {
		super(header);
	}
	
	public ModifiedInstances(DataInputStream reader, DataInputStream classReader) throws IOException {
		super(reader, classReader);
		setUpSources();
	}
	*/
	
	/**
	 * @param reader
	 * @param capacity
	 * @throws IOException
	 */
	public ModifiedInstances(Reader reader, int capacity) throws IOException {
		super(reader, capacity);
		setUpSources();
	}
	/**
	 * @param name
	 * @param attInfo
	 * @param capacity
	 */
	public ModifiedInstances(String name, FastVector attInfo, int capacity) {
		super(name, attInfo, capacity);
	}
	
	public static ModifiedInstances[] splitDocs(ModifiedInstances all){
	  ModifiedInstances[] result = new ModifiedInstances[all.getNumDocuments()];
	  //System.err.println("docs "+all.getNumDocuments());
	  int currentDoc = -1;
	  int index = 0;
	  Attribute docnum = all.attribute("DOCNUM");
	  ModifiedInstances cur = null;
	  for(int i =0; i<all.numInstances(); i++){
	    Instance ins = all.instance(i);
	    
	    int  docNum = (int)ins.value(docnum);
	    if(currentDoc!=docNum){
	      //Set up a new document
	     // System.err.println("Addin doc "+docNum+" position "+index);
	      cur = new ModifiedInstances(all,0);
	      result[index++]= cur;
	      currentDoc = docNum;
	    }
	    cur.add(ins);
	  }
	  return result;
	}
	/**
	 * Set up the data so it is ready for the task. That involves three tasks:
	 * 1) Set the weights to non sources to 0
	 * 2) Set the number of documents
	 * 3) Set the maximum id
	 * 
	 * @return
	 */
	public void setUpSources(){
		Attribute source = attribute("SOURCES");
		m_DOCNO = attribute("DOCNUM");
		
		if(m_DOCNO==null){
		  System.err.println("Not a sources file");
		  return;
		}
		
		m_ID1 = attribute("ID1");
		m_ID2  = attribute("ID2");
		m_Positive = attribute("class").indexOfValue("+");
		m_Negative = attribute("class").indexOfValue("-");
		m_Prediction = attribute("predicted_class");
		int sourceInd = 0;
		
		if(source != null)
		  sourceInd = source.indexOfValue("S");
		
		int numDocs = 0;
		int curDoc = -1;
		
		for(int i=0; i<numInstances(); i++){
			Instance ins = instance(i);
			boolean isSource = (source==null)||(ins.value(source)==sourceInd);
			if(!isSource)
				ins.setWeight(0);
			int docNum = (int)ins.value(m_DOCNO);
			int id = (int)ins.value(m_ID2);
			if(docNum!=curDoc){
				numDocs++;
				curDoc = docNum;
			}
			if(id>maxId)
				maxId = id;
		}
		
		//System.err.println(numDocs+" documents");
		setNumDocuments(numDocs > 0 ? numDocs : 0);		
	}
	
	public void setInstWeights(Attribute Class, float ratio){
		int positive = Class.indexOfValue("T");
		
		for(int i=0; i<numInstances(); i++){
			Instance ins = instance(i);
			if(ins.weight()>0){
			  if((int)ins.value(Class)==positive)
			    ins.setWeight(ins.weight()*ratio);
			}
		}
	}

	public int getMaxId() {
		return maxId;
	}
	public void setMaxId(int maxId) {
		this.maxId = maxId;
	}
	
	public int getPositiveClass(){
	  return m_Positive;
	}
	
	public void setPositiveClass(int c){
		//System.err.println("Setting pos class to "+c);
	  m_Positive = c;
	}
	
	/* (non-Javadoc)
	 * @see weka.core.Instances#enumerateAttributes()
	 */
	public Enumeration enumerateAttributes() {
		FastVector res = (FastVector)m_Attributes.copy();
		Attribute DOCNUM = this.attribute("DOCNUM");
		//System.err.println("Setting docnum to "+m_DOCNUM);
		Attribute ID1 = this.attribute("ID1");
		Attribute ID2 = this.attribute("ID2");
		Attribute real_class = this.attribute("real_class");
		Attribute predicted_class = this.attribute("predicted_class");
		Attribute covered = this.attribute("covered");
		Attribute sources = this.attribute("SOURCES");
		//System.err.println(DOCNUM);
		//System.err.println("Removing element" + res.indexOf(DOCNUM));
		res.removeElementAt(res.indexOf(DOCNUM));
		res.removeElementAt(res.indexOf(ID1));
		res.removeElementAt(res.indexOf(ID2));
		res.removeElementAt(res.indexOf(real_class));
		res.removeElementAt(res.indexOf(predicted_class));
		res.removeElementAt(res.indexOf(covered));
		res.removeElementAt(res.indexOf(sources));
		//System.err.println(res);
		Attribute cl = this.attribute("class");
		return res.elements(res.indexOf(cl));
	}
	/* (non-Javadoc)
	 * @see weka.core.Instances#enumerateInstances()
	 */
	public Enumeration enumerateInstances() {
		// TODO Auto-generated method stub
		return super.enumerateInstances();
	}
	/* (non-Javadoc)
	 * @see weka.core.Instances#numAttributes()
	 */
	public int numAttributes() {
		return super.numAttributes();
	}
	
	public double[] getClassCounts(){
		int classValues = attribute(m_ClassIndex).numValues();
		double result[] = new double[classValues];
		for(int k=0; k<classValues; k++){
			result[k]=0;
		}
		for(int i = 0; i<this.numInstances(); i++){
			int j = (int)this.instance(i).value(m_ClassIndex);
			result[j]+= instance(i).weight();
		}
		return result;
	}
	
	/*
	 * Split the instances into two bags, the first containing numDocuments documents
	 * and the second containig the rest of the instances.
	 */
	public Instances[] copyDocInstances(int numDocuments){
		Instances result[]=new Instances[2];
		result[0] = new ModifiedInstances(this, numInstances());
		result[1] = new ModifiedInstances(this, numInstances());
		Attribute docID = attribute("DOCNUM");
		int documentNum = 0;
		int curDoc = -1;
		for(int i=0; i<numInstances();i++){
			Instance cur = instance(i);
			int docNum = (int)cur.value(docID);
			if(docNum!=curDoc){
				documentNum++;
				curDoc = docNum;
			}
			if(documentNum>numDocuments)
				result[1].add(cur);
			else
				result[0].add(cur);
		}
		return result;
	}

	/*
	 * Print the document order; for debugging purposes.
	 */
	public String printDocOrder(){
		String result = "";
		Attribute docID = attribute("DOCNUM");
		int curDoc = -1;
		for(int i=0; i<numInstances();i++){
			Instance cur = instance(i);
			int docNum = (int)cur.value(docID);
			if(docNum!=curDoc){
			  result+=docNum+", ";
			  curDoc = docNum;
			}
		}
		return result;
	}	
	/*
	 * Get the first document from the supplied Instance set and add it to
	 * the current object.
	 * 
	 */
	public int addDoc(Instances data, int startIndex){
		Attribute docID = attribute("DOCNUM");
		int curDoc = -1;
		boolean done = false;
		int index = 0;
		for(int i=startIndex; i<data.numInstances() && !done; i++){
			Instance cur = data.instance(i);
			int docNum = (int)cur.value(docID);
			if(docNum!=curDoc){
				if(curDoc==-1){
					curDoc=docNum;
					this.add(cur);
				}else{
					done = true;
					index = i;
				}
			}else{
				this.add(cur);
			}
		}
		return index;
	}
	
	//@Override
  /**
   * Adds one instance to the end of the set. 
   * WITHOUT Shallow copying instance before it is added. 
   * This is different from the add in the Instances class. Increases the
   * size of the dataset if it is not large enough. Does not
   * check if the instance is compatible with the dataset.
   *
   * @param instance the instance to be added
   */
  public void add(/*@non_null@*/ Instance instance) {
		Attribute docID = attribute("DOCNUM");
		int docNum = 0;
		if(docID!=null)
		  docNum = (int)instance.value(docID);
		m_NumDocuments = m_NumDocuments >= docNum+1?m_NumDocuments:docNum+1;
		instance.setDataset(this);
		m_Instances.addElement(instance);
	}
	
  /**
   * Computes the sum of all the instances' weights.
   *
   * @return the sum of all the instances' weights as a double
   */
  public /*@pure@*/ double sumOfWeights() {
    
    double sum = 0;
    Attribute covered = attribute("covered");
    int positive = covered.indexOfValue("T");
    for (int i = 0; i < numInstances(); i++) {
    	double cov = instance(i).value(covered);
    	if(Double.isNaN(cov) || (int)cov!=positive)
    		sum += instance(i).weight();
    }
    return sum;
  }
  
  /**
   * Removes an instance at the given position from the set.
   * Should not happen in this implementaion.
   * @param index the instance's position
   */
  //@ requires 0 <= index && index < numInstances();
  public void delete(int index) {
    
    throw new RuntimeException("Deleting instances");
  }
	

}

