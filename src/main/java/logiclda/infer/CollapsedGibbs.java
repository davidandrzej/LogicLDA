package logiclda.infer;


import java.util.Random;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import logiclda.eval.EvalLDA;
import logiclda.infer.MirrorDescent;
import logiclda.rules.GroundableRule;
import logiclda.rules.LogicRule;
import logiclda.infer.GroundRules;
import logiclda.Corpus;
import logiclda.LDAParameters;
import logiclda.MiscUtil;

/**
 * 
 * Collapsed Gibbs Sampling inference for Latent Dirichlet Allocation 
 * (Griffiths & Steyvers PNAS 2004) 
 * 
 * @author david
 *
 */
public class CollapsedGibbs {

	/**
	 * Do a single Collapsed Gibbs sample, but with topic-words Phi fixed
	 * 
	 * @param c Contains words, documents
	 * @param p Contains hyperparameters
	 * @param s Sample object to be updated in place
	 * @param onlineInit If true, don't pre-subtract counts 
	 */
	public static void fixedPhiSample(Corpus c, LDAParameters p, DiscreteSample s,
			boolean onlineInit, double[][] phi)
	{
		int N = c.N;
		int T = p.T;
		
		double[] tmp = new double[T];
		for(int i = 0; i < N; i++)
		{
			// Reset sampling normalization sum to 0
			double normsum = 0;

			// Remove current assignment from counts
			// (unless we're doing 'online-style' init)
			if(!onlineInit)
			{
				s.updateCounts(c.w[i], s.z[i], c.d[i], -1);				
			}
		
			// Get un-normalized probabilities for each topic
			for(int j = 0; j < T; j++)
			{				
				double phiterm = phi[j][c.w[i]];
				double num2 = s.nd[c.d[i]][j] + p.alpha[j];				
				tmp[j] = phiterm * num2;
				normsum += tmp[j];
			}		
			
			// Sample the assignment
			s.z[i] = MiscUtil.multSample(p.rng, tmp, normsum);
			
			// Update the count matrices
			s.updateCounts(c.w[i], s.z[i], c.d[i], 1);			
		}		
		return;	
	}
	
	public static DiscreteSample runGroundGibbs(Corpus c, LDAParameters p, 
			DiscreteSample s, List<LogicRule> rules, int numsamp)
	{
		// Cast LogicRule to GroundableRule
		List<GroundableRule> grules = GroundRules.groundCast(rules);
		
		// Apply evidence
		for(GroundableRule gr : grules)
			gr.applyEvidence(c, p.T);
		// Ground the Logic Rules
		GroundRules gr = new GroundRules(grules, s.z, p.rng, p.T);
		
		// Best overall z-assignment found thus far
		//		
		int[] bestz = new int[s.z.length];
		System.arraycopy(s.z, 0, bestz, 0, s.z.length);
		MirrorDescent logicrules = new MirrorDescent(rules, p.rng);
		double bestobj = EvalLDA.ldaLoglike(s, p) + logicrules.satWeight(s.z);
		
		// Do the samples
		for(int si = 0; si < numsamp; si++)
		{
			System.out.println(String.format("Sample %d of %d", si, numsamp));
			CollapsedGibbs.groundGibbsSample(gr, c, p, s, false);
			
			// Do we have a new best sample?
			if((si+1) % 500 == 0)
			{
				double newobj = EvalLDA.ldaLoglike(s, p) + logicrules.satWeight(s.z);
				if(newobj > bestobj)
				{
					System.arraycopy(s.z, 0, bestz, 0, s.z.length);
					bestobj = newobj;
				}
			}
		}
		
		// Use the best sample
		s.repopZ(c, bestz);
		return s;
	}
	
