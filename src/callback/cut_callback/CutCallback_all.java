package callback.cut_callback;

import java.util.ArrayList;

import formulation.interfaces.IFEdgeVEdgeW;
import ilog.concert.IloException;
import inequality_family.AbstractInequality;
import separation.AbstractSeparation;
import separation.SeparationSTGrotschell;
import separation.SeparationSTKL;
import separation.SeparationSTLabbe;
import separation.SeparationTCCKLFixedSize;

public class CutCallback_all extends AbstractCutCallback{

	public int MAX_CUT;	

	public CutCallback_all(IFEdgeVEdgeW formulation, int MAX_CUT) {
		super(formulation);

		sep.add(new SeparationSTGrotschell(formulation, this.variableGetter(), MAX_CUT));
		sep.add(new SeparationSTLabbe(formulation, this.variableGetter()));	
		sep.add(new SeparationSTKL(formulation, this.variableGetter(), 2, true));
		sep.add(new SeparationTCCKLFixedSize(formulation, this.variableGetter(), 2, null, true));
	}

	@Override
	public void separates() throws IloException {

		for(AbstractSeparation<?> algo : sep){
			
			ArrayList<AbstractInequality<?>> ineq = algo.separate();
			// System.out.println("user cuts generated with: " + algo.name);
			
			for(AbstractInequality<?> in : ineq)
				this.addRange(in.getRange(), 0);
			
			//System.out.println(ineq.size() + " " + algo.name);
		}
		
	}

}
