package logiclda.rules;

import java.util.List;
import java.util.ArrayList;

public class Grounding 
{
	public int[] values;
	
	public int get(int idx)
	{		
		return this.values[idx];
	}
	
	public Grounding(int a)
	{
		this.values = new int[1];
		this.values[0] = a;
	}
	
	public Grounding(int a, int b)
	{
		this.values = new int[2];
		this.values[0] = a;
		this.values[1] = b;
	}
	
	public Grounding(int a, int b, int c)
	{
		this.values = new int[3];
		this.values[0] = a;
		this.values[1] = b;
		this.values[2] = c;
	}
	
	/**
	 * Groundings are equal if corresponding .values lists are equal
	 * (tricky! see http://www.artima.com/lejava/articles/equality.html)
	 */
	@Override
	public boolean equals(Object other)
	{
		boolean retval = false;
		if(other instanceof Grounding)
		{
			Grounding o = (Grounding) other;
			if(o.values.length != this.values.length)
				retval = false;
			else
			{
				retval = true;
				for(int i = 0; i < this.values.length; i++)
					if(this.values[i] != o.values[i])
						retval = false;				
			}
		}
		return retval;
	}
	
	/**
	 * Hacky piece of junk hashcode function
	 */
	public int hashCode()
	{
		int hc = 0;
		int power = 0;
		for(int val : this.values)
			hc = hc ^ ((new Double(val)).hashCode() << power);
		return hc;
	}
}

