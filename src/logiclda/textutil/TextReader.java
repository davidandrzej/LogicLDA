package logiclda.textutil;
import java.io.*;
import java.util.*;

import logiclda.textutil.FileUtil;
import opennlp.tools.lang.english.*;

public class TextReader {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException 	
	{
		String operation = args[0];
		
		if(operation.equals("docfilter"))
		{
			// Find documents containing *all* of our filter words
			String docFilesName = args[1];
			String filterName = args[2];
			String outputName = args[3];
			documentFilter(docFilesName, filterName, outputName);
		}
		else if(operation.equals("wordcount"))
		{
			// At each threshold, how big would our vocab be?			
			String docFilesName = args[1];
			String stopWordFileName = args[2];			
			String outputName = args[3];
			countWords(docFilesName, stopWordFileName, outputName);
		}		
		else if(operation.equals("getvocab"))
		{
			// Build vocabulary 
			String docFilesName = args[1];
			String stopWordFileName = args[2];
			int threshold = Integer.parseInt(args[3]);
			String outputName = args[4];
			getVocab(docFilesName, stopWordFileName, threshold, outputName);
		}
		else if(operation.equals("makecorpus"))
		{
			// Construct actual corpus
			String docFilesName = args[1];
			String vocabName = args[2];
			String outputName = args[3];
			buildCorpus(docFilesName, vocabName, outputName);
		}
	}

	/**
	 * Build corpus files (*.words, *.docs, *.sent) 
	 * from *.doclist and *.vocab
	 * 
	 * @param docFilesName
	 * @param vocabName
	 * @param outname
	 * @throws IOException
	 */
	public static void buildCorpus(String docFilesName, 
			String vocabName, String outname) throws IOException
	{
		// Init OpenNLP tokenizer and sentence detector
		//
		Tokenizer tkizer = 
			new Tokenizer("./models/EnglishTok.bin.gz");		
		SentenceDetector sdtor = 
			new SentenceDetector("./models/EnglishSD.bin.gz");
				
		// Load vocabulary
		//
		List<String> vocabWords = FileUtil.readLines(vocabName);
		ListIterator<String> vocIter = vocabWords.listIterator();
		HashMap<String, Integer> vocab = new HashMap<String, Integer>();
		while(vocIter.hasNext())
		{			
			String word = vocIter.next();
			vocab.put(word, vocIter.previousIndex());
		}
	
		// Init output files
		//
		FileWriter wordOut = 
			new FileWriter(new File(String.format("%s.words",outname)));
		FileWriter docOut = 
			new FileWriter(new File(String.format("%s.docs",outname)));
		FileWriter sentOut = 
			new FileWriter(new File(String.format("%s.sent",outname)));
		
		// Read through actual files
		//
		List<String> documents = getDocs(docFilesName);
		
		int i = 0;
		int di = 0;
		int si = 0;
		int D = documents.size();
		
		// FOR EACH DOCUMENT
		//
		for(String docName : documents)
		{
			System.out.println(String.format("Doc %d of %d", di, D));
			
			String doc = FileUtil.fileSlurp(docName);
			String[] sentences = sdtor.sentDetect(doc);
			
			// FOR EACH SENTENCE
			//
			for(String sent : sentences)
			{
				String[] toks = tkizer.tokenize(sent);
				
				// FOR EACH TOKEN
				//
				boolean emptySentence = true;
				for(String tok : toks)
				{
					if(vocab.containsKey(tok))
					{
						emptySentence = false;
						wordOut.write(String.format("%d ", vocab.get(tok)));
						docOut.write(String.format("%d ", di));
						sentOut.write(String.format("%d ", si));
						i += 1;
						if(i % 1000 == 0)
						{
							wordOut.write("\n");
							sentOut.write("\n");
							docOut.write("\n");
						}
					}
				}
				// Only increment the sentence counter for non-empty sentence
				//
				if(!emptySentence)
					si += 1;
			}		
			// Assume no empty documents...!
			//
			di += 1;
		}	

		// Cleanup file handles
		wordOut.close();
		docOut.close();
		sentOut.close();
	}
	
