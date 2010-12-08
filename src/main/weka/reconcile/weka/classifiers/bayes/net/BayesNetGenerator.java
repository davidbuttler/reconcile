/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * BayesNet.java
 * Copyright (C) 2003 Remco Bouckaert
 * 
 */

package reconcile.weka.classifiers.bayes.net;


import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import reconcile.weka.classifiers.bayes.BayesNet;
import reconcile.weka.classifiers.bayes.net.estimate.DiscreteEstimatorBayes;
import reconcile.weka.core.Attribute;
import reconcile.weka.core.FastVector;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Option;
import reconcile.weka.core.OptionHandler;
import reconcile.weka.core.Utils;
import reconcile.weka.estimators.Estimator;


/**
 * BayesNetGenerator offers facilities for generating random
 * Bayes networks and random instances based on a Bayes network.
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.1 $
 */
public class BayesNetGenerator extends BayesNet {
	int m_nSeed = 1;
    Random random;

	/**
	 * Constructor for BayesNetGenerator.
	 */
	public BayesNetGenerator() {
		super();
	} // c'tor

	/* Generate random connected Bayesian network with discrete nodes
	 * having all the same cardinality.
	 * @param nNodes: number of nodes in the Bayes net to generate
	 * @param nValues: number of values each of the nodes can take
	 * @param nArcs: number of arcs to generate. Must be between nNodes - 1 and nNodes * (nNodes-1) / 2
	 * */
	public void generateRandomNetwork () throws Exception {
		if (m_otherBayesNet == null) {
			// generate from scratch
			Init(m_nNrOfNodes, m_nCardinality);
			generateRandomNetworkStructure(m_nNrOfNodes, m_nNrOfArcs);
			generateRandomDistributions(m_nNrOfNodes, m_nCardinality);
		} else {
			// read from file, just copy parent sets and distributions
			m_nNrOfNodes = m_otherBayesNet.getNrOfNodes();
			m_ParentSets = m_otherBayesNet.getParentSets();
			m_Distributions = m_otherBayesNet.getDistributions();


			random = new Random(m_nSeed);
			// initialize m_Instances
			FastVector attInfo = new FastVector(m_nNrOfNodes);
			// generate value strings

			for (int iNode = 0; iNode < m_nNrOfNodes; iNode++) {
				int nValues = m_otherBayesNet.getCardinality(iNode);
				FastVector nomStrings = new FastVector(nValues + 1);
				for (int iValue = 0; iValue < nValues; iValue++) {
					nomStrings.addElement(m_otherBayesNet.getNodeValue(iNode, iValue));
				}
				Attribute att = new Attribute(m_otherBayesNet.getNodeName(iNode), nomStrings);
				attInfo.addElement(att);
			}

			m_Instances = new Instances(m_otherBayesNet.getName(), attInfo, 100);
			m_Instances.setClassIndex(m_nNrOfNodes - 1);
		}
	} // GenerateRandomNetwork

	/* Init defines a minimal Bayes net with no arcs
	 * @param nNodes: number of nodes in the Bayes net 
	 * @param nValues: number of values each of the nodes can take
	 */
	public void Init(int nNodes, int nValues) throws Exception {
		random = new Random(m_nSeed);
		// initialize structure
		FastVector attInfo = new FastVector(nNodes);
		// generate value strings
        FastVector nomStrings = new FastVector(nValues + 1);
        for (int iValue = 0; iValue < nValues; iValue++) {
			nomStrings.addElement("Value" + (iValue + 1));
        }

		for (int iNode = 0; iNode < nNodes; iNode++) {
			Attribute att = new Attribute("Node" + (iNode + 1), nomStrings);
			attInfo.addElement(att);
		}
 		m_Instances = new Instances("RandomNet", attInfo, 100);
 		m_Instances.setClassIndex(nNodes - 1);
		setUseADTree(false);
// 		m_bInitAsNaiveBayes = false;
// 		m_bMarkovBlanketClassifier = false;
		initStructure();
		
		// initialize conditional distribution tables
		m_Distributions = new Estimator[nNodes][1];
		for (int iNode = 0; iNode < nNodes; iNode++) {
			m_Distributions[iNode][0] = 
			  new DiscreteEstimatorBayes(nValues, getEstimator().getAlpha());
		}
	} // DefineNodes

