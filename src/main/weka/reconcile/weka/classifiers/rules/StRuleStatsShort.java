/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    RuleStats.java
 *    Copyright (C) 2001 Xin Xu
 */

package reconcile.weka.classifiers.rules;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Random;

import reconcile.weka.core.AttributeShort;
import reconcile.weka.core.FastVector;
import reconcile.weka.core.InstanceShort;
import reconcile.weka.core.InstancesShort;
import reconcile.weka.core.ModifiedInstancesShort;
import reconcile.weka.core.Utils;


/**
 * This class implements the statistics functions used in the propositional rule
 * learner, from the simpler ones like count of true/false positive/negatives,
 * filter data based on the ruleset, etc. to the more sophisticated ones such as
 * MDL calculation and rule variants generation for each rule in the ruleset.
 * <p>
 * 
 * Obviously the statistics functions listed above need the specific data and
 * the specific ruleset, which are given in order to instantiate an object of
 * this class.
 * <p>
 * 
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
/**
 * @author ves
 * 
 */
public class StRuleStatsShort implements Serializable {

	/** The data on which the stats calculation is based */
	//private InstancesShort m_Data;

	/** The specific ruleset in question */
	private FastVector m_Ruleset;

	/** The simple stats of each rule */
	private FastVector m_SimpleStats;

	/** The set of instances filtered by the ruleset */
	// private FastVector m_Filtered;
	/**
	 * The total number of possible conditions that could appear in a rule
	 */
	private double m_Total;

	/** The redundancy factor in theory description length */
	private static double REDUNDANCY_FACTOR = 0.5;

	/** The theory weight in the MDL calculation */
	private double MDL_THEORY_WEIGHT = 1.0;

	/** The class distributions predicted by each rule */
	private FastVector m_Distributions;

	/** Whether or not to output debug information */
	private boolean m_Debug = false;

	/** The fp and fn weights **/
	private float m_FPCost = 1;
	private float m_FNCost = 1;
	
	/** Default constructor 
	public StRuleStatsShort() {
		//m_Data = null;
		m_Ruleset = null;
		m_SimpleStats = null;
		// m_Filtered = null;
		m_Distributions = null;
		m_Total = -1;
	}
	*/

	/** Constructor to set up the debug value */
	public StRuleStatsShort(boolean debug, float fpCost, float fnCost) {
		//m_Data = null;
		m_Ruleset = null;
		m_SimpleStats = null;
		// m_Filtered = null;
		m_Distributions = null;
		m_Total = -1;
		m_Debug = debug;
		m_FPCost = fpCost;
		m_FNCost = fnCost;
	}

	/**
	 * Constructor that provides ruleset and data
	 * 
	 * @param data
	 *          the data
	 * @param rules
	 *          the ruleset
	 *
	public StRuleStatsShort(InstancesShort data, FastVector rules) {
		this();
		//m_Data = data;
		m_Ruleset = rules;
	}
	*/

	/**
	 * Constructor that provides ruleset and data + sets the debug
	 * 
	 * @param data
	 *          the data
	 * @param rules
	 *          the ruleset
	 */
	public StRuleStatsShort(InstancesShort data, FastVector rules, boolean debug, float fpCost, float fnCost) {
		this(debug, fpCost, fnCost);
		//m_Data = data;
		m_Ruleset = rules;
	}

	/**
	 * Set the number of all conditions that could appear in a rule in this
	 * RuleStats object, if the number set is smaller than 0 (typically -1), then
	 * it calcualtes based on the data store
	 * 
	 * @param total
	 *          the set number
	 */
	public void setNumAllConds(double total) {
		if (total < 0)
			throw new RuntimeException("Number of all conditions < 0");
			//m_Total = numAllConditions(m_Data);
		else
			m_Total = total;
	}

	/**
	 * Set the data of the stats, overwriting the old one if any
	 * 
	 * @param data
	 *          the data to be set
	 */
//	public void setData(InstancesShort data) {
//		m_Data = data;
//	}

	/**
	 * Get the data of the stats
	 * 
	 * @return the data
	 */
//	public InstancesShort getData() {
//		return m_Data;
//	}

	/**
	 * Set the ruleset of the stats, overwriting the old one if any
	 * 
	 * @param rules
	 *          the set of rules to be set
	 */
	public void setRuleset(FastVector rules) {
		m_Ruleset = rules;
	}

	/**
	 * Get the ruleset of the stats
	 * 
	 * @return the set of rules
	 */
	public FastVector getRuleset() {
		return m_Ruleset;
	}

	/**
	 * Get the size of the ruleset in the stats
	 * 
	 * @return the size of ruleset
	 */
	public int getRulesetSize() {
		return m_Ruleset.size();
	}

	/**
	 * Get the simple stats of one rule, including 6 parameters: 0: coverage;
	 * 1:uncoverage; 2: true positive; 3: true negatives; 4: false positives; 5:
	 * false negatives
	 * 
	 * @param index
	 *          the index of the rule
	 * @return the stats
	 */
	public double[] getSimpleStats(int index) {
		if ((m_SimpleStats != null) && (index < m_SimpleStats.size()))
			return (double[]) m_SimpleStats.elementAt(index);

		return null;
	}

