package logiclda.rules;

import java.util.*;

public class NMinusOneGram 
{
	
	private ArrayList<Integer> rest;
	
	public NMinusOneGram(StringTokenizer strtok)	
	{
		rest = new ArrayList<Integer>();
		while(strtok.hasMoreTokens())
		{
			rest.add(Integer.parseInt(strtok.nextToken()));
		}				
	}
	
	public boolean isHit(int[] w, int start)
	{
		boolean hit = true;
		for(int i = 0; i < rest.size(); i++)
		{
			if(start + i == w.length 
					|| w[start + i] != rest.get(i))
			{
				hit = false;
				break;
			}			
		}
		return hit;
	}
	
	public int nGramLength()
	{
		return rest.size() + 1;
	}
	
	public String toString()
	{
		StringBuilder retval = new StringBuilder();
		for(Integer word : rest)
			retval.append(String.format(" %d",word));
		return retval.toString();
	}
}