	/* GenerateRandomNetworkStructure generate random connected Bayesian network 
	 * @param nNodes: number of nodes in the Bayes net to generate
	 * @param nArcs: number of arcs to generate. Must be between nNodes - 1 and nNodes * (nNodes-1) / 2
	 */
	public void generateRandomNetworkStructure(int nNodes, int nArcs) 
		throws Exception
	{
		if (nArcs < nNodes - 1) {
			throw new Exception("Number of arcs should be at least (nNodes - 1) = " + (nNodes - 1) + " instead of " + nArcs);
		}
		if (nArcs > nNodes * (nNodes - 1) / 2) {
			throw new Exception("Number of arcs should be at most nNodes * (nNodes - 1) / 2 = "+ (nNodes * (nNodes - 1) / 2) + " instead of " + nArcs);
		}
		if (nArcs == 0) {return;} // deal with  patalogical case for nNodes = 1

	    // first generate tree connecting all nodes
	    generateTree(nNodes);
	    // The tree contains nNodes - 1 arcs, so there are 
	    // nArcs - (nNodes-1) to add at random.
	    // All arcs point from lower to higher ordered nodes
	    // so that acyclicity is ensured.
	    for (int iArc = nNodes - 1; iArc < nArcs; iArc++) {
	    	boolean bDone = false;
	    	while (!bDone) {
				int nNode1 = random.nextInt(nNodes);
				int nNode2 = random.nextInt(nNodes);
				if (nNode1 == nNode2) {nNode2 = (nNode1 + 1) % nNodes;}
				if (nNode2 < nNode1) {int h = nNode1; nNode1 = nNode2; nNode2 = h;}
				if (!m_ParentSets[nNode2].contains(nNode1)) {
					m_ParentSets[nNode2].addParent(nNode1, m_Instances);
					bDone = true;
				}
	    	}
	    }

	} // GenerateRandomNetworkStructure
	
	/* GenerateTree creates a tree-like network structure (actually a
	 * forest) by starting with a randomly selected pair of nodes, add 
	 * an arc between. Then keep on selecting one of the connected nodes 
	 * and one of the unconnected ones and add an arrow between them, 
	 * till all nodes are connected.
	 * @param nNodes: number of nodes in the Bayes net to generate
	 */
	void generateTree(int nNodes) {
        boolean [] bConnected = new boolean [nNodes];
        // start adding an arc at random
		int nNode1 = random.nextInt(nNodes);
		int nNode2 = random.nextInt(nNodes);
		if (nNode1 == nNode2) {nNode2 = (nNode1 + 1) % nNodes;}
		if (nNode2 < nNode1) {int h = nNode1; nNode1 = nNode2; nNode2 = h;}
		m_ParentSets[nNode2].addParent(nNode1, m_Instances);
		bConnected[nNode1] = true;
		bConnected[nNode2] = true;
		// Repeatedly, select one of the connected nodes, and one of 
		// the unconnected nodes and add an arc.
	    // All arcs point from lower to higher ordered nodes
	    // so that acyclicity is ensured.
		for (int iArc = 2; iArc < nNodes; iArc++ ) {
			int nNode = random.nextInt(nNodes);
			nNode1 = 0; //  one of the connected nodes
			while (nNode >= 0) {
				nNode1 = (nNode1 + 1) % nNodes;
				while (!bConnected[nNode1]) {
					nNode1 = (nNode1 + 1) % nNodes;
				}
				nNode--;
			}
			nNode = random.nextInt(nNodes);
			nNode2 = 0; //  one of the unconnected nodes
			while (nNode >= 0) {
				nNode2 = (nNode2 + 1) % nNodes;
				while (bConnected[nNode2]) {
					nNode2 = (nNode2 + 1) % nNodes;
				}
				nNode--;
			}
			if (nNode2 < nNode1) {int h = nNode1; nNode1 = nNode2; nNode2 = h;}
			m_ParentSets[nNode2].addParent(nNode1, m_Instances);
			bConnected[nNode1] = true;
			bConnected[nNode2] = true;
		}
	} // GenerateTree
	
