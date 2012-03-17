package logiclda;

import java.io.IOException;
import java.util.*;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.exceptions.MatrixException;

import logiclda.FileUtil;

public class LDAParameters {

	private Matrix matalpha;
	private Matrix matbeta;
	public double[] alpha;
	public double[][] beta;
	public double[] betasums;
	public int T;
	public int W;
	public int randseed;
	public Random rng;
	
	/**
	 * 
	 * @param basefn will load %s.alpha/beta
	 * @throws IOException 
	 * @throws MatrixException 
	 */
	public LDAParameters(String basefn, int randseed) throws MatrixException, IOException
	{
		// Read in alpha/beta files
		//
		matalpha = FileUtil.readDoubleMatFile(String.format("%s.alpha",basefn));
		matbeta = FileUtil.readDoubleMatFile(String.format("%s.beta",basefn));
		alpha = matalpha.toDoubleArray()[0];
		beta = matbeta.toDoubleArray();	
		
		// Ensure that alpha/beta agree on T
		//
		assert(matbeta.getSize(0) == matalpha.getSize(1));
		
		// Pre-calculate beta row sums
		//
		T = (int) matbeta.getSize(0);
		W = (int) matbeta.getSize(1);		
		betasums = new double[T];		
		for(int j = 0; j < T; j++)
		{		
			betasums[j] = 
				(matbeta.subMatrix(Ret.LINK,j,0,j,W-1)).getValueSum();		
		}
		
		// Construct random number generator
		//
		this.randseed = randseed;
		this.rng = new Random(randseed);
	}
			
	public double alphaSum()
	{
		return matalpha.getValueSum();		
	}
}
