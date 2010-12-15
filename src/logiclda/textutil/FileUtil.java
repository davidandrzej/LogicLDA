package logiclda.textutil;


import java.io.*;
import java.util.*;

import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;
import org.ujmp.core.enums.FileFormat;
import org.ujmp.core.exceptions.MatrixException;

/**
 * Static methods for reading/writing files
 * 
 * @author andrzeje
 *
 */
public class FileUtil 
{	
	public static String intArrayToString(int[] a)
	{
		StringBuilder retval = new StringBuilder();
		for(int val : a)
		{
			retval.append(String.format("%d ", val));
		}
		
		return retval.toString();
	}
	
	/**
	 * Dump Collection to file, one item per line
	 * @param values
	 * @param outname
	 * @throws IOException
	 */
	public static void writeLines(Collection<String> values, String outname)
		throws IOException
	{
		FileWriter out = new FileWriter(new File(outname));
		for(String val : values)
		{				
			out.write(String.format("%s\n", val));
		}
	}
			
	/**
	 * Write these integers out to plaintext file
	 * 
	 * @param filename
	 * @param vals
	 * @throws IOException
	 */
	public static void writeIntFile(String filename, List<Integer> vals) 
		throws IOException
	{
		FileWriter out = new FileWriter(new File(filename));
		int i = 0;
		for(Integer val : vals)
		{
			out.write(String.format("%d ", val));
			i += 1;
			if(i % 1000 == 0)
				out.write("\n");
		}
		out.close();
	}
	
	/**
	 * Parse line of integers
	 * 
	 * @param line
	 * @return nothing - but adds elements to vec
	 */
	public static Vector<Integer> parseIntLine(String line, 
			Vector<Integer> vec)
	{
		StringTokenizer stok = new StringTokenizer(line);
		while(stok.hasMoreTokens())
			vec.add(Integer.parseInt(stok.nextToken()));
		return vec;
	}

	/**
	 * Just return each line of the file as String
	 * 
	 * @param filename
	 * @return
	 */
	public static List<String> readLines(String filename)
	{
		ArrayList<String> retval = new ArrayList<String>();
		try
	    {
			BufferedReader in = 
				new BufferedReader(new FileReader(filename));
			String curLine = in.readLine();
			while(curLine != null)
		    {
				retval.add(curLine.trim());
				curLine = in.readLine();
		    }
			return retval;
	    }
	catch (IOException ioe)
	    {
		System.out.println(String.format("Bad file(name): %s\n", 
						 ioe.toString()));
		return null;
	    }
	}
	
	/**
	 * Read a plaintext file containing a sequence of integers
	 * 
	 * @param filename 
	 * @return
	 */
	public static Vector<Integer> readIntFile(String filename)
	{
		Vector<Integer> vals = new Vector<Integer>();
		try
	    {
			BufferedReader in = 
				new BufferedReader(new FileReader(filename));
			String curLine = in.readLine();
			while(curLine != null)
		    {
				vals = parseIntLine(curLine, vals);
				curLine = in.readLine();
		    }
			return vals;		
	    }
	catch (IOException ioe)
	    {
		System.out.println(String.format("Bad file(name): %s\n", 
						 ioe.toString()));
		return null;
	    }
	}

	/**
	 * Parse line of doubles
	 * 
	 * @param line
	 * @return nothing - but adds elements to vec
	 */
	public static Vector<Double> parseDoubleLine(String line)
	{
		Vector<Double> vec = new Vector<Double>();
		StringTokenizer stok = new StringTokenizer(line);
		while(stok.hasMoreTokens())
			vec.add(Double.parseDouble(stok.nextToken()));
		return vec;
	}

	/**
	 * Read entire file as a single String
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static String fileSlurp(String filename) throws IOException
	{
		StringBuilder fileContent = new StringBuilder();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line = in.readLine();
		while(line != null)
		{				
			fileContent.append(line);
			fileContent.append(" ");
			line = in.readLine();
		}			
		in.close();
		return fileContent.toString();
	}

	/**
	 * Write a single String out to file
	 * 
	 * @param filename
	 * @param output
	 * @throws IOException
	 */
	public static void fileSpit(String filename, String output) throws IOException
	{
		FileWriter out = new FileWriter(filename);
		out.write(output);
		out.close();
	}
	
	/**
	 * Write matrix out to plaintext file
	 * 
	 * @param filename
	 * @param mat
	 * @throws IOException
	 */
	public static void writeMatrix(String filename, Matrix mat) throws IOException
	{
		FileWriter matout = new FileWriter(filename);
		mat.exportToWriter(FileFormat.TXT, matout);
		matout.close();	
	}
	
	/**
	 * Read an UJMP matrix from text file
	 * @param filename
	 * @return
	 * @throws IOException 
	 * @throws MatrixException 
	 */
	public static Matrix readDoubleMatFile(String filename) 
		throws MatrixException, IOException 
	{
		Vector<Vector<Double>> rows = new Vector<Vector<Double>>();
		try
	    {
			BufferedReader in = 
				new BufferedReader(new FileReader(filename));
			String curLine = in.readLine();
			while(curLine != null)
		    {
				rows.add(parseDoubleLine(curLine));
				curLine = in.readLine();
		    }		
	    }
		catch (IOException ioe)
	    {
			System.out.println(String.format("Bad file(name): %s\n", 
					ioe.toString()));
			return null;
	    }
	
		// Copy values into matrix
		Matrix retval = MatrixFactory.zeros(rows.size(),rows.get(0).size());
		int rowi = 0;
		for(Vector<Double> row : rows)
		{
			assert(row.size() == retval.getSize(1));
			int coli = 0;
			for(double d : row)
			{
				retval.setAsDouble(d, rowi, coli);
				coli += 1;
			}
			rowi += 1;
		}		
		return retval;
	}
}
