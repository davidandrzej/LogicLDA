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
		
		// Construct train/test folds
		int k = 4;
		CrossFold cf = new CrossFold(new Corpus(basefn), k);

		for(int ki = 0; ki < k; ki++)
		{
			Corpus train = cf.getTrain(ki);
			Corpus test = cf.getTest(ki);
			
			// Load logic rules 
			// (ldaRule controls whether to include LDA phi/theta rule)
			//
			boolean ldaRule = true;
			MirrorDescent rs = LogicLDA.constructRuleSet(basefn, 
					train, p.T, randseed, ldaRule);		
			System.out.println(rs.toString());
									
			// Run standard LDA to init
			DiscreteSample s = StandardLDA.runStandardLDA(train, p, numsamp);	 		
			// Run LogicLDA MAP inference	 		
	 		RelaxedSample relax = LogicLDA.runLogicLDA(train, p, rs, s, numouter, numinner);

	 		// Extract phi
	 		double[][] phi = relax.phi;
	 			 			 		
	 		// Run Gibbs inference with phi fixed
	 		DiscreteSample testz = new DiscreteSample(test.N, p.T, test.W, test.D); 
	 		CollapsedGibbs.fixedPhiSample(test, p, testz, true, phi);
	 		
	 		//
	 		MirrorDescent testrules = LogicLDA.constructRuleSet(basefn,
	 				test, p.T, randseed, false);
	 		
	 		LogicRule[] rules = testrules.rules;
	 		int[] satcts = new int[rules.length];
	 		
	 		int Kest = 5;
	 		AvgSat as = new AvgSat(rules);
	 		for(int ti = 0; ti < Kest; ti++)
	 		{
	 			for(int j = 0; j < 100; j++)
	 				CollapsedGibbs.fixedPhiSample(test, p, testz, false, phi);	 			
	 			as.recordSatisfaction(testz.z);	 				 			
	 		}
	 		
	 		System.out.println(as.report());			
		}
		
		//
		// Test train/testset
		//
//		int k = 2;
//		CrossFold cf = new CrossFold(c, k);
//		
//		System.out.println("Full dataset");		
//		System.out.println(String.format("%d docs = %s", c.D, c.doclist.toString()));
//		
//		for(int ki = 0; ki < k; ki++)
//		{
//			Corpus ctrain = cf.getTrain(ki);
//			Corpus ctest = cf.getTest(ki);
//			
//			System.out.println(String.format("Fold %d", ki));
//			System.out.println(String.format("%d train docs = %s", ctrain.D, ctrain.doclist.toString()));
//			System.out.println(String.format("%d test docs = %s", ctest.D, ctest.doclist.toString()));
//			System.out.println(String.format("train w=%s", FileUtil.intArrayToString(ctrain.w)));
//			System.out.println(String.format("train d=%s", FileUtil.intArrayToString(ctrain.d)));
//			System.out.println();
//		}
		

//		// Initialize Logic LDA
//		//
//		File init = new File(String.format("%s.%s", basefn, "init"));
//		DiscreteSample s;
//		if(init.exists())
//			// from *.init file
//			s = new DiscreteSample(c.N, p.T, c.W, c.D, init.getCanonicalPath(), c);
//		else
//			// run standard LDA for numsamp 
//			s = StandardLDA.runStandardLDA(c, p, numsamp);
//
//		// Run LogicLDA MAP inference
//		//
//		RelaxedSample relax = LogicLDA.runLogicLDA(c, p, rs, s, numouter, numinner);
//
//		// Write out results
//		//
//		relax.writePhiTheta(p, basefn);
//		relax.writeSample(basefn);
//		rs.satReport(relax.getZ(), basefn);
//		c.writeTopics(basefn, relax.getPhi(p), topN);
	}
}