	/**
	 * Print the simple stats of one rule in readable format, including 6
	 * parameters: 0: coverage; 1:uncoverage; 2: true positive; 3: true negatives;
	 * 4: false positives; 5: false negatives
	 * 
	 * @param stats
	 *          the array containing the stats
	 * @return a string with the stats in readable form
	 */
	public static String printSimpleStats(double[] stats) {
		if (stats == null || stats.length != 6)
			throw new RuntimeException("Invalid format of the rule statistics");
		String result = "Cov|uncov:\t" + stats[0] + "|" + stats[1] + ".\n";
		result += "Positive (t|f):\t" + stats[2] + "|" + stats[4] + ".\n";
		result += "Negative (t|f):\t" + stats[3] + "|" + stats[5] + ".\n";

		return result;
	}
	
	/**
	 * Print the RIPPER stats of one rule in readable format, including 2
	 * parameters:  2: true positive and  4: false positives; 
	 * 
	 * @param stats
	 *          the array containing the stats
	 * @return a string with the stats in readable form
	 */
	public static String printRIPPERStats(double[] stats) {
		if (stats == null || stats.length != 6)
			throw new RuntimeException("Invalid format of the rule statistics");
		String result = " " + (int)stats[2] + " " + (int)stats[4];

		return result;
	}
	


	/**
	 * Get the data after filtering the given rule
	 * 
	 * @param index
	 *          the index of the rule
	 * @return the data covered and uncovered by the rule
	 */
	// public Instances[] getFiltered(int index) {
	//
	// if ((m_Filtered != null) && (index < m_Filtered.size()))
	// return (Instances[]) m_Filtered.elementAt(index);
	//
	// return null;
	// }
	/**
	 * Get the class distribution predicted by the rule in given position
	 * 
	 * @param index
	 *          the position index of the rule
	 * @return the class distributions
	 */
	public double[] getDistributions(int index) {

		if ((m_Distributions != null) && (index < m_Distributions.size()))
			return (double[]) m_Distributions.elementAt(index);

		return null;
	}

	/**
	 * Set the weight of theory in MDL calcualtion
	 * 
	 * @param weight
	 *          the weight to be set
	 */
	public void setMDLTheoryWeight(double weight) {
		MDL_THEORY_WEIGHT = weight;
	}

	/**
	 * Compute the number of all possible conditions that could appear in a rule
	 * of a given data. For nominal attributes, it's the number of values that
	 * could appear; for numeric attributes, it's the number of values * 2, i.e. <=
	 * and >= are counted as different possible conditions.
	 * 
	 * @param data
	 *          the given data
	 * @return number of all conditions of the data
	 */
	public static double numAllConditions(InstancesShort data) {
		double total = 0;
		Enumeration attEnum = data.enumerateAttributes();
		while (attEnum.hasMoreElements()) {
			AttributeShort att = (AttributeShort) attEnum.nextElement();
			if (att.isNominal())
				total += (double) att.numValues();
			else
				total += 2.0 * (double) data.numDistinctValues(att);
		}
		return total;
	}

	/**
	 * Filter the data according to the ruleset and compute the basic stats:
	 * coverage/uncoverage, true/false positive/negatives of each rule
	 */
	public void countData(InstancesShort data) {
		// if ((m_Filtered != null) || (m_Ruleset == null) || (m_Data == null))
		// return;
		if((m_Ruleset == null))// || (m_Data == null))
			return;

		int size = m_Ruleset.size();
		//m_Filtered = new FastVector(size);
		m_SimpleStats = new FastVector(size);
		m_Distributions = new FastVector(size);
		//InstancesShort data = m_Data;
		((StRipShort.RipperRule)m_Ruleset.elementAt(0)).clear(data,false);
		for (int i = 0; i < size; i++) {
			double[] stats = new double[6]; // 6 statistics parameters
			double[] classCounts = new double[data.classAttribute().numValues()];
			//Instances[] filtered = 
			((StRipShort.RipperRule)m_Ruleset.elementAt(i)).apply(m_Ruleset, data, i);
			computeSimpleStats(i, data, stats, classCounts);
			//m_Filtered.addElement(filtered);
			m_SimpleStats.addElement(stats);
			m_Distributions.addElement(classCounts);
			//data = filtered[1]; // Data not covered
		}
	}

	/**
	 * Count data from the position index in the ruleset assuming that given data
	 * are not covered by the rules in position 0...(index-1), and the statistics
	 * of these rules are provided. <br>
	 * This procedure is typically useful when a temporary object of RuleStats is
	 * constructed in order to efficiently calculate the relative DL of rule in
	 * position index, thus all other stuff is not needed.
	 * 
	 * @param index
	 *          the given position
	 * @param data
	 *          the data with the covered attribute set
	 * @param prevRuleStats
	 *          the provided stats of previous rules
	 */
	public void countData(int index, InstancesShort data, double[][] prevRuleStats) {
		if(m_Ruleset==null)
			return;

		int size = m_Ruleset.size();
		m_SimpleStats = new FastVector(size);

		for (int i = 0; i < index; i++) {
			m_SimpleStats.addElement(prevRuleStats[i]);
		}

		for (int j = index; j < size; j++) {
			double[] stats = new double[6]; // 6 statistics parameters
			((StRipShort.RipperRule)m_Ruleset.elementAt(j)).apply(m_Ruleset,data,j);
			computeSimpleStats(j, data, stats, null);
			m_SimpleStats.addElement(stats);
		}
	}

