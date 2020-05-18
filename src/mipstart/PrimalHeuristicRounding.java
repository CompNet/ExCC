package mipstart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;

import formulation.interfaces.IFEdgeVEdgeW;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.UnknownObjectException;
import formulation.Edge;

import java.util.Collections;


/**
 * This class aims at performing a simple primal heuristic called <i>Greedy-based Rounding</i>.
 * Since it is greedy approach, we do not directly use edge variable values which
 *  are in general fractional (i.e. not integer)
 * What is important is if the objective function value is improved when merging 2 clusters.
 * So, the solution that we will obtain at the end is always feasible
 *  (so, no doubt about the validation of triangle constraints)
 */
public class PrimalHeuristicRounding implements AbstractMIPStartGenerate{

	IFEdgeVEdgeW formulation;
	int bestMembership [];
	

	public PrimalHeuristicRounding(IFEdgeVEdgeW s) {
		this.formulation = s;
		this.bestMembership =  new int [formulation.n()];
		
		for(int m = 0 ; m < formulation.n() ; ++m)
			bestMembership[m] = m+1; // assign each node to diff. cluster (starting from 1)
	}
	
	public double evaluate(int[] membership) {
		double result = 0.0;

		for(int i = 1 ; i < formulation.n() ; ++i){
			for(int j = 0 ; j < i ; ++j){
				
				double weight = formulation.edgeWeight(i, j);
				if(membership[i] == membership[j] && weight<0) {
					result += Math.abs(weight);
				} else if(membership[i] != membership[j] && weight>0) {
					result += weight;
				}

			}
		}
		
		return result;
	}


	/**
	 * 
	 * Since x_ij = 1 if the edge (i,j) is in the partition and 0 otherwise,
	 *  we use the following steps:
	 * <ul>
	 * <li> Initially each node constitutes its own cluster </li>
	 * <li> The method terminates <i> a pass</i> when it iterates over
	 *  all edge variables <i></i> </li>
	 * <li> Order edge variables once in decreasing order
	 *  (during the passes, it uses always the same order)</li>
	 * <li> Until there is an improvement in the previous pass</li>
	 * 	  <ul>
	 * 		<li> Iterate over edge variables whose value is > 0.5 (i.e. x_ij > 0.5),
	 * 		 for each edge variable: </li>
	 * 		<li> merge the 2 clusters to which the corresponding nodes
	 * 		 (sharing the edge) belong</li>
	 * 		<li> compute objective function (i.e. evaluating) </li>
	 * 		<li> if the objective function value is improved 
	 * 		(i.e. reduced compared to the best current solution),
	 * 			 assign the solution as the best current solution. </li>
	 *    </ul>
	 * </ul>
	 * 
	 */
	public SolutionManager generateMIPStart() throws IloException {
		
		SolutionManager mip = new SolutionManager(formulation);		
		
		Map<Edge, Double> edgesId = new LinkedHashMap<Edge, Double>();
		
				
		/* While all the edges variables have not been displayed */
		for(int i=1; i<formulation.n(); i++) {
			for(int j=0; j<i; j++) {
				
				double value;
				try {
					IloNumVar x_ij = formulation.edgeVar(i, j);
					value = formulation.variableGetter().getValue(x_ij);
				} catch (UnknownObjectException e) {
					value = 0.0;
					//e.printStackTrace();
				} catch (IloException e) {
					value = 0.0;
					//e.printStackTrace();
				}
				
				Edge e = new Edge(i,j);
				edgesId.put(e, value);
			}
		}
		
		
		List<Map.Entry<Edge, Double>> entryList;
		entryList = new ArrayList<Map.Entry<Edge, Double>>(edgesId.entrySet());
	    Collections.sort(entryList, new Comparator<Map.Entry<Edge, Double>>() {
	        @Override
	        public int compare(Entry<Edge, Double> o1, Entry<Edge, Double> o2) {
	            return o2.getValue().compareTo(o1.getValue());
	        }
	    });
	    
	    
	    // ======================================================
	    
	    
	    double best = Double.MAX_VALUE;
	    int[] membership =  new int [formulation.n()];
		for(int m = 0 ; m < formulation.n() ; ++m)
			membership[m] = m+1; // assign each node to different cluster (starting from 1)
	    
		
		// ------------------------------------------------------------------
	    // after sorting
		// ------------------------------------------------------------------

		
		boolean changed = true;
		while(changed) {
			changed = false;
//			System.out.println(" ------------- iteration begin --------------");
			
			for(Map.Entry<Edge, Double> entry : entryList){
	        	Edge e = entry.getKey();
	        	int i = e.getSource();
	        	int j = e.getDest();
	        	double value = entry.getValue();
	        	
 
	        	if(value > 0.5){ // OLD: value > 1E-4

					int c_i = membership[i];
					int c_j = membership[j];

					// merge both clusters as the value of their edge variable is > 0.5
					membership[j] = c_i;
					
					/* if there are other other nodes being in the 'c_j' cluster,
					 *  put them also in the 'c_i' cluster */
					for(int m = 0 ; m < formulation.n() ; ++m){						
						if(membership[m] == c_j)
							membership[m]=c_i;
					}
					

				
					double newObjVal = evaluate(membership);
					if(newObjVal < best) { // improved
						changed = true;
						best = newObjVal;
						
						for(int m = 0 ; m < formulation.n() ; ++m)
							bestMembership[m] = membership[m];
												
					} else { // if not improved
						
						// restore the 'membership' vector based on best membership
						for(int m = 0 ; m < formulation.n() ; ++m)
							membership[m] = bestMembership[m];
					}
				
					
				}
	
	        }
			
			
			// debug: print the membership vector
			//System.out.print("membership:");
			for(int i1=0;i1<formulation.n();i1++) {
				// System.out.print(" " + bestMembership[i1]);
			}
			// System.out.print("\n");
			
//			System.out.println(" ---------- iteration end ------------");
		}
		// ------------------------------------------------------------------

		
		
		
//		// ---------------------------------------------------------------------
//		for(int m = 0 ; m < formulation.n() ; ++m)
//			membership[m] = 0;
//		
//		
//		for(int i=1; i<formulation.n(); i++) {
//			for(int j=0; j<i; j++) {
//				if(bestMembership[i] == bestMembership[j]) {
//					membership[i] = 1;
//					membership[j] = 1
//				}
//			}
//		}
//		// ---------------------------------------------------------------------
//		
		
		
		for(int i=1; i<formulation.n(); i++) {
			for(int j=0; j<i; j++) {
				if(bestMembership[i] == bestMembership[j])
					mip.setEdge(i,j,1.0);
			}
		}
		
        mip.setMembership(bestMembership);
        

		
        
		return(mip);
	}
	
	
	
}
