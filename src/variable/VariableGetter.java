package variable;

import cplex.Cplex;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.UnknownObjectException;

public abstract class VariableGetter {
	
	public Cplex cplex;
	
	public VariableGetter(Cplex cplex) {
		this.cplex = cplex;
	}
	
	public abstract double getValue(IloNumVar var) throws UnknownObjectException, IloException;

}
