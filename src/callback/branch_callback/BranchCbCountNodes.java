package callback.branch_callback;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.BranchCallback;


public class BranchCbCountNodes extends BranchCallback{

	int nodes=0;
	
	@Override
	protected void main() throws IloException {

		nodes++;
		
	}

}