	/**
	 * Find all the instances in the dataset covered/not covered by the rule in
	 * given index, and the correponding simple statistics and predicted class
	 * distributions are stored in the given double array, which can be obtained
	 * by getSimpleStats() and getDistributions(). <br>
	 * 
	 * @param index
	 *          the given index, assuming correct
	 * @param insts
	 *          the dataset to be covered by the rule
	 * @param stats
	 *          the given double array to hold stats, side-effected
	 * @param dist
	 *          the given array to hold class distributions, side-effected if
	 *          null, the distribution is not necessary
	 * @return the number of covered instances by the rule
	 */
	private int computeSimpleStats(int index, InstancesShort insts, double[] stats,
			double[] dist) {
		StRipShort.RipperRule rule = (StRipShort.RipperRule) m_Ruleset.elementAt(index);
		AttributeShort predClass = insts.attribute("predicted_class");
		AttributeShort Covered = insts.attribute("covered");
		int pos = predClass.indexOfValue("+");

		// Instances[] data = new ModifiedInstances[2];
		// data[0] = new ModifiedInstances(insts, insts.numInstances());
		// data[1] = new ModifiedInstances(insts, insts.numInstances());

		StRipShort.RipperRule ripperRule = (StRipShort.RipperRule) m_Ruleset.elementAt(index);
		//ripperRule.clear(insts, false);
		ripperRule.apply(insts, true);
		StRipShort.transitiveClosure(insts, predClass);
		int previouslyCovered = 0;
		for (int i = 0; i < insts.numInstances(); i++) {
			InstanceShort datum = insts.instance(i);
			double weight = datum.weight();
			boolean covers = (int) datum.value(predClass) == pos;
			double cov = datum.value(Covered);
			boolean notPreviouslyCovered = Double.isNaN(cov) ||  (int) cov != pos;
			if (notPreviouslyCovered) {
				if (covers) {
					//System.err.println("Covered" + datum);
					// data[0].add(datum); // Covered by this rule
					stats[0] += weight; // Coverage
					if ((int) datum.classValue() == (int) rule.getConsequent())
						stats[2] += weight; // True positives
					else
						stats[4] += weight; // False positives
					if (dist != null)
						dist[(int) datum.classValue()] += weight;
				} else {
					//System.err.println("Covered" + datum);
					//data[1].add(datum); // Not covered by this rule
					stats[1] += weight;
					if ((int) datum.classValue() != (int) rule.getConsequent())
						stats[3] += weight; // True negatives
					else
						stats[5] += weight; // False negatives
				}
			}else
				previouslyCovered+=weight;
		}
		System.err.println("Compute stats for index "+index+": PrevCovered: "+previouslyCovered+
				" covered: "+(int)stats[0]+" uncovered: "+(int)stats[1]+". Total covered "+
				(int)(stats[0]+previouslyCovered));
		return (int)stats[0]; // data;
	}

	/**
	 * Add a rule to the ruleset and update the stats
	 * 
	 * @param the
	 *          rule to be added
	 */
	public void addAndUpdate(StRipShort.RipperRule lastRule, InstancesShort data) {
		if (m_Ruleset == null)
			m_Ruleset = new FastVector();
		m_Ruleset.addElement(lastRule);

		//InstancesShort data = m_Data;// (m_Filtered == null) ? m_Data : ((Instances[])
														// m_Filtered.lastElement())[1];
		double[] stats = new double[6];
		double[] classCounts = new double[data.classAttribute().numValues()];
		// Instances[] filtered =
		data = ((StRipShort.RipperRule)lastRule).apply(m_Ruleset,data,m_Ruleset.size()-1);
		int cover = computeSimpleStats(m_Ruleset.size() - 1, data, stats, classCounts);

		// if (m_Filtered == null)
		// m_Filtered = new FastVector();
		// m_Filtered.addElement(filtered);

		if (m_SimpleStats == null)
			m_SimpleStats = new FastVector();
		m_SimpleStats.addElement(stats);

		if (m_Distributions == null)
			m_Distributions = new FastVector();
		m_Distributions.addElement(classCounts);
		//if(m_Debug)
			System.err.println("Stats after add and update (covered "+cover+" insts) "+lastRule);
	}

	/**
	 * Subset description length: <br>
	 * S(t,k,p) = -k*log2(p)-(n-k)log2(1-p)
	 * 
	 * Details see Quilan: "MDL and categorical theories (Continued)",ML95
	 * 
	 * @param t
	 *          the number of elements in a known set
	 * @param k
	 *          the number of elements in a subset
	 * @param p
	 *          the expected proportion of subset known by recipient
	 */
	public static double subsetDL(double t, double k, double p) {
		double rt = Utils.gr(p, 0.0) ? (-k * Utils.log2(p)) : 0.0;
		rt -= (t - k) * Utils.log2(1 - p);
		return rt;
	}

	/**
	 * The description length of the theory for a given rule. Computed as: <br>
	 * 0.5* [||k||+ S(t, k, k/t)] <br>
	 * where k is the number of antecedents of the rule; t is the total possible
	 * antecedents that could appear in a rule; ||K|| is the universal prior for k ,
	 * log2*(k) and S(t,k,p) = -k*log2(p)-(n-k)log2(1-p) is the subset encoding
	 * length.
	 * <p>
	 * 
	 * Details see Quilan: "MDL and categorical theories (Continued)",ML95
	 * 
	 * @param index
	 *          the index of the given rule (assuming correct)
	 * @exception if
	 *              index out of range or object not initialized yet
	 * @return the theory DL, weighted if weight != 1.0
	 */
	public double theoryDL(int index) {

		double k = ((StRipShort.RipperRule) m_Ruleset.elementAt(index)).size();

		if (k == 0)
			return 0.0;

		double tdl = Utils.log2(k);
		if (k > 1) // Approximation
			tdl += 2.0 * Utils.log2(tdl); // of log2 star
		tdl += subsetDL(m_Total, k, k / m_Total);
		// System.out.println("!!!theory: "+MDL_THEORY_WEIGHT *
		// REDUNDANCY_FACTOR * tdl);
		return MDL_THEORY_WEIGHT * REDUNDANCY_FACTOR * tdl;
	}

