package callback.cut_callback;

import formulation.interfaces.IFormulation;
import ilog.concert.IloException;

public class EmptyCutCallback extends AbstractCutCallback{

	public EmptyCutCallback(IFormulation formulation) {
		super(formulation);
	}

	@Override
	public void separates() throws IloException {
	}

}
