package callback.lazy_callback;

import java.util.ArrayList;

import formulation.MyPartition;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import inequality_family.AbstractInequality;
import inequality_family.Range;
import separation.SeparationTriangle;

public class LazyCBTriangle extends AbstractLazyCallback{

	SeparationTriangle sep;
	
	public LazyCBTriangle(MyPartition p, int MAX_CUT) {
		super(p);
//		IFormulationEdgeVarNodeVar formulation, VariableGetter vg, 
		sep = new SeparationTriangle(p, this.variableGetter(), MAX_CUT);		
	}


	@Override
	public void separates() throws IloException {

		ArrayList<AbstractInequality<? extends IFormulation>> al = sep.separate();
		
		for(AbstractInequality<? extends IFormulation> i : al){
			Range r = i.getRange();
			this.add(formulation.getCplex().range(r.lbound, r.expr, r.ubound),
					IloCplex.CutManagement.UseCutPurge);
		}
		
		System.out.println(al.size() + " lazy triangle");
	}

}
