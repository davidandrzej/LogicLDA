/**
    LogicLDA - Topic modeling with First-Order Logic (FOL) domain knowledge        
    Copyright (C) 2010, David Andrzejewski (andrzeje@cs.wisc.edu)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package logiclda;


import java.util.*;
import java.io.*;

import logiclda.infer.CollapsedGibbs;
import logiclda.infer.DiscreteSample;
import logiclda.infer.RelaxedSample;
import logiclda.infer.MirrorDescent;
import logiclda.rules.LDARule;
import logiclda.rules.LogicRule;
import logiclda.rules.RuleType;
import logiclda.rules.SeedRule;
import logiclda.rules.SentExclRule;
import logiclda.rules.SentInclRule;

import org.ujmp.core.Matrix;
import org.ujmp.core.enums.FileFormat;
import org.ujmp.core.exceptions.MatrixException;

public class LogicLDA {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws MatrixException 
	 */
	public static void main(String[] args) throws MatrixException, IOException 
	{
		// Parse command-line args
		//
		String basefn = args[0];
		int numsamp = Integer.parseInt(args[1]);
		int numouter= Integer.parseInt(args[2]); 
		int numinner = Integer.parseInt(args[3]); 
		int topN = Integer.parseInt(args[4]); 
		int randseed = Integer.parseInt(args[5]); 

		// Load corpus and parameters (checking vocab dim agreement)
		//
		LDAParameters p = new LDAParameters(basefn, randseed);
		Corpus c = new Corpus(basefn);		
		assert(p.W == c.W);
				
		// Load logic rules 
		// (ldaRule controls whether to include LDA phi/theta rule)
		//
		boolean ldaRule = true;
		MirrorDescent rs = constructRuleSet(basefn, c, p.T, randseed, ldaRule);		
		System.out.println(rs.toString());
		
		// Initialize Logic LDA
		//
		File init = new File(String.format("%s.%s", basefn, "init"));
		DiscreteSample s;
		if(init.exists())
			// from *.init file
			s = new DiscreteSample(c.N, p.T, c.W, c.D, init.getCanonicalPath(), c);
		else
			// run standard LDA for numsamp 
			s = runStandardLDA(c, p, numsamp);
 		
		// Run LogicLDA MAP inference
 		//
 		RelaxedSample relax = runLogicLDA(c, p, rs, s, numouter, numinner);

		// Write out results
		//
		relax.writePhiTheta(p, basefn);
		relax.writeSample(basefn);
		rs.satReport(relax.getZ(), basefn);
		c.writeTopics(basefn, relax.getPhi(p), topN);
	}
	
	/**
	 * Standard (no logic) LDA, using collapsed Gibbs sampling
	 * 
	 * @param c
	 * @param p
	 * @param numsamp
	 * @return
	 */
	public static DiscreteSample runStandardLDA(Corpus c, LDAParameters p, int numsamp)
	{	
		// Do Collapsed Gibbs sampling and return final sample
		DiscreteSample s = CollapsedGibbs.doGibbs(c, p, numsamp);		
		return s;
	}
	
	/**
	 * Run LogicLDA MAP inference, starting from init DiscreteSample
	 * 
	 * @param c
	 * @param p
	 * @param rs
	 * @param s
	 * @param numouter
	 * @param numinner
	 * @return
	 */
	public static RelaxedSample runLogicLDA(Corpus c, LDAParameters p, MirrorDescent rs, 
			DiscreteSample s, int numouter, int numinner)
	{				
		// Init relaxed z-sample and RuleSet
		RelaxedSample relax = new RelaxedSample(c, p, s);						
		
		// Do LogicLDA MAP inference via Stochastic Gradient Descent				
		double stepa = Math.sqrt(numinner);
		double stepb = (double) numinner;
		relax = rs.doSGD(c, p, relax, numouter, numinner, stepa, stepb);
		
		// Return final RelaxedSample
		return relax;
	}

	/**
	 * Build rule set from *.rules file
	 * 
	 * @param basefn
	 * @param c
	 * @param T
	 * @param randseed
	 * @param ldaRule
	 * @return
	 */
	public static MirrorDescent constructRuleSet(String basefn, Corpus c, 
			int T, int randseed, boolean ldaRule)
	{		
		// Read logic rules from *.rules file
		Vector<LogicRule> rules = readRules(String.format("%s.rules",basefn));
		// Add LDA rule
		if(ldaRule)
			rules.add(new LDARule(c, 1));			
		// Apply the evidence
 		for(LogicRule lr : rules)
			lr.applyEvidence(c, T);
		// Create RuleSet
 		return new MirrorDescent(rules, randseed + 1);	
	}
	
	/**
	 * Static factory for constructing rules from file
	 */
	public static Vector<LogicRule> readRules(String rulefn)
	{
		Vector<LogicRule> rules = new Vector<LogicRule>();
				
		try
	    {
			BufferedReader in = 
				new BufferedReader(new FileReader(rulefn));
			String curLine = in.readLine();
			while(curLine != null)
		    {				
				Vector<String> ruleToks= new Vector<String>();
				
				StringTokenizer stok = new StringTokenizer(curLine,"-");
		    	while(stok.hasMoreTokens())
		    		ruleToks.add(stok.nextToken());
		    	
		    	// Pop sampling and step weights 
		    	double sampWeight = Double.parseDouble(ruleToks.remove(0));
		    	double stepWeight = Double.parseDouble(ruleToks.remove(0));
		    	// Rest of string contains rule parameters
		    	String rtStr = ruleToks.remove(0);
		    	
		    	for(RuleType rt : RuleType.values())
		    	{
		    		if(rt.matchesName(rtStr))
		    		{
		    			switch(rt) 
		    			{
		    				case SEED:
                                                    rules.add(new SeedRule(sampWeight, stepWeight,
                                                                           ruleToks));		 
		    					break;
		    				case SENTEXCL:
		    					rules.add(new SentExclRule(sampWeight, 
		    							stepWeight, ruleToks));		    					
		    					break;	
		    				case SENTINCL:
		    					rules.add(new SentInclRule(sampWeight,
		    							stepWeight, ruleToks));
		    					break;
		    				default: 
		    					System.out.println("Unknown rule type");
		    					System.out.println(rtStr);
		    			}
		    		}	
		    	}
				curLine = in.readLine();
		    }			
			return rules;
	    }
	catch (IOException ioe)
	    {
		System.out.println(String.format("Bad file(name): %s\n", 
						 ioe.toString()));
		return null;
	    }
	}
}
