package logiclda.eval;

import logiclda.Corpus;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a re-ordering of indices for a train/test fold split 
 * 
 * 
 * @author andrzejewski1
 *
 */
public class Fold 
{
	// maps indices to their new (within-fold) values	
	public HashMap<Integer, Integer> trainidx;
	public HashMap<Integer, Integer> testidx;
	
	public Fold(List<Integer> indices, int k, int fidx)
	{
		// Testset indices from testStart (inclusive) to testEnd (exclusive)
		int N = indices.size();
		int testStart = (int) Math.round(Math.floor(((double) N * fidx) / k));
		int testEnd = (int) Math.round(Math.floor(((double) N * (fidx+1)) / k));
		//Math.min(foldSize * (fidx + 1), indices.size());
		
		// need to do i - testStart to account for testset *offset*		
		testidx = new HashMap<Integer, Integer>();
		for(int i = testStart; i < testEnd; i++)		
			testidx.put(indices.get(i), i - testStart);

		// need to do i -  testEnd + testStart to account for testset *gap*
		trainidx = new HashMap<Integer, Integer>();
		for(int i = 0; i < testStart; i ++)
			trainidx.put(indices.get(i), i);
		for(int i = testEnd; i < indices.size(); i++)
			trainidx.put(indices.get(i), i - testEnd + testStart);
	}
	
	public Corpus getTrain(Corpus c)
	{
		return getDocs(c, this.trainidx);
	}
	
	public Corpus getTest(Corpus c)
	{
		return getDocs(c, this.testidx);
	}
	
	/**
	 * Copy this corpus and re-number the documents according to mapping
	 * @param c
	 * @param mapping
	 * @return
	 */
	public Corpus getDocs(Corpus c, Map<Integer,Integer> mapping) 
	{
		Corpus newc = new Corpus(c);
		
		// Track the new number of documents and which ground truth 
		// documents the new indices refer to 
		newc.D = mapping.keySet().size();
		newc.doclist = new ArrayList<String>();
		for(int di = 0; di < newc.D; di++)
			newc.doclist.add("");
				
		for(int di = 0; di < c.D; di++)
			if(mapping.containsKey(di))
				newc.doclist.set(mapping.get(di),
						c.doclist.get(di));			
		
		// Convert the w and d arrays 
		ArrayList<Integer> neww = new ArrayList<Integer>();
		ArrayList<Integer> newd = new ArrayList<Integer>();		
		for(int i = 0; i < c.w.length; i++)
		{
			// Is this doc in the trainset?
			if(mapping.containsKey(c.d[i]))
			{
				neww.add(c.w[i]);
				newd.add(mapping.get(c.d[i]));
			}				
		}		
		newc.w = new int[neww.size()];
		newc.d = new int[newd.size()];
		for(int i = 0; i < neww.size(); i++)
		{
			newc.w[i] = neww.get(i);
			newc.d[i] = newd.get(i);
		}
		// Record new corpus size
		newc.N = neww.size();
		
		return newc;
	}
}
