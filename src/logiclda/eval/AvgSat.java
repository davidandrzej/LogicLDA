package logiclda.eval;

import logiclda.rules.LogicRule;

/**
 * Used to record average satisfaction over multiple samples
 * 
 * @author andrzejewski1
 *
 */
public class AvgSat 
{
	private double[] avgsat;
	private LogicRule[] rules;
	private int n;
	
	public AvgSat(LogicRule[] rules)
	{
		// The rules we are interested in 
		this.rules = rules;
		// How many samples have we seen?
		int n = 0;
		// Average number of satisfied groundings
		avgsat = new double[rules.length];
	}
	
	/**
	 * Record satisfaction of each logic rule
	 * @param z
	 */
	public void recordSatisfaction(int[] z)
	{
		if(this.n > 0)
		{
			for(int ri = 0; ri < rules.length; ri++)
			{
				this.avgsat[ri] = (((double) n) / (n+1)) * this.avgsat[ri];
				this.avgsat[ri] += (((double) 1) / (n+1)) * rules[ri].numSat(z);
			}		
		}
		else
		{
			for(int ri = 0; ri < rules.length; ri++)
				this.avgsat[ri] = (double) rules[ri].numSat(z);
		}
		this.n++;
	}

	public String report()
	{
		StringBuilder retval = new StringBuilder();
		
		retval.append(String.format("Satisfaction averaged over %d samples\n\n", this.n));
		
		int totalGround = 0;
		double totalAvg = 0;
		for(int ri = 0; ri < this.rules.length; ri++)
 		{
 			LogicRule lr = this.rules[ri];
 			retval.append(lr.toString());
			long numground = lr.numGroundings();
			retval.append(String.format("%d groundings (weight = %.1f)\n",
					numground, lr.getWeight()));			
			retval.append(String.format("avg %f groundings satisfied (%f unsat)\n\n",
					avgsat[ri], numground - avgsat[ri]));
			
			totalGround += numground;
			totalAvg += avgsat[ri];
 		}
 		
		retval.append(String.format("Total: avg %f of %d groundings satisfied (%.8f percent)\n\n",
				totalAvg, totalGround, totalAvg / totalGround));
				
		return retval.toString();
	}
	
}
