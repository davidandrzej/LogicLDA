package logiclda.infer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;
import org.ujmp.core.enums.FileFormat;
import org.ujmp.core.enums.ValueType;

import logiclda.Corpus;
import logiclda.LDAParameters;
import logiclda.MiscUtil;
import logiclda.textutil.FileUtil;

public class DiscreteSample implements Sample {
	
	// Minimum values for MAP estimates 
	public static final double MIN_PHI = 0.000001;
	public static final double MIN_THETA = 0.000001;
	
	public long[][] nw;
	public long[][] nd;
	public long[] nwcolsums;
	public int[] z;
	
	
	/**
	 * Init from a given *.init or *.sample file
	 * 
	 * @param N
	 * @param T
	 * @param W
	 * @param D
	 * @param filename
	 * @param c
	 */
	public DiscreteSample(int N, int T, int W, int D,
			String filename, Corpus c)
	{
		// Hidden topic assignments
		z = new int[N];		
		// Count matrices
		nw = new long[W][T];
		nd = new long[D][T];		
		// Column sums for word-topic count array
		nwcolsums = new long[T];
		for(int j = 0; j < T; j++)		
			nwcolsums[j] = 0;
		
		this.fromFile(filename, c);
	}
	
	/**
	 * Init to empty sample
	 * 
	 * @param N
	 * @param T
	 * @param W
	 * @param D
	 */
	public DiscreteSample(int N, int T, int W, int D)
	{
		// Hidden topic assignments
		z = new int[N];		
		// Count matrices
		nw = new long[W][T];
		nd = new long[D][T];		
		// Column sums for word-topic count array
		nwcolsums = new long[T];
		for(int j = 0; j < T; j++)		
			nwcolsums[j] = 0;				
	}
	
	/**
	 *  Init from a given z-assignment
	 * @param N
	 * @param T
	 * @param W
	 * @param D
	 * @param givenz
	 * @param c
	 */
	public DiscreteSample(int N, int T, int W, int D,
			int[] givenz, Corpus c)
	{
		assert(givenz.length == N && c.N == N);
		this.z = new int[N];		
						
		// Count matrices
		nw = new long[W][T];
		nd = new long[D][T];		
		// Column sums for word-topic count array
		nwcolsums = new long[T];
		for(int j = 0; j < T; j++)		
			nwcolsums[j] = 0;		
		
		// Set count matrices accordingly
		for(int i = 0; i < z.length; i++)
		{
			assert(givenz[i] > 0 && givenz[i] < T);
			this.z[i] = givenz[i];
			updateCounts(c.w[i], z[i], c.d[i], 1);
		}
	}
	
	/** 
	 * Do bookkeeping assoc with reassigning z[idx] to newz
	 * @param c
	 * @param idx
	 * @param newz
	 */
	public void reassign(Corpus c, int idx, int newz)
	{
		this.updateCounts(c.w[idx], this.z[idx], c.d[idx], -1);
		this.z[idx] = newz;				
		this.updateCounts(c.w[idx], this.z[idx], c.d[idx], 1);	
	}
	
	
	/**
	 * Calculate MAP phi
	 * 
	 * @param p
	 * @param enw
	 */
	public double[][] mapPhi(LDAParameters p, double[][] phi)
	{				
		// Estimate entries
		for(int t = 0; t < p.T; t++)
		{
			double normsum = 0;
			for(int w = 0; w < p.W; w++)
			{
				// Cannot allow negative entries
				phi[t][w] = Math.max(MIN_PHI, this.nw[w][t] + p.beta[t][w] - 1);
				normsum += phi[t][w];
			}
			// Normalize
			for(int w = 0; w < p.W; w++)
				phi[t][w] /= normsum;		
		}
		return phi;
	}

