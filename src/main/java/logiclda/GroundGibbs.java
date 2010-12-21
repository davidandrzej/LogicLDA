package logiclda;

import java.util.ArrayList;
import java.util.List;

import logiclda.infer.DiscreteSample;
import logiclda.infer.MirrorDescent;
import logiclda.infer.CollapsedGibbs;
import logiclda.rules.GroundableRule;
import logiclda.rules.LogicRule;

public class GroundGibbs 
{

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
			// Parse command-line args
			//
			String basefn = args[0];
			int numsamp = Integer.parseInt(args[1]);					
			int randseed = Integer.parseInt(args[2]);

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
			
			// Read in and convert rules
			//
			List<LogicRule> rules = LogicLDA.readRules(
					String.format("%s.rules", basefn));
			for(LogicRule lr : rules)
				lr.applyEvidence(c, p.T);
			List<GroundableRule> grules = new ArrayList<GroundableRule>();
			for(LogicRule r : rules)
				grules.add((GroundableRule) r);			
									
			// Do Ground Gibbs for numsamp
			//										
			DiscreteSample finalz = 
				CollapsedGibbs.doGroundGibbs(grules, c, p, numsamp);
							
			// Write out results
			//
			finalz.writePhiTheta(p, basefn);
			finalz.writeSample(basefn);
			c.writeTopics(basefn, finalz.getPhi(p), 
					Math.min(c.vocab.size(), 10));
			MirrorDescent md = new MirrorDescent(rules, p.rng);
			md.satReport(finalz.z, basefn);
		}
	}