	/**
	 * The description length of data given the parameters of the data based on
	 * the ruleset.
	 * <p>
	 * Details see Quinlan: "MDL and categorical theories (Continued)",ML95
	 * <p>
	 * 
	 * @param expFPOverErr
	 *          expected FP/(FP+FN)
	 * @param cover
	 *          coverage
	 * @param uncover
	 *          uncoverage
	 * @param fp
	 *          False Positive
	 * @param fn
	 *          False Negative
	 */
	public double dataDL(double expFPOverErr, double cover,
			double uncover, double fp, double fn) {
		double totalBits = Utils.log2(cover + uncover + 1.0); // how many data?
		double coverBits, uncoverBits; // What's the error?
		double expErr; // Expected FP or FN

		if (Utils.gr(cover, uncover)) {
			expErr = expFPOverErr * (m_FPCost*fp + fn*m_FNCost);
			coverBits = subsetDL(cover, m_FPCost*fp, expErr / cover);
			uncoverBits = Utils.gr(uncover, 0.0) ? subsetDL(uncover, m_FNCost*fn, fn / uncover)
					: 0.0;
		} else {
			expErr = (1.0 - expFPOverErr) * (m_FPCost*fp + fn*m_FNCost);
			coverBits = Utils.gr(cover, 0.0) ? subsetDL(cover, fp*m_FPCost, fp / cover) : 0.0;
			uncoverBits = subsetDL(uncover, fn*m_FNCost, expErr / uncover);
		}

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		System.err.println("DataDL - cover: " + nf.format(cover) + "|uncov" + nf.format(uncover)
				+ "|coverBits: " + nf.format(coverBits) + "|uncBits: " + nf.format(uncoverBits) + "|FPRate:"
				+ nf.format(expFPOverErr) + "|expErr: " + nf.format(expErr) + "|fp: " + nf.format(fp) + "|fn: " + nf.format(fn)
				+ "|total:" + nf.format(totalBits));
		 
		return (totalBits + coverBits + uncoverBits);
	}

	/**
	 * Calculate the potential to decrease DL of the ruleset, i.e. the possible DL
	 * that could be decreased by deleting the rule whose index and simple
	 * statstics are given. If there's no potentials (i.e. smOrEq 0 && error rate <
	 * 0.5), it returns NaN.
	 * <p>
	 * 
	 * The way this procedure does is copied from original RIPPER implementation
	 * and is quite bizzare because it does not update the following rules' stats
	 * recursively any more when testing each rule, which means it assumes after
	 * deletion no data covered by the following rules (or regards the deleted
	 * rule as the last rule). Reasonable assumption?
	 * <p>
	 * 
	 * @param index
	 *          the index of the rule in m_Ruleset to be deleted
	 * @param expFPOverErr
	 *          expected FP/(FP+FN)
	 * @param rulesetStat
	 *          the simple statistics of the ruleset, updated if the rule should
	 *          be deleted
	 * @param ruleStat
	 *          the simple statistics of the rule to be deleted
	 * @param checkErr
	 *          whether check if error rate >= 0.5
	 * @return the potential DL that could be decreased
	 */
	public double potential(int index, double expFPOverErr, double[] rulesetStat,
			double[] ruleStat, boolean checkErr) {
		m_Debug = true;
		if (m_Debug) {
			System.err.println("From potential: "
					+ ((StRipShort.RipperRule) m_Ruleset.elementAt(index)).toString(null));
			System.err.print("Total ruleset: " + printSimpleStats(rulesetStat));
			System.err.println("Rule: " + printSimpleStats(ruleStat));
		}
		// Restore the stats if deleted
		double pcov = rulesetStat[0] - ruleStat[0];
		double puncov = rulesetStat[1] + ruleStat[0];
		double pfp = rulesetStat[4] - ruleStat[4];
		double pfn = rulesetStat[5] + ruleStat[2];

		double dataDLWith = dataDL(expFPOverErr, rulesetStat[0], rulesetStat[1],
				rulesetStat[4], rulesetStat[5]);
		double theoryDLWith = theoryDL(index);
		double dataDLWithout = dataDL(expFPOverErr, pcov, puncov, pfp, pfn);

		double potential = dataDLWith + theoryDLWith - dataDLWithout;
		double err = ruleStat[4] / ruleStat[0];

		if (m_Debug)
			System.err.println("Potential = "+potential+ "based on dataDLWith = " 
					+ dataDLWith + " | theory = " + theoryDLWith + " | dataDLWithout = "
					+ dataDLWithout + "| Positives (t/f) " + ruleStat[0] + " / " + ruleStat[4]);

		boolean overErr = Utils.grOrEq(err, 0.5);
		if (!checkErr)
			overErr = false;

		if (Utils.grOrEq(potential, 0.0) || overErr) {
			// If deleted, update ruleset stats. Other stats do not matter
			rulesetStat[0] = pcov;
			rulesetStat[1] = puncov;
			rulesetStat[4] = pfp;
			rulesetStat[5] = pfn;
			return potential;
		} else
			return Double.NaN;
	}

