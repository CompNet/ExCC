package callback.presolve_callback;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.PresolveCallback;


public class CallBackPresolveInfo extends PresolveCallback{
	
	@Override
	protected void main() throws IloException {

		System.out.println("Aggregation : " + this.getNaggregations());
		System.out.println("Modified coeffs : " + this.getNmodifiedCoeffs());
		System.out.println("Removed cols : " + this.getNremovedCols());
		System.out.println("Removed rows : " + this.getNremovedRows());
		
	}

	
}
