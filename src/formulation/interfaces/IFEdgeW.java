package formulation.interfaces;

import ilog.concert.IloException;

/**
 * Interface of objects which associate weights to the edges of a graph
 * 
 * @author zach
 *
 */
public interface IFEdgeW extends IFormulation{
	
	/**
	 * @return Number of nodes in the graph 
	 */
	public int n();
	
	/** 
	 * Get the variable associated to an edge.
	 * @param i First node of the edge.
	 * @param j Second node of the edge.
	 * @return The variable associated to edge (ij).
	 * @throws IloException
	 */
	public double edgeWeight(int i, int j);
	
}