	/**
	 * Compute the minimal data description length of the ruleset if the rule in
	 * the given position is deleted. <br>
	 * The min_data_DL_if_deleted = data_DL_if_deleted - potential
	 * 
	 * @param index
	 *          the index of the rule in question
	 * @param expFPRate
	 *          expected FP/(FP+FN), used in dataDL calculation
	 * @param checkErr
	 *          whether check if error rate >= 0.5
	 * @param return
	 *          the minDataDL
	 */
	public double minDataDLIfDeleted(InstancesShort data, int index, double expFPRate, boolean checkErr) {
		// System.out.println("!!!Enter without: ");
		double[] rulesetStat = new double[6]; // Stats of ruleset if deleted
		int more = m_Ruleset.size() - 1 - index; // How many rules after?
		FastVector indexPlus = new FastVector(more); // Their stats

		// 0...(index-1) are OK
		for (int j = 0; j < index; j++) {
			// Covered stats are cumulative
			rulesetStat[0] += ((double[]) m_SimpleStats.elementAt(j))[0];
			rulesetStat[2] += ((double[]) m_SimpleStats.elementAt(j))[2];
			rulesetStat[4] += ((double[]) m_SimpleStats.elementAt(j))[4];
		}

		// Recount data from index+1
		//InstancesShort data = (index == 0) ? m_Data : ((Instances[]) m_Filtered
				//.elementAt(index - 1))[1];
		//InstancesShort data = m_Data;
		// System.out.println("!!!without: " + data.sumOfWeights());
		((StRipShort.RipperRule)m_Ruleset.elementAt(index)).apply(m_Ruleset,data,index);
		for (int j = (index + 1); j < m_Ruleset.size(); j++) {
			double[] stats = new double[6];
			//Instances[] split = 
			computeSimpleStats(j, data, stats, null);
			indexPlus.addElement(stats);
			rulesetStat[0] += stats[0];
			rulesetStat[2] += stats[2];
			rulesetStat[4] += stats[4];
			((StRipShort.RipperRule)m_Ruleset.elementAt(index)).applyWhileSkipping(m_Ruleset,data,index,j+1);
		}
		// Uncovered stats are those of the last rule
		if (more > 0) {
			rulesetStat[1] = ((double[]) indexPlus.lastElement())[1];
			rulesetStat[3] = ((double[]) indexPlus.lastElement())[3];
			rulesetStat[5] = ((double[]) indexPlus.lastElement())[5];
		} else if (index > 0) {
			rulesetStat[1] = ((double[]) m_SimpleStats.elementAt(index - 1))[1];
			rulesetStat[3] = ((double[]) m_SimpleStats.elementAt(index - 1))[3];
			rulesetStat[5] = ((double[]) m_SimpleStats.elementAt(index - 1))[5];
		} else { // Null coverage
			rulesetStat[1] = ((double[]) m_SimpleStats.elementAt(0))[0]
					+ ((double[]) m_SimpleStats.elementAt(0))[1];
			rulesetStat[3] = ((double[]) m_SimpleStats.elementAt(0))[3]
					+ ((double[]) m_SimpleStats.elementAt(0))[4];
			rulesetStat[5] = ((double[]) m_SimpleStats.elementAt(0))[2]
					+ ((double[]) m_SimpleStats.elementAt(0))[5];
		}

		// Potential
		double potential = 0;
		for (int k = index + 1; k < m_Ruleset.size(); k++) {
			double[] ruleStat = (double[]) indexPlus.elementAt(k - index - 1);
			double ifDeleted = potential(k, expFPRate, rulesetStat, ruleStat,
					checkErr);
			if (!Double.isNaN(ifDeleted))
				potential += ifDeleted;
		}

		// Data DL of the ruleset without the rule
		// Note that ruleset stats has already been updated to reflect
		// deletion if any potential
		double dataDLWithout = dataDL(expFPRate, rulesetStat[0], rulesetStat[1],
				rulesetStat[4], rulesetStat[5]);
		System.out.println("MinDL if deleted without: "+dataDLWithout + " |potential: "+
		 potential);
		// Why subtract potential again? To reflect change of theory DL??
		return (dataDLWithout - potential);
	}