	/**
	 * Calculate MAP theta
	 * 
	 * @param p
	 * @param end
	 */
	public double[][] mapTheta(LDAParameters p, double[][] theta)
	{
		// Estimate entries
		for(int d = 0; d < nd.length; d++)
		{
			double normsum = 0;
			for(int t = 0; t < p.T; t++)
			{
				// Cannot allow negative entries
				theta[d][t] = Math.max(MIN_THETA, nd[d][t] + p.alpha[t] - 1);
				normsum += theta[d][t];
			}
			// Normalize
			for(int t = 0; t < p.T; t++)
				theta[d][t] /= normsum;			
		}
		return theta;
	}
	
	
	/**
	 * Initialize from a *.sample file
	 * @param filename
	 * @param c
	 */
	public void fromFile(String filename, Corpus c)
	{
		// Read from file
		int[] zfile = MiscUtil.intListUnbox(FileUtil.readIntFile(filename));
		assert(zfile.length == z.length);
		z = zfile;
		
		// Set count matrices accordingly
		for(int i = 0; i < z.length; i++)
		{
			updateCounts(c.w[i], z[i], c.d[i], 1);
		}
	}

	/**
	 * Return the discrete z-vector
	 * 
	 * @return
	 */
	public int[] getZ()
	{
		return z;
	}
	
	/**
	 * Update count matrices used for Gibbs sampling
	 * @param w
	 * @param t
	 * @param d
	 * @param update
	 */
	public void updateCounts(int w, int t, int d, int update)
	{
		nw[w][t] += update;
		nwcolsums[t] += update;
		nd[d][t] += update;
	}
	
	/**
	 * Estimate mean of Phi posterior from current sample
	 * @param p
	 * @return
	 */
	public Matrix getPhi(LDAParameters p)
	{
		long W = nw.length;
		long T = nw[0].length;
		long D = nd.length;

		// Alloc phi matrix
		long[] msize2 = new long[2];
		msize2[0] = T;
		msize2[1] = W;		
		Matrix phi = MatrixFactory.zeros(ValueType.DOUBLE, msize2);

		// Estimate entries
		for(int ti = 0; ti < T; ti++)
		{
			double colsum = nwcolsums[ti];
			double bsum = p.betasums[ti];
			for(int wi = 0; wi < W; wi++)
			{
				double val = (nw[wi][ti] + p.beta[ti][wi]) / (colsum + bsum);
				phi.setAsDouble(val, ti, wi);
			}
		}				
		return phi;
	}

	/**
	 * Estimate mean of Theta posterior from current sample 
	 * @param p
	 * @return
	 */
	public Matrix getTheta(LDAParameters p)
	{
		long D =  nd.length;
		long T =  nd[0].length;
		long[] msize2 = new long[2];
		msize2[0] = D;
		msize2[1] = T;		
		Matrix theta = MatrixFactory.zeros(ValueType.DOUBLE, msize2);		

		// Pre-calculate alpha sum
		double asum = p.alphaSum();

		for(int di = 0; di < D; di++)
		{
			// Get sum of counts for this document			
			double doclen = 0;
			for(int ti = 0; ti < T; ti++)
				doclen += nd[di][ti];

			// Calculate entries
			for(int ti = 0; ti < T; ti++)
			{
				double val = (nd[di][ti] + p.alpha[ti]) / (doclen + asum);
				theta.setAsDouble(val, di, ti);
			}
		}				
		return theta;
	}
	
	public void writeSample(String basefn)
	{
		try
		{			
			ArrayList<Integer> zlist = new ArrayList<Integer>();
			for(int zi : z)
				zlist.add(zi);
			FileUtil.writeIntFile(String.format	("%s.sample", basefn), zlist);
		}
		catch(IOException ioe)
		{
			System.out.println("Problem writing sample out to file");
			System.out.println(ioe.toString());
		}
	}
	 	
	
	/**
	 * Estimate phi/theta and write out to %s.phi/theta   
	 * 
	 * @param p Hyperparameters alpha/beta are required to estimate phi/theta 
	 * @param basefn Base output filename
	 */
	public void writePhiTheta(LDAParameters p, String basefn)
	{		
		try
		{
			FileUtil.writeMatrix(String.format("%s.phi", basefn), 
					this.getPhi(p));
			FileUtil.writeMatrix(String.format("%s.theta", basefn), 
					this.getTheta(p));			
		}
		catch(IOException ioe)
		{
			System.out.println("Phi/theta writeout failed");
			System.out.println(ioe.toString());			
		}		
	}
}
