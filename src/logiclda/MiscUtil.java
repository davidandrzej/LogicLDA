package logiclda;

import java.util.*;

public class MiscUtil {
	

	public static void matrixDestructAdd(double[][] m1, double[][] m2)
	{
		assert(m1.length == m2.length);
		assert(m1[0].length == m2[0].length);
		
		for(int i = 0; i < m1.length; i++)
			for(int j = 0; j < m1[0].length; j++)
				m1[i][j] = m1[i][j] + m2[i][j];
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
