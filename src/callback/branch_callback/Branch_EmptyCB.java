package callback.branch_callback;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.BranchDirection;

/**
 * Empty callback which use the branching of cplex (used to deactivate cplex improvement)
 * @author zach
 *
 */
public class Branch_EmptyCB extends BranchCallback{

	@Override
	protected void main() throws IloException {
		
		int nb = this.getNbranches(); 
		IloNumVar[][] vars = new IloNumVar[nb][];
		double[][] bounds = new double[nb][];
		BranchDirection[][] dirs = new BranchDirection[nb][];
		
		this.getBranches(vars, bounds, dirs);
		
		/* For each direction */
		for(int i = 0 ; i < nb ; i++)
			makeBranch(vars[i], bounds[i], dirs[i], this.getObjValue());
	}
}
