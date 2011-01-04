package logiclda;

import java.util.*;

import logiclda.rules.IndependentRule;

public class MiscUtil {
	

	/**
	 * (destructively) merge these two hashes by summing in-place in h1.
	 * 
	 * 
	 * @param h1
	 * @param h2
	 * @return
	 */
	public static Map<Integer, ArrayList<Double>> hashMerge(
			Map<Integer, ArrayList<Double>> h1,
			Map<Integer, ArrayList<Double>> h2)
	{

		for(Map.Entry<Integer, ArrayList<Double>> kv : h2.entrySet())
		{
			if(h1.containsKey(kv.getKey()))
			{
				ArrayList<Double> vals1 = h1.get(kv.getKey());				
				for(int ti = 0; ti < vals1.size(); ti++)				
					vals1.add(ti, vals1.get(ti) + kv.getValue().get(ti));									
			}
			else							
				h1.put(kv.getKey(), kv.getValue());
		}
		return h1;
	}
	
	public static void matrixDestructAdd(double[][] m1, double[][] m2)
	{
		assert(m1.length == m2.length);
		assert(m1[0].length == m2[0].length);
		
		for(int i = 0; i < m1.length; i++)
			for(int j = 0; j < m1[0].length; j++)
				m1[i][j] = m1[i][j] + m2[i][j];
	}
	
	/**
	 * Return a new int[] which is the concatenation of a and b 
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static int[] intArrayConcat(int[] a, int[] b)
	{
		int[] retval = new int[a.length + b.length];
		System.arraycopy(a, 0, retval, 0, a.length);
		System.arraycopy(b, 0, retval, a.length, b.length);
		return retval;		
	}
	
	/**
	 * Simple conversion of List<Integer> to int[]
	 *  
	 * @param vec
	 * @return
	 */
	public static int[] intListUnbox(List<Integer> lst)
	{
		int[] res = new int[lst.size()];
		ListIterator<Integer> listIter = lst.listIterator();
		while(listIter.hasNext())
		{
			res[listIter.nextIndex()] = listIter.next();
		}
		return res;
	}
	
	/**
	 * Simple conversion of List<Double> to double[]
	 *  
	 * @param vec
	 * @return
	 */
	public static double[] doubleListUnbox(List<Double> lst)
	{
		double[] res = new double[lst.size()];
		ListIterator<Double> listIter = lst.listIterator();
		while(listIter.hasNext())
		{
			res[listIter.nextIndex()] = listIter.next();
		}
		return res;
	}
	
	/**
	 * Get the maximum value from an AbstractCollection
	 * 
	 * @param <T>
	 * @param seq
	 * @return
	 */
	public static <T extends Comparable<? super T>> T seqMax(
			AbstractCollection<T> seq)
	{
		T maxval = null;
		for(T val : seq)
		{
			if(maxval == null || maxval.compareTo(val) <= 0)
				maxval = val;
		}
		return maxval;
	}

	/**
	 * Draw a multinomial sample from (un-normalized) vals
	 * 
	 * @param rng
	 * @param vals
	 * @param normsum
	 * @return
	 */
	public static int multSample(Random rng, double[] vals, double normsum)
	{
		double rval = rng.nextDouble() * normsum;
		double cumsum = 0;
		int j = 0;
		while(cumsum < rval || j == 0)
		{
			cumsum += vals[j];
			j++;
		}
		return j - 1;
	}
	

}
