package logiclda;

import java.io.*;

import logiclda.infer.CollapsedGibbs;
import logiclda.infer.DiscreteSample;

import org.ujmp.core.exceptions.MatrixException;

public class StandardLDA {
	/**
	 * @param args
	 * @throws IOException 
	 * @throws MatrixException 
	 */
	public static void main(String[] args) throws MatrixException, IOException 
	{
		// Parse command-line args
		//
		String basefn = args[0];
		int numsamp = Integer.parseInt(args[1]);
		int topN = Integer.parseInt(args[2]); 
		int randseed = Integer.parseInt(args[3]); 

		// Load corpus and parameters (checking vocab dim agreement)
		//
		LDAParameters p = new LDAParameters(basefn, randseed);
		Corpus c = new Corpus(basefn);		
		assert(p.W == c.W);
		
		// Run standard LDA for numsamp 
		//
		DiscreteSample s = runStandardLDA(c, p, numsamp);
 		
		// Write out results
		//
		s.writePhiTheta(p, basefn);
		s.writeSample(basefn);
		c.writeTopics(basefn, s.getPhi(p), topN);
	}
	
	/**
	 * Standard (no logic) LDA, using collapsed Gibbs sampling
	 * 
	 * @param c
	 * @param p
	 * @param numsamp
	 * @return
	 */
	public static DiscreteSample runStandardLDA(Corpus c, LDAParameters p, int numsamp)
	{	
		// Do Collapsed Gibbs sampling and return final sample
		DiscreteSample s = CollapsedGibbs.doGibbs(c, p, numsamp);		
		return s;
	}
}
package logiclda;

import java.io.*;

import logiclda.infer.CollapsedGibbs;
import logiclda.infer.DiscreteSample;

import org.ujmp.core.exceptions.MatrixException;

public class StandardLDA {
	/**
	 * @param args
	 * @throws IOException 
	 * @throws MatrixException 
	 */
	public static void main(String[] args) throws MatrixException, IOException 
	{
		// Parse command-line args
		//
		String basefn = args[0];
		int numsamp = Integer.parseInt(args[1]);
		int topN = Integer.parseInt(args[2]); 
		int randseed = Integer.parseInt(args[3]); 

		// Load corpus and parameters (checking vocab dim agreement)
		//
		LDAParameters p = new LDAParameters(basefn, randseed);
		Corpus c = new Corpus(basefn);		
		assert(p.W == c.W);
		
		// Run standard LDA for numsamp 
		//
		DiscreteSample s = runStandardLDA(c, p, numsamp);
 		
		// Write out results
		//
		s.writePhiTheta(p, basefn);
		s.writeSample(basefn);
		c.writeTopics(basefn, s.getPhi(p), topN);
	}
	
	/**
	 * Standard (no logic) LDA, using collapsed Gibbs sampling
	 * 
	 * @param c
	 * @param p
	 * @param numsamp
	 * @return
	 */
	public static DiscreteSample runStandardLDA(Corpus c, LDAParameters p, int numsamp)
	{	
		// Do Collapsed Gibbs sampling and return final sample
		DiscreteSample s = CollapsedGibbs.doGibbs(c, p, numsamp);		
		return s;
	}
}
