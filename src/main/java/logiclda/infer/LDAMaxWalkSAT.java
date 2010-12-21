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
		List<LogicRule> rules = LogicLDA.readRules(String.format("%s.rules",basefn));
		for(LogicRule lr : rules)
			lr.applyEvidence(c, p.T);
				
		// Do MaxWalkSAT for numiter
		//
		List<GroundableRule> grules = new ArrayList<GroundableRule>();
		for(LogicRule r : rules)
			grules.add((GroundableRule) r);		
		doLDAMWS(c, p, grules, numiter, numiter, prand, s);
				
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
	public static DiscreteSample doLDAMWS(Corpus c, 
			LDAParameters p, List<GroundableRule> lstRules,
			int numouter, int numinner,
			double prand, DiscreteSample s)
	{		
		// Init data structures
		//
		GroundRules gr = new GroundRules(lstRules, s.z, p.rng);
		
		double[] randvsgreedy = new double[2];
		randvsgreedy[0] = prand;
		randvsgreedy[1] = 1 - prand;		

		// Init phi/theta
		double[][] phi = new double[p.T][p.W];
		double[][] theta = new double[c.D][p.T];
		
		for(int i = 0; i < numouter; i++)
		{
			//
			// OUTER LOOP
			// Estimate MAP phi/theta
			//
			phi = s.mapPhi(p, phi);
			theta = s.mapTheta(p, theta);
			
			for(int j = 0; j < numinner; j++)
			{
				//
				// INNER LOOP
				// Optimize Z with LDA-MaxWalkSAT
				//
										
				// TODO: Optimize the non-logic z with argmax
				
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
				s.updateCounts(c.w[idx], s.z[idx], c.d[idx], -1);				
				s.z[idx] = newz;
				s.updateCounts(c.w[idx], s.z[idx], c.d[idx], 1);				
				
				// Update ground rule satisfaction
				gr.updateUnsat(s.z, idx);
			}
		}
		
		return s;
	}
	
}