	/**
	 * Externally called method for Ground LogicLDA sampling
	 * @param gr
	 * @param c
	 * @param p
	 * @param numsamp
	 * @return
	 */
	public static DiscreteSample doGroundGibbs(List<GroundableRule> grules,
			Corpus c, LDAParameters p, int numsamp)
	{
		// Get relevant dimensions
		int N = c.N;		
		int T = p.T;
		int W = p.W;
		int D = c.D;
		
		// Online initialization
		DiscreteSample s = new DiscreteSample(N, T, W, D);
		CollapsedGibbs.gibbsSample(c, p, s, true);
						
		// Ground the Logic Rules
		GroundRules gr = new GroundRules(grules, s.z, p.rng, p.T);		
		
		// Do the samples
		for(int si = 0; si < numsamp; si++)
		{
			System.out.println(String.format("Sample %d of %d", si, numsamp));
			CollapsedGibbs.groundGibbsSample(gr, c, p, s, false);
		}
		
		return s;
	}
	
	
	/**
	 * Do a single Ground LogicLDA sample
	 * 
	 * @param logicweights
	 * @param c
	 * @param p
	 * @param s
	 * @param onlineInit
	 */
	public static void groundGibbsSample(GroundRules gr,
			Corpus c, LDAParameters p, DiscreteSample s, 
			boolean onlineInit)            
	{				
		int N = c.N;
		int T = p.T;
		
		double[] tmp = new double[T];
		for(int i = 0; i < N; i++)
		{
			// Reset sampling normalization sum to 0
			double normsum = 0;

			// Remove current assignment from counts
			// (unless we're doing 'online-style' init)
			if(!onlineInit)
			{
				s.updateCounts(c.w[i], s.z[i], c.d[i], -1);				
			}
		
			// Get un-normalized probabilities for each topic
			for(int j = 0; j < T; j++)
			{
				double num1 = s.nw[c.w[i]][j] + p.beta[j][c.w[i]];
				double den1 = s.nwcolsums[j] + p.betasums[j];
				double num2 = s.nd[c.d[i]][j] + p.alpha[j];			
								
				tmp[j] = (num1 / den1) * num2;
				
				// If applicable, multiply the standard Collapsed Gibbs term 
				// by the exp of the logic contribution				
				if(gr.inLogic(i))
				{
					s.z[i] = j;
					tmp[j] *= Math.exp(gr.evalAssign(s.z, i));
				}					
								
				normsum += tmp[j];
			}		
			
			// Sample the assignment
			s.z[i] = MiscUtil.multSample(p.rng, tmp, normsum);
			
			// Update the count matrices
			s.updateCounts(c.w[i], s.z[i], c.d[i], 1);			
		}		
		return;
	}
	
	
	/**
	 * External method for doing online-init, then numsamp Logic Gibbs samples
	 * 
	 * @param logicweights N x T matrix of (independent) rule contributions
	 * @param c Contains words, documents
	 * @param p Contains hyperparameters
	 * @param numsamp How many samples to do
	 * @param randseed Seed for random number generator 
	 * @return The final sample from the Markov Chain
	 */
	public static DiscreteSample doLogicGibbs(double[][] logicweights, 
			Corpus c, LDAParameters p, int numsamp)
	{
		// Get relevant dimensions
		int N = c.N;		
		int T = p.T;
		int W = p.W;
		int D = c.D;
		
		// Online initialization
		DiscreteSample s = new DiscreteSample(N, T, W, D);
		CollapsedGibbs.logicGibbsSample(logicweights, c, p, s, true);
		
		// Do the samples
		for(int si = 0; si < numsamp; si++)
		{
			System.out.println(String.format("Sample %d of %d", si, numsamp));
			CollapsedGibbs.logicGibbsSample(logicweights, c, p, s, false);
		}
		
		return s;
	}
	
