package logiclda.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Random;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;

import logiclda.Corpus;
import logiclda.infer.Gradient;
import logiclda.infer.RelaxedSample;

public class MLRule implements GroundableRule
{
	// The two word types to be Must-Linked
	private int wordA;
	private int wordB;

	// Gradient sampling and stepsize weights 
	private double sampWeight;
	private double stepWeight;

	// indices of occurrences of word A and word B
	private ArrayList<Integer> idxA;
	private ArrayList<Integer> idxB;

	private int[] indices;
	private double[][] gradient;
	private int T;

	// below members are only used if rule is grounded as GroundableRule
	private Map<Integer, Set<Grounding>> invIndex;
	private Set<Grounding> unsat;
	
	public MLRule(double sampWeight, double stepWeight,
			Vector<String> argToks)
	{
		// Error check input
		assert (argToks.size() == 2);

		// Save weights 		
		this.sampWeight = sampWeight; // used for rule sampling 
		this.stepWeight = stepWeight; // used for gradient step-size

		// Get the two Must-Linked words
		//
		this.wordA = new Integer(argToks.get(0));
		this.wordB = new Integer(argToks.get(1));

		// Init other members
		// (will be set by applyEvidence)
		//
		this.indices = null;
		this.gradient = null;
		this.idxA = null;
		this.idxB = null;
		
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
	
	
	/**
	 * Ensure that evidence has been applied 
	 */	
	private void evidenceCheck(String methodname)
	{
		if(this.idxA == null)
		{
			String errmsg = String.format("ERROR: %s called without applying evidence", 
					methodname);
			System.out.println(errmsg);					
			System.exit(1);		
		}		
	}

	/**
	 * Total weight of rule (for sampling purposes) 
	 */
	public double getTotalSamplingWeight() 
	{
		evidenceCheck("getWeight()");
		return this.sampWeight * this.numGroundings();
	}

	/**
	 * Sample a random rule grounding
	 */
	public Gradient randomGradient(RelaxedSample relax, Random rng) 
	{
		evidenceCheck("randomGradient()");
		// Sample a word A idx and a word B idx
		int ai = this.idxA.get(rng.nextInt(this.idxA.size()));
		int bi = this.idxB.get(rng.nextInt(this.idxB.size()));		
		indices[0] = ai;
		indices[1] = bi;

		// ML logic clause to corresponding polynomial is
		// (zi0 => zj0) ^ (zi1 => zj1) 
		// 1 - ( (1-zi0)*zj0 + (1-zi1)*zj1 + ...)
		//
		// We need to choose which 'side' of our bidirectional implication
		// to take the gradient of (shouldn't really matter...)		
		//
		int bidir = rng.nextInt(2);
		if(bidir == 0)
		{
			for(int ti = 0; ti < this.T; ti++)
			{		
				// Construct the gradient zit => zjt
				this.gradient[0][ti] = 
					stepWeight * (relax.zrelax[bi][ti] - 1);
				this.gradient[1][ti] = 
					stepWeight * relax.zrelax[ai][ti];
			}
		}
		else
		{
			for(int ti = 0; ti < this.T; ti++)
			{		
				// Construct the gradient for zit <= zjt
				this.gradient[0][ti] = 
					stepWeight * relax.zrelax[bi][ti];
				this.gradient[1][ti] = 
					stepWeight * (relax.zrelax[ai][ti] - 1);
			}
		}	
				
		return new Gradient(gradient, indices);		
	}

	/**
	 * Given the corpus, find occurrences of word A and word B
	 */
	public void applyEvidence(Corpus c, int T) 
	{
		this.idxA = new ArrayList<Integer>();
		this.idxB = new ArrayList<Integer>();

		for(int idx = 0; idx < c.w.length; idx++)
		{
			if(c.w[idx] == this.wordA)
				this.idxA.add(idx);
			else if(c.w[idx] == this.wordB)
				this.idxB.add(idx);
		}

		// Init other fields
		this.T = T;
		this.indices = new int[2];
		this.gradient = new double[2][T];		
	}

	/**
	 * Nice summary of rule for logic report
	 */
	public String toString()
	{
		return String.format("Must-Link Rule " +
				"(samp weight=%.1f, step weight=%.1f)" +
				"\nWord A = %d\nWord B = %d\n",
				sampWeight, stepWeight, wordA, wordB);
	}
	
	/**
	 * How many (non-trivial) groundings does this rule have?
	 * 
	 * (could multiply by 2 to account for each side of bidirectional,
	 * but do not, in order to be consistent with Cannot-Link rule)
	 */
	public long numGroundings() 
	{
		evidenceCheck("numGroundings()");
		return (this.idxA.size() * this.idxB.size());
	}

	/**
	 * For this z-assignment, how many groundings are satisfied?
	 */
	public int numSat(int[] z) 
	{		
		// Get topic assignment counts for all word A idx 
		int[] za = new int[this.T]; // default value is zero
		for(int idx : this.idxA)
			za[z[idx]] += 1;

		// Get topic assignment counts for all word B idx 
		int[] zb = new int[this.T]; // default value is zero
		for(int idx : this.idxB)
			zb[z[idx]] += 1;

		// For each topic, all PAIRS of idxA-idxB entries assigned 
		// to that topic correspond to satisfied groundings
		long numSat = 0;
		for(int zi = 0; zi < this.T; zi++)
			numSat += za[zi] * zb[zi];

		return (int) numSat;
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
		int idxa = g.get(0);
		int idxb = g.get(1);
		return (z[idxa] == z[idxb]);
	}
	
	public void groundRule(int[] z)
	{
		// Must have applied evidence before we can 
		// generate non-trivial groundings
		evidenceCheck("groundRule()");
		
		// Init data struct
		this.invIndex = new HashMap<Integer, Set<Grounding>>();
		this.unsat = new HashSet<Grounding>();
		
		for(int idxa : this.idxA)
			for(int idxb : this.idxB)
			{
				// Ensure we have inverted index entries for idxa and idxb
				if(!this.invIndex.containsKey(idxa))
					this.invIndex.put(idxa, new HashSet<Grounding>());
				if(!this.invIndex.containsKey(idxb))
					this.invIndex.put(idxb, new HashSet<Grounding>());
				
				// Add this grounding to each entry
				Grounding newg = new Grounding(idxa, idxb);								
				this.invIndex.get(idxa).add(newg);
				this.invIndex.get(idxb).add(newg);
					
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