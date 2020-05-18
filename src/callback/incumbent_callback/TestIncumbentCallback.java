package callback.incumbent_callback;

import ilog.concert.IloException;
import ilog.cplex.IloCplex.IncumbentCallback;

/**
 * Enable to check if a solution proposed in a HeuristicCallback has been accepted by cplex
 * @author zach
 *
 */
public class TestIncumbentCallback extends IncumbentCallback{
	
	@Override
	public void main() {
		try {
			System.out.println("Incumbent called: " + getObjValue());
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
}
