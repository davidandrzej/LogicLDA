package logiclda.rules;

import java.util.Map;
import java.util.List;

/**
 * Interface for a LogicRule which can easily be grounded/propositionalized
 * because it does not rely on more than one latent z value
 * 
 * (eg, a seed z-label or doc-label type rule)
 * 
 * @author andrzejewski1
 *
 */
public interface IndependentRule extends LogicRule 
{
	
	/**
	 * After calling applyEvidence, this returns a 2D array mapping each 
	 * corpus idx -> logic weights for each potential z-value [w1, ..., wT]
	 *    
	 * This can then easily be consulted when doing Logic Collapsed Gibbs, etc
	 * 
	 * @param T
	 * @return
	 */
	public double[][] toZLabel(int N, int T);	
	
}
