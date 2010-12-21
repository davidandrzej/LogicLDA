package logiclda.infer;



import java.util.*;
import java.io.*;

import logiclda.Corpus;
import logiclda.LDAParameters;
import logiclda.MiscUtil;
import logiclda.rules.GroundableRule;
import logiclda.rules.Grounding;
import logiclda.rules.LogicRule;
import logiclda.rules.SeedRule;
import logiclda.rules.IndependentRule;
import logiclda.textutil.FileUtil;

import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;

/**
 * Represents a set of grounded LogicRules (each of which must implement Groundable)
 * 
 * For use with grounded LogicLDA inference schemes (MWS, CGS, M+L) 
 * 
 * @author andrzejewski1
 *
 */
public class GroundRules 
{
	private List<GroundableRule> rules;
	private int[] unsatCounts;
	public Random rng;
	
		
	/**
	 * Each LogicRule has already been initialized with *.applyEvidence()
	 * 
	 */
	public GroundRules(List<GroundableRule> lstRules, int[] z, Random rng)
	{
		// Ground each rule
		this.rules = lstRules;
		for(GroundableRule r : this.rules)
			r.groundRule(z);
		
		// Get initial unsat counts for each ground rule
		this.unsatCounts = new int[this.rules.size()];
		int idx = 0;
		for(GroundableRule r : this.rules)
		{
			this.unsatCounts[idx] = r.getUnSat().size();
			idx++;
		}
		
		// Init random number generator
		this.rng = rng;
	}

	/**
	 * After an update of z at position idx, update unsat ground clause
	 * @param z
	 * @param idx
	 */
	public void updateUnsat(int[] z, int idx)	
	{
		int ridx = 0;
		for(GroundableRule gr : this.rules)
		{
			gr.updateUnSat(z, idx);
			this.unsatCounts[ridx] = gr.getUnSat().size();
			ridx++;
		}
	}
	
	/**
	 * Calc global obj fcn impact of z[idx]'s current value
	 * 
	 * @param z
	 * @param idx
	 * @return
	 */
	public double evalAssign(int[] z, int idx)
	{
		double weight = 0;
		for(GroundableRule gr : this.rules)
			weight += gr.evalAssign(z, idx);
		return weight;
	}
	
	/**
	 * Given Grounding g, return [idx, newz] for best greedy move 
	 * 
	 * @param g
	 * @return
	 */
	public int[] getGreedy(int[] z, Grounding g, int T)
	{
		int bestidx = -1;
		int bestnewz = -1;
		double bestweight = Double.NEGATIVE_INFINITY;
		
		for(int idx : g.values)
		{
			// Save old value
			int oldz = z[idx];
			for(int newz = 0; newz < T; newz++)
			{
				z[idx] = newz;
				double newweight = this.evalAssign(z, idx);
				if(newweight > bestweight)
				{
					bestweight = newweight;
					bestidx = idx;
					bestnewz = newz; 
				}
			}
			// Restore old value before trying next index
			z[idx] = oldz;
		}
		
		// Return the best index and newz value
		int[] retval = new int[2];
		retval[0] = bestidx;
		retval[1] = bestnewz;
		return retval;		
	}
	
	/**
	 * Return a randomly sampled unsatisfied clause, 
	 * or null if all clauses are satisfied 
	 * @return
	 */
	public Grounding randomUnsat()
	{
		int unsatSum = 0;
		for(int ct : this.unsatCounts)
			unsatSum += ct;
		
		if(unsatSum == 0)
			return null;
	
		// First determine which rule we are sampling from
		int gidx = rng.nextInt(unsatSum);
		int ridx = 0;
		int cumsum = 0;
		while(gidx >= cumsum)
		{
			cumsum += this.unsatCounts[ridx];
			ridx++;
		}
		ridx--;
		
		// Now return the appropriate entry from this set
		gidx -= (cumsum - this.unsatCounts[ridx]);
		int counter = 0;
		for(Grounding g : this.rules.get(ridx).getUnSat())
		{
			if(counter == gidx)
				return g;
			else 
				counter++;
		}
						
		System.out.println("ERROR IN randomUnsat()");
		System.exit(1);
		return null; 
	}
	
	
		
}