	/* GenerateRandomDistributions generates discrete conditional distribution tables
	 * for all nodes of a Bayes network once a network structure has been determined.
	 * @param nNodes: number of nodes in the Bayes net 
	 * @param nValues: number of values each of the nodes can take
	 */
    void generateRandomDistributions(int nNodes, int nValues) {
	    // Reserve space for CPTs
    	int nMaxParentCardinality = 1;
	    for (int iAttribute = 0; iAttribute < nNodes; iAttribute++) {
            if (m_ParentSets[iAttribute].getCardinalityOfParents() > nMaxParentCardinality) {
	             nMaxParentCardinality = m_ParentSets[iAttribute].getCardinalityOfParents();
            } 
        } 

        // Reserve plenty of memory
        m_Distributions = new Estimator[m_Instances.numAttributes()][nMaxParentCardinality];

        // estimate CPTs
        for (int iAttribute = 0; iAttribute < nNodes; iAttribute++) {
        	int [] nPs = new int [nValues + 1];
        	nPs[0] = 0;
        	nPs[nValues] = 1000;
            for (int iParent = 0; iParent < m_ParentSets[iAttribute].getCardinalityOfParents(); iParent++) {
            	// fill array with random nr's
            	for (int iValue = 1; iValue < nValues; iValue++)  {
            		nPs[iValue] = random.nextInt(1000);
            	}
            	// sort
            	for (int iValue = 1; iValue < nValues; iValue++)  {
	            	for (int iValue2 = iValue + 1; iValue2 < nValues; iValue2++)  {
	            		if (nPs[iValue2] < nPs[iValue]) {
	            			int h = nPs[iValue2]; nPs[iValue2] = nPs[iValue]; nPs[iValue] = h;
	            		}
	            	}
            	}
            	// assign to probability tables
            	DiscreteEstimatorBayes d = new DiscreteEstimatorBayes(nValues, getEstimator().getAlpha());
            	for (int iValue = 0; iValue < nValues; iValue++)  {
            		d.addValue(iValue, nPs[iValue + 1] - nPs[iValue]);
            	}
	            m_Distributions[iAttribute][iParent] = d;
            } 
        } 
    } // GenerateRandomDistributions
    
	/* GenerateInstances generates random instances sampling from the
	 * distribution represented by the Bayes network structure. It assumes
	 * a Bayes network structure has been initialized
	 * @param nInstances: nr of isntances to generate
	 */
	public void generateInstances(){
		for (int iInstance = 0; iInstance < m_nNrOfInstances; iInstance++) {
		    int nNrOfAtts = m_Instances.numAttributes();
			Instance instance = new Instance(nNrOfAtts);
			instance.setDataset(m_Instances);
			for (int iAtt = 0; iAtt < nNrOfAtts; iAtt++) {

				double iCPT = 0;

				for (int iParent = 0; iParent < m_ParentSets[iAtt].getNrOfParents(); iParent++) {
				  int nParent = m_ParentSets[iAtt].getParent(iParent);
				  iCPT = iCPT * m_Instances.attribute(nParent).numValues() + instance.value(nParent);
				} 
	
				double fRandom = random.nextInt(1000) / 1000.0f;
				int iValue = 0;
				while (fRandom > m_Distributions[iAtt][(int) iCPT].getProbability(iValue)) {
					fRandom = fRandom - m_Distributions[iAtt][(int) iCPT].getProbability(iValue);
					iValue++ ;
				}
				instance.setValue(iAtt, iValue);
			}
			m_Instances.add(instance);
		}
	} // GenerateInstances
    
  	public String toString() {
		if (m_bGenerateNet) {
		   return toXMLBIF03();
		}
    	StringBuffer text = new StringBuffer();
    	return m_Instances.toString();
  	} // toString
  	

	boolean m_bGenerateNet = false;
	int m_nNrOfNodes = 10;
	int m_nNrOfArcs = 10;
	int m_nNrOfInstances = 10;
	int m_nCardinality = 2;
	String m_sBIFFile = "";

	void setNrOfNodes(int nNrOfNodes) {m_nNrOfNodes = nNrOfNodes;}
	void setNrOfArcs(int nNrOfArcs) {m_nNrOfArcs = nNrOfArcs;}
	void setNrOfInstances(int nNrOfInstances) {m_nNrOfInstances = nNrOfInstances;}
	void setCardinality(int nCardinality) {m_nCardinality = nCardinality;}
	void setSeed(int nSeed) {m_nSeed = nSeed;}

