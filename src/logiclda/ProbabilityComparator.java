package logiclda;

import java.util.Comparator;

public class ProbabilityComparator implements Comparator<Integer> {
	
	double[] values;
	int order;
	
	/**
	 * Sort the <b>indices</b> of an array by the corresponding array values
	 * 
	 * @param values Values to sort indices by 
	 * @param order If > 0, ascending order (else descending)
	 */
	public ProbabilityComparator(double[] values, int order)			
	{
		this.values = values;
		this.order = order;
	}
	
	public int compare(Integer arg0, Integer arg1) 
	{
		if(values[arg0] > values[arg1])
			return 	1 * this.order;
		else if(values[arg0] < values[arg1])
			return -1 * this.order;
		else
			return 0;			
	}
	
}
