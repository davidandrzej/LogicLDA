package logiclda.infer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.enums.FileFormat;
import org.ujmp.core.enums.ValueType;

import logiclda.Corpus;
import logiclda.LDAParameters;
import logiclda.textutil.FileUtil;

public class RelaxedSample implements Sample
{
	// MAP estimates should really have 
	public static final double MIN_PHI = 0.000001;
	public static final double MIN_THETA = 0.000001;
	
	public double[][] zrelax;
	public double[][] phi;
	public double[][] theta;
	
	public RelaxedSample(Corpus c, LDAParameters p, DiscreteSample s)
	{		
		// Init phi/theta as mean of posteriors from prev sample
		//
		zrelax = new double[c.N][p.T];		
		phi = (s.getPhi(p)).toDoubleArray();
		theta = (s.getTheta(p)).toDoubleArray();
		
		// Init z-relax as posterior given phi/theta
		for(int i = 0; i < s.z.length; i++)
		{
			double normsum = 0;
			for(int t = 0; t < p.T; t++)
			{
				zrelax[i][t] = theta[c.d[i]][t] * phi[t][c.w[i]];
				normsum += zrelax[i][t]; 
			}
			for(int t = 0; t < p.T; t++)
				zrelax[i][t] /= normsum;						
		}		
	}
	
	/**
	 * Do argmax to convert relaxed z to a single "hard" z sample
	 * @return
	 */
	public int[] getZ()
	{
		int N = zrelax.length;
		int T = zrelax[0].length;
		int[] hardz = new int[N];
		for(int i = 0; i < N; i++)
		{
			int maxidx = 0;
			double maxval= -1;
			for(int t = 0; t < T; t++)
			{
				if(zrelax[i][t] > maxval)
				{
					maxidx = t;
					maxval = zrelax[i][t];					
				}				
			}
			hardz[i] = maxidx;			
		}
		return hardz;
	}
		
	/**
	 * Do an Entropic Mirror Descent step on a single relaxed z-entry
	 * 
	 * @param i Index to update
	 * @param gradSignMag Sign and magnitude of gradient
	 * @param tGradIdx Indices of entries w/ non-zero gradients
	 * @param stepSize Step size parameter
	 */
	public void emdaStep(Gradient stepGrad, double stepSize)
	{		
		// Each z-entry where we have gradient information
		for(int gi = 0; gi < stepGrad.gradients.length; gi++)
		{
			// Get true index and gradient
			int i = stepGrad.indices[gi];
			double[] curGrad = stepGrad.gradients[gi];
			// Exponentiate entries w/ non-zero gradients
			for(int t = 0; t < curGrad.length; t++)
				zrelax[i][t] *= Math.exp(stepSize * curGrad[t]);
			// Re-normalize
			double normsum = 0;
			for(double val : zrelax[i])
				normsum += val;
			for(int t = 0; t < curGrad.length; t++)
				zrelax[i][t] /= normsum;
		}
	}	
		
	public Matrix getPhi(LDAParameters p)
	{
		return MatrixFactory.importFromArray(phi);
	}
	
	public Matrix getTheta(LDAParameters p)
	{
		return MatrixFactory.importFromArray(theta);
	}
	
	/**
	 * Update MAP estimates of phi/theta
	 * @param c
	 * @param p
	 */
	public void updatePhiTheta(Corpus c, LDAParameters p)
	{
		// Calculate 'expected' NW / ND count matrices 
		double[][] enw = new double[p.W][p.T];
		double[][] end = new double[c.D][p.T];		
		for(int i = 0; i < c.N; i++)
		{
			for(int t = 0; t < p.T; t++)
			{
				enw[c.w[i]][t] += zrelax[i][t];
				end[c.d[i]][t] += zrelax[i][t];				
			}
		}
		
		// Update our MAP estimates
		updateMapPhi(p, enw);
		updateMapTheta(p, end);
	}

	/**
	 * Re-calculate MAP phi
	 * 
	 * @param p
	 * @param enw
	 */
	private void updateMapPhi(LDAParameters p, double[][] enw)
	{				
		// Estimate entries
		for(int t = 0; t < p.T; t++)
		{
			double normsum = 0;
			for(int w = 0; w < p.W; w++)
			{
				// Cannot allow negative entries
				phi[t][w] = Math.max(MIN_PHI, enw[w][t] + p.beta[t][w] - 1);
				normsum += phi[t][w];
			}
			// Normalize
			for(int w = 0; w < p.W; w++)
				phi[t][w] /= normsum;								
		}
	}

	/**
	 * Re-calculate MAP theta
	 * 
	 * @param p
	 * @param end
	 */
	private void updateMapTheta(LDAParameters p, double[][] end)
	{
		// Estimate entries
		for(int d = 0; d < end.length; d++)
		{
			double normsum = 0;
			for(int t = 0; t < p.T; t++)
			{
				// Cannot allow negative entries
				theta[d][t] = Math.max(MIN_THETA, end[d][t] + p.alpha[t] - 1);
				normsum += theta[d][t];
			}
			// Normalize
			for(int t = 0; t < p.T; t++)
				theta[d][t] /= normsum;			
		}
	}
	
	/**
	 * Convert to discrete z-vector and write out to file
	 * 
	 * @param basefn
	 */
	public void writeSample(String basefn)
	{
		try
		{			
			ArrayList<Integer> zlist = new ArrayList<Integer>();
			for(int zi : this.getZ())
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
			Matrix matphi = MatrixFactory.importFromArray(phi);
			FileWriter phiout = new FileWriter(String.format("%s.phi",
					basefn));
			matphi.exportToWriter(FileFormat.TXT, phiout);
			phiout.close();
					
			Matrix mattheta = MatrixFactory.importFromArray(theta);
			FileWriter thetaout = new FileWriter(String.format("%s.theta",
					basefn));
			mattheta.exportToWriter(FileFormat.TXT, thetaout);
			thetaout.close();
		}
		catch(IOException ioe)
		{
			System.out.println("Phi/theta writeout failed");
			System.out.println(ioe.toString());			
		}		
	}
}
