package callback.branch_callback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import formulation.MyPartition;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.BranchDirection;
import ilog.cplex.IloCplex.IntegerFeasibilityStatus;

import java.lang.Math; 


public class BranchFurtherFromInteger_old extends BranchCallback{

	public MyPartition myp;
	public int n ;
	public int[] membership;
	
//	public void test() {
//		
//		List<EdgeVariable> listEdgeVars = new ArrayList<EdgeVariable>();
//		
//		EdgeVariable e = new EdgeVariable(31, 37, 0.5, 1.68, 0.57);
//		listEdgeVars.add(e);
//		e = new EdgeVariable(18, 39, 0.5, 1.53, 1.0);
//		listEdgeVars.add(e);
//		e = new EdgeVariable(18, 20, 0.5, 1.44, 0.87);
//		listEdgeVars.add(e);
//		e = new EdgeVariable(18, 49, 0.5, 1.44, 0.25);
//		listEdgeVars.add(e);
//		e = new EdgeVariable(18, 31, 0.44, 1.53, 0.04);
//		listEdgeVars.add(e);	
//		e = new EdgeVariable(37, 45, 0.5, 1.32, 0.43);
//		listEdgeVars.add(e);
//		e = new EdgeVariable(21, 37, 0.5, 1.18, 0.93);
//		listEdgeVars.add(e);
//		
//		Collections.sort(listEdgeVars, new EdgeVariableChainedComparator(
//				new EdgeVariableValueComparator(),
//                new EdgeVariableUpPsuedoCostComparator(),
//                new EdgeVariableDownPsuedoCostComparator()
//                )
//        );
//		
//		for(EdgeVariable e1 : listEdgeVars)
//			System.out.println(e1);
//		
//	}
	
	
	@Override
	protected void main() throws IloException {
		// source: https://www.ibm.com/developerworks/community/forums/html/topic?id=9b2f163e-5cef-49fe-949d-98c2fa49a455
		// it says that we should only branch on variables that have a status of "integer infeasible"
		
		// another source: https://www.ibm.com/support/pages/node/397111#Item3
		
		// https://www.ibm.com/support/knowledgecenter/SSSA5P_12.10.0/ilog.odms.ide.help/refcppopl/html/branch.html
		
		//int nb = 2;
		int nb = this.getNbranches(); 
		IloNumVar[][] vars = new IloNumVar[nb][];
		double[][] bounds = new double[nb][];
		BranchDirection[][] dirs = new BranchDirection[nb][];
		
		this.getBranches(vars, bounds, dirs);
		
		double value = this.getValue(vars[0][0]);
		System.out.println(value);
		if(Math.abs(value-0.5)>0.05){
			System.out.println("!! we intervene !!");

			// we intervene in the branching process
			
			List<EdgeVariable> listEdgeVars = new ArrayList<EdgeVariable>();
	        for(int i = 0 ; i < n ; ++i)
				for(int j = i+1 ; j < n ; ++j){
					if(this.getFeasibility(myp.v_edge[i][j]).equals(IntegerFeasibilityStatus.Infeasible)){
						//System.out.println(this.getFeasibility(myp.v_edge[i][j]));
						EdgeVariable e = new EdgeVariable(i, j, this.getValue(myp.v_edge[i][j]), this.getUpPseudoCost(myp.v_edge[i][j]),
								this.getDownPseudoCost(myp.v_edge[i][j]));
						listEdgeVars.add(e);
					}
				}
	        
			Collections.sort(listEdgeVars, new EdgeVariableChainedComparator(
					new EdgeVariableValueComparator(),
	                new EdgeVariableUpPsuedoCostComparator(),
	                new EdgeVariableDownPsuedoCostComparator()
	                )
	        );
			
//			System.out.println("---begin---------------------");
//			for(EdgeVariable e : listEdgeVars)
//				System.out.println(e);

			
			int best_i = listEdgeVars.get(0).i;
			int best_j = listEdgeVars.get(0).j;
			
			vars[0][0] = myp.v_edge[best_i][best_j];
			vars[1][0] = myp.v_edge[best_i][best_j];
			
			bounds[0][0] = this.getLB(vars[0][0]);
			bounds[1][0] = this.getUB(vars[0][0]);
			
			dirs[0][0] = BranchDirection.Down;
			dirs[1][0] = BranchDirection.Up;
			
		}
		
		
		/* For each direction */
		for(int i = 0 ; i < nb ; i++){
			makeBranch(vars[i], bounds[i], dirs[i], this.getObjValue());
		}
		
        
	}
	
	
	public void setPartition(MyPartition myp2){
		this.myp = myp2;
		n = myp.v_edge.length;
		System.out.println("n: " + n);
	}
	
	
	public void setMIPStartSolution(int[] membership){
		this.membership = membership;
	}
	
	
	
	public class EdgeVariable {
		public int i;
		public int j;
		public double value;
		public double upPseudoCost;
		public double downPseudoCost;
		
