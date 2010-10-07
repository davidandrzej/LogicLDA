package logiclda.infer;

/**
 * Gradient over topic assignments, to be passed to RelaxedSample.emdaStep()
 * 
 * @author david
 */
public class Gradient {

	public double[][] gradients;
	public int[] indices;
	
	/**
	 * gradients[i] is z-gradient wrt indices[i]
	 *   
	 * @param gradients
	 * @param indices
	 */
	public Gradient(double[][] gradients, int[] indices)
	{
		assert(gradients.length == indices.length);
		this.gradients = gradients;
		this.indices = indices;
	}
}