	/**
	 * Compute the minimal data description length of the ruleset if the rule in
	 * the given position is NOT deleted. <br>
	 * The min_data_DL_if_n_deleted = data_DL_if_n_deleted - potential
	 * 
	 * @param index
	 *          the index of the rule in question
	 * @param expFPRate
	 *          expected FP/(FP+FN), used in dataDL calculation
	 * @param checkErr
	 *          whether check if error rate >= 0.5
	 * @param return
	 *          the minDataDL
	 */
	public double minDataDLIfExists(int index, double expFPRate, boolean checkErr) {
		// System.out.println("!!!Enter with: ");
		double[] rulesetStat = new double[6]; // Stats of ruleset if rule exists
		for (int j = 0; j < m_SimpleStats.size(); j++) {
			// Covered stats are cumulative
			rulesetStat[0] += ((double[]) m_SimpleStats.elementAt(j))[0];
			rulesetStat[2] += ((double[]) m_SimpleStats.elementAt(j))[2];
			rulesetStat[4] += ((double[]) m_SimpleStats.elementAt(j))[4];
			if (j == m_SimpleStats.size() - 1) { // Last rule
				rulesetStat[1] = ((double[]) m_SimpleStats.elementAt(j))[1];
				rulesetStat[3] = ((double[]) m_SimpleStats.elementAt(j))[3];
				rulesetStat[5] = ((double[]) m_SimpleStats.elementAt(j))[5];
			}
		}

		// Potential
		double potential = 0;
		for (int k = index + 1; k < m_SimpleStats.size(); k++) {
			double[] ruleStat = (double[]) getSimpleStats(k);
			double ifDeleted = potential(k, expFPRate, rulesetStat, ruleStat,
					checkErr);
			if (!Double.isNaN(ifDeleted))
				potential += ifDeleted;
		}

		// Data DL of the ruleset without the rule
		// Note that ruleset stats has already been updated to reflect deletion
		// if any potential
		double dataDLWith = dataDL(expFPRate, rulesetStat[0], rulesetStat[1],
				rulesetStat[4], rulesetStat[5]);
		System.out.println("minDataDLIfExists:"+this+"dataDLWith: "+dataDLWith + " |potential: "+
		 potential);
		return (dataDLWith - potential);
	}

	/**
	 * The description length (DL) of the ruleset relative to if the rule in the
	 * given position is deleted, which is obtained by: <br>
	 * MDL if the rule exists - MDL if the rule does not exist <br>
	 * Note the minimal possible DL of the ruleset is calculated(i.e. some other
	 * rules may also be deleted) instead of the DL of the current ruleset.
	 * <p>
	 * 
	 * @param index
	 *          the given position of the rule in question (assuming correct)
	 * @param expFPRate
	 *          expected FP/(FP+FN), used in dataDL calculation
	 * @param checkErr
	 *          whether check if error rate >= 0.5
	 * @return the relative DL
	 */
	public double relativeDL(InstancesShort data, int index, double expFPRate, boolean checkErr) {
		double with = minDataDLIfExists(index, expFPRate, checkErr);
		double theory = theoryDL(index);
		double without = minDataDLIfDeleted(data, index, expFPRate, checkErr);
		double result = (with + theory - without);
		System.err.println("Relative dl = " + result + "="+with+"+"+theory+"-"+without);
		return result;
	}

	/**
	 * Try to reduce the DL of the ruleset by testing removing the rules one by
	 * one in reverse order and update all the stats
	 * 
	 * @param expFPRate
	 *          expected FP/(FP+FN), used in dataDL calculation
	 * @param checkErr
	 *          whether check if error rate >= 0.5
	 */
	public void reduceDL(InstancesShort data, double expFPRate, boolean checkErr) {

		boolean needUpdate = false;
		double[] rulesetStat = new double[6];
		for (int j = 0; j < m_SimpleStats.size(); j++) {
			// Covered stats are cumulative
			rulesetStat[0] += ((double[]) m_SimpleStats.elementAt(j))[0];
			rulesetStat[2] += ((double[]) m_SimpleStats.elementAt(j))[2];
			rulesetStat[4] += ((double[]) m_SimpleStats.elementAt(j))[4];
			if (j == m_SimpleStats.size() - 1) { // Last rule
				rulesetStat[1] = ((double[]) m_SimpleStats.elementAt(j))[1];
				rulesetStat[3] = ((double[]) m_SimpleStats.elementAt(j))[3];
				rulesetStat[5] = ((double[]) m_SimpleStats.elementAt(j))[5];
			}
		}

		// Potential
		double potential = 0;
		for (int k = m_SimpleStats.size() - 1; k >= 0; k--) {

			double[] ruleStat = (double[]) m_SimpleStats.elementAt(k);

			// rulesetStat updated
			double ifDeleted = potential(k, expFPRate, rulesetStat, ruleStat,
					checkErr);
			if (!Double.isNaN(ifDeleted)) {
				if (m_Debug || true)
					System.err.println("!!!deleted (" + k + "): save " + ifDeleted
							+ " | " + rulesetStat[0] + " | " + rulesetStat[1] + " | "
							+ rulesetStat[4] + " | " + rulesetStat[5]);

				if (k == (m_SimpleStats.size() - 1))
					removeLast();
				else {
					m_Ruleset.removeElementAt(k);
					needUpdate = true;
				}
			}
		}

		if (needUpdate) {
			//m_Filtered = null;
			m_SimpleStats = null;
			countData(data);
		}
	}

	/**
	 * Remove the last rule in the ruleset as well as it's stats. It might be
	 * useful when the last rule was added for testing purpose and then the test
	 * failed
	 */
	public void removeLast() {
		int last = m_Ruleset.size() - 1;
		m_Ruleset.removeElementAt(last);
		// m_Filtered.removeElementAt(last);
		m_SimpleStats.removeElementAt(last);
		if (m_Distributions != null)
			m_Distributions.removeElementAt(last);
	}

