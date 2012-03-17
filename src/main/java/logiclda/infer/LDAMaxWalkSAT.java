package logiclda.infer;



import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.ArrayList;

import logiclda.StandardLDA;
import logiclda.LogicLDA;
import logiclda.Corpus;
import logiclda.LDAParameters;
import logiclda.MiscUtil;
import logiclda.rules.LogicRule;
import logiclda.rules.GroundableRule;
import logiclda.rules.Grounding;
import logiclda.EvalLDA;
import logiclda.infer.GroundRules;
import logiclda.infer.DiscreteSample;

/**
 * 
 * LDAMaxWalkSAT - like MaxWalkSAT but take LDA phi/theta into account
 * 
 * @author david
 *
 */
public class LDAMaxWalkSAT {

		
	public static void main(String [] args)
	{
		// Parse command-line args
		//
		String basefn = args[0];
		int numsamp = Integer.parseInt(args[1]);		
		int numiter = Integer.parseInt(args[2]);		
		int randseed = Integer.parseInt(args[3]);
		double prand = Double.parseDouble(args[4]); 

		// Load corpus and parameters (checking vocab dim agreement)
		//
		LDAParameters p = null;
		try 
		{
			p = new LDAParameters(basefn, randseed);
		}
		catch (Exception ioe)
		{
			ioe.printStackTrace();
		}
		Corpus c = new Corpus(basefn);		
		assert(p.W == c.W);
		
		// Run standard LDA for numsamp 
		//
		DiscreteSample s = StandardLDA.runStandardLDA(c, p, numsamp);
 		
		// Read in and convert rules
		//
		List<LogicRule> rules = 
			LogicLDA.readRules(String.format("%s.rules",basefn));
		for(LogicRule lr : rules)
			lr.applyEvidence(c, p.T);
				
		// Do MaxWalkSAT for numiter
		//			
		LDAMaxWalkSAT.runLDAMWS(c, p, rules, numiter, numiter, prand, s);
				
		DiscreteSample finalz = new DiscreteSample(c.N, p.T, p.W, c.D, s.z, c);
						
		// Write out results
		//
		finalz.writePhiTheta(p, basefn);
		finalz.writeSample(basefn);
		c.writeTopics(basefn, finalz.getPhi(p), Math.min(c.vocab.size(), 10));
		MirrorDescent md = new MirrorDescent(rules, p.rng);
		md.satReport(finalz.z, basefn);
	}
	
	/**
	 * For all zi which are not part of any logic formula,
	 * simply assign by argmax (phi*theta) 
	 * 
	 * @param c
	 * @param gr
	 * @param phi
	 * @param theta
	 * @param s
	 * @return
	 */
	public static DiscreteSample argmaxZ(Corpus c,  GroundRules gr, 
			double[][] phi, double[][] theta, DiscreteSample s)	
	{
		// Iterate over every position in the corpus
		for(int i = 0; i < c.N; i++)
		{
			// If involved in any logic, skip
			if(gr.inLogic(i))
				continue;
			
			// Otherwise set to argmax phi*theta
			double bestval = Double.NEGATIVE_INFINITY;
			int bestz = -1;
			
			for(int t = 0; t < phi.length; t++)
				if(phi[t][c.w[i]] * theta[c.d[i]][t] > bestval)
				{
					bestval = phi[t][c.w[i]] * theta[c.d[i]][t];
					bestz = t;
				}
			
			s.reassign(c, i, bestz);			
		}
		
		return s;
	}
	
	/**
	 * 
	 * Do LDA-MaxWalkSAT inference
	 * 
	 * @param lstRules
	 * @param p
	 * @param numiter
	 * @param prand
	 * @param s
	 * @param randseed
	 * @return
	 */
	public static DiscreteSample runLDAMWS(Corpus c, 
			LDAParameters p, List<LogicRule> lstRules,
			int numouter, int numinner,
			double prand, DiscreteSample s)
	{		
		// Apply evidence
		for(LogicRule gr : lstRules)
			gr.applyEvidence(c, p.T);
		
		// Init data structures
		//
		GroundRules gr = 
			new GroundRules(GroundRules.groundCast(lstRules), s.z, p.rng, p.T);
		
		double[] randvsgreedy = new double[2];
		randvsgreedy[0] = prand;
		randvsgreedy[1] = 1 - prand;		

		// Init phi/theta
		double[][] phi = new double[p.T][p.W];
		double[][] theta = new double[c.D][p.T];

		// Record best obj fcn found thus far
		//		
		int[] bestz = new int[s.z.length];
		System.arraycopy(s.z, 0, bestz, 0, s.z.length);
		MirrorDescent logicrules = new MirrorDescent(lstRules, p.rng);
		double bestobj = EvalLDA.ldaLoglike(s, p) + logicrules.satWeight(s.z);
				
		for(int i = 0; i < numouter; i++)
		{
			System.out.println(String.format("LDA-MWS outer %d of %d",
					i, numouter));			
			//
			// OUTER LOOP
			// Estimate MAP phi/theta
			//
			phi = s.mapPhi(p, phi);
			theta = s.mapTheta(p, theta);
			
			// Argmax non-logic Z
			s = LDAMaxWalkSAT.argmaxZ(c, gr, phi, theta, s);
			
			for(int j = 0; j < numinner; j++)
			{
				//
				// INNER LOOP
				// Optimize Z with LDA-MaxWalkSAT
				//														
				// Sample an unsatisfied clause 
				Grounding g = gr.randomUnsat();
			
				// If none, then we're done!
				if(g == null)
					break;
			
				// Decide on random vs greedy step
				int idx, newz;
				if(0 == MiscUtil.multSample(gr.rng, randvsgreedy, 1.0))
				{
					// RANDOM STEP
					idx = g.values[gr.rng.nextInt(g.values.length)];
					newz = gr.rng.nextInt(p.T);								
				}
				else
				{
					// GREEDY STEP
					int[] bestidxnewz = gr.getGreedy(s.z, g, p.T);
					idx = bestidxnewz[0]; 
					newz = bestidxnewz[1];
				}	
			
				// Take the step
				s.reassign(c, idx, newz);				
				
				// Update ground rule satisfaction
				gr.updateUnsat(s.z, idx);
			}
			
			// New champion?						
			double newobj = EvalLDA.ldaLoglike(s.nw, s.nd, phi, 
					theta, p.beta, p.alpha) + logicrules.satWeight(s.z);
			if(newobj > bestobj)
			{
				System.arraycopy(s.z, 0, bestz, 0, s.z.length);
				bestobj = newobj;				
			}
		}		
		// Use the best sample
		s.repopZ(c, bestz);
		return s;
	}
	
}