	/**
	 * Read document filenames from *.doclist file (one per line)
	 * @param docFilesName
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<String> getDocs(String docFilesName)
		throws IOException
	{
		ArrayList<String> documents = new ArrayList<String>(); 		
		documents.addAll(FileUtil.readLines(docFilesName));		
		return documents; 
	}
	
	/**
	 * Filter vocabulary using *.stop stopword file (one per line)
	 * 
	 * @param counts
	 * @param stopWordFileName
	 * @return
	 * @throws IOException
	 */
	public static HashMap<String, Integer> stopFilter(HashMap<String, Integer> counts,
			String stopWordFileName) throws IOException 
	{
		Collection<String> stop = readStopWords(stopWordFileName);			
		for(String word : stop)		
			counts.remove(word);		
		return counts;
	}		
	
	/**
	 * Given *.doclist, *.stop, and min occur threshold, construct 
	 * vocabulary *.vocab file (down-case and punc-filtered)
	 *  
	 * @param docFilesName
	 * @param stopWordFileName
	 * @param threshold
	 * @param outname
	 * @throws IOException
	 */
	public static void getVocab(String docFilesName, String stopWordFileName, 
			int threshold, String outname) throws IOException
	{
		// Init OpenNLP tokenizer
		//
		Tokenizer tkizer = 
			new Tokenizer("./models/EnglishTok.bin.gz");
		
		// Which documents are we processing?
		//
		ArrayList<String> documents = getDocs(docFilesName);
		
		// Get word occurrence counts
		HashMap<String,Integer> counts = getCounts(documents, tkizer); 

		// Apply stop word filter
		counts = stopFilter(counts, stopWordFileName);
		
		// Remove any words which consist entirely of punctuation
		//
		counts = puncFilter(counts);
		
		// Remove any words which occur < threshold times
		//
		counts = threshFilter(counts, threshold);
		
		FileUtil.writeLines(counts.keySet(), outname);		
	}

	/**
	 * Filter vocabulary by min occurrence threshold
	 * 
	 * @param counts
	 * @param threshold
	 * @return
	 */
	public static HashMap<String, Integer> threshFilter(HashMap<String, Integer> counts,
			int threshold)
	{
		Iterator<String> keyiter = (counts.keySet()).iterator();
		while(keyiter.hasNext())			
		{
			String key = keyiter.next();
			if(counts.get(key) < threshold)
				keyiter.remove();			
		}		
		return counts;		
	}
	
	/**
	 * Generate corpus-wide word counts 
	 * (useful for setting vocab threshold)
	 * 
	 * @param docFilesName
	 * @param stopWordFileName
	 * @param outname
	 * @throws IOException
	 */
	public static void countWords(String docFilesName, String stopWordFileName, 
			String outname) throws IOException
	{
		// Init OpenNLP tokenizer
		//
		Tokenizer tkizer = 
			new Tokenizer("./models/EnglishTok.bin.gz");
		
		// Which documents are we processing?
		//
		ArrayList<String> documents = getDocs(docFilesName);
		
		// First pass - get word occurrence counts
		//
		HashMap<String,Integer> counts = getCounts(documents, tkizer); 

		// Apply stop word filter
		//
		HashSet<String> stop = readStopWords(stopWordFileName);			
		for(String word : stop)
		{
			counts.remove(word);
		}
		
		// Remove any words which consist entirely of punctuation
		//
		counts = puncFilter(counts);
					
		// Sort by frequency
		//
		String[] sortedWords = freqSortWords(counts, 1);
		int W = sortedWords.length;
		FileWriter out = new FileWriter(new File(outname));
		for(int i = 0; i < 50; i++)
		{	
			out.write(String.format("%s = %d occur\n",
					sortedWords[W-1-i], 
					counts.get(sortedWords[W-1-i])));
		}
		out.write("\n");
		
		// Print out vocab size after filtering at various thresholds
		//			
		int cutoff = 0;
		for(int thresh = 0;  thresh < 50; thresh++)
		{
			while(counts.get(sortedWords[cutoff]) < thresh)
				cutoff += 1;
			out.write(String.format("%d words at thresh %d\n",
					W-cutoff, thresh));
		}
		out.close();
	}

