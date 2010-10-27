package logiclda.infer;

import java.util.*;
import java.io.*;

import logiclda.Corpus;
import logiclda.LDAParameters;
import logiclda.MiscUtil;
import logiclda.rules.LogicRule;
import logiclda.rules.SeedRule;
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
	
	public MirrorDescent(List<LogicRule> lstRules, int randseed)
	{		
		// Rule weights will be used for sampling
		ruleWeightSum = 0;
		Vector<Double> vecRuleWeights = new Vector<Double>();
		for(LogicRule lr : lstRules)
		{
			double weight = lr.getWeight();
			ruleWeightSum += weight;
			vecRuleWeights.add(weight);
		}
		ruleWeights = MiscUtil.doubleListUnbox(vecRuleWeights);
		
		// Save the actual rules and random number generator
		rng = new Random(randseed);
		rules = new LogicRule[lstRules.size()];
		rules = lstRules.toArray(rules);
	}

	/**
	 * Get the z-label weights for all SeedRule
	 * (for use as input to pSSLDA code)
	 *  
	 * @param N
	 * @param T
	 * @return
	 * @throws ClassNotFoundException
	 */
	public double[][] seedsToZL(int N, int T) throws ClassNotFoundException 
	{
		double[][] retval = new double[N][T];
		for(LogicRule lr : rules)		
			if(lr instanceof SeedRule)			
				MiscUtil.matrixDestructAdd(retval, 
						((SeedRule) lr).toZLabel(N, T));
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
					numground, lr.getWeight());
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
					lr.numGroundings(), lr.getWeight());								
		}
		return retval;
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
