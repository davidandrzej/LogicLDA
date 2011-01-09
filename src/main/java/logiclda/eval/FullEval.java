package logiclda.eval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ujmp.core.Matrix;

import logiclda.Corpus;
import logiclda.LDAParameters;
import logiclda.LogicLDA;
import logiclda.StandardLDA;
import logiclda.infer.GroundRules;
import logiclda.infer.InferScheme;
import logiclda.infer.CollapsedGibbs;
import logiclda.infer.MaxWalkSAT;
import logiclda.infer.LDAMaxWalkSAT;
import logiclda.infer.DiscreteSample;
import logiclda.infer.MirrorDescent;
import logiclda.infer.RelaxedSample;
import logiclda.rules.GroundableRule;
import logiclda.rules.LogicRule;
import logiclda.textutil.FileUtil;

public class FullEval {

	// Constants for evaluation 
	static final int BURNIN = 500;
	static final int INTERVAL = 50;
	static final int MCSAMP = 10;
	static final double PRAND = 0.05; 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{				
		// Parse command-line args
		//
		String basefn = args[0];
		
		// If fewer args, simply output logic grounding report and quit
		if(args.length < 7)
		{
			try{							
				// Output logic grounding report for Corpus subsamples
				// 
				LDAParameters p = new LDAParameters(basefn, 194582);

				// How many Corpus subsets?
				int k = Integer.parseInt(args[1]);
				CrossFold cf = new CrossFold(new Corpus(basefn), k);

				// Initialize corpus and rule set								
				int ki = 0;
				System.out.println(
						String.format(
								"Analyzing groundings for fold %d of %d", 
								ki, k));
				
				if(k == 1)
				{
					Corpus c = new Corpus(basefn);
					MirrorDescent rs = LogicLDA.constructRuleSet(basefn, c, 
							p.T, p.randseed, false);
					FileUtil.fileSpit(String.format("%s-ALL.groundings", basefn),
							rs.toString());
					System.exit(0);
				}
									
				Corpus c = cf.getTest(ki);			
				MirrorDescent rs = LogicLDA.constructRuleSet(basefn, c, 
						p.T, p.randseed, false);				
				FileUtil.fileSpit(String.format("%s-%d.groundings", basefn, ki+1), 
						rs.toString());

				// Iterate over the subsets
				for(ki = 1; ki < k; ki++)
				{
					System.out.println(
							String.format(
									"Analyzing groundings for fold %d of %d", 
									ki, k));
					
					c.concatCorpus(cf.getTest(ki));
					rs = LogicLDA.constructRuleSet(basefn, c, p.T, p.randseed, false);
					FileUtil.fileSpit(String.format("%s-%d.groundings", basefn, ki+1), 
							rs.toString());
				}
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
			System.exit(0);
		}
		
		InferScheme scheme = InferScheme.valueOf(args[1]);		
		int numsamp = Integer.parseInt(args[2]);
		int numouter= Integer.parseInt(args[3]); 
		int numinner = Integer.parseInt(args[4]); 		
		int randseed = Integer.parseInt(args[5]); 
		int k = Integer.parseInt(args[6]);
				
		try 
		{
			// Each row corresponds to one fold
			// -1st col is E[LDA] over MC estimation samples
			// -2nd col is E[Logic] over MC estimation samples 
			double[][] genResults = new double[k][2];
			ArrayList<String> testlines = new ArrayList<String>();
			ArrayList<String> trainlines = new ArrayList<String>();
			
			// Load corpus and parameters (checking vocab dim agreement)
			LDAParameters p = new LDAParameters(basefn, randseed);

			// Construct train/test folds
			CrossFold cf = new CrossFold(new Corpus(basefn), k, randseed);
						
			// For each fold
			for(int ki = 0; ki < k; ki++)
			{
				// Adjust random seed and re-instantiate parameters
				randseed++;
				p = new LDAParameters(basefn, randseed);
				
				// Get train/test corpora
				Corpus train = cf.getTrain(ki);
				Corpus test = cf.getTest(ki);				
				FileUtil.fileSpit(String.format("%s.train%d", basefn, ki),
						train.doclist.toString());
				FileUtil.fileSpit(String.format("%s.test%d", basefn, ki),
						test.doclist.toString());				
				
				// Load trainset logic rules (without an LDA rule)
				// 				
				MirrorDescent rs = LogicLDA.constructRuleSet(basefn, 
						train, p.T, randseed, false);
				System.out.println(rs.toString());

				// Get init sample from StandardLDA Collapsed Gibbs
				DiscreteSample s = StandardLDA.runStandardLDA(train, p, numsamp);
				
				// Get phi depending on which scheme we are using
				double[][] phi = null;
				List<LogicRule> rules;
				switch(scheme)
				{
				case LDA: 	
					// we already have standard LDA result
					phi = new double[p.T][train.W];
					phi = s.mapPhi(p, phi);					
					break;
				case MWS:
					// Load logic rules
					rules = 
						LogicLDA.readRules(String.format("%s.rules",basefn));
					// Run MaxWalkSAT for numouter steps
					s = MaxWalkSAT.runMWS(train, p, s, rules, PRAND, numinner*numouter);
					phi = new double[p.T][train.W];
					phi = s.mapPhi(p, phi);
					break;
				case CGS:
					// Load logic rules
					rules = 
						LogicLDA.readRules(String.format("%s.rules",basefn));				
					// Run GroundGibbs
					s = CollapsedGibbs.runGroundGibbs(train, p, 
							s, rules, numsamp); 
					phi = new double[p.T][train.W];
					phi = s.mapPhi(p, phi);
					break;					
				case MPL:
					// Load logic rules
					rules = 
						LogicLDA.readRules(String.format("%s.rules",basefn));				
					// Run LDAMaxWalkSAT
					s = LDAMaxWalkSAT.runLDAMWS(train, p, rules, 
							numouter, numinner, PRAND, s);						 
					phi = new double[p.T][train.W];
					phi = s.mapPhi(p, phi);
					break;
				case MIR:
					// Load logic rules
					rules = 
						LogicLDA.readRules(String.format("%s.rules",basefn));				
					// Run LDAMaxWalkSAT
					RelaxedSample relax = MirrorDescent.runSGD(train, p, rules, 
							numouter, numinner, s);
					s = new DiscreteSample(train.N, p.T, train.W, train.D, 
							relax.getZ(), train);					
					phi = relax.phi;					
					break;
				case ALC:
					// Load pre-computed phi directly from file
					Matrix matphi = 
							FileUtil.readDoubleMatFile(String.format
									("%s-%d.phi", basefn, ki));
					List<Integer> alclist = FileUtil.readIntFile(
							String.format("%s-%d.alcsample", basefn, ki));
					int[] alcsample = new int[alclist.size()];
					int i = 0;
					for(Integer zi : alclist)
					{
						alcsample[i] = zi;
						i++;
					}
					s = new DiscreteSample(train.N, p.T, train.W, train.D, 
							alcsample, train);
					phi = matphi.toDoubleArray();
					break;
				default:
					System.out.println("ERROR: No match for scheme?!");
					System.exit(1);
				}
				
				// Use fixed phi to estimate LDA and Logic objective function values
				MirrorDescent testrules = 
					LogicLDA.constructRuleSet(basefn, test, p.T, p.randseed, false);
				
				// Save numerical values and generate output line
				genResults[ki] = estimateObjectives(test, p, testrules, phi);
				testlines.add(String.format("%f\t%f\t%f", genResults[ki][0], 
						genResults[ki][1],
						testrules.totalWeight()));
				
				// Do the same for trainset objective
				double trainlda = EvalLDA.ldaLoglike(s.nw, s.nd, phi, 
							s.mapTheta(p, new double[train.D][p.T]),
							p.beta, p.alpha);
				MirrorDescent trainrules =
					LogicLDA.constructRuleSet(basefn, train, p.T, 
							p.randseed, false);
				double trainlogic = trainrules.satWeight(s.z);
				trainlines.add(String.format("%f\t%f\t%f", trainlda, 
						trainlogic, trainrules.totalWeight()));
				
				// Write out topics
				test.writeTopics(String.format("%s-%s-%d", 
						basefn, scheme.toString(), ki),  						
						phi, Math.min(10, test.vocab.size()));
			}
			
			// Write out results to files						
			FileUtil.writeLines(testlines, String.format("%s-%s.cfv", basefn, 
					scheme.toString()));			
			FileUtil.writeLines(trainlines, String.format("%s-%s.traincfv", basefn, 
					scheme.toString()));
		}										
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
		
	/**
	 * Do numsamp samples on test corpus with fixed phi, 
	 * starting from init sample testz
	 * 
	 * @param test
	 * @param p
	 * @param phi
	 * @param testz
	 * @param numsamp
	 * @return
	 */
	public static DiscreteSample doFixedPhiSamples(Corpus test, LDAParameters p, 
			double[][] phi, DiscreteSample testz, int numsamp)
	{
		for(int j = 0; j < numsamp; j++)		
			CollapsedGibbs.fixedPhiSample(test, p, testz, false, phi);
		return testz;
	}
	
	
	/**
	 * Given a fixed phi, estimate expectations of LDA and Logic objectives 
	 * on test documents using Collapsed Gibbs sampling
	 * 
	 * @param test
	 * @param p
	 * @param rules
	 * @param phi
	 * @return
	 */
	public static double[] estimateObjectives(Corpus test, LDAParameters p, 
			MirrorDescent rules, double[][] phi)
	{
		// Init Gibbs inference with phi fixed
		DiscreteSample testz = new DiscreteSample(test.N, p.T, test.W, test.D); 
		CollapsedGibbs.fixedPhiSample(test, p, testz, true, phi);

		// Do burnin samples		
		for(int i = 0; i < BURNIN; i++)
			CollapsedGibbs.fixedPhiSample(test, p, testz, false, phi);

		// Do MC estimation samples
		double[][] theta = new double[test.D][p.T];
		double[] retval = new double[2];		
		for(int i = 0; i < MCSAMP; i++)
		{
			for(int j = 0; j < INTERVAL; j++)
				CollapsedGibbs.fixedPhiSample(test, p, testz, false, phi);
			theta = testz.mapTheta(p, theta);
			
			retval[0] += EvalLDA.ldaLoglike(testz.nw, testz.nd, phi, theta, 
					p.beta, p.alpha) / MCSAMP;
			retval[1] += rules.satWeight(testz.z) / MCSAMP;
		}
		return retval;
	}
}