	/**
	 * Static utility function to count the data covered by the rules after the
	 * given index in the given rules, and then remove them. It returns the data
	 * not covered by the successive rules.
	 * 
	 * @param data
	 *          the data to be processed
	 * @param rules
	 *          the ruleset
	 * @param index
	 *          the given index
	 * @return the data after processing
	 */
	public static InstancesShort rmCoveredBySuccessives(InstancesShort data,
			FastVector rules, int index) {
		InstancesShort rt = new ModifiedInstancesShort(data, 0);

		for (int i = 0; i < data.numInstances(); i++) {
			InstanceShort datum = data.instance(i);
			boolean covered = false;

			for (int j = index + 1; j < rules.size(); j++) {
			  StRipShort.RipperRule rule = (StRipShort.RipperRule) rules.elementAt(j);
				if (rule.covers(datum)) {
					covered = true;
					break;
				}
			}

			if (!covered)
				rt.add(datum);
		}
		return rt;
	}

	/**
	 * Stratify the given data into the given number of bags based on the class
	 * values. It differs from the <code>Instances.stratify(int fold)</code>
	 * that before stratification it sorts the instances according to the class
	 * order in the header file. It assumes no missing values in the class.
	 * 
	 * @param data
	 *          the given data
	 * @param folds
	 *          the given number of folds
	 * @param rand
	 *          the random object used to randomize the instances
	 * @return the stratified instances
	 */
	public static final InstancesShort stratify(InstancesShort data, int folds, Random rand) {
		if (!data.classAttribute().isNominal())
			return data;

		InstancesShort result = new ModifiedInstancesShort(data, 0);
		InstancesShort[] bagsByClasses = new ModifiedInstancesShort[data.numClasses()];

		for (int i = 0; i < bagsByClasses.length; i++)
			bagsByClasses[i] = new ModifiedInstancesShort(data, 0);

		// Sort by class
		for (int j = 0; j < data.numInstances(); j++) {
			InstanceShort datum = data.instance(j);
			bagsByClasses[(int) datum.classValue()].add(datum);
		}

		// Randomize each class
		for (int j = 0; j < bagsByClasses.length; j++)
			bagsByClasses[j].randomize(rand);

		for (int k = 0; k < folds; k++) {
			int offset = k, bag = 0;
			oneFold: while (true) {
				while (offset >= bagsByClasses[bag].numInstances()) {
					offset -= bagsByClasses[bag].numInstances();
					if (++bag >= bagsByClasses.length)// Next bag
						break oneFold;
				}

				result.add(bagsByClasses[bag].instance(offset));
				offset += folds;
			}
		}

		return result;
	}

	/**
	 * Randomize the given data into the given number of bags while keeping
	 * instances that belong to the same document togther.
	 * 
	 * @param data
	 *          the given data
	 * @param folds
	 *          the given number of folds
	 * @param rand
	 *          the random object used to randomize the instances
	 * @return the stratified instances
	 */
	public static final InstancesShort randomizeByDoc(InstancesShort data, int folds,
			Random rand) {
		if (!data.classAttribute().isNominal())
			return data;

		int numDocs = data.getNumDocuments();

		// System.err.println("Randomizing " + numDocs + data.toString(false));
		// Need to split "numDocs" documents into "folds" number of folds
		int docsPerFold = (numDocs + (folds - 1)) / folds; // note hack to ensure
																												// rounding
		docsPerFold = docsPerFold > 0 ? docsPerFold : 1;
		int[] index = new int[numDocs];

		for (int i = 0; i < numDocs; i++) {
			int foldNum = i / docsPerFold;
			index[i] = foldNum;
		}
		// now randomize the assignments
		for (int j = numDocs - 1; j > 0; j--)
			swap(index, j, rand.nextInt(j + 1));

		InstancesShort result = new ModifiedInstancesShort(data, 0);
		InstancesShort[] bagsByDocument = new ModifiedInstancesShort[folds];

		for (int i = 0; i < folds; i++)
			bagsByDocument[i] = new ModifiedInstancesShort(data, 0);

		int startNextDoc = 0;
		for (int i = 0; i < numDocs; i++) {
			startNextDoc = ((ModifiedInstancesShort) bagsByDocument[index[i]]).addDoc(
					data, startNextDoc);
		}

		for (int k = 0; k < folds; k++) {
			for (int j = 0; j < bagsByDocument[k].numInstances(); j++)
				result.add(bagsByDocument[k].instance(j));
		}
		//System.err.println("Randomized: " + ((ModifiedInstancesShort)result).printDocOrder());
		return result;
	}
	
	/**
	 * Randomize and split the given data while keeping
	 * instances that belong to the same document togther. Partitions the 
	 * data into 2, first of which has (numFolds-1)/numFolds of the
	 * data and the second has 1/numFolds of the data
	 * 
	 * 
	 * @param data
	 *          the given data
	 * @param folds
	 *          the given number of folds
	 * @param rand
	 *          the random object used to randomize the instances
	 * @return the stratified instances
	 */
	public static final InstancesShort[] randomizeAndPartitionByDoc(InstancesShort data, int folds,
			Random rand) {

		int numDocs = data.getNumDocuments();

		// System.err.println("Randomizing " + numDocs + data.toString(false));
		// Need to split "numDocs" documents into "folds" number of folds
		int docsPerFold = (numDocs + (folds - 1)) / folds; // note hack to ensure preper rounding

		docsPerFold = docsPerFold > 0 ? docsPerFold : 1;
		int[] index = new int[numDocs];

		//Randomly select one fold worth of documents 
		for (int i = 0; i < numDocs; i++) {
		  index[i] = 0;
		}
		int selected = 0;
		while(selected<docsPerFold){
		  int selection = rand.nextInt(numDocs-1);
		  if(index[selection]==0){
		    selected++;
		    index[selection]=1;
		  }
		}
		//for (int i = 0; i < numDocs; i++) {
		//	int foldNum = i / docsPerFold;
		//	index[i] = foldNum<(folds-1)?0:1;//The first numFolds - 1 folds go in the first part
		//}
		// now randomize the assignments
		//for (int j = numDocs - 1; j > 0; j--)
		//	swap(index, j, rand.nextInt(j + 1));

		ModifiedInstancesShort[] result = new ModifiedInstancesShort[2];
		result[0] = new ModifiedInstancesShort(data, 0);
		result[1] = new ModifiedInstancesShort(data, 0);
		//InstancesShort[] bagsByDocument = new ModifiedInstancesShort[folds];

		//for (int i = 0; i < folds; i++)
		//	bagsByDocument[i] = new ModifiedInstancesShort(data, 0);

		int startNextDoc = 0;
		for (int i = 0; i < numDocs; i++) {
			startNextDoc = result[index[i]].addDoc(data, startNextDoc);
		}


		//System.err.println("Randomized: " + ((ModifiedInstancesShort)result[0]).printDocOrder());
		return result;
	}