	/**
	 * Returns an enumeration describing the available options
	 * 
	 * @return an enumeration of all the available options
	 */
	public Enumeration listOptions() {
		Vector newVector = new Vector(6);

		newVector.addElement(new Option("\tGenerate network (instead of instances)\n", "B", 0, "-B"));
		newVector.addElement(new Option("\tNr of nodes\n", "N", 1, "-N <integer>"));
		newVector.addElement(new Option("\tNr of arcs\n", "A", 1, "-A <integer>"));
		newVector.addElement(new Option("\tNr of instances\n", "M", 1, "-M <integer>"));
		newVector.addElement(new Option("\tCardinality of the variables\n", "C", 1, "-C <integer>"));
		newVector.addElement(new Option("\tSeed for random number generator\n", "S", 1, "-S <integer>"));

		return newVector.elements();
	} // listOptions

	/**
	 * Parses a given list of options. Valid options are:<p>
	 * 
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		m_bGenerateNet = Utils.getFlag('B', options);

		String sNrOfNodes = Utils.getOption('N', options);
		if (sNrOfNodes.length() != 0) {
		  setNrOfNodes(Integer.parseInt(sNrOfNodes));
		} else {
			setNrOfNodes(10);
		} 

		String sNrOfArcs = Utils.getOption('A', options);
		if (sNrOfArcs.length() != 0) {
		  setNrOfArcs(Integer.parseInt(sNrOfArcs));
		} else {
			setNrOfArcs(10);
		} 

		String sNrOfInstances = Utils.getOption('M', options);
		if (sNrOfInstances.length() != 0) {
		  setNrOfInstances(Integer.parseInt(sNrOfInstances));
		} else {
			setNrOfInstances(10);
		} 

		String sCardinality = Utils.getOption('C', options);
		if (sCardinality.length() != 0) {
		  setCardinality(Integer.parseInt(sCardinality));
		} else {
			setCardinality(2);
		} 

		String sSeed = Utils.getOption('S', options);
		if (sSeed.length() != 0) {
		  setSeed(Integer.parseInt(sSeed));
		} else {
			setSeed(1);
		} 

		String sBIFFile = Utils.getOption('F', options);
		if ((sBIFFile != null) && (sBIFFile != "")) {
			setBIFFile(sBIFFile);
		}
	} // setOptions

	/**
	 * Gets the current settings of the classifier.
	 * 
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {
		String[] options = new String[13];
		int current = 0;
		if (m_bGenerateNet) {
		  options[current++] = "-B";
		} 

		options[current++] = "-N";
		options[current++] = "" + m_nNrOfNodes;

		options[current++] = "-A";
		options[current++] = "" + m_nNrOfArcs;

		options[current++] = "-M";
		options[current++] = "" + m_nNrOfInstances;

		options[current++] = "-C";
		options[current++] = "" + m_nCardinality;

		options[current++] = "-S";
		options[current++] = "" + m_nSeed;

                if (m_sBIFFile.length() != 0) {
                  options[current++] = "-F";
                  options[current++] = "" + m_sBIFFile;
                }

		// Fill up rest with empty strings, not nulls!
		while (current < options.length) {
			options[current++] = "";
		}

		return options;
	} // getOptions

    /**
     * prints all the options to stdout
     */
    protected static void printOptions(OptionHandler o) {
      Enumeration enm = o.listOptions();
      
      System.out.println("Options for " + o.getClass().getName() + ":\n");
      
      while (enm.hasMoreElements()) {
        Option option = (Option) enm.nextElement();
        System.out.println(option.synopsis());
        System.out.println(option.description());
      }
    }

    static public void main(String [] Argv) {
		BayesNetGenerator b = new BayesNetGenerator();
    	try {
		if ( (Argv.length == 0) || (Utils.getFlag('h', Argv)) ) {
                        printOptions(b);
			return;
		}
	    	b.setOptions(Argv);
	    	
	    	b.generateRandomNetwork();
	    	if (!b.m_bGenerateNet) { // skip if not required
				b.generateInstances();
	    	}
	    	System.out.println(b.toString());
    	} catch (Exception e) {
    		e.printStackTrace();
                printOptions(b);
    	}
    } // main
    
} // class BayesNetGenerator
