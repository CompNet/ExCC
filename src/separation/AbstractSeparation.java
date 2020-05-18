package separation;
import java.util.ArrayList;

import callback.cut_callback.AbstractCutCallback;
import formulation.MyPartition;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import inequality_family.AbstractInequality;
import variable.VariableGetter;

/**
 * Abstract class for all the separation algorithms.
 *  It includes a default Abstract_CutCallback class which can be used whenever
 *   the user separation step consists only in performing the separation described
 *    in the current class.
 * @author zach
 *
 */
public abstract class AbstractSeparation<Formulation extends IFormulation>{

	protected double eps = 1.0e-4;

	public String name;
	public int added_cuts = 0;
	protected VariableGetter vg;
	protected Formulation formulation;

	public AbstractSeparation(String name, Formulation formulation, VariableGetter vg){
		this.name = name;
		this.vg = vg;
		this.formulation = formulation;
	}
	
	public void setFormulation(Formulation formulation) {
		
	}

	/**
	 * Find cut that separate the relaxation from the integer polyhedron.
	 * @return The list of violated inequalities found.
	 * @throws IloException
	 */
	public abstract ArrayList<AbstractInequality<? extends IFormulation>> separate();

	public AbstractCutCallback createDefaultCallback(MyPartition p){
		DefaultCallback d = new DefaultCallback(p);

		return d;
	}

	/**
	 * Abstract_CutCallback which only use the current separation algorithm
	 *  and add the obtained ranges.
	 * @author zach
	 *
	 */
	public class DefaultCallback extends AbstractCutCallback{

		public DefaultCallback(MyPartition p) {
			super(p);
			sep.add(AbstractSeparation.this);
		}

		@Override
		public void separates() throws IloException{
			ArrayList<AbstractInequality<? extends IFormulation>> ineq = separate();

			for(AbstractInequality<? extends IFormulation> i : ineq)
				this.addRange(i.getRange(), 0);
		}

	}

}
