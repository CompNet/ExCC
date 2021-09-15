package inequality_family;

import java.io.Serializable;

import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import ilog.concert.IloRange;
import variable.VariableGetter;



public abstract class AbstractInequality<Formulation extends IFormulation> implements Serializable{
	
	private static final long serialVersionUID = -6530941531155997162L;
	public IloRange ilorange = null;
	public Range range = null;
	public double eps = 1E-6;
	public Formulation formulation;
	
	private Class<Formulation> typeChecker;
	
	public AbstractInequality(Formulation formulation, Class<Formulation> theClass) {
		this.formulation = formulation;
		typeChecker = theClass;
	}
	
	public Range getRange(){
		range = createRange();
		return range;
	}
	
	public boolean setFormulation(IFormulation formulation) {
		
		boolean result = false;
		
		if(typeChecker.isInstance(formulation)) {
			this.formulation = typeChecker.cast(formulation);
			result = true;
		}
		
		return result;
	}
	
	public abstract Range createRange();
	public abstract AbstractInequality<Formulation> clone();
	
	protected abstract double evaluate(VariableGetter vg) throws IloException;	
	public abstract double getSlack(VariableGetter vg) throws IloException;

	
	
	/**
	 * Test if the inequality reaches its bound in the best known integer solution
	 * A constraint of the form ax <= b is tight if ax = b
	 * 
	 * @return True if the bound is reached, false otherwise.
	 */
	public boolean isTight(VariableGetter vg){
		try {
			return Math.abs(getSlack(vg)) < eps; // True if 0 or very close to 0
		} catch (IloException e) {
			e.printStackTrace();
			return false;
		}
	}	
}
