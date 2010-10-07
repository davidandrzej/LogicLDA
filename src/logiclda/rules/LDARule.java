package logiclda.rules;

import java.util.HashMap;
import java.util.Random;

import logiclda.Corpus;
import logiclda.infer.Gradient;
import logiclda.infer.RelaxedSample;

public class LDARule implements LogicRule {

	private double ruleWeight;
	private int N;
	private Corpus c;
	
	public LDARule(Corpus c, double ruleWeight)
	{
		this.c = c;
		this.ruleWeight = ruleWeight;
		this.N = c.N;
	}
	
	public double getWeight() 
	{
		return N * ruleWeight;
	}
	
	public long numGroundings()
	{
		return N;
	}

	public String toString() 
	{
		return String.format("LDA pseudorule (corpus length = %d)\n", N);
	}

	/**
	 * Calculate LDA gradient for a given index i
	 * @param i 
	 * @return
	 */
	public double[][] ldaGradient(Corpus c, double[][] phi, double[][] theta,
			int i)
	{
		int T = phi.length;
		double[][] gradient = new double[1][T];
		for(int t = 0; t < T; t++)
			gradient[0][t] = phi[t][c.w[i]] * theta[c.d[i]][t];
		return gradient;
	}
	
	public Gradient randomGradient(RelaxedSample relax, Random rng) 
	{
		int[] indices = new int[1];
		indices[0] = rng.nextInt(N);								
		return new Gradient(ldaGradient(c, relax.phi, relax.theta, indices[0]), 
				indices); 
	}

	public LogicRule[] docPartition(Corpus c, HashMap<Integer,Integer> docMap)
	{
		// TODO: Implement LDA rule splitting
		// (add another layer of indirection)
		return null;
	}
	
	/**
	 * Number of satisfied groundings is undefined for LDARule
	 */
	public int numSat(int[] z)
	{
		return 0;
	}
	
	public void applyEvidence(Corpus c, int T)
	{	
		return;
	}
}
