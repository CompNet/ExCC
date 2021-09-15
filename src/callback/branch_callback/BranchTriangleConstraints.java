package callback.branch_callback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import callback.branch_callback.BranchFurtherFromInteger.EdgeVariable;
import callback.branch_callback.BranchFurtherFromInteger.EdgeVariableValueComparator;
import formulation.MyPartition;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.BranchDirection;
import ilog.cplex.IloCplex.IntegerFeasibilityStatus;
import ilog.cplex.IloCplex.NodeId;

import java.lang.Math; 


public class BranchTriangleConstraints extends BranchCallback{

	public MyPartition myp;
	public int n ;
	public TreeSet<ArrayList<Integer>> clustersInArrayFormat = null;
	int nbCluster = 0;
	public double eps = 1E-6;
	public ArrayList<ArrayList<EdgeVariable>> prevVarsList = null;
	
	
	
	public BranchTriangleConstraints(){
		prevVarsList = new ArrayList<ArrayList<EdgeVariable>>();
	}
	
	@Override
	protected void main() throws IloException {
		
		BranchInfo info = null;
	    Object raw;
	    String msg = ">>> At node " + getNodeId() + ", ";
	    NodeId id;
	    raw = getNodeData();  // get the current node's attachment, if any
	    double obj = getObjValue();  // current node bound
	    
	    if (raw == null) {
	    	// no data object -- let use our custom branching scheme (with modulo)
	    	
	    	int randNumbr = 1 + (int)(Math.random() * ((5 - 1) + 1)); // generate a integer random nbr from the range [1,5]
	    	if((randNumbr % 5) != 0)
	    		return; // let cplex do its branching
	    	
	    	// --------------------------------------------
	    	
	    	// TODO: check if there are at least 2 or 3 clusters
	    	ArrayList<EdgeVariable> selEdgeVars = processPickTreeEdgeVariablesFromOneClusters();
	    	// ArrayList<EdgeVariable> selEdgeVars = processPickTreeEdgeVariablesFromTwoClusters();
	    	//ArrayList<EdgeVariable> selEdgeVars = processPickTreeEdgeVariablesFromThreeClusters();
	    	if(selEdgeVars.size() == 0) // if nothing returned,  let cplex do its branching
	    		return;
	    	
	    	prevVarsList.add(selEdgeVars);
	    	
//	    	System.out.println(selEdgeVars.get(0));
//	    	System.out.println(selEdgeVars.get(1));
//	    	System.out.println(selEdgeVars.get(2));
	    	
	    	int selEdgeVarsSize = selEdgeVars.size();
	    	Double currSum = selEdgeVars.get(0).value + selEdgeVars.get(1).value + selEdgeVars.get(2).value;
	    	// if(currSum == 0 || currSum == 3 || currSum == 1)) // I do not use this if block, just in case of epsilon differences
	    	if(currSum-eps<0 || currSum+eps>3 || (currSum+eps>1 && currSum-eps<1))
	    		return; // let cplex do its branching
	    	
	    	ArrayList<Double> sumList = new ArrayList<Double>();
	    	sumList.add(1.0);
	    	sumList.add(3.0);
	    	info = new BranchInfo(sumList, selEdgeVars); // prepare the object 'info' for the right child
	    	
	    	// we create the left child: it is not a composite node
		    IloRange[] cuts = new IloRange[1];
			cuts[0] = info.createSumConstr(0.0); // here, we use the object 'info' just for creating a sum constr 
			makeBranch(cuts, obj); // Branch 1
	        
			// we create the right child: it is a composite node
			// 			handling the composite node: technically we clone the parent node for the right child
			makeBranch(new IloRange[0], obj, info); // branches 2 and 3 will be handled in the next invocation
	    	
	    } else if (raw instanceof BranchInfo) {
			// convert the object to an instance of BranchInfo
			info = (BranchInfo) raw;
	      
			ArrayList<Double> sumList = info.getSumList();
			if(sumList.size() == 2){
				IloRange[] cuts;
				
				// branch 2
				double sum1 = sumList.get(0);
				cuts = new IloRange[1];
				cuts[0] = info.createSumConstr(sum1);
				makeBranch(cuts, obj);
				
				// branch 3
				double sum2 = sumList.get(1);
				cuts = new IloRange[1];
				cuts[0] = info.createSumConstr(sum2);
				makeBranch(cuts, obj);
				
			} else {
				System.err.println("Encountered sumList size greater than 2 " + msg);
				// do nothing, since we have only 3 branches
				// if you need more child branches, you should apply the same principle of composite node here
			}
			
			
	      
	    } else {
	      // unknown node data type -- should never happen
	      System.err.println("Encountered unknown node data type" + msg);
	      abort();
	    }
	    
	    // -----
	    
	    
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
	
	
	
	
	
	public ArrayList<EdgeVariable> processPickTreeEdgeVariablesFromOneClusters() throws IloException {
		
		ArrayList<EdgeVariable> selEdgeVars = new ArrayList<EdgeVariable>();
		boolean ok = false;
		Iterator<ArrayList<Integer>> it = clustersInArrayFormat.iterator();
		
		while(it.hasNext()){
			ArrayList<Integer> cluster = it.next();
			
			selEdgeVars = pickThreeEdgeVariablesFromOneClusters(cluster);
			if(selEdgeVars.size()>0){
				ok = true;
				break;
			} else {
				// if the operation is not success, then change the cluster2
				// so, do nothing
			}
			
		}
        
        return(selEdgeVars);
	}


	
	// most fractional
		// close to zero
		public ArrayList<EdgeVariable> pickThreeEdgeVariablesFromOneClusters(ArrayList<Integer> cluster) throws IloException{
			ArrayList<EdgeVariable> listEdgeVars = new ArrayList<EdgeVariable>();
			ArrayList<EdgeVariable> forbiddenIntraEdgeList = new ArrayList<EdgeVariable>();
			
			boolean isOk = false;
			while(!isOk) {
				ArrayList<EdgeVariable> forbiddenInnerIntraEdgeList = new ArrayList<EdgeVariable>();
				
				EdgeVariable var = null;
				var = pickIntraEdgeVariable(cluster, true, true, forbiddenIntraEdgeList); // i = var.i and k = var.j
				if(var == null)
					var = pickIntraEdgeVariable(cluster, false, true, forbiddenIntraEdgeList);
				if(var == null)
					var = pickIntraEdgeVariable(cluster, false, false, forbiddenIntraEdgeList);
				if(var == null)
					break;
				
				forbiddenInnerIntraEdgeList.add(var);
				
				boolean isOk2 = false;
				while(!isOk2) {
					EdgeVariable var2 = null;
					var2 = pickIntraEdgeVariable(cluster, var.i, true, true, forbiddenInnerIntraEdgeList); // i = var.i and k = var.j
					if(var2 == null)
						var2 = pickIntraEdgeVariable(cluster, var.i, false, true, forbiddenInnerIntraEdgeList);
					if(var2 == null)
						var2 = pickIntraEdgeVariable(cluster, var.i, false, false, forbiddenInnerIntraEdgeList);
					if(var2 == null)
						break;
					
					// if this 'if' block is false, we will pick another inter edge variable
					// myp.v_edge[var2.j][var.j] is intra-edge variable in cluster 2
					if(this.getFeasibility(myp.v_edge[var2.j][var.j]).equals(IntegerFeasibilityStatus.Infeasible)){
						isOk = true;
						isOk2 = true;
						
						double value = this.getValue(myp.v_edge[var2.j][var.j]);
						EdgeVariable var3 = new EdgeVariable(var2.j, var.j, value);
						
						listEdgeVars.add(var);
						listEdgeVars.add(var2);
						listEdgeVars.add(var3);
					} else {
						forbiddenInnerIntraEdgeList.add(var2);
					}
					
				}
				
				if(!isOk)
					forbiddenIntraEdgeList.add(var);
				
			}
			
			return(listEdgeVars);
		}
	
	
	
	
	
	
	
	public ArrayList<EdgeVariable> processPickTreeEdgeVariablesFromThreeClusters() throws IloException {
		
		ArrayList<EdgeVariable> selEdgeVars = new ArrayList<EdgeVariable>();
		boolean ok = false;
		TreeSet<ArrayList<Integer>> clone = (TreeSet<ArrayList<Integer>>) clustersInArrayFormat.clone();
        while(clone.size()>0) {
        	ArrayList<Integer> cluster1 = clone.pollFirst();
        	TreeSet<ArrayList<Integer>> clone2 = (TreeSet<ArrayList<Integer>>) clone.clone();
        	ArrayList<Integer> cluster2 = clone2.pollFirst();
        	
    		Iterator<ArrayList<Integer>> it3 = clone2.iterator();
        	
			while(it3.hasNext()){
				ArrayList<Integer> cluster3 = it3.next();
				
				selEdgeVars = pickThreeEdgeVariablesFromThreeClusters(cluster1, cluster2, cluster3);
				if(selEdgeVars.size()>0){
					ok = true;
					break;
				} else {
					// if the operation is not success, then change the cluster2
					// so, do nothing
				}
				
			}
			if(ok)
				break;
        }
        
        return(selEdgeVars);
	}

	

	// most fractional
	// close to zero
	public ArrayList<EdgeVariable> pickThreeEdgeVariablesFromThreeClusters(ArrayList<Integer> cluster1, ArrayList<Integer> cluster2, ArrayList<Integer> cluster3) throws IloException{
		ArrayList<EdgeVariable> listEdgeVars = new ArrayList<EdgeVariable>();
		ArrayList<EdgeVariable> forbiddenInterEdgeList = new ArrayList<EdgeVariable>();
		
		boolean isOk = false;
		while(!isOk) {
			ArrayList<EdgeVariable> forbiddenInnerInterEdgeList = new ArrayList<EdgeVariable>();
			
			EdgeVariable var = null;
			var = pickInterEdgeVariable(cluster1, cluster2, true, true, forbiddenInterEdgeList); // i = var.i and j = var.j
			if(var == null)
				var = pickInterEdgeVariable(cluster1, cluster2, false, true, forbiddenInterEdgeList);
			if(var == null)
				var = pickInterEdgeVariable(cluster1, cluster2, false, false, forbiddenInterEdgeList);
			if(var == null)
				break;
			
			forbiddenInnerInterEdgeList.add(var);
			
			boolean isOk2 = false;
			while(!isOk2) {
				EdgeVariable var2 = null;
				var2 = pickInterEdgeVariable(cluster3, var.i, true, true, forbiddenInnerInterEdgeList); // i = var.i and k = var.j
				if(var2 == null)
					var2 = pickInterEdgeVariable(cluster3, var.i, false, true, forbiddenInnerInterEdgeList);
				if(var2 == null)
					var2 = pickInterEdgeVariable(cluster3, var.i, false, false, forbiddenInnerInterEdgeList);
				if(var2 == null)
					break;
				
				// if this 'if' block is false, we will pick another inter edge variable
				// myp.v_edge[var2.j][var.j] is intra-edge variable in cluster 2
				if(this.getFeasibility(myp.v_edge[var2.j][var.j]).equals(IntegerFeasibilityStatus.Infeasible)){
					isOk = true;
					isOk2 = true;
					
					double value = this.getValue(myp.v_edge[var2.j][var.j]);
					EdgeVariable var3 = new EdgeVariable(var2.j, var.j, value);
					
					listEdgeVars.add(var);
					listEdgeVars.add(var2);
					listEdgeVars.add(var3);
				} else {
					forbiddenInnerInterEdgeList.add(var2);
				}
				
			}
			
			if(!isOk)
				forbiddenInterEdgeList.add(var);
			
		}
		
		return(listEdgeVars);
	}
	
	
	
	
	
	
	
	public ArrayList<EdgeVariable> processPickTreeEdgeVariablesFromTwoClusters() throws IloException {
		
		ArrayList<EdgeVariable> selEdgeVars = new ArrayList<EdgeVariable>();
		boolean ok = false;
		TreeSet<ArrayList<Integer>> clone = (TreeSet<ArrayList<Integer>>) clustersInArrayFormat.clone();
        while(clone.size()>0) {
        	ArrayList<Integer> cluster1 = clone.pollFirst();
    		Iterator<ArrayList<Integer>> it2 = clone.iterator();
        	
			while(it2.hasNext()){
				ArrayList<Integer> cluster2 = it2.next();
				
				selEdgeVars = pickThreeEdgeVariablesFromTwoClusters(cluster1, cluster2);
				if(selEdgeVars.size()>0){
					ok = true;
					break;
				} else {
					// if the operation is not success, then change the cluster2
					// so, do nothing
				}
				
			}
			if(ok)
				break;
        }
        
        return(selEdgeVars);
	}
	
	
	
	
	public ArrayList<EdgeVariable> pickThreeEdgeVariablesFromTwoClusters(ArrayList<Integer> cluster1, ArrayList<Integer> cluster2) throws IloException{
		ArrayList<EdgeVariable> listEdgeVars = new ArrayList<EdgeVariable>();
		ArrayList<EdgeVariable> forbiddenInterEdgeList = new ArrayList<EdgeVariable>();
		
		boolean isOk = false;
		while(!isOk) {
			ArrayList<EdgeVariable> forbiddenInnerInterEdgeList = new ArrayList<EdgeVariable>();
			
			EdgeVariable var = null;
			var = pickInterEdgeVariable(cluster1, cluster2, true, true, forbiddenInterEdgeList); // i = var.i and j = var.j
			if(var == null)
				var = pickInterEdgeVariable(cluster1, cluster2, false, true, forbiddenInterEdgeList);
			if(var == null)
				var = pickInterEdgeVariable(cluster1, cluster2, false, false, forbiddenInterEdgeList);
			if(var == null)
				break;
			
			forbiddenInnerInterEdgeList.add(var);
			
			boolean isOk2 = false;
			while(!isOk2) {
				EdgeVariable var2 = null;
				var2 = pickInterEdgeVariable(cluster2, var.i, true, true, forbiddenInnerInterEdgeList); // i = var.i and k = var.j
				if(var2 == null)
					var2 = pickInterEdgeVariable(cluster2, var.i, false, true, forbiddenInnerInterEdgeList);
				if(var2 == null)
					var2 = pickInterEdgeVariable(cluster2, var.i, false, false, forbiddenInnerInterEdgeList);
				if(var2 == null)
					break;
				
				// if this 'if' block is false, we will pick another inter edge variable
				// myp.v_edge[var2.j][var.j] is intra-edge variable in cluster 2
				if(this.getFeasibility(myp.v_edge[var2.j][var.j]).equals(IntegerFeasibilityStatus.Infeasible)){
					isOk = true;
					isOk2 = true;
					
					double value = this.getValue(myp.v_edge[var2.j][var.j]);
					EdgeVariable var3 = new EdgeVariable(var2.j, var.j, value);
					
					listEdgeVars.add(var);
					listEdgeVars.add(var2);
					listEdgeVars.add(var3);
				} else {
					forbiddenInnerInterEdgeList.add(var2);
				}
				
			}
			
			if(!isOk)
				forbiddenInterEdgeList.add(var);
			
		}
		
		return(listEdgeVars);
	}
	
	
	
	
//	public ArrayList<EdgeVariable> pickThreeEdgeVariablesFromTwoClusters(ArrayList<Integer> cluster1, ArrayList<Integer> cluster2) throws IloException{
//		ArrayList<EdgeVariable> listEdgeVars = new ArrayList<EdgeVariable>();
//		ArrayList<EdgeVariable> forbiddenInterEdgeList = new ArrayList<EdgeVariable>();
//		ArrayList<EdgeVariable> forbiddenIntraEdgeList = new ArrayList<EdgeVariable>();
//		
//		boolean isOk = false;
//		while(!isOk) {
//			EdgeVariable var = null;
//			var = pickInterEdgeVariable(cluster1, cluster2, true, true, forbiddenInterEdgeList); // i = var.i and j = var.j
//			if(var == null)
//				var = pickInterEdgeVariable(cluster1, cluster2, false, true, forbiddenInterEdgeList);
//			if(var == null)
//				var = pickInterEdgeVariable(cluster1, cluster2, false, false, forbiddenInterEdgeList);
//			if(var == null)
//				break;
//			
//			boolean isOk2 = false;
//			while(!isOk2) {
//				EdgeVariable var2 = null;
//				var2 = pickIntraEdgeVariable(cluster1, var.i, true, true, forbiddenIntraEdgeList); // i = var.i and k = var.j
//				if(var2 == null)
//					var2 = pickIntraEdgeVariable(cluster1, var.i, false, true, forbiddenIntraEdgeList);
//				if(var2 == null)
//					var2 = pickIntraEdgeVariable(cluster1, var.i, false, false, forbiddenIntraEdgeList);
//				if(var2 == null)
//					break;
//				
//				// if this 'if' block is false, we will pick another intra edge variable
//				// myp.v_edge[var2.j][var.j] is another inter-edge variable
//				if(this.getFeasibility(myp.v_edge[var2.j][var.j]).equals(IntegerFeasibilityStatus.Infeasible)){
//					isOk = true;
//					isOk2 = true;
//					
//					double value = this.getValue(myp.v_edge[var2.j][var.j]);
//					EdgeVariable var3 = new EdgeVariable(var2.j, var.j, value);
//					
//					listEdgeVars.add(var);
//					listEdgeVars.add(var2);
//					listEdgeVars.add(var3);
//				} else {
//					forbiddenIntraEdgeList.add(var2);
//				}
//				
//			}
//			
//			if(!isOk)
//				forbiddenInterEdgeList.add(var);
//			
//		}
//		
//		return(listEdgeVars);
//	}
	
	
	/**
	 * Given a cluster, pick an edge variable (i.e. 2 vertices) by respecting the conditions of 'isFrustrated' and 'isMostFractional'
	 */
	public EdgeVariable pickIntraEdgeVariable(ArrayList<Integer> cluster, boolean isFrustrated, 
			boolean isMostFractional, ArrayList<EdgeVariable> forbiddenList) throws IloException{
		EdgeVariable var = null;
		
    	for(int i=0; i<(cluster.size()-1); i++){
    		if(var != null)
    			break;
    		
    		for(int j=i+1; j<cluster.size(); j++){
    			Integer vertexId1 = cluster.get(i);
    			Integer vertexId2 = cluster.get(j);
    			
    			boolean isOk = true;
    			for(EdgeVariable e : forbiddenList){
    				if((e.i==vertexId1 && e.j==vertexId2) || (e.j==vertexId1 && e.i==vertexId2)){
    					isOk = false;
    					break;
    				}
    			}
    			
    			if(isOk && this.getFeasibility(myp.v_edge[vertexId1][vertexId2]).equals(IntegerFeasibilityStatus.Infeasible)){
    				double value = this.getValue(myp.v_edge[vertexId1][vertexId2]);

    				if(var == null) // to be able to provide at least 1 edge var in the worst case
    					var = new EdgeVariable(vertexId1, vertexId2, value);
    				
    				if(isFrustrated && this.myp.d[vertexId1][vertexId2]<0 && isMostFractional && value>0.4 && value<0.6) {
    					var = new EdgeVariable(vertexId1, vertexId2, value);
    					break;
    				}
    				else if(!isFrustrated && isMostFractional && value>0.4 && value<0.6){
    					var = new EdgeVariable(vertexId1, vertexId2, value);
    					break;
    				} 
    				else if(!isFrustrated && !isMostFractional){
    					var = new EdgeVariable(vertexId1, vertexId2, value);
    					break;
    				}
    				
    			}
    		}
    	}
		
		return(var);
	}
	
	
	
	
	
	
	
	
	/**
	 * Given a cluster, pick a vertex adjacent to 'vertexId1' by respecting the conditions of 'isFrustrated' and 'isMostFractional'
	 */
	public EdgeVariable pickIntraEdgeVariable(ArrayList<Integer> cluster, Integer vertexId1, boolean isFrustrated, 
			boolean isMostFractional, ArrayList<EdgeVariable> forbiddenList) throws IloException{
		EdgeVariable var = null;
		
    	for(int i=0; i<(cluster.size()-1); i++){
    		Integer vertexId2 = cluster.get(i);
    		if(vertexId1 != vertexId2){
    			
    			boolean isOk = true;
    			for(EdgeVariable e : forbiddenList){
    				if((e.i==vertexId1 && e.j==vertexId2) || (e.j==vertexId1 && e.i==vertexId2)){
    					isOk = false;
    					break;
    				}
    			}
    			
				if(isOk && this.getFeasibility(myp.v_edge[vertexId1][vertexId2]).equals(IntegerFeasibilityStatus.Infeasible)){
					double value = this.getValue(myp.v_edge[vertexId1][vertexId2]);
	
	//				if(var == null) // to be able to provide at least 1 edge var in the worst case
	//					var = new EdgeVariable(vertexId1, vertexId2, value);
					
					if(isFrustrated && this.myp.d[vertexId1][vertexId2]<0 && isMostFractional && value>0.4 && value<0.6) {
						var = new EdgeVariable(vertexId1, vertexId2, value);
						break;
					}
					else if(!isFrustrated && isMostFractional && value>0.4 && value<0.6){
						var = new EdgeVariable(vertexId1, vertexId2, value);
						break;
					}
					else if(!isFrustrated && !isMostFractional){
						var = new EdgeVariable(vertexId1, vertexId2, value);
						break;
					}
					
				}
    		}
    	}
		
		return(var);
	}
	
	
	
	/**
	 * Given a cluster, pick a vertex adjacent to 'vertexId1' by respecting the conditions of 'isFrustrated' and 'isMostFractional'
	 * Note that 'vertexId1' should not be in the 'cluster'
	 */
	public EdgeVariable pickInterEdgeVariable(ArrayList<Integer> cluster, Integer vertexId1, boolean isFrustrated, 
			boolean isMostFractional, ArrayList<EdgeVariable> forbiddenList) throws IloException{
		EdgeVariable var = null;
		
    	for(int i=0; i<(cluster.size()-1); i++){
    		Integer vertexId2 = cluster.get(i);
    		
    			
			boolean isOk = true;
			for(EdgeVariable e : forbiddenList){
				if((e.i==vertexId1 && e.j==vertexId2) || (e.j==vertexId1 && e.i==vertexId2)){
					isOk = false;
					break;
				}
			}
			
			if(isOk && this.getFeasibility(myp.v_edge[vertexId1][vertexId2]).equals(IntegerFeasibilityStatus.Infeasible)){
				double value = this.getValue(myp.v_edge[vertexId1][vertexId2]);

//				if(var == null) // to be able to provide at least 1 edge var in the worst case
//					var = new EdgeVariable(vertexId1, vertexId2, value);
				
				if(isFrustrated && this.myp.d[vertexId1][vertexId2]<0 && isMostFractional && value>0.4 && value<0.6) {
					var = new EdgeVariable(vertexId1, vertexId2, value);
					break;
				}
				else if(!isFrustrated && isMostFractional && value>0.4 && value<0.6){
					var = new EdgeVariable(vertexId1, vertexId2, value);
					break;
				}
				else if(!isFrustrated && !isMostFractional){
					var = new EdgeVariable(vertexId1, vertexId2, value);
					break;
				}
				
			}
    		
    	}
		
		return(var);
	}
	
	
	
	public EdgeVariable pickInterEdgeVariable(ArrayList<Integer> cluster1, ArrayList<Integer> cluster2, boolean isFrustrated,
			boolean isMostFractional, ArrayList<EdgeVariable> forbiddenList) throws IloException
	{
		EdgeVariable var = null;
		
    	for(int i=0; i<cluster1.size(); i++){
    		if(var != null)
    			break;
    		
    		for(int j=0; j<cluster2.size(); j++){
    			Integer vertexId1 = cluster1.get(i);
    			Integer vertexId2 = cluster2.get(j);
    			
    			boolean isOk = true;
    			for(EdgeVariable e : forbiddenList){
    				if((e.i==vertexId1 && e.j==vertexId2) || (e.j==vertexId1 && e.i==vertexId2)){
    					isOk = false;
    					break;
    				}
    			}
    			
    			if(isOk && this.getFeasibility(myp.v_edge[vertexId1][vertexId2]).equals(IntegerFeasibilityStatus.Infeasible)){
    				double value = this.getValue(myp.v_edge[vertexId1][vertexId2]);

//    				if(var == null) // to be able to provide at least 1 edge var in the worst case
//    					var = new EdgeVariable(i, j, value);
    				
    				if(isFrustrated && this.myp.d[vertexId1][vertexId2]<0 && isMostFractional && value>0.4 && value<0.6) {
    					var = new EdgeVariable(vertexId1, vertexId2, value);
    					break;
    				}
    				else if(!isFrustrated && isMostFractional && value>0.4 && value<0.6){
    					var = new EdgeVariable(vertexId1, vertexId2, value);
    					break;
    				} 
    				else if(!isFrustrated && !isMostFractional){
    					var = new EdgeVariable(vertexId1, vertexId2, value);
    					break;
    				}
    				
    			}
    		}
    	}
		
		return(var);
	}
	
	
	public Integer pickNodeVertexFromCluster(ArrayList<Integer> cluster)
	{
		Integer vertexId = -1;
		Random rand = new Random(); 
		vertexId = cluster.get(rand.nextInt(cluster.size())); 
		return(vertexId);
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
	
	
	
	public class BranchInfo {
		  private final ArrayList<Double> sumList;  // sum of the binary variables. 0, 1 or 3
		  private final ArrayList<EdgeVariable> edgeVars; // vertices 1, 2 and 3

		  /**
		   * Constructor.
		   * @param the sum of the binary variables
		   * @param the sum of the binary variables
		   */
		  public BranchInfo(final ArrayList<Double> sumList, final ArrayList<EdgeVariable> edgeVars) {
		    this.sumList = sumList;
		    this.edgeVars = edgeVars;
		  }

		  /**
		   * Get the sum of the binary variables
		   * @return the sum of the binary variables
		   */
		  public final ArrayList<Double> getSumList() {
		    return sumList;
		  }
		  
		  
		  public final IloRange createSumConstr(double sumValue) throws IloException{
			  IloLinearNumExpr expr = myp.getCplex().linearNumExpr();
			  expr.addTerm(1.0, myp.edgeVar(edgeVars.get(0).i, edgeVars.get(0).j));
			  expr.addTerm(1.0, myp.edgeVar(edgeVars.get(1).i, edgeVars.get(1).j));
			  expr.addTerm(1.0, myp.edgeVar(edgeVars.get(2).i, edgeVars.get(2).j));
			  return( myp.getCplex().eq(sumValue, expr) );
		  }
	}
	
}

