package callback.branch_callback;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.BranchDirection;


public class BranchCB_CplexPlusRep extends BranchCallback{

	@Override
	protected void main() throws IloException {
		// TODO Auto-generated method stub
		
		int nb = this.getNbranches(); 
		IloNumVar[][] vars = new IloNumVar[nb][];
		double[][] bounds = new double[nb][];
		BranchDirection[][] dirs = new BranchDirection[nb][];
//		double [] est = new double[nb];
		
		for(int i = 0 ; i < nb ; i++)	
			makeBranch(vars[i], bounds[i], dirs[i], this.getObjValue());
		
	}

}
