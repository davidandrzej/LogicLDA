package logiclda.infer;

import java.util.Random;

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
	 * Do a single Collapsed Gibbs sample, but with topic-words Phi fixed
	 * 
	 * @param c Contains words, documents
	 * @param p Contains hyperparameters
	 * @param s Sample object to be updated in place
	 * @param rng Random number generator for sampling
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
