package formulation.interfaces;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;

/**
 * Interface of objects which associate variables to the edges of a graph
 * 
 * @author Zacharie ALES
 *
 */
public interface IFEdgeV extends IFormulation{

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
	public IloNumVar edgeVar(int i, int j) throws IloException;
	
}