	/**
	 * Swap two elements in an arrayy
	 * 
	 * @param ar
	 *          the array
	 * @param ind1
	 *          the index of the first element
	 * @param ind2
	 *          the index of the second element
	 */

	private static void swap(int[] ar, int ind1, int ind2) {
		int temp = ar[ind1];
		ar[ind1] = ar[ind2];
		ar[ind2] = temp;
	}

	/**
	 * Compute the combined DL of the ruleset in this class, i.e. theory DL and
	 * data DL. Note this procedure computes the combined DL according to the
	 * current status of the ruleset in this class
	 * 
	 * @param expFPRate
	 *          expected FP/(FP+FN), used in dataDL calculation
	 * @param predicted
	 *          the default classification if ruleset covers null
	 * @return the combined class
	 */
	public double combinedDL(InstancesShort data, double expFPRate, double predicted) {
		double rt = 0;

		if (getRulesetSize() > 0) {
			double[] stats = (double[]) m_SimpleStats.lastElement();
			for (int j = getRulesetSize() - 2; j >= 0; j--) {
				stats[0] += getSimpleStats(j)[0];
				stats[2] += getSimpleStats(j)[2];
				stats[4] += getSimpleStats(j)[4];
			}
			rt += dataDL(expFPRate, stats[0], stats[1], stats[4], stats[5]); // Data
			// DL
		} else { // Null coverage ruleset
			double fn = 0.0;
			for (int j = 0; j < data.numInstances(); j++)
				if ((int) data.instance(j).classValue() == (int) predicted)
					fn += data.instance(j).weight();
			rt += dataDL(expFPRate, 0.0, data.sumOfWeights(), 0.0, fn);
		}

		for (int i = 0; i < getRulesetSize(); i++)
			// Theory DL
			rt += theoryDL(i);

		return rt;
	}

	/**
	 * Patition the data into 2, first of which has (numFolds-1)/numFolds of the
	 * data and the second has 1/numFolds of the data
	 * 
	 * 
	 * @param data
	 *          the given data
	 * @param numFolds
	 *          the given number of folds
	 * @return the patitioned instances
	 */
	public static final InstancesShort[] partition(InstancesShort data, int numFolds) {
		InstancesShort[] rt = new ModifiedInstancesShort[2];
		int splits = data.numInstances() * (numFolds - 1) / numFolds;

		rt[0] = new ModifiedInstancesShort(data, 0, splits);
		rt[1] = new ModifiedInstancesShort(data, splits, data.numInstances() - splits);

		return rt;
	}

	/**
	 * Same as the partition function, but all instances belonging to the same
	 * document are kept together.
	 * 
	 * 
	 * @param data
	 *          the given data
	 * @param numFolds
	 *          the given number of folds
	 * @return the patitioned instances
	 */
	public static final InstancesShort[] partitionByDocument(InstancesShort data,
			int numFolds) {
		InstancesShort[] rt = new ModifiedInstancesShort[2];
		int numDocuments = data.getNumDocuments();
		int splits = numDocuments * (numFolds - 1) / numFolds;

		rt = ((ModifiedInstancesShort) data).copyDocInstances(splits);

		return rt;
	}

	/*
	 * Return a string representation of the class
	 * 
	 */

	public String toString() {
		String result = "";
		for (int i = 0; i < m_SimpleStats.size(); i++) {
			result += ((StRipShort.RipperRule) m_Ruleset.elementAt(i)).toString() + "\n";
			result += printSimpleStats((double[]) m_SimpleStats.elementAt(i));
		}
		return result;
	}

	/*
	 * Return a string representation of the class in traditional RIPPER format
	 * 
	 */

	public String toRIPPERString() {
		String result = "";
		int i;
		for (i = 0; i < m_Ruleset.size(); i++) {
			StRipShort.RipperRule rule = ((StRipShort.RipperRule) m_Ruleset.elementAt(i));
			result += "+";
			result += printRIPPERStats((double[]) m_SimpleStats.elementAt(i));
			result += " IF " + rule.toRIPPERString() + "\n";
		}
		int ind = i > 0? i-1:0;
		double[] lastRuleStats = ((double[]) m_SimpleStats.elementAt(ind));
		result += "- " + (int)lastRuleStats[3] + " " + (int)lastRuleStats[5] + " IF .";
		return result;
	}
	

}