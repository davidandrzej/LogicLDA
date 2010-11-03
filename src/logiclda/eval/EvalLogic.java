package logiclda.eval;

import java.util.*;
import java.io.*;

import logiclda.LogicLDA;
import logiclda.StandardLDA;
import logiclda.LDAParameters;
import logiclda.Corpus;
import logiclda.eval.CrossFold;
import logiclda.eval.AvgSat;
import logiclda.infer.CollapsedGibbs;
import logiclda.infer.DiscreteSample;
import logiclda.infer.Sample;
import logiclda.infer.RelaxedSample;
import logiclda.infer.MirrorDescent;
import logiclda.rules.LDARule;
import logiclda.rules.LogicRule;
import logiclda.rules.RuleType;
import logiclda.rules.SeedRule;
import logiclda.rules.SentExclRule;
import logiclda.rules.SentInclRule;
import logiclda.rules.CLRule;
import logiclda.rules.MLRule;
import logiclda.textutil.FileUtil;

import org.ujmp.core.exceptions.MatrixException;

public class EvalLogic {

	// Constants for evaluation 
	static final int BURNIN = 50;
	static final int INTERVAL = 10;
	static final int MCSAMP = 10;
	
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
		int randseed = Integer.parseInt(args[4]); 
		int k = Integer.parseInt(args[5]);

		try 
		{
			// Standard LDA results
			AvgSat[] ldaResults = estLogicGeneralization(basefn, numsamp, 
					0, 0, randseed, k);
			crossfoldReport(ldaResults, String.format("%s-LDA", basefn));			
			
			// LogicLDA results
			AvgSat[] logicResults = estLogicGeneralization(basefn, numsamp, 
					numouter, numinner, randseed, k);		
			crossfoldReport(logicResults, String.format("%s-Logic", basefn));
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	public static void crossfoldReport(AvgSat[] satResults, String basefn)
	throws IOException	
	{
		String outname = String.format("%s.crossfold", basefn);			
		FileWriter out = new FileWriter(new File(outname));
		
		out.write("PERCENT SATISFACTION ACROSS FOLDS\n");
		double totalPercent = 0;
		for(int ki = 0; ki < satResults.length; ki++)
		{
			double avgpercent = satResults[ki].getAvgPercentSat(false);
			double stdpercent = satResults[ki].getStdPercentSat(false);
			out.write(String.format("Fold %d: %f percent (+/- %f)\n", ki, avgpercent, stdpercent));
			totalPercent += avgpercent;
		}
		out.write(String.format("Avg over folds = %f percent\n\n", totalPercent / satResults.length));
		
		for(int ki = 0; ki < satResults.length; ki++)
		{
			out.write(String.format("Fold %d\n%s\n", ki, satResults[ki].report()));
		}
		
		out.close();
	}

	
	static public AvgSat[] estLogicGeneralization(String basefn,
			int numsamp, int numouter, int numinner,
			int randseed, int k) throws IOException
	{
		// Load corpus and parameters (checking vocab dim agreement)
		//
		LDAParameters p = new LDAParameters(basefn, randseed);

		// Do inference over train/test folds
		//
		CrossFold cf = new CrossFold(new Corpus(basefn), k, randseed);
		AvgSat[] satResults = new AvgSat[k];
		for(int ki = 0; ki < k; ki++)
		{
			// Get train/test corpora
			Corpus train = cf.getTrain(ki);
			Corpus test = cf.getTest(ki);

			// Load logic rules
			// (ldaRule controls whether to include LDA phi/theta rule)
			//
			boolean ldaRule = true;
			MirrorDescent rs = LogicLDA.constructRuleSet(basefn, 
					train, p.T, randseed, ldaRule);		
			System.out.println(rs.toString());

			// Get init sample from Collapsed Gibbs
			//
			DiscreteSample s;
			if(numouter > 0)
			{
				// If we are going to run Logic SGD inference later, 
				// run Logic Collapsed Gibbs for numsamp
				double[][] logicweights = rs.seedsToZL(train.N, p.T);
				s = CollapsedGibbs.doLogicGibbs(logicweights, train, p, numsamp);
			}
			else
			{
				// else assume we were meant to use no logic whatsoever...
				s = StandardLDA.runStandardLDA(train, p, numsamp);
			}
			
			
			// Run LogicLDA MAP inference	 		
			RelaxedSample relax = LogicLDA.runLogicLDA(train, p, rs, s, numouter, numinner);
			// Extract learned phi
			double[][] phi = relax.phi;

			// Init Gibbs inference with phi fixed
			DiscreteSample testz = new DiscreteSample(test.N, p.T, test.W, test.D); 
			CollapsedGibbs.fixedPhiSample(test, p, testz, true, phi);

			// Construct testset rules (no LDA rule)
			MirrorDescent testrules = LogicLDA.constructRuleSet(basefn,
					test, p.T, randseed, false);

			// Do burnin samples
			System.out.println(String.format("[Fold %d of %d] fixed phi evaluation: burnin",ki+1,k));
			for(int i = 0; i < BURNIN; i++)
				CollapsedGibbs.fixedPhiSample(test, p, testz, false, phi);

			// Do MC estimation samples
			AvgSat as = new AvgSat(testrules.rules);
			for(int i = 0; i < MCSAMP; i++)
			{
				System.out.println(
						String.format("[Fold %d of %d] fixed phi evaluation: MC sample %d of %d",
								ki+1, k,
								i, MCSAMP));
				for(int j = 0; j < INTERVAL; j++)
				{
					CollapsedGibbs.fixedPhiSample(test, p, testz, false, phi);
				}
				as.recordSatisfaction(testz.z);	 				 			
			}

			// Save results from this fold
			satResults[ki] = as;	 		
		}
		
		return satResults;
	}

}

//
// Test train/testset
//
//int k = 2;
//CrossFold cf = new CrossFold(c, k);
//
//System.out.println("Full dataset");		
//System.out.println(String.format("%d docs = %s", c.D, c.doclist.toString()));
//
//for(int ki = 0; ki < k; ki++)
//{
//	Corpus ctrain = cf.getTrain(ki);
//	Corpus ctest = cf.getTest(ki);
//	
//	System.out.println(String.format("Fold %d", ki));
//	System.out.println(String.format("%d train docs = %s", ctrain.D, ctrain.doclist.toString()));
//	System.out.println(String.format("%d test docs = %s", ctest.D, ctest.doclist.toString()));
//	System.out.println(String.format("train w=%s", FileUtil.intArrayToString(ctrain.w)));
//	System.out.println(String.format("train d=%s", FileUtil.intArrayToString(ctrain.d)));
//	System.out.println();
//}


//// Initialize Logic LDA
////
//File init = new File(String.format("%s.%s", basefn, "init"));
//DiscreteSample s;
//if(init.exists())
//	// from *.init file
//	s = new DiscreteSample(c.N, p.T, c.W, c.D, init.getCanonicalPath(), c);
//else
//	// run standard LDA for numsamp 
//	s = StandardLDA.runStandardLDA(c, p, numsamp);
//
//// Run LogicLDA MAP inference
////
//RelaxedSample relax = LogicLDA.runLogicLDA(c, p, rs, s, numouter, numinner);
//
//// Write out results
////
//relax.writePhiTheta(p, basefn);
//relax.writeSample(basefn);
//rs.satReport(relax.getZ(), basefn);
//c.writeTopics(basefn, relax.getPhi(p), topN);