	/**
	 * Given some keywords, find documents which contain ALL keywords
	 * (useful for shrinking down huge corpus)
	 * 
	 * @param docFilesName
	 * @param keywordFileName
	 * @param outname
	 * @throws IOException
	 */
	public static void documentFilter(String docFilesName, String keywordFileName, 
			String outname) throws IOException
	{
		// Write out filenames of all documents in this directory
		// which contain *all* keywords
		//
		HashSet<String> keywords = 
			new HashSet<String>(FileUtil.readLines(keywordFileName));
		ArrayList<String> docHits = new ArrayList<String>();
		
		Tokenizer tkizer = 
			new Tokenizer("./models/EnglishTok.bin.gz");
		
		// Which documents are we processing?
		//
		ArrayList<String> documents = getDocs(docFilesName);
				
		int ctr = 0;
		int N = documents.size();
		for(String txtFile : documents)
		{
			System.out.println(String.format("Doc %d of %d", ctr, N));
			ctr += 1;
			
			HashSet<String> docToks = new HashSet<String>();
			BufferedReader in = new BufferedReader(new FileReader(txtFile));
			String line = in.readLine();
			while(line != null)
			{				
				for(String tok : tkizer.tokenize(line))
				{
					docToks.add(tok.toLowerCase());					
				}				
				line = in.readLine();
			}			
			in.close();
			// Now decide whether or not to add document
			//
			if(docToks.containsAll(keywords))
				docHits.add(txtFile);				
		}	

		FileWriter out = new FileWriter(new File(outname));
		for(String hit : docHits)
		{	
			out.write(String.format("%s\n", hit)); 
		}
		out.close();
	}
	
	/**
	 * Filter out terms which consist entirely of punctuation
	 * 
	 * @param counts
	 * @return
	 */
	public static HashMap<String, Integer> puncFilter(HashMap<String, Integer> counts)
	{
		Iterator<String> keyiter = (counts.keySet()).iterator();
		while(keyiter.hasNext())			
		{
			String key = keyiter.next();
			boolean allpunc = true;
			for(int i = 0; i < key.length(); i++)
			{
				if(Character.isLetterOrDigit(key.charAt(i)))
				{
					allpunc = false;
					break;
				}				
			}
			if(allpunc)
				keyiter.remove();
		}		
		return counts;
	}
	
	/**
	 * Sort words by frequency
	 * @param counts
	 * @param sortDirection
	 * @return
	 */
	public static String[] freqSortWords(HashMap<String, Integer> counts, 
			int sortDirection)
	{
		CountComparator cc = new CountComparator(counts, sortDirection);
		Set<String> wordset = counts.keySet();
		int N = wordset.size();
		String[] sortedWords = new String[N];
		sortedWords = wordset.toArray(sortedWords);
		Arrays.sort(sortedWords, cc);
		return sortedWords;
	}
	
	/**
	 * Read and downcase stopwords
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static HashSet<String> readStopWords(String filename)
	{
		List<String> origStop = FileUtil.readLines(filename);
		HashSet<String> stop = new HashSet<String>();
		for(String word : origStop)
			stop.add(word.toLowerCase());
		return stop;
	}
	
	/**
	 * Get word counts
	 * @param dirName
	 * @param tkizer
	 * @return
	 * @throws IOException
	 */	
	public static HashMap<String, Integer> getCounts(List<String> documents, 
			Tokenizer tkizer) throws IOException
	{
		HashMap<String,Integer> counts = new HashMap<String,Integer>();		
		int ctr = 0;
		int N = documents.size();
		for(String txtFile : documents)
		{			
			System.out.println(String.format("Doc %d of %d", ctr + 1, N));
			ctr += 1;
			
			BufferedReader in = new BufferedReader(new FileReader(txtFile));
			String line = in.readLine();
			while(line != null)
			{				
				for(String tok : tkizer.tokenize(line))
				{
					String lowTok= tok.toLowerCase();
					if(counts.containsKey(lowTok))
						counts.put(lowTok, counts.get(lowTok) + 1);
					else
						counts.put(lowTok, 1);
				}
				line = in.readLine();
			}			
			in.close();
		}	
		
		return counts;
	}

}
