package logiclda.rules;

import java.util.*;

import logiclda.Corpus;
import logiclda.MiscUtil;
import logiclda.SideInfoType;
import logiclda.infer.Gradient;
import logiclda.infer.RelaxedSample;

public class SentExclRule implements LogicRule {

	private int excluder;
	private int excludee;
	
	private double sampWeight;
	private double stepWeight;
	
	// Maps sentence idx --> list of corpus idxs in that sentence
	private HashMap<Integer, ArrayList<Integer>> sentences;
	private double[] sentWeights; 
	private long numGround;
	
	private int[] indices;
	private int T;
	
	public SentExclRule(double sampWeight, double stepWeight,
			Vector<String> argToks) 
	{	
		// Error check input
		assert (argToks.size() == 2);
	
		// Save weights 		
		this.sampWeight = sampWeight; // used for rule sampling 
		this.stepWeight = stepWeight; // used for gradient step-size
		
		// Get "excluder" topic
		//
		this.excluder = new Integer(argToks.get(0));
		
		// Get "excludee" topics
		//
		this.excludee = new Integer(argToks.get(1));
		
		// Init sentences to null (will be set later by applyEvidence)
		//
		this.indices = new int[2];		
		this.sentences = null;
	}
	
	public String toString()
	{
		return String.format("Sentence Exclusion Rule " +
				"(samp weight=%.1f, step weight=%.1f)" +
				"\nExcluder = %d\nExcludee = %d\n",
				sampWeight, stepWeight, excluder, excludee);				
	}
	
	public void applyEvidence(Corpus c, int T) {
		String sentStr = SideInfoType.SENTENCE.infoName;
		assert(c.sideInfo.containsKey(sentStr));
		assert(excluder < T && excludee < T);
		
		// Not sure how to make this cast type-safe?
		//
		// Idea: put (int[]) type in SideInfoType SENTENCE enum,
		// (but I don't know how to do that in Java...)
		int[] sentidx = (int[]) c.sideInfo.get(sentStr); 
		
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
		
		// We will sample groundings uniformly from sentence length squared
		//  
		sentWeights = new double[sentences.keySet().size()];
		numGround = 0;
		try 
		{
			for(Map.Entry<Integer, ArrayList<Integer>> entry : 
				sentences.entrySet())
			{
				double current = Math.pow(entry.getValue().size(), 2);
				sentWeights[entry.getKey()] = current;
				numGround += current;
			}
		}
		catch (ArrayIndexOutOfBoundsException aioobe)
		{
			System.out.println(aioobe.toString());
			System.out.println("ERROR: non-sequential sentence indices");
			System.out.println("(e.g., 1 2 3 5)");
			System.exit(1);
		}
		
		this.T = T;		
	}

	//
	// Methods below should only be called after applyEvidence()
	//
	
	/**
	 * Ensure that evidence has been applied 
	 */	
	private void evidenceCheck(String methodname)
	{
		if(sentences == null)
		{
			String errmsg = String.format("ERROR: %s called without applying evidence", 
					methodname);
			System.out.println(errmsg);					
			System.exit(1);		
		}		
	}
	
	public double getWeight() 
	{
		evidenceCheck("getWeight()");
		return sampWeight * numGround;		
	}

	public Gradient randomGradient(RelaxedSample relax, Random rng) 
	{	
		evidenceCheck("randomGradient()");
		
		// Sample a random sentence
		//
		int si = MiscUtil.multSample(rng, sentWeights, (double) numGround);
		
		// Sample a pair of corpus indices within this sentence
		//
		ArrayList<Integer> sentIdx = sentences.get(si);
		int excluderIdx = sentIdx.get(rng.nextInt(sentIdx.size()));
		int excludeeIdx = sentIdx.get(rng.nextInt(sentIdx.size()));
		indices[0] = excluderIdx;
		indices[1] = excludeeIdx;
		
		// Construct gradient
		//
		double[][] gradient = new double[2][this.T]; // default value is zero 
		gradient[0][excluder] = -1 * stepWeight * relax.zrelax[excludeeIdx][excludee];
		gradient[1][excludee] = -1 * stepWeight * relax.zrelax[excluderIdx][excluder];
		
		return new Gradient(gradient, indices);
	}
	
	public long numGroundings() 	
	{		
		evidenceCheck("numGroundings()");		
		return numGround;
	}

	public int numSat(int[] z) 
	{
		evidenceCheck("numSat()");		
		
		// Count number of unsatisifed groundings
		long numUnsat = 0;
		for(Map.Entry<Integer, ArrayList<Integer>> entry : 
			sentences.entrySet())
		{	
			int nExcluder = 0;
			int nExcludee = 0;
			for(int idx : entry.getValue())
				if(z[idx] == excluder)
					nExcluder += 1;
				else if(z[idx] == excludee)
					nExcludee += 1;
			numUnsat += nExcluder * nExcludee;			
		}
		
		return (int) (numGroundings() - numUnsat); 				
	}

}
