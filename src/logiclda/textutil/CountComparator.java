package logiclda.textutil;
import java.util.*;

public class CountComparator implements Comparator<String> {

	private HashMap<String, Integer> counts;
	private int order;
	
	
	
	
	/**
	 * For sorting strings by count (ie, word frequency)
	 * 
	 * @param counts The counts we will use to sort strings
	 * @param order If > 0, ascending order (else descending)
	 */
	public CountComparator(HashMap<String, Integer> counts,
			int order)
	{
		this.counts = counts;
		this.order = order;
	}
	
	public int compare(String arg0, String arg1) 
	{
		if(counts.get(arg0) > counts.get(arg1))
			return 	1 * this.order;
		else if(counts.get(arg0) < counts.get(arg1))
			return -1 * this.order;
		else
			return 0;			
	}

}
