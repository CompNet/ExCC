package callback.branch_callback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import formulation.MyPartition;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.BranchDirection;
import ilog.cplex.IloCplex.IntegerFeasibilityStatus;

import java.lang.Math; 


public class BranchFurtherFromInteger extends BranchCallback{

	public MyPartition myp;
	public int n ;
	public TreeSet<ArrayList<Integer>> clustersInArrayFormat = null;
	int nbCluster = 0;
	
	// ArrayList<ArrayList<Integer>> clusters = getClustersInArrayFormat();
	
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
		
		
		int nb = 2;
		//int nb = this.getNbranches(); 
		IloNumVar[][] vars = new IloNumVar[nb][];
		vars[0] = new IloIntVar[1];
		vars[1] = new IloIntVar[1];
		
		double[][] bounds = new double[nb][];
		bounds[0] = new double[1];
		bounds[1] = new double[1];
		
		BranchDirection[][] dirs = new BranchDirection[nb][];
		dirs[0] = new BranchDirection[1];
		dirs[1] = new BranchDirection[1];
		
		// this.getBranches(vars, bounds, dirs);
		
		
		// ---
		

		int better_i = -1;
		int better_j = -1;
		boolean ok = false;
		List<EdgeVariable> listEdgeVars = new ArrayList<EdgeVariable>();
		List<EdgeVariable> listFrustratedEdgeVars = new ArrayList<EdgeVariable>();
		TreeSet<ArrayList<Integer>> clone = (TreeSet<ArrayList<Integer>>) clustersInArrayFormat.clone();
        while(clone.size()>0) {
        	ArrayList<Integer> cluster1 = clone.pollFirst();
    		Iterator<ArrayList<Integer>> it2 = clone.iterator();
        	
			while(it2.hasNext()){
				ArrayList<Integer> cluster2 = it2.next();
				
				for(int i = 1 ; i <= cluster1.size() ; ++i){
					for(int j = 1 ; j <= cluster2.size() ; ++j){
						int u = cluster1.get(i-1);
						int v = cluster2.get(j-1);

						if(!ok && this.getFeasibility(myp.v_edge[u][v]).equals(IntegerFeasibilityStatus.Infeasible)){
							double value = this.getValue(myp.v_edge[u][v]);
							listEdgeVars.add(new EdgeVariable(u, v, value));

							boolean isFrustratedEdge = this.myp.d[u][v]>0; // inter-cluster positive link
							
							if(isFrustratedEdge) {
								listFrustratedEdgeVars.add(new EdgeVariable(u, v, value));
								//System.out.println(value);
								
								if(value>0.4 && value<0.6){
									ok = true;
									better_i = u;
									better_j = v;
									break;
								}
							}
						}
					}
					if(ok)
						break;
				}
				if(ok)
					break;
				
			}
			if(ok)
				break;
        }
        
        // if there is no inter-cluster frustrated edge candidate whose the value is in the range [0.4, 0.6]
        if(!ok){
        	if(listFrustratedEdgeVars.size()>0) {
        		Collections.sort(listFrustratedEdgeVars, new EdgeVariableValueComparator());
        		better_i = listFrustratedEdgeVars.get(0).i;
        		better_j = listFrustratedEdgeVars.get(0).j;
        	} else {
        		Collections.sort(listEdgeVars, new EdgeVariableValueComparator());
        		better_i = listEdgeVars.get(0).i;
        		better_j = listEdgeVars.get(0).j;
        	}
        }
        
//        System.out.println("---");
//        System.out.println(myp.v_edge[better_i][better_j]);
//      	System.out.println(this.getValue(myp.v_edge[better_i][better_j]));
		
	        
//			Collections.sort(listEdgeVars, new EdgeVariableChainedComparator(
//					new EdgeVariableValueComparator(),
//	                new EdgeVariableUpPsuedoCostComparator(),
//	                new EdgeVariableDownPsuedoCostComparator()
//	                )
//	        );
			
        
//			System.out.println("---begin---------------------");
//			for(EdgeVariable e : listEdgeVars)
//				System.out.println(e);

      	
		
		vars[0][0] = myp.v_edge[better_i][better_j];
		vars[1][0] = myp.v_edge[better_i][better_j];
		
		bounds[0][0] = this.getLB(vars[0][0]);
		bounds[1][0] = this.getUB(vars[0][0]);
		
		dirs[0][0] = BranchDirection.Down;
		dirs[1][0] = BranchDirection.Up;
			
		/* For each direction */
		for(int i = 0 ; i < nb ; i++){
			makeBranch(vars[i], bounds[i], dirs[i], this.getObjValue());
		}
        
	}
	
	
	public void setPartition(MyPartition myp2){
		this.myp = myp2;
		n = myp.v_edge.length;
		//System.out.println("n: " + n);
	}
	
	
	public void setMIPStartSolution(TreeSet<ArrayList<Integer>> mipStartInArrayFormat){
		this.clustersInArrayFormat = mipStartInArrayFormat;
		this.nbCluster = mipStartInArrayFormat.size();
	}
	
	
	
	public class EdgeVariable {
		public int i;
		public int j;
		public double value;
		
		public EdgeVariable(int i, int j, double value){
			this.i = i;
			this.j = j;
			this.value = value;
		}
		
		@Override
		public String toString(){			
			return "(" + this.i + "," + this.j + "): " + this.value;
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
			
			
			if(minGapEdge1<minGapEdge2)
				return(1);
			else if(minGapEdge1>minGapEdge2)
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

