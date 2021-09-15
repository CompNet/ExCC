package callback.cut_callback;
import java.util.ArrayList;

import callback.control_callback.IControlCallback;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex.CutManagement;
import ilog.cplex.IloCplex.UserCutCallback;
import inequality_family.Range;
import separation.AbstractSeparation;
import variable.CallbackVariableGetter;
import variable.VariableGetter;


public abstract class AbstractCutCallback extends UserCutCallback implements IControlCallback{

	public IFormulation formulation = null;
	public double root_relaxation = -1.0;
	public int iterations = 0;
	public ArrayList<AbstractSeparation<?>> sep = new ArrayList<AbstractSeparation<?>>();

	public double eps = 0.0000001;
	private CallbackVariableGetter rvg;

	public AbstractCutCallback(IFormulation formulation){
		this.formulation = formulation;
		rvg = new CallbackVariableGetter(formulation.getCplex(), this);
	}

	/** Time spent in the callback */
	public double time = 0.0;

	@Override
	protected void main() throws IloException {

		if(root_relaxation == -1.0)
			root_relaxation = this.getBestObjValuePublic();

		if(!this.isAfterCutLoop()){
			iterations++;
			time -= formulation.getCplex().getCplexTime();
			separates();
			time += formulation.getCplex().getCplexTime();
		}

	}

	public void addRange(IloRange range, int idSep){
		try {
			this.add(range, CutManagement.UseCutFilter);
			sep.get(idSep).added_cuts++;
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public void addLocalRange(Range ri, int idSep) {
		try {
			this.addLocal(formulation.getCplex().range(ri.lbound, ri.expr, ri.ubound));
			if(idSep >= 0)
				sep.get(idSep).added_cuts++;
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public void addRange(Range ri, int idSep) {
		try {
			this.add(formulation.getCplex().range(ri.lbound, ri.expr, ri.ubound),
					CutManagement.UseCutFilter);
			if(idSep >= 0)
				sep.get(idSep).added_cuts++;
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public void addLe(IloLinearNumExpr expr, double ubound, int idSep){
		try {
			this.add(formulation.getCplex().le(expr, ubound), CutManagement.UseCutFilter);
			sep.get(idSep).added_cuts++;
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public void addGe(IloLinearNumExpr expr, double lbound, int idSep){
		try {
			this.add(formulation.getCplex().ge(expr, lbound), CutManagement.UseCutFilter);
			sep.get(idSep).added_cuts++;
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public abstract void separates() throws IloException;

	public void abortVisible(){
		this.abort();
	}

	@Override
	public double getBestObjValuePublic() throws IloException {
		return this.getBestObjValue();
	}

	@Override
	public double getObjValuePublic() throws IloException {
		return this.getObjValue();
	}
	
	@Override
	public double getValuePublic(IloNumVar var) throws IloException{
		return this.getValue(var);
	}

	@Override
	public VariableGetter variableGetter() {
		return rvg;
	}
	
}
