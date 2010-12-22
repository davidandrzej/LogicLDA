package logiclda.infer;


import java.util.*;
import java.io.*;

import logiclda.Corpus;
import logiclda.LDAParameters;
import logiclda.MiscUtil;
import logiclda.rules.LogicRule;
import logiclda.rules.SeedRule;
import logiclda.rules.IndependentRule;
import logiclda.textutil.FileUtil;

import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;

public class MirrorDescent 
{

	private Random rng;
	private double[] ruleWeights;
	private double ruleWeightSum;
	public LogicRule[] rules;
	private double pullfactor;
	
	public MirrorDescent(List<LogicRule> lstRules, Random rng)
	{		
		// Rule weights will be used for sampling
		ruleWeightSum = 0;
		Vector<Double> vecRuleWeights = new Vector<Double>();
		for(LogicRule lr : lstRules)
		{
			double weight = lr.getTotalSamplingWeight();
			ruleWeightSum += weight;
			vecRuleWeights.add(weight);
		}
		ruleWeights = MiscUtil.doubleListUnbox(vecRuleWeights);
		
		// Save the actual rules and random number generator
		this.rng = rng;
		rules = new LogicRule[lstRules.size()];
		rules = lstRules.toArray(rules);
	}

	/**
	 * Calc total satisfied weight over all rules
	 * 
	 * @param z
	 * @return
	 */
	public double satWeight(int[] z)
	{
		double weight = 0;
		for(LogicRule lr : this.rules)
			weight += lr.getRuleWeight() * lr.numSat(z);
		return weight;
	}
	
	public double totalWeight()
	{
		double total = 0;
		for(LogicRule lr : this.rules)
			total += lr.getRuleWeight() * lr.numGroundings();
		return total;
	}
	
	/**
	 * Get the z-label style weights for all IndependentRule 
	 * (for use in Logic Collapsed Gibbs, etc)
	 *  
	 * @param N
	 * @param T
	 * @return
	 */
	public double[][] seedsToZL(int N, int T)
	{
		double[][] retval = new double[N][]; // default to null
		for(LogicRule lr : rules)	
		{
			if(lr instanceof IndependentRule)			
			{
				// Need to 'sparse-add' these double[][] 
				double[][] ruleweights = ((IndependentRule) lr).toZLabel(N, T); 
				for(int i = 0; i < N; i++)
				{
					if(ruleweights[i] != null && retval[i] != null)
					{
						// both non-null, need to add
						for(int t = 0; t < ruleweights[i].length; t++)
							retval[i][t] += ruleweights[i][t];
					}
					else if(ruleweights[i] != null)
					{
						// only ruleweights non-null, just copy over
						retval[i] = ruleweights[i];
					}
					// ruleweights null so just continue
				}
			}
		}
		return retval;
	}
	
	/**
	 * Write logic satisfaction report out to *.logic file
	 * 
	 * @param z
	 * @param basename
	 */
	public void satReport(int[] z, String basename)
	{
		try 
		{
			String filename = String.format("%s.logic", basename);
			FileUtil.fileSpit(filename,this.satReport(z));
		}
		catch (IOException ioe)
		{
			System.out.println("Problem writing sample out to file");
			System.out.println(ioe.toString());
		}
	}
	
	
	/**
	 * Return logic satisfaction report as String
	 * 
	 * @param z
	 * @return
	 */
	public String satReport(int[] z)
	{
		String retval = "";
		retval += String.format("%d rules\n-------\n\n", rules.length);
		for(LogicRule lr : rules)
		{
			retval += lr.toString();
			long numground = lr.numGroundings();
			retval += String.format("%d groundings (weight = %.1f)\n",
					numground, lr.getTotalSamplingWeight());
			int numsat = lr.numSat(z); 
			retval += String.format("%d groundings satisfied (%d unsat)\n\n",
					numsat, numground - numsat);
		}
		return retval;
	}
	
	/**
	 * Nice summary of rule set
	 */
	public String toString()
	{
		String retval = "";
		retval += String.format("%d rules\n-------\n\n", rules.length);
		for(LogicRule lr : rules)
		{
			retval += lr.toString();
			retval += String.format("%d groundings (weight = %.1f)\n\n",
					lr.numGroundings(), lr.getTotalSamplingWeight());								
		}
		return retval;
	}	
	
	public static RelaxedSample runSGD(Corpus c, LDAParameters p, 
			List<LogicRule> rules, int numouter, 
			int numinner, DiscreteSample s)
	{
		// Apply evidence
		for(LogicRule lr : rules)
			lr.applyEvidence(c, p.T);
		
		// Init rule set
		MirrorDescent rs = new MirrorDescent(rules, p.rng);
		
		// Init relaxed z-sample 
		RelaxedSample relax = new RelaxedSample(c, p, s);						
						
		// Do LogicLDA MAP inference via Stochastic Gradient Descent				
		double stepa = Math.sqrt(numinner);
		double stepb = (double) numinner;
		relax = rs.doSGD(c, p, relax, numouter, numinner, stepa, stepb);
		
		// Return final RelaxedSample
		return relax;
	}
	
	 /**
	  * Do stochastic gradient descent MAP inference
	  * 
	  * @param c
	  * @param p
	  * @param relax
	  * @param numouter
	  * @param numinner
	  * @param stepa
	  * @param stepb
	  * @return
	  */
	public RelaxedSample doSGD(Corpus c, LDAParameters p,
			RelaxedSample relax,
			int numouter, int numinner,
			double stepa, double stepb)							
	{
		// Each outer loop, re-estimate phi/theta
		for(int nout = 0; nout < numouter; nout++)
		{
			System.out.println(String.format("Outer loop %d of %d (%d inner)", 
					nout, numouter, numinner));
			relax.updatePhiTheta(c, p);
			// Each inner loop takes a single stochastic gradient step			
			for(int nin = 0; nin < numinner; nin++)
			{
				double stepSize = stepa / Math.sqrt(stepb + nin);
				// Randomly sample a rule
				int chosen = MiscUtil.multSample(rng, 
						ruleWeights, ruleWeightSum);
				
				// Randomly sample a grounding and gradient
				Gradient rGrad = rules[chosen].randomGradient(relax, rng);
				
				// Take an EMDA step
				relax.emdaStep(rGrad, stepSize);
			}			
		}				
		return relax;
	}
		
}
