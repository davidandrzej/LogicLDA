package logiclda.rules;

import java.util.*;


public class Seeds 
{

	private HashMap<Integer, ArrayList<NMinusOneGram>> seedHits;
	
	/**
	 * 
	 * @param seedWords format is "123 333, 452, 567 121 411, ..."
	 */
	public Seeds(String seedString)
	{
		seedHits= new HashMap<Integer, ArrayList<NMinusOneGram>>();
				
		// Each comma-separated entry is a seed n-gram
		//
		StringTokenizer seedtok = new StringTokenizer(seedString, ",");
		while (seedtok.hasMoreTokens())
		{			
			// Get the first word in the n-gram
			//
			String currentSeed = seedtok.nextToken();
			StringTokenizer ngramtok = new StringTokenizer(currentSeed);
			Integer first = Integer.parseInt(ngramtok.nextToken());
			
			if(!seedHits.containsKey(first))
				seedHits.put(first, new ArrayList<NMinusOneGram>());			
			seedHits.get(first).add(new NMinusOneGram(ngramtok));				
		}
	}
	
	/**
	 * Return 0 if no n-gram seed hit, otherwise
	 * return length of longest n-gram seed hit
	 * 
	 * @param w
	 * @param start
	 * @return
	 */
	public int hit(int[] w, int start)
	{
		if(!seedHits.containsKey(w[start]))
			return 0;		
		
		int hitlen = 0;
		for(NMinusOneGram nmog : seedHits.get(w[start]))
			if(nmog.isHit(w, start + 1))
				if(nmog.nGramLength() > hitlen)
					hitlen = nmog.nGramLength();
		return hitlen;
	}
	
	public String toString() 
	{
		StringBuilder retval = new StringBuilder();
		String sep = "";
		for(Integer first : seedHits.keySet())
		{			
			for(NMinusOneGram nmog : seedHits.get(first))
			{
				retval.append(sep);
				retval.append(String.format("%d%s",
						first, nmog.toString()));
				sep = ",";			
			}		
		}
		return retval.toString();
	}
}
