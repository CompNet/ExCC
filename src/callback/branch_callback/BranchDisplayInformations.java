package callback.branch_callback;
import java.util.ArrayList;
import java.util.List;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.BranchDirection;


public class BranchDisplayInformations extends BranchCallback{

	@Override
	protected void main() throws IloException {
		
		int nb = this.getNbranches(); 
		IloNumVar[][] vars = new IloNumVar[nb][];
		double[][] bounds = new double[nb][];
		BranchDirection[][] dirs = new BranchDirection[nb][];
		
		this.getBranches(vars, bounds, dirs);
		
		/* For each direction */
		for(int i = 0 ; i < nb ; i++){
			
			List<EdgeVariableFixedToOne> l_fv = new ArrayList<EdgeVariableFixedToOne>();
			
			/* For each fixed variable */
			for(int j = 0 ; j < vars[i].length ; ++j){
				
				EdgeVariableFixedToOne fv = getFixedVariable(vars[i][j].getName(), bounds[i][j]);
				
				if(fv != null)
					l_fv.add(fv);
			}
				
			if(l_fv.size() > 0)
				makeBranch(vars[i], bounds[i], dirs[i], this.getObjValue(), l_fv);
			else
				makeBranch(vars[i], bounds[i], dirs[i], this.getObjValue());
		}
		
//		System.out.println();
		
	}
	
	public class EdgeVariableFixedToOne{
		
		public int i;
		public int j;
		
		public EdgeVariableFixedToOne(int i, int j){
			this.i = i;
			this.j = j;
		}
		
		@Override
		public String toString(){			
			return i + "," + j; 
		}
		
		
	}

	public EdgeVariableFixedToOne getFixedVariable(String s, double bound){
		
		EdgeVariableFixedToOne result = null;
		
		if(bound == 1.0 && s.contains("x_")){
			String[] temp = s.split("_");
			
			try{
				result = new EdgeVariableFixedToOne(Integer.parseInt(temp[1]), Integer.parseInt(temp[2]));
			}catch(NumberFormatException nfe){}
		}
		
		return result;
		
	}
}
