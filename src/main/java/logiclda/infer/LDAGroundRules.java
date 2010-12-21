package logiclda.infer;

import java.util.List;
import java.util.Random;

import logiclda.Corpus;
import logiclda.rules.GroundableRule;
import logiclda.rules.Grounding;

public class LDAGroundRules extends GroundRules
{
	private List<GroundableRule> rules;
	private int[] unsatCounts;
	public Random rng;
		
	/**
	 * Each LogicRule has already been initialized with *.applyEvidence()
	 * 
	 */
	public LDAGroundRules(List<GroundableRule> lstRules, int[] z, Random rng)
	{
		super(lstRules, z, rng);
	}
	
	/**
	 * Given Grounding g and LDA phi/theta, 
	 * return [idx, newz] for best greedy move 
	 * 
	 * @param g
	 * @return
	 */
	public int[] getGreedy(Corpus c, double[][] phi, double[][] theta, 
			Grounding g, int[] z)
	{
		int bestidx = -1;
		int bestnewz = -1;
		double bestweight = Double.NEGATIVE_INFINITY;
		
		for(int idx : g.values)
		{
			// Save old value
			int oldz = z[idx];
			for(int newz = 0; newz < phi.length; newz++)
			{
				z[idx] = newz;
								
				// Logic weight contribution
				double newweight = this.evalAssign(z, idx);				
				// LDA component
				newweight += Math.log(phi[newz][c.w[idx]]);
				newweight += Math.log(theta[c.d[idx]][newz]);
				
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
	
	
}
