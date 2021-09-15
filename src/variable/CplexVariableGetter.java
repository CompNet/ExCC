package variable;

import cplex.Cplex;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.UnknownObjectException;

public class CplexVariableGetter extends VariableGetter {

	public CplexVariableGetter(Cplex cplex) {
		super(cplex);
	}

//	@Override
	public double getValue(IloNumVar var) throws UnknownObjectException, IloException {		
		return cplex.iloCplex.getValue(var);
	
	}
	

}
