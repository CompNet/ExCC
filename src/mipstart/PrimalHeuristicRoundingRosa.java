package mipstart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import formulation.interfaces.IFEdgeVEdgeW;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.UnknownObjectException;
import formulation.Edge;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;



public class PrimalHeuristicRoundingRosa implements AbstractMIPStartGenerate{

	IFEdgeVEdgeW formulation;
	int bestMembership [];
	

	public PrimalHeuristicRoundingRosa(IFEdgeVEdgeW s) {
		this.formulation = s;
		this.bestMembership =  new int [formulation.n()];
		
		for(int m = 0 ; m < formulation.n() ; ++m)
			bestMembership[m] = m+1; // assign each node to different cluster (starting from 1)
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


	public SolutionManager generateMIPStart() throws IloException {

//		System.out.println("SolutionManagerRepresentative :: getMIPStart()");
		
		SolutionManager mip = new SolutionManager(formulation);

//		formulation.displaySolution();
		
		ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();
		
		Map<Edge, Double> edgesId = new LinkedHashMap<Edge, Double>();
		
		/* Check if each point is a cluster of size > 1 */
		boolean[] tb = new boolean[formulation.n()];
				
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
	    
	    
	    // =====================================================================
	    
	    
	    double best = Double.MAX_VALUE;
	    int[] membership =  new int [formulation.n()];
		for(int m = 0 ; m < formulation.n() ; ++m)
			membership[m] = 0; // assign each node to different cluster (starting from 1)
	    
		
		// ---------------------------------------------------------------------
	    // after sorting
		// ---------------------------------------------------------------------

		int counter=0;
		
		boolean changed = true;
		while(changed) {
			changed = false;
//			System.out.println(" ----------------- iteration begin ------------------");
			
			for(Map.Entry<Edge, Double> entry : entryList){
	        	Edge e = entry.getKey();
	        	int i = e.getSource();
	        	int j = e.getDest();
	        	double value = entry.getValue();
	        	
 
	        	if(value == 1) {
	        		if(membership[i] == 0) {
	        			counter += 1;
	        			membership[i] = counter;
	        		}
	        		int c_i = membership[i];
					int c_j = membership[j];

					// merge both clusters
					membership[j] = c_i;
					
					/* if there are other other nodes being in the 'c_j' cluster,
					 *  put them also in the 'c_i' cluster */
					for(int m = 0 ; m < formulation.n() ; ++m){						
						if(c_j != 0 && membership[m] == c_j)
							membership[m]=c_i;
					}
	        	}
	        	if(value > 0.5){ // value > 1E-4

					System.out.print("x" + i + "-" + j + "(" + value + ")\t\t");
	        		
	        		
	        		// ---------------------------------------------------------
	        		boolean valid=false;
	        		double ij=formulation.variableGetter().getValue(formulation.edgeVar(i, j));
	        		double ji=ij;
	        		for(int k=0; k<formulation.n();k++) {
	        			if(i!=k && j!=k) {
		        			System.out.println("i:"+i+" j:"+j+" k="+k);
		           			double ki=formulation.variableGetter().getValue(formulation.edgeVar(k, i));
		           			double ik=ki;
		        			double jk=formulation.variableGetter().getValue(formulation.edgeVar(j, k));
		        			double kj=jk;
		        			
		        			if( (ij + jk - ik <= 1) && (ij - jk + ik <= 1) && (- ij + jk + ik <= 1) )
		        				valid = true;
		        			else
		        				valid=false;

	        			}
	        		}
	        		// ---------------------------------------------------------
					
	        		if(valid) {
	        			
						int c_i = membership[i];
						int c_j = membership[j];

						// merge both clusters as the value of their edge variable is >= 0.5
						membership[j] = c_i;
						
						/* if there are other other nodes being in the 'c_j' cluster, 
						 * put them also in the 'c_i' cluster */ 
						for(int m = 0 ; m < formulation.n() ; ++m){						
							if(membership[m] == c_j)
								membership[m]=c_i;
						}

					
	        		} else {
	        			System.out.println("!!!!!! not valid !!!!!");
	        		}
					
					
				}
	
	        }
			
			
			// debug: print the membership vector
			//System.out.print("membership:");
			for(int i1=0;i1<formulation.n();i1++) {
				//System.out.print(" " + bestMembership[i1]);
			}
			//System.out.print("\n");
			
//			System.out.println("-------- iteration end ------------");
		}
		// ------------------------------------------------------------------

		
		
		
//		// -------------------------------------------------------
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
//		// -------------------------------------------------------
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

	@Override
	public SolutionManager loadIntSolution(int[] initialPartitionMembership) throws IloException {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	
	
}
