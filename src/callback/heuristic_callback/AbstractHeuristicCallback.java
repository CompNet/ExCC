package callback.heuristic_callback;
import java.util.ArrayList;

import formulation.Partition;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.HeuristicCallback;
import separation.AbstractSeparation;


public abstract class AbstractHeuristicCallback extends HeuristicCallback{

	public Partition rep = null;
	public int iterations = 0;
	public ArrayList<AbstractSeparation<?>> sep = new ArrayList<AbstractSeparation<?>>();


	public AbstractHeuristicCallback(Partition p){
		rep = p;
	}

	/** Time spent in the callback */
	public double time = 0.0;	

	public abstract void separates() throws IloException;

	public void abortVisible(){
		this.abort();
	}

	public double getBestObjValuePublic() throws IloException {
		return this.getBestObjValue();
	}

	public double getObjValue2() throws IloException {
		return this.getObjValue();
	}

}
