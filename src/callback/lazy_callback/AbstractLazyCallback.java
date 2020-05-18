package callback.lazy_callback;
import callback.control_callback.IControlCallback;
import formulation.Partition;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.LazyConstraintCallback;
import variable.CallbackVariableGetter;
import variable.VariableGetter;
import variable.VariableValueProvider;


public abstract class AbstractLazyCallback extends LazyConstraintCallback implements IControlCallback {

	public IFormulation formulation = null;
	public int iterations = 0;
	
	public double eps = 0.0000001;
	private CallbackVariableGetter rvg;
	
	public AbstractLazyCallback(IFormulation formulation){
		this.formulation = formulation;
		rvg = new CallbackVariableGetter(formulation.getCplex(), this);
	}
	
	/** Time spent in the callback */
	public double time = 0.0;

	protected void main() throws IloException {
		
		iterations++;
		time -= formulation.getCplex().getCplexTime();
		separates();
		time += formulation.getCplex().getCplexTime();
		
	}

	public void addRange(IloRange range){
		try {
			this.add(range, IloCplex.CutManagement.UseCutFilter);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	public void addLe(IloLinearNumExpr expr, double ubound){
		try {
//			this.add(rep.le(expr, ubound));
			this.add(formulation.getCplex().le(expr, ubound),
					IloCplex.CutManagement.UseCutFilter);
		} catch (IloException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void addGe(IloLinearNumExpr expr, double lbound){
		try {
			this.add(formulation.getCplex().ge(expr, lbound),
					IloCplex.CutManagement.UseCutFilter);
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
