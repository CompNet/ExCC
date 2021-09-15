package mipstart;

import ilog.concert.IloException;

/**
 * When you are solving a mixed integer programming problem (MIP), you can supply hints
 *  to help CPLEX find an initial solution.
 *  These hints consist of pairs of variables and values, known as a <i>MIP start</i>,
 *   an <i>advanced start</i>, or a <i>warm start</i>.
 *  
 *  Before providing CPLEX with a MIP Start, we need to decide the best MIP Start 
 *   among the other (feasible, integer) solutions obtained during the Root Relaxation
 *    which is performed in <i>solve()</i> method in AbstractCuttingPlane class.
 *    Generating feasible solution is called as <i>Primal Heuristic</i>.
 *    
 *    Each class which will inherente this abstract class will propose
 *     a different approach for primal heuristic.
 *   
 *  @author Nejat ARINIK
 */
public interface AbstractMIPStartGenerate {
	
	public abstract SolutionManager generateMIPStart() throws IloException;

	public abstract SolutionManager loadIntSolution(int[] initialPartitionMembership) throws IloException;

}
