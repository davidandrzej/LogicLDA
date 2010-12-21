package logiclda;

import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.util.*;

import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.enums.FileFormat;
import org.ujmp.core.enums.ValueType;

import logiclda.textutil.CountComparator;
import logiclda.textutil.FileUtil;

/**
 * Represents a text corpus
 * 
 * @author david
 *
 */
public class Corpus {

	public int N;
	public int D;
	public int W;
	public int[] w;
	public int[] d;
	
	public List<String> vocab;
	public List<String> doclist;
	
	public HashMap<String,Object> sideInfo;
	
	
	/**
	 * Copy this corpus
	 * 
	 * TODO: does not support general metadata copying...!
	 * 
	 * @param c
	 */
	public Corpus(Corpus c)
	{
		this.N = c.N;
		this.D = c.D;
		this.W = c.W;
		this.w = new int[c.w.length];
		System.arraycopy(c.w, 0, this.w, 0, c.w.length);
		this.d = new int[c.d.length];
		System.arraycopy(c.d, 0, this.d, 0, c.d.length);
				
		this.vocab = new ArrayList<String>();
		for(String v : c.vocab)
			this.vocab.add(v);
		
		this.doclist = new ArrayList<String>();
		for(String doc : c.doclist)
			this.doclist.add(doc);		
	}
	
	/**
	 * Load text corpus from base filename
	 * 
	 * @param basefn will load %s.words/docs
	 */
	public Corpus(String basefn)
	{		
		// Read in corpus, record/check dimensionality
		//
		Vector<Integer> wvec = 
			FileUtil.readIntFile(String.format("%s.words",basefn)); 
		w = MiscUtil.intListUnbox(wvec);
		Vector<Integer> dvec = 
			FileUtil.readIntFile(String.format("%s.docs",basefn)); 
		d = MiscUtil.intListUnbox(dvec);
		N = w.length;
		D = MiscUtil.seqMax(dvec) + 1;
		W = MiscUtil.seqMax(wvec) + 1;
		assert(w.length == d.length);
		
		// Read vocab and document list
		//
		vocab = FileUtil.readLines(String.format("%s.vocab", basefn));
		doclist = FileUtil.readLines(String.format("%s.doclist", basefn));
		
		// sideInfo maps String--> [other metadata] 
		// (eg sentence indices)
		//
		try
		{
			sideInfo = getSideInfo(basefn);
		}
		catch (IOException ioe)
		{
			System.out.println("Problem reading side info files");
			System.out.println(ioe.toString());
		}
	}
		
	/**
	 * Write out top N most probable words for each topic
	 * 
	 * @param basefn
	 * @param phi
	 * @param topN
	 */
	public void writeTopics(String basefn, Matrix matphi, int topN)
	{
		try
		{
			
			FileWriter out = new FileWriter(String.format("%s.topics", basefn));
			
			int T = (int) matphi.getSize(0);
			int W = (int) matphi.getSize(1);
			double[][] phi = matphi.toDoubleArray();
			
			// Indices to be used for printing out Top N words
			Integer[] idx = new Integer[W];
			for(int wi = 0; wi < W; wi++)
				idx[wi] = wi;
			
			for(int t = 0; t < T; t++)
			{
				// Sort indices by topic probability
				ProbabilityComparator pc = new ProbabilityComparator(phi[t], -1);
				Arrays.sort(idx, pc);
								
				// Print out the top N words
				out.write(String.format("\nTopic %d\n", t));
				for(int i = 0; i < topN; i++)
				{					
					out.write(String.format("%s = %.4f\n", vocab.get(idx[i]), 
							phi[t][idx[i]]));
				}
			}
			out.close();
		}
		catch(IOException ioe)
		{
			System.out.println("Topics writeout failed");
			System.out.println(ioe.toString());			
		}		
	}
	
	/**
	 * Load any recognized side information files
	 *  
	 * @param basefn
	 * @return
	 * @throws IOException
	 */
	private static HashMap<String, Object> getSideInfo(String basefn) 
		throws IOException
	{
		// Will map side info name to data struct (eg, "sent"-->int[])
		//
		HashMap<String,Object> sideInfo = new HashMap<String,Object>(); 
		
		// Try each type of side information
		//
		for(SideInfoType st : SideInfoType.values())
		{
			File f = new File(String.format("%s.%s", basefn, st.infoName));
			if(f.exists())
			{				
				switch(st)
				{
				case SENTENCE:
					int[] sent = 
						MiscUtil.intListUnbox(FileUtil.readIntFile(f.getCanonicalPath())); 
					sideInfo.put("sent", sent);
					break;	
				case DOCLABEL:
					int[] doclabel = 
						MiscUtil.intListUnbox(FileUtil.readIntFile(f.getCanonicalPath())); 
					sideInfo.put("doclabel", doclabel);
					break;	
				default:
					System.out.println(
							String.format("SideInfoType %s not handled in Corpus",
									st.toString()));
				}
			}
		}		
		return sideInfo;
	}
	
	/**
	 * Read integer sequence metadata to sideInfo
	 * 
	 * @param filename
	 * @param mdName
	 */
	public void addVecMetaData(String filename, String mdName)
	{
		Vector<Integer> mdVec = FileUtil.readIntFile(String.format(filename));
		sideInfo.put(mdName, MiscUtil.intListUnbox(mdVec));		
	}
}
