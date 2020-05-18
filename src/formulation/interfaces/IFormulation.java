package formulation.interfaces;

import cplex.Cplex;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.UnknownObjectException;
import variable.CplexVariableGetter;
import variable.VariableValueProvider;


/**
 * 
 * @author Zacharie ALES
 *
 */
public interface IFormulation extends VariableValueProvider{
	
	public Cplex getCplex();
	public CplexVariableGetter variableGetter();
	
	public void displaySolution() throws UnknownObjectException, IloException;

}
