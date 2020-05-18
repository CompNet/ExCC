package mipstart;

import java.util.HashMap;

import formulation.MyPartition;
import formulation.interfaces.IFEdgeVEdgeW;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import formulation.Edge;


/**
 * This class aims at containing a MIP Start (feasible, integer) solution
 *  provided by a primal heuristic.
 * It stores the feasible solution as <i>var</i> (edge variables) and <i>val</i> 
 * (values of the associated edge variables) arrays,
 *  which is required by CPLEX.
 * This feasible solution is created in the method aiming at performing primal heuristic.
 * 
 * @param author Zacharie ALES, Nejat ARINIK
 */
public class SolutionManager {
	
	
	IFEdgeVEdgeW formulation;
	public IloNumVar[] var; // edge variables in array format instead of matrix
	public double[] val; // edge values in array format instead of matrix
	public int[] membership; // partition information array
	
	// total weight of the misplaced links in the current best formulation
	private double evaluation = -1.0;

	/**
	 * Links between and edge and its index in <i>val</i> and <i>var</i> arrays.
	 */
	HashMap<Edge, Integer> id = new HashMap<Edge, Integer>();

	
	/**
	 * Copies the formulation in input.
	 * Initializes the class variables <i>var</i> (i.e. edge variables),
	 * 	 <i>val</i>  (i.e. values of the edge variables)
	 *  and <i>membership</i> (partition information)
	 * 
	 * @param formulation  current best <i>formulation</i>
	 * @throws IloException
	 */
	public SolutionManager(IFEdgeVEdgeW formulation) throws IloException{

		this.formulation = formulation;
		var = new IloNumVar[arraySize()];
		val = new double[arraySize()];
		
		membership = new int[formulation.n()];

		setValToZeroAndCreateID(formulation.n());
		setVar();
	}
	
	/**
	 * Returns the size of the arrays associated with '<i>var</i>' and '<i>val</i>' arrays.
	 * If we suppose that there are <i>n</i> vertices in the graph,
	 *  the size will be <i>(n * (n-1)) / 2</i>.
	 * 
	 * @return size
	 */
	private int arraySize() {
		return formulation.n()*(formulation.n() - 1) / 2;
	}
	
	
	/**
	 * Returns the stored best partition (i.e. membership) array.
	 * 
	 * @return membership
	 */
	public int [] getMembership() {
		return membership;
	}
	
	
	/**
	 * Saves the partition in input into the stored best partition (i.e. membership) array.
	 * 
	 * @param membership
	 */
	public void setMembership(int [] mem) {
		for(int i=0;i<formulation.n();i++) {
			membership[i]=mem[i];
		}
	}
	

	/**
	 * Set all the edge variables to zero, and creates and id for each edge variable
	 * 
	 * @param n  number of nodes
	 */
	public void setValToZeroAndCreateID(int n){

		/* Set all the edge variables to zero */

		int v = 0;

		for(int i = 1 ; i < n ; ++i)
			for(int j = 0 ; j < i ; ++j){
				val[v] = 0;
				id.put(new Edge(i,j), v);
				v++;
			}
	}

	
	/**
	 * Set all the edge variables from the current best <i>formulation</i>.
	 * 
	 * @throws IloException
	 */
	public void setVar() throws IloException{

		var = new IloNumVar[arraySize()];

		int v = 0;

		for(int i = 1 ; i < formulation.n() ; ++i){
			for(int j = 0 ; j < i ; ++j){
				var[v] = formulation.edgeVar(i,j);
				v++;
			}
		}

	}


	/**
	 * Set the value of the corresponding edge variable e_ij.
	 * 
	 * @param i <i>i</i>.th node
	 * @param j <i>j</i>.th node
	 * @param value  weight of the corresponding edge
	 * @throws IloException
	 */
	public void setEdge(int i, int j, double value){
		this.val[id.get(new Edge(i,j))] = value;
	}

	
	/**
	 * It computes the objective function value of Correlation Clustering.
	 * The '<i>membership</i>' vector contains the partition information obtained
	 *  by 'PrimalHeuristicRounding.java'
	 * 
	 * @return evaluation
	 */
	public double evaluate(){

		if(evaluation == -1.0){
			double result = 0.0;

			for(int i = 1 ; i < formulation.n() ; ++i){
				for(int j = 0 ; j < i ; ++j){
					
					double weight = formulation.edgeWeight(i, j);
					if(membership[i] == membership[j] && weight<0) {
						result += Math.abs(weight);
					} else if(membership[i] != membership[j] && weight>0) {
						result += weight;
					}

				}
			}

			return result;
		}
		else
			return evaluation;
	}

	

	/**
	 * Updates the formulation and the variables. The number of nodes must be
	 *  the same in both formulations.
	 * 
	 * @param formulation2  The new formulation
	 * @throws IloException
	 */
	public void updateFormulationAndVariables(MyPartition formulation2) throws IloException {

		if(formulation.n() == formulation2.n()) {
			this.formulation = formulation2;

			var = new IloNumVar[arraySize()];
			setVar();
		}
	}

}
