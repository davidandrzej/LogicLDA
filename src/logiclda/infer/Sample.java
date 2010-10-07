package logiclda.infer;

import logiclda.LDAParameters;

import org.ujmp.core.Matrix;

public interface Sample 
{
	public int[] getZ();
	
	public Matrix getPhi(LDAParameters p);
	public Matrix getTheta(LDAParameters p);

	public void writeSample(String basefn);
	public void writePhiTheta(LDAParameters p, String basefn);
}