	/**
	 * Do a single Logic Collapsed Gibbs sample
	 * 
	 * @param logicweights N x T matrix of (independent) rule contributions
	 * @param c Contains words, documents
	 * @param p Contains hyperparameters
	 * @param s Sample object to be updated in place
	 * @param rng Random number generator for sampling
	 * @param onlineInit If true, don't pre-subtract counts 
	 */
	public static void logicGibbsSample(double[][] logicweights,
			Corpus c, LDAParameters p, DiscreteSample s, 
			boolean onlineInit)            
	{				
		int N = c.N;
		int T = p.T;
		
		double[] tmp = new double[T];
		for(int i = 0; i < N; i++)
		{
			// Reset sampling normalization sum to 0
			double normsum = 0;

			// Remove current assignment from counts
			// (unless we're doing 'online-style' init)
			if(!onlineInit)
			{
				s.updateCounts(c.w[i], s.z[i], c.d[i], -1);				
			}
		
			// Get un-normalized probabilities for each topic
			for(int j = 0; j < T; j++)
			{
				double num1 = s.nw[c.w[i]][j] + p.beta[j][c.w[i]];
				double den1 = s.nwcolsums[j] + p.betasums[j];
				double num2 = s.nd[c.d[i]][j] + p.alpha[j];			
				if(logicweights[i] != null)
					tmp[j] = (num1 / den1) * num2 * Math.exp(logicweights[i][j]);
				else
					tmp[j] = (num1 / den1) * num2;
				normsum += tmp[j];
			}		
			
			// Sample the assignment
			s.z[i] = MiscUtil.multSample(p.rng, tmp, normsum);
			
			// Update the count matrices
			s.updateCounts(c.w[i], s.z[i], c.d[i], 1);			
		}		
		return;
	}
	
	/**
	 * External method for doing online-init, then numsamp Gibbs samples
	 * @param c Contains words, documents
	 * @param p Contains hyperparameters
	 * @param numsamp How many samples to do
	 * @param randseed Seed for random number generator 
	 * @return The final sample from the Markov Chain
	 */
	public static DiscreteSample doGibbs(Corpus c, LDAParameters p, int numsamp)
	{
		// Get relevant dimensions
		int N = c.N;		
		int T = p.T;
		int W = p.W;
		int D = c.D;
		
		// Online initialization
		DiscreteSample s = new DiscreteSample(N, T, W, D);
		CollapsedGibbs.gibbsSample(c, p, s, true);
		
		// Do the samples
		for(int si = 0; si < numsamp; si++)
		{
			System.out.println(String.format("Sample %d of %d", si, numsamp));
			CollapsedGibbs.gibbsSample(c, p, s, false);
		}
		
		return s;
	}
	
	/**
	 * Do a single Collapsed Gibbs sample
	 * 
	 * @param c Contains words, documents
	 * @param p Contains hyperparameters
	 * @param s Sample object to be updated in place
	 * @param rng Random number generator for sampling
	 * @param onlineInit If true, don't pre-subtract counts 
	 */
	public static void gibbsSample(Corpus c, LDAParameters p, DiscreteSample s, 
			boolean onlineInit)            
	{				
		int N = c.N;
		int T = p.T;
		
		double[] tmp = new double[T];
		for(int i = 0; i < N; i++)
		{
			// Reset sampling normalization sum to 0
			double normsum = 0;

			// Remove current assignment from counts
			// (unless we're doing 'online-style' init)
			if(!onlineInit)
			{
				s.updateCounts(c.w[i], s.z[i], c.d[i], -1);				
			}
		
			// Get un-normalized probabilities for each topic
			for(int j = 0; j < T; j++)
			{
				double num1 = s.nw[c.w[i]][j] + p.beta[j][c.w[i]];
				double den1 = s.nwcolsums[j] + p.betasums[j];
				double num2 = s.nd[c.d[i]][j] + p.alpha[j];				
				tmp[j] = (num1 / den1) * num2;
				normsum += tmp[j];
			}		
			
			// Sample the assignment
			s.z[i] = MiscUtil.multSample(p.rng, tmp, normsum);
			
			// Update the count matrices
			s.updateCounts(c.w[i], s.z[i], c.d[i], 1);			
		}		
		return;
	}
}
