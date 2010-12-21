package logiclda.rules;

import java.util.*;

import logiclda.Corpus;
import logiclda.infer.Gradient;
import logiclda.infer.RelaxedSample;
 
import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;

public class SeedRule implements IndependentRule, GroundableRule {

	private Seeds seedWords;
	private HashSet<Integer> hashSeedTopics;
	private int[] groundings;
	
	private double sampWeight;
	private double stepWeight;
	
	private int[] indices;
	private double[][] gradient;
	
	// below members are only used if rule is grounded as GroundableRule
	private Map<Integer, Set<Grounding>> invIndex;
	private Set<Grounding> unsat;
	
	/**
	 * If wi in seed words, then zi in seed topics
	 *   
	 * @param argToks
	 */
	public SeedRule(double sampWeight, double stepWeight,
			Vector<String> argToks) {
	
		// Error check input
		assert (argToks.size() == 2);
	
		// Save weights 		
		this.sampWeight = sampWeight; // used for rule sampling 
		this.stepWeight = stepWeight; // used for gradient step-size
		
		// Get HashSet of seed words (or n-grams)
		//
		// Format:
		// 3434, 456 732, 563
		// means that our seed terms are:
		// 3434
		// 456 732 (only when appearing as bigram)
		// 563
		// 
		//
		seedWords = new Seeds(argToks.get(0));
		
		// Get HashSet of seed topics
		//
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
		
		// Init yet other members
		// (will be set by groundRule)
		// below members are only used if rule is grounded as GroundableRule
		this.invIndex = null;
		this.unsat = null;
	}

	public double getRuleWeight()
	{
		return this.sampWeight * this.stepWeight;
	}
	
	public double[][] toZLabel(int N, int T)
	{
		evidenceCheck("toZLabel()");

		 // entries *not* associated with weights will be null
		double[][] retval = new double[N][];
		
		for(int i : groundings)	
		{
			retval[i] = new double[T];		
			for(int t = 0; t < T; t++)			
				retval[i][t] = gradient[0][t] * sampWeight;
		}
		return retval;
	}
	
	public String toString()
	{
		return String.format("Seed Rule (samp weight=%.1f, step weight=%.1f)" +
				"\nWords = %s\nTopics = %s\n",
				sampWeight, stepWeight,
				seedWords.toString(),
				Arrays.toString(hashSeedTopics.toArray()));
	}
	
	/**
	 * Find non-trivial groundings of this rule
	 * @param c
	 * @param p
	 */
	public void applyEvidence(Corpus c, int T)
	{
		// Get all non-trival ground indices
		//
		HashSet<Integer> hashGround = new HashSet<Integer>();
		for(Integer wi = 0; wi < c.N; wi++)
		{
			int hit = seedWords.hit(c.w, wi);
			if(hit > 0)
				for(int hiti = 0; hiti < hit; hiti++)
					hashGround.add(wi + hiti);			
		}		
		 
		// Convert from HashSet to int[]
		//
		Integer[] boxground = new Integer[hashGround.size()];
		boxground = hashGround.toArray(boxground);	
		groundings = new int[hashGround.size()];
		for(int i = 0; i < groundings.length; i++)
			groundings[i] = boxground[i];
		
		// Pre-calc gradients calculations
		// (not dependent on zrelax so will never change)
		//
		gradient = new double[1][T];
		for(Integer t = 0; t < T; t++)		
			if(hashSeedTopics.contains(t))		
				gradient[0][t] = stepWeight;
			else
				gradient[0][t] = 0;		
	}
	
	//
	// Methods below should only be called after applyEvidence()
	//
	
	/**
	 * Ensure that evidence has been applied 
	 */	
	private void evidenceCheck(String methodname)
	{
		if(groundings == null)
		{
			String errmsg = String.format("ERROR: %s called without applying evidence", 
					methodname);
			System.out.println(errmsg);					
			System.exit(1);		
		}		
	}
	
	public double getTotalSamplingWeight() 
	{
		evidenceCheck("getWeight()");
		
		return (this.groundings.length) * this.sampWeight;		
	}

	public long numGroundings()
	{
		evidenceCheck("numGroundings()");
		
		return this.groundings.length;
	}
	
	public Gradient randomGradient(RelaxedSample relax, Random rng)
	{
		evidenceCheck("randomGradient()");
		
		// Sample a random grounding
		indices[0] = this.groundings[rng.nextInt(this.groundings.length)];		
		
		// Return random gradient
		return new Gradient(gradient, indices);		
	}
	
	/**
	 * Count satisfied groundings for this rule
	 * (easy to calc)
	 */
	public int numSat(int[] z)
	{
		evidenceCheck("numSat()");
				
		int sat = 0;
		for(int gi : this.groundings)
			if(hashSeedTopics.contains(z[gi]))
				sat += 1;
		return sat;
	}
	
	//
	// GroundableRule methods
	//

	/**
	 * Ensure that evidence has been applied 
	 */	
	private void groundCheck(String methodname)
	{
		if(this.invIndex == null)
		{
			String errmsg = String.format(
					"ERROR: %s called before grounding rule", 
					methodname);
			System.out.println(errmsg);					
			System.exit(1);		
		}		
	}
	
	/**
	 * For this rule, is a particular grounding satisfied?
	 * 
	 * @param z
	 * @param g
	 * @return
	 */
	private boolean groundingSat(int[] z, Grounding g)
	{
		return(this.hashSeedTopics.contains(z[g.get(0)]));		
	}
	
	public void groundRule(int[] z)
	{
		// Must have applied evidence before we can 
		// generate non-trivial groundings
		evidenceCheck("groundRule()");
		
		// Init data struct
		this.invIndex = new HashMap<Integer, Set<Grounding>>();
		this.unsat = new HashSet<Grounding>();
		
		for(int idxa : this.groundings)
		{
			// Ensure we have inverted index entries for idxa			
			if(!this.invIndex.containsKey(idxa))
				this.invIndex.put(idxa, new HashSet<Grounding>());				
			
			// Add this grounding to each entry
			Grounding newg = new Grounding(idxa);
			this.invIndex.get(idxa).add(newg);
				
			// Initialize unsat
			if(!groundingSat(z, newg))
				this.unsat.add(newg);				
		}
	}
	
	public double evalAssign(int[] z, int idx)
	{
		groundCheck("evalAssign()");
		
		if(!this.invIndex.containsKey(idx))
			return 0;
		
		double satweight = 0;
		for(Grounding g : this.invIndex.get(idx))
		{
			if(groundingSat(z, g))
				satweight += this.sampWeight * this.stepWeight;
		}
		return satweight;
	}

	public Map<Integer, Set<Grounding>> getInvIndex()
	{
		groundCheck("getInvIndex()");
		return this.invIndex;
	}
		
	public Set<Grounding> getUnSat()
	{
		groundCheck("getUnSat()");
		return this.unsat;
	}
	
	public void updateUnSat(int[] z, int idx)
	{			
		groundCheck("updateUnSat()");
		
		if(!this.invIndex.containsKey(idx))
			return;
		
		for(Grounding g : this.invIndex.get(idx))
		{
			if(!groundingSat(z,g))			
				this.unsat.add(g);
			else
				this.unsat.remove(g);
		}
	}

}
