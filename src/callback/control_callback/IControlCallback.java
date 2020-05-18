package callback.control_callback;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import variable.VariableValueProvider;

/**
 * Interface implemented by the control callbacks (e.g., UserCutCallback, LazyCallback, ...)
 *  which enables to obtain access to invisible methods
 * @author zach
 *
 */
public interface IControlCallback extends VariableValueProvider{
	
	/**
	 * Public method which enables to get the value of a variable in a callback
	 * Implementation should look like this: this.getValue(var);
	 * @param var The variable
	 * @return The variable value
	 * @throws IloException
	 */
	public double getValuePublic(IloNumVar var) throws IloException;

	/**
	 * Public method which enables to get the objective value of the best relaxed solution 
	 * Implementation should look like this: this.getBestObjValue();
	 * @return The best relaxed objective value
	 * @throws IloException
	 */
	public double getBestObjValuePublic() throws IloException;

	/**
	 * Public method which enables to get the objective value of the best feasible known solution
	 * Implementation should look like this: this.getObjValue();
	 * @return The best feasible objective value
	 * @throws IloException
	 */
	public double getObjValuePublic() throws IloException;


}