		public EdgeVariable(int i, int j, double value, double upPseudoCost, double downPseudoCost){
			this.i = i;
			this.j = j;
			this.value = value;
			this.upPseudoCost = upPseudoCost;
			this.downPseudoCost = downPseudoCost;
		}
		
		
		@Override
		public String toString(){			
			return "(" + this.i + "," + this.j + "): " + this.value + 
					", upPsuedoCost:"+this.upPseudoCost+", downPsuedoCost:"+downPseudoCost; 
		}
	}
	
	public class EdgeVariableValueComparator implements Comparator<EdgeVariable> {
		
		@Override
	    public int compare(EdgeVariable edge1, EdgeVariable edge2) {
			double valEdge1 = edge1.value;
			double valEdge2 = edge2.value;

			double gapToOneEdge1 = 1.0 - valEdge1;
			double minGapEdge1 = valEdge1;
			if(gapToOneEdge1 < minGapEdge1) minGapEdge1 = gapToOneEdge1;
			
			double gapToOneEdge2 = 1.0 - valEdge2;
			double minGapEdge2 = valEdge2;
			if(gapToOneEdge2 < minGapEdge2) minGapEdge2 = gapToOneEdge2;
			
			
			//System.out.println("--> " + (minGapEdge2 - minGapEdge1));
			
//			if((minGapEdge2 - minGapEdge1)>0.05 || (minGapEdge2 - minGapEdge1)<-0.05){
//				if(minGapEdge1<minGapEdge2)
//					return(1);
//				else if(minGapEdge1>minGapEdge2)
//					return(-1);
//			}
//			
//			return(0);
			
			if(minGapEdge1<minGapEdge2)
				return(1);
			else if(minGapEdge1>minGapEdge2)
				return(-1);
			else
				return(0);
			
	    }
	
	}
	
	public class EdgeVariableUpPsuedoCostComparator implements Comparator<EdgeVariable> {
			
		@Override
	    public int compare(EdgeVariable edge1, EdgeVariable edge2) {
			//int intDiff = (int) ((edge2.upPseudoCost - edge1.upPseudoCost)*10);
			
			int val1 = (int) (edge1.upPseudoCost * 10);
			int val2 = (int) (edge2.upPseudoCost * 10);
//			int intDiff = val2 - val1;
//			return(intDiff);
			
			if(val1<val2)
				return(1);
			else if(val1>val2)
				return(-1);
			else
				return(0);
	    }
	
	}
	
	

	public class EdgeVariableDownPsuedoCostComparator implements Comparator<EdgeVariable> {
		
		@Override
	    public int compare(EdgeVariable edge1, EdgeVariable edge2) {
			//int intDiff = (int) ((edge2.downPseudoCost - edge1.downPseudoCost)*10);

			int val1 = (int) (edge1.downPseudoCost * 10);
			int val2 = (int) (edge2.downPseudoCost * 10);
//			int intDiff = val2 - val1;
//			return(intDiff);
			
			if(val1<val2)
				return(1);
			else if(val1>val2)
				return(-1);
			else
				return(0);
	    }
	
	}
	
	
	/**
	 * This is a chained comparator that is used to sort a list by multiple
	 * attributes by chaining a sequence of comparators of individual fields
	 * together.
	 *
	 */
	public class EdgeVariableChainedComparator implements Comparator<EdgeVariable> {
		
		/*
		 *  An example of output:
		 *  (31,37): 0.5, upPsuedoCost:1.687775288586181, downPsuedoCost:0.5768473558544542
			(18,39): 0.5, upPsuedoCost:1.5311704586521842, downPsuedoCost:1.0
			(18,20): 0.5, upPsuedoCost:1.4474803698018945, downPsuedoCost:0.8775257110112307
			(18,49): 0.5, upPsuedoCost:1.441635005911678, downPsuedoCost:0.2540151763129188
			(10,21): 0.5, upPsuedoCost:1.5126436465132542, downPsuedoCost:0.14779607175529463
			(18,31): 0.44476881038866733, upPsuedoCost:1.5351798733462174, downPsuedoCost:0.0459267043648667 => is not any error, 
																	since diff betw 0.5 and 0.44 is 0 according to our comparator
			(37,45): 0.5, upPsuedoCost:1.3239723793614075, downPsuedoCost:0.4311269547190477
			(21,37): 0.5, upPsuedoCost:1.1822213560858472, downPsuedoCost:0.9303732239611691
		 * 
		 */
	 
	    private List<Comparator<EdgeVariable>> listComparators;
	 
	    @SafeVarargs
	    public EdgeVariableChainedComparator(Comparator<EdgeVariable>... comparators) {
	        this.listComparators = Arrays.asList(comparators);
	    }
	 
	    @Override
	    public int compare(EdgeVariable edge1, EdgeVariable edge2) {
	        for (Comparator<EdgeVariable> comparator : listComparators) {
	            int result = comparator.compare(edge1, edge2);
	            if (result != 0) {
	                return result;
	            }
	        }
	        return 0;
	    }
	}
	
}

