package logiclda.rules;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for a LogicRule which can be exhaustively grounded/propositionalized
 * (may have > 1 z variable, eg Cannot-Link or Must-Link)
 * 
 * @author andrzejewski1
 *
 */
public interface GroundableRule extends LogicRule 
{
	
	/**
	 * Initialize ground rule data structures 
	 * (must be called before any other GroundableRule interface calls)
	 * 
	 * @param z
	 */
	public void groundRule(int[] z);
	
	/**
	 * Evaluate the satisfied weight wrt a single idx in the grounding
	 * @param grounding
	 * @param z
	 * @return
	 */
	public double evalAssign(int[] z, int idx);

	/**
	 * The inverted index maps corpus indices to affected groundings
	 * @return
	 */
	public Map<Integer, Set<Grounding>> getInvIndex();
	
	/**
	 * Return a List of all currently unsatisifed groundings
	 * @return
	 */
	public Set<Grounding> getUnSat();

	/**
	 * Given an updated sample index idx, update unsatisfied groundings	
	 * @param z
	 * @param idx
	 */
	public void updateUnSat(int[] z, int idx);
}
