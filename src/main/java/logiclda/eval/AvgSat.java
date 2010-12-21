package logiclda.eval;

import logiclda.rules.LogicRule;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to record average satisfaction over multiple samples
 * 
 * @author andrzejewski1
 *
 */
public class AvgSat 
{
	private ArrayList<ArrayList<Double>> sat;
	private LogicRule[] rules;	
	
	public AvgSat(LogicRule[] rules)
	{
		// The rules we are interested in 
		this.rules = rules;
		// Number of satisfied groundings for each sample
		sat = new ArrayList<ArrayList<Double>>();
		for(LogicRule lr : rules)
			sat.add(new ArrayList<Double>());		
	}
	
	public static void main(String[] args)
	{
		ArrayList<Double> testvals = new ArrayList<Double>();
		testvals.add(3.0);
		testvals.add(4.0);
		testvals.add(5.0);		
		System.out.print(String.format("std dev = %f", std(testvals)));
		System.out.print(String.format("mean = %f", avg(testvals)));
		
	}
	
	/**
	 * Record satisfaction of each logic rule
	 * @param z
	 */
	public void recordSatisfaction(int[] z)
	{
		for(int ri = 0; ri < rules.length; ri++)
			sat.get(ri).add((double) rules[ri].numSat(z));		
	}

	/**
	 * Summing over all rules, what was the percentage satisfied?
	 *  
	 * @return
	 */
	public double getAvgPercentSat(boolean weighted)	
	{
		return getTotalAvgSat(weighted) / getTotalGround(weighted);
	}
	
	/**
	 * Summing over all rules, what was std of percentage satisfied 
	 * over the samples?
	 * 
	 * @param weighted
	 * @return
	 */
	public double getStdPercentSat(boolean weighted)
	{
		// How many samples do we have per rule?
		int n = this.sat.get(0).size();
		
		// For each sample, sum the satisfied weight of all rules
		ArrayList<Double> sampTotals = new ArrayList<Double>();
		for(int ni = 0; ni < n; ni++)
			sampTotals.add(getSampTotal(ni, weighted) / getTotalGround(weighted));
				
		return std(sampTotals);		
	}
	
	/**
	 * For a given sample ni, the total satisfied weight across rules
	 * 
	 * @param ni
	 * @param weighted
	 * @return
	 */
	private double getSampTotal(int ni, boolean weighted)
	{
		// Sum ni-th entry of sat counts for each rule
		double total = 0;
		for(int ri = 0; ri < this.rules.length; ri++)
		{
			if(weighted)
				total += this.sat.get(ri).get(ni) * this.rules[ri].getRuleWeight();
			else
				total += this.sat.get(ri).get(ni);							
		}
		return total;	
	}
	
	/**
	 * What are the total number of non-trivial groundings for all rules?
	 * 
	 * @return
	 */
	public double getTotalGround(boolean weighted)
	{
		double retval = 0;
		for(LogicRule lr : this.rules)		
		{
			if(weighted)
				retval += lr.getRuleWeight() * lr.numGroundings();
			else
				retval += lr.numGroundings();				
		}			
		return retval;
	}
	
	public static double avg(List<Double> vals)
	{
		double sum = 0;
		for(double val : vals)
			sum += val;
		return ((double) sum) / vals.size();			
	}
	
	public static double std(List<Double> vals)
	{
		double mu = avg(vals);
		double sqdiffsum = 0;
		for(double val : vals)
			sqdiffsum += Math.pow(mu - val, 2); 			
		return Math.sqrt(sqdiffsum / (vals.size() - 1));		
	}
	
	/**
	 * For each rule, what were avg satisfaction over MC samples?
	 * 
	 * @return
	 */
	public double getTotalAvgSat(boolean weighted)
	{
		double retval = 0;
		for(int ri = 0; ri < this.rules.length; ri++)
		{
			ArrayList<Double> ruleSat = this.sat.get(ri);
			if(weighted)
				retval += avg(ruleSat) * this.rules[ri].getRuleWeight();
			else
				retval += avg(ruleSat);
		}
					
		return retval;		  
	}
	
	/**
	 * Nice satisfaction report string
	 * 
	 * @return
	 */
	public String report()
	{
		StringBuilder retval = new StringBuilder();
		
		retval.append(String.format("Satisfaction averaged over %d samples\n\n", 
				this.sat.get(0).size()));
				
		for(int ri = 0; ri < this.rules.length; ri++)
 		{
 			LogicRule lr = this.rules[ri];
 			retval.append(lr.toString());
			long numground = lr.numGroundings();
			retval.append(String.format("%d groundings (weight = %.1f)\n",
					numground, lr.getTotalSamplingWeight()));
						
			retval.append(String.format("avg %f groundings satisfied (+/- %f)\n\n",
					avg(this.sat.get(ri)), std(this.sat.get(ri))));
 		}
 		
		double totalAvg = this.getTotalAvgSat(false);
		double totalGround = this.getTotalGround(false);
		retval.append(String.format("Total: avg %f of %f groundings satisfied (%.8f percent)\n\n",
				totalAvg, totalGround, totalAvg / totalGround));
				
		return retval.toString();
	}
	
}
