package logiclda.eval;

import logiclda.infer.DiscreteSample;
import logiclda.LDAParameters;
import org.apache.commons.math.special.Gamma;

public class EvalLDA 
{
		
	/**
	 * Calc uncollapsed LDA log-likelihood 
	 * @param nw
	 * @param nd
	 * @param phi
	 * @param theta
	 * @param beta
	 * @param alpha
	 * @return
	 */
	public static double ldaLoglike(long[][] nw, long[][] nd, 
			double[][] phi, double[][] theta,
			double[][] beta, double[] alpha)
	{
		double retval = 0;
		
		// Dirichlet term contributions
		retval += logDirMat(beta, phi);
		retval += logDirMat(alpha, theta);
		
		// Count contributions
		retval += logPhi(phi, nw);
		retval += logTheta(theta, nd);
		
		return retval;
	}
	
	public static double ldaLoglike(DiscreteSample s, LDAParameters p)
	{
		double[][] phi = new double[p.T][p.W];
		double[][] theta = new double[s.nd.length][p.T];
		
		phi = s.mapPhi(p, phi);
		theta = s.mapTheta(p, theta);
		
		return ldaLoglike(s.nw, s.nd, phi, theta, p.beta, p.alpha);
	}
	
	/**
	 * Dirichlet log-likelihood of multinomial parameter vector x 
	 * given Dirichlet hyperparameter vector alpha
	 * 
	 * @param alpha
	 * @param x
	 * @return
	 */
	public static double dirichletLoglike(double[] alpha, double[] x)
	{
		assert(alpha.length == x.length);
		double xsum = 0;
		for(int i = 0; i < alpha.length; i++)
		{
			assert(alpha[i] > 0);
			assert(x[i] > 0);
			xsum += x[i];			
		}
		assert(Math.abs(xsum - 1) < 0.00001);
		//
		// Log [ Dir (x | alpha ) ]  
		// = loggamma(sum alpha) 
		// - sum(loggamma(alpha))
		// + sum((alpha-1)*log(x))
		// 		
		double alphasum = 0;
		for(double a : alpha)
			alphasum += a;				
		double lnormnum = Gamma.logGamma(alphasum);
		
		double lnormdenom = 0;
		for(double a : alpha)
			lnormdenom += Gamma.logGamma(a);
		
		double lparams = 0;
		for(int i = 0; i < alpha.length; i++)		
			lparams += (alpha[i] - 1) * Math.log(x[i]);
		
		return lnormnum - lnormdenom + lparams;
	}	
	
	/**
	 * Calculate contrib of phi-counts to loglike
	 * @param phi TxW topic P(w|z) matrix
	 * @param nw WxT count matrix 
	 * @return
	 */
	public static double logPhi(double[][] phi, long[][] nw)
	{
		assert(phi.length == nw[0].length);
		assert(phi[0].length == nw.length);
		
		double retval = 0;
		for(int t = 0; t < phi.length; t++)
			for(int w = 0; w < nw.length; w++)
				retval += nw[w][t] * Math.log(phi[t][w]);
		return retval;
	}
	
	/**
	 * Calculate contrib of theta-counts to loglike
	 * @param theta DxT topic P(z|d) matrix
	 * @param nd DxT count matrix 
	 * @return
	 */
	public static double logTheta(double[][] theta, long[][] nd)
	{
		assert(theta.length == nd.length);
		assert(theta[0].length == nd[0].length);
		
		double retval = 0;
		for(int d = 0; d < theta.length; d++)
			for(int t = 0; t < theta[0].length; t++)
				retval += nd[d][t] * Math.log(theta[d][t]);
		return retval;
	}
	
	/**
	 * Calc sum of logDir contrib for multinomial parameter matrix x
	 * @param alpha
	 * @param x
	 * @return
	 */
	public static double logDirMat(double[][] alpha, double[][] x)
	{
		assert(alpha.length == x.length);
		assert(alpha[0].length == x[0].length);
		
		double retval = 0;
		for(int row = 0; row < x.length; row++)
			retval += EvalLDA.dirichletLoglike(alpha[row], x[row]);
		return retval;
	}
	
	/**
	 * Calc sum of logDir contrib for multinomial parameter matrix x
	 * (case where same alpha used for all realization rows of x)
	 * 
	 * @param alpha
	 * @param x
	 * @return
	 */
	public static double logDirMat(double[] alpha, double[][] x)
	{
		assert(alpha.length == x[0].length);
				
		double retval = 0;
		for(int row = 0; row < x.length; row++)
			retval += EvalLDA.dirichletLoglike(alpha, x[row]);
		return retval;
	}
}
