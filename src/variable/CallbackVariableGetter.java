package variable;

import callback.control_callback.IControlCallback;
import cplex.Cplex;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.UnknownObjectException;

public class CallbackVariableGetter extends VariableGetter {

	IControlCallback cb;
	
	public CallbackVariableGetter(Cplex cplex, IControlCallback cb) {
		super(cplex);
		this.cb = cb;
	}

	@Override
	public double getValue(IloNumVar var) throws UnknownObjectException, IloException {
		
		return cb.getValuePublic(var);
		
//		try {
//			return cb.getValuePublic(var);
//		} catch (UnknownObjectException e) {
//			return 0.0;
//			//e.printStackTrace();
//		} catch (IloException e) {
//			return 0.0;
//			//e.printStackTrace();
//		}
		
	}

}
