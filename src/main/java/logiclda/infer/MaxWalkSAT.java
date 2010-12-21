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
 * MaxWalkSAT solver for weighted satisfiability 
 * 
 * @author david
 *
 */
public class MaxWalkSAT {

	
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
		doMWS(grules, p, numiter, prand, s);
		
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
	 * Do MaxWalkSAT inference
	 * 
	 * @param lstRules
	 * @param p
	 * @param numiter
	 * @param prand
	 * @param s
	 * @param randseed
	 * @return
	 */
	public static DiscreteSample doMWS(List<GroundableRule> lstRules,
			LDAParameters p,
			int numiter, 
			double prand, DiscreteSample s)
	{
		
		// Init data structures
		//
		GroundRules gr = new GroundRules(lstRules, s.z, p.rng);
		
		double[] randvsgreedy = new double[2];
		randvsgreedy[0] = prand;
		randvsgreedy[1] = 1 - prand;		
				
		for(int i = 0; i < numiter; i++)
		{
			if(i % 100 == 0)
			{
				System.out.println(String.format("MWS Iter %d of %d", i+1, numiter));
				for(GroundableRule rule : lstRules)
					System.out.println(String.format("Rule: %s\t%d sat\n\t%d unsat\n\t%d total",
							rule.toString(), rule.numSat(s.z), rule.getUnSat().size(), 
							rule.numGroundings()));
			}
			
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
			s.z[idx] = newz;				
			gr.updateUnsat(s.z, idx);
		}				
		return s;
	}
	
}
