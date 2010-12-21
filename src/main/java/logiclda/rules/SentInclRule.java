package logiclda.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import logiclda.Corpus;
import logiclda.SideInfoType;
import logiclda.infer.Gradient;
import logiclda.infer.RelaxedSample;
import logiclda.MiscUtil;

public class SentInclRule implements LogicRule 
{
	private int includer;
	private int includee;
	
	private double sampWeight;
	private double stepWeight;
	
	// Maps sentence idx --> list of corpus idxs in that sentence
	private HashMap<Integer, ArrayList<Integer>> sentences;
	private int[] sentidx;
	private long numGround;

	public SentInclRule(double sampWeight, double stepWeight,
			Vector<String> argToks) 
	{	
		// Error check input
		assert (argToks.size() == 2);
	
		// Save weights 		
		this.sampWeight = sampWeight; // used for rule sampling 
		this.stepWeight = stepWeight; // used for gradient step-size
		
		// Get "includer" topic
		//
		this.includer = new Integer(argToks.get(0));
		
		// Get "includee" topics
		//
		this.includee = new Integer(argToks.get(1));
		
		// Init sentences and sentidx to null 
		// (will be set later by applyEvidence)
		//		
		this.sentences = null;
	}
	
	public double getRuleWeight()
	{
		return this.sampWeight * this.stepWeight;
	}
	
	public String toString()
	{
		return String.format("Sentence Inclusion Rule " +
				"(samp weight=%.1f, step weight=%.1f)" +
				"\nIncluder = %d\nIncludee = %d\n",
				sampWeight, stepWeight, includer, includee);				
	}
	
	public void applyEvidence(Corpus c, int T) 
	{
		// Extract sentence information from Corpus side info
		String sentStr = SideInfoType.SENTENCE.infoName;
		assert(c.sideInfo.containsKey(sentStr));
		assert(includer < T && includee < T);
		
		// Not sure how to make this cast type-safe?
		//
		// Idea: put (int[]) type in SideInfoType SENTENCE enum,
		// (but I don't know how to do that in Java...)
		sentidx = (int[]) c.sideInfo.get(sentStr); 
		
		// Build hash: [sentence idx] --> [associated corpus idx]
		//
		sentences = new HashMap<Integer, ArrayList<Integer>>();
		for(int i = 0; i < sentidx.length; i++)
		{
			int si = sentidx[i];
			
			if(!sentences.containsKey(si))
				sentences.put(si, new ArrayList<Integer>());			
			sentences.get(si).add(i);			
		}
		
		// The number of groundings is simply equal to the corpus length
		numGround = sentidx.length;		
	}	
	
	/**
	 * Ensure that evidence has been applied 
	 */	
	private void evidenceCheck(String methodname)
	{
		if(sentences == null)
		{
			String errmsg = 
				String.format("ERROR: %s called without applying evidence",
						methodname);
			System.out.println(errmsg);					
			System.exit(1);		
		}		
	}
	
	
	public double getTotalSamplingWeight() 
	{
		evidenceCheck("getWeight()");				
		return numGround * sampWeight;
	}

	public long numGroundings() 
	{
		evidenceCheck("numGroundings()");
		return numGround;
	}

	public Gradient randomGradient(RelaxedSample relax, Random rng) 
	{
		evidenceCheck("randomGradient()");
		
		// Randomly sample any index (this will be the includee)
		//
		int incleeIdx = rng.nextInt(sentidx.length);
		
		// Get the indices of all other words in that sentence
		//
		ArrayList<Integer> coSentence = sentences.get(sentidx[incleeIdx]);
		
		// Init gradients and indices
		//
		int S = coSentence.size();		
		int T = relax.zrelax[0].length;
		int[] indices = MiscUtil.intListUnbox(coSentence);
		double[][] gradients = new double[S][T];
		
		// Populate gradient
		//
		// Poly: 1 - (1 - zjTr) (1 - zkTr) ... (ziTe)
		// 
		// (i is incleeIdx, Te is inclee topic, Tr is incler topic)
		//
		for(int i = 0; i < indices.length; i++)
		{
			// Which topic has a non-zero gradient for this index?
			int t = -1;
			if(indices[i] == incleeIdx)
			{
				t = this.includee;
				gradients[i][t] = -1 * stepWeight;
			}
			else
			{
				t = this.includer;
				gradients[i][t] = 1 * stepWeight;
			}
						
			// To calc gradient, sum entries for all entries OTHER THAN i			
			for(int j = 0; j < indices.length; j++)
			{
				if(j == i)
					continue;
				
				// Need to handle the includee index differently 
				if(indices[j] == incleeIdx)
				{
					gradients[i][t] *= relax.zrelax[indices[j]][includee];
				}
				else
				{
					gradients[i][t] *= (1 - relax.zrelax[indices[j]][includer]);
				}				
			}
		}
		
		return new Gradient(gradients, indices);
	}
	
	public int numSat(int[] z) 
	{
		evidenceCheck("numSat()");
		
		long numUnsat = 0;

		// Look for unsatisifed occurrences in each sentence
		for(Map.Entry<Integer, ArrayList<Integer>> entry : 
			sentences.entrySet())
		{	
			// Have we seen the includER in this sentence?
			boolean includerPresent = false;
			// How many includEEs have we seen in this sentence?
			int nIncludee = 0;
			
			for(int idx : entry.getValue())
				if(z[idx] == includer)
					includerPresent = true;
				else if(z[idx] == includee)
					nIncludee += 1;
			
			// If we did not see the includER, then
			// all includEEs represent rule violations
			if(!includerPresent)
				numUnsat += nIncludee;
		}
		
		return (int) (numGroundings() - numUnsat); 				
	}

}


