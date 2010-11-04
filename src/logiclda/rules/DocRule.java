package logiclda.rules;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import logiclda.Corpus;
import logiclda.SideInfoType;
import logiclda.infer.Gradient;
import logiclda.infer.RelaxedSample;

public class DocRule implements IndependentRule {

	private int docLabel;
	private HashSet<Integer> hashSeedTopics;
	private int[] groundings;
	
	private double sampWeight;
	private double stepWeight;
	
	private int[] indices;
	private double[][] gradient;
	
	
	/**
	 * If doc has label, then z-label words in doc
	 * to provided topics
	 */
	public DocRule(double sampWeight, double stepWeight,
			Vector<String> argToks)
	{
		// Error check input
		assert (argToks.size() == 2);

		// Save weights 		
		this.sampWeight = sampWeight; // used for rule sampling 
		this.stepWeight = stepWeight; // used for gradient step-size

		// Get docLabel of interest	
		docLabel = Integer.parseInt(argToks.get(0));

		// Get HashSet of seed topics
		Vector<Integer> vecSeedTopics = new Vector<Integer>();
		StringTokenizer seedtok = new StringTokenizer(argToks.get(1), ",");
		while (seedtok.hasMoreTokens())
			vecSeedTopics.add(Integer.parseInt(seedtok.nextToken()));
		hashSeedTopics = new HashSet<Integer>();
		for(Integer zi : vecSeedTopics)		
			hashSeedTopics.add(zi);		

		// Init groundings to null (will be set later by applyEvidence)
		this.indices = new int[1];
		this.groundings = null;			
	}
	
	@Override
	public void applyEvidence(Corpus c, int T) 
	{
		// error-check 
		String doclabelStr = SideInfoType.DOCLABEL.infoName;
		assert(c.sideInfo.containsKey(doclabelStr));
		for(int t : hashSeedTopics)
			assert(t >= 0 && t < T);
		
		// get document labels 
		int[] doclabels = (int[]) c.sideInfo.get(doclabelStr); 

		// find all idx where corresponding doc has label doclabel
		List<Integer >groundList = new ArrayList<Integer>();
		for(int i = 0; i < c.d.length; i++)
			if(doclabels[c.d[i]] == docLabel)
				groundList.add(i);
		this.groundings = new int[groundList.size()];
		for(int i = 0; i < groundList.size(); i++)
			this.groundings[i] = groundList.get(i);
		
		// Pre-calc gradients calculations
		// (not dependent on zrelax so will never change)
		//
		this.gradient = new double[1][T];
		for(Integer t = 0; t < T; t++)		
			if(hashSeedTopics.contains(t))		
				gradient[0][t] = stepWeight;
			else
				gradient[0][t] = 0;	
	}
	
	/**
	 * Ensure that evidence has been applied 
	 */	
	private void evidenceCheck(String methodname)
	{
		if(this.groundings == null)
		{
			String errmsg = String.format("ERROR: %s called without applying evidence", 
					methodname);
			System.out.println(errmsg);					
			System.exit(1);		
		}		
	}	
	
	//
	// Methods below should only be called after applyEvidence()
	//	
	
	public Map<Integer, ArrayList<Double>> toZLabel(int N, int T)
	{
		evidenceCheck("toZLabel()");

		// Instantiate gradient		
		ArrayList<Double> gradvals = new ArrayList<Double>();
		for(int t = 0; t < T; t++)
			gradvals.add(gradient[0][t] * sampWeight);
		
		// Populate z-label weights
		Map<Integer, ArrayList<Double>> retval = 
			new HashMap<Integer, ArrayList<Double>>();	 			
		for(int i : groundings)		
			retval.put(i, gradvals);
								
		return retval;		
	}
	
	@Override
	public double getTotalSamplingWeight() 
	{		
		evidenceCheck("getTotalSamplingWeight()");

		return this.sampWeight * this.groundings.length;
	}

	@Override
	public double getRuleWeight() 
	{
		evidenceCheck("getRuleWeight()");

		return this.sampWeight * this.stepWeight;
	}

	@Override
	public Gradient randomGradient(RelaxedSample relax, Random rng) 
	{
		evidenceCheck("randomGradient");

		// Sample a random grounding
		indices[0] = this.groundings[rng.nextInt(this.groundings.length)];		
		
		// Return random gradient
		return new Gradient(gradient, indices);
	}	

	@Override
	public long numGroundings() 
	{
		evidenceCheck("numGroundings()");

		return this.groundings.length;
	}

	@Override
	public int numSat(int[] z) 
	{
		evidenceCheck("numSat()");
				
		int sat = 0;
		for(int gi : this.groundings)
			if(hashSeedTopics.contains(z[gi]))
				sat += 1;
		return sat;		
	}

}
