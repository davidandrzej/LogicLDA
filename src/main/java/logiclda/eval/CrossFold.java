package logiclda.eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Random;
import logiclda.Corpus;

/**
 * Cross-fold validation on docs
 * 
 * @author andrzejewski1
 *
 */
public class CrossFold 
{

	public Corpus originalCorpus;	
	public Random rng;
	
	public List<Fold> folds;	
	
	public CrossFold(Corpus c, int k)
	{
		this(c, k, 194582);
	}
	
	public CrossFold(Corpus c, int k, int randseed)
	{
		// Keep pointer to original corpus
		this.originalCorpus = c;
		
		// Shuffle the documents
		this.rng = new Random(randseed);
		ArrayList<Integer> docs = new ArrayList<Integer>();
		for(int di = 0; di < c.doclist.size(); di++)
			docs.add(di);
		Collections.shuffle(docs, this.rng);
		System.out.println(String.format("Permuted = %s", docs.toString()));
		
		// Create the folds
		this.folds = new ArrayList<Fold>();
		for(int ki = 0; ki < k; ki++)
			folds.add(new Fold(docs, k, ki));		
	}	
	
	public Corpus getTrain(int k)
	{
		return this.folds.get(k).getTrain(this.originalCorpus);
	}
	
	public Corpus getTest(int k)
	{
		return this.folds.get(k).getTest(this.originalCorpus);
	}
	
}
