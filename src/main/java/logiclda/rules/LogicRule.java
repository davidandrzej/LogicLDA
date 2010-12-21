package logiclda.rules;

import java.util.*;

import logiclda.Corpus;
import logiclda.infer.Gradient;
import logiclda.infer.RelaxedSample;

public interface LogicRule 
{
	
	/**
	 * Weight of this rule for random sampling purposes
	 * @return [total sampling weight] = [sampling weight] x [num groundings] 
	 */
	public double getTotalSamplingWeight();
	
	/**
	 * Actual weight of this logic rule
	 * @return [rule weight] = [sampling weight] * [step weight]
	 */
	public double getRuleWeight();	
		 	
	/**
	 * Randomly sample a grounding and take a gradient step wrt to it
	 * @param relax Relaxed z-array (will be updated in-place!)
	 */
	public Gradient randomGradient(RelaxedSample relax, Random rng);
	
	/**
	 * 
	 * @param c
	 * @param T
	 */
	public void applyEvidence(Corpus c, int T);
	
	/**
	 * How many (non-trivial) groundings  
	 * @return
	 */
	public long numGroundings();
	
	/**
	 * Given a sample z, how many groundings are satisfied?
	 * @param z
	 * @return
	 */
	public int numSat(int[] z);
}
