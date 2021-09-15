package separation;
import java.util.ArrayList;
import java.util.Random;

import formulation.interfaces.IFEdgeV;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import inequality_family.AbstractInequality;
import inequality_family.TCCInequality;
import variable.VariableGetter;



/**
 * Use Kernighan-Lin type of algorithm to find violated TCC-inequalities violated.
 * Let C be an odd cycle of size 2p+1 and Cb its two-chords. 
 * The corresponding TCC inequality is:
 * x(C)-x(Cb) <= p
 * 
 * 
 * Let U be the set of vertices which are not in C.
 * We consider the following transformations:
 * - exchange two vertex i and j such that either:!
 * 		- i and j are in C
 * 		- i or j is in C
 *  
 *  Repeat several times :
 * 		- Randomly generate C. 
 * 		- Apply transformations until no transformation remain
 * 			 (a transformation can only occur once).
 * 		- Add the best inequality found.  
 * 
 * @author zach
 *
 */
public class SeparationTCCKLFixedSize extends AbstractKLSeparation<IFEdgeV>{
	
	/** Let a,b,c,d,e be five successive nodes in C.
	 * Let i be a node in V.
	 * Let p_c be the position of c in the cycle
	 * The array xiC[i][p_c] contains the value -x_a,i + x_b,i + x_d,i - x_e_i */
	private double[][] xiC;
	
	/** Array which contain the cost to exchange two nodes
	 * 
	 * exchange[i][j] is equal worstValue if the transformation have already been done
	 *  or if the nodes i and j are both not in C
	 */
	double[][] exchange;
	
	/** Number of iterations */
	int phase = 1;
	
	ArrayList<Integer> sizesCycle;

	public SeparationTCCKLFixedSize(IFEdgeV formulation, VariableGetter vg, int iterations, 
			ArrayList<Integer> sizesCycle, boolean stopIteratingAfterCutFound){
		super("TCC_KL", formulation, vg, iterations, stopIteratingAfterCutFound);
		
		this.sizesCycle = sizesCycle;		
	}
	
	int size;
	int p;
	
	@Override
	public ArrayList<AbstractInequality<? extends IFormulation>> separate(){
		
		/* If sizesCycle == null search cycle of all possible odd sizes >= 5 */
		if(sizesCycle == null){
			sizesCycle = new ArrayList<Integer>();
			for(int i = 5 ; i <= formulation.n() ; i += 2){
				sizesCycle.add(i);
			}
		}
		
		ArrayList<AbstractInequality<? extends IFormulation>> result = new ArrayList<>();
		
		/* For each size of cycle */
		for(int s = 0 ; s < sizesCycle.size() ; s++){
					
			if(!stopIteratingWhenCutFound || result.size() == 0){
				size = sizesCycle.get(s);
				result.addAll(super.separate());
			}
		}
		
		return result;
	}
		
	@Override
	public void setSets(AbstractInequality<IFEdgeV> sets) {
		try{
			
		currentSets = ((TCCInequality)sets).clone();
		}catch(NullPointerException e){
			e.printStackTrace();
			System.exit(0);
		}
	}

	@Override
	public void updateSets(Transformation t) {

		Exchange e = (Exchange)t;
		TCCInequality set = currentSet();
		
		/* Get the nodes involved in the best transformation */
		int c1 = e.id[0];
		int c2 = e.id[1];

		int posC1 = set.inC[c1];
		int posC2 = set.inC[c2];

		/* Update C */
		if(posC1 != -1){
			
			/* If both c1 and c2 are in C */
			if(posC2 != -1){
				set.inC[c1] = posC2;
				set.inC[c2] = posC1;
				
				set.C.set(posC1, c2);
				set.C.set(posC2, c1);
			}
			/* If only c1 is in C */
			else{
				addToC(c2, posC1);
				set.inC[c1] = -1;
			}
		}
		
		/* If only c2 is in C (it's sure in that case since at least one of
		 *  the two nodes must be in C */
		else {

			addToC(c1,posC2);
			set.inC[c2] = -1;
		}

	}

	@Override
	public void initializeSets() {
		
		Random random = new Random();
		currentSets = new TCCInequality(formulation, size);
		TCCInequality set = currentSet();
		
		/* Create C randomly */
		for(int i = 0 ; i < formulation.n() ; ++i)
			set.inC[i] = -1;
		
		for(int i = 0 ; i < size ; ++i){
			
			set.C.add(0);
			
			/* Add one node in C */
			int c = random.nextInt(formulation.n());
			while(set.inC[c] != -1)
				c=(c+1)%formulation.n();
			
			addToC(c, i);
		}
		
	}

	@Override
	public void initializeSubSlacks() throws IloException {
		
		/* Let c and u be any vertices respectively in C and U.
		 * Let a,b,d and e be the neighbor of c in the cycle (i.e.: a,b,c,d,e is a part of the cycle)
		 * And let p_c be the position of node c in the cycle C.
		 * 
		 *   Cost of exchanging c and u (i.e.: c <-> u)
		 *   If c and u are exchanged, the cost of the TCC-inequality x(C)-x(Cb)
		 *   	  is modified by - C1 + C2 with
		 * 	   - "-C1": cost of adding u in C :
		 * 			 - (-x_a,u + x_b,u + x_d,u - x_e,u) = - xiC[u][p_c]
		 * 	   - "-C2": cost of removing c from C :
		 * 			 -x_a,c + x_b,c + x_d,c - x_e,c = + xiC[c][p_c]
		 * 		
		 */
		xiC = new double[formulation.n()][size];
		
		TCCInequality set = currentSet();

		/* Compute xiC */
		for(int i = 0 ; i < formulation.n() ; i++){
			for(int pos = 0 ; pos < size ; pos++){

				int a = set.C.get((pos-2+size)%size);
				int b = set.C.get((pos-1+size)%size);
				int c = set.C.get(pos);
				int d = set.C.get((pos+1)%size);
				int e = set.C.get((pos+2)%size);
				
				/* If the node in the cycle at a distance 1 or 2 of c */
				if(i == a || i == b || i == d || i == e){
//					xiC[i][pos] = worstValue;
					exchange[i][c] = worstValue;
					exchange[c][i] = worstValue;
				}
				else{
					
					xiC[i][pos]  = -vg.getValue(formulation.edgeVar(a,i));
					xiC[i][pos] +=  vg.getValue(formulation.edgeVar(b,i));
					xiC[i][pos] +=  vg.getValue(formulation.edgeVar(d,i));
					xiC[i][pos] -=  vg.getValue(formulation.edgeVar(e,i));
					
				}
						
			}
			
		}
		
	}

	@Override
	public void initializeTransformationArrays() {
		exchange = new double[formulation.n()][formulation.n()];		
	}
	
	@Override
	public void computeTransformationSlacks() {
		
		bestNextTransformation = new Exchange();
		TCCInequality set = currentSet();

		/* For each node in the cycle */
		for(int c = 0 ; c < size ; c++){

			int c_id = set.C.get(c);
			int c_pos = set.inC[c_id];
			
			/* For each node i */
			for(int i = 0 ; i < formulation.n() ; ++i){
				
				/* If i is not c_id
				 * and if i is not in C or if is in C but the exchange i <-> c is possible
				 *  (i.e. they are at least at a distance of 3 in the cycle)
				 */
//				if(i != c_id && (inC[i] == -1 || exchange[i][c_id] != worstValue)){
				if(i != c_id && exchange[i][c_id] != worstValue){

						/* Set the cost for setting i in the cycle at position c_pos and
						 *  for removing c_id from this position */
						exchange[i][c_id] = - xiC[i][c_pos] + xiC[c_id][c_pos];

						int pos_i = set.inC[i];
						
						/* If i is also in C */
						if(pos_i != -1){


							/* Add the cost for setting c_id in the cycle at position pos_i
							 *  and for removing i from this position */
							exchange[i][c_id] += - xiC[c_id][pos_i] + xiC[i][pos_i];
						}
						
						exchange[c_id][i] = exchange[i][c_id];
						
						if(exchange[i][c_id] < bestNextTransformation.slack)
							((Exchange)bestNextTransformation).set(exchange[i][c_id], i, c_id);		
//					}
				}
			}
		}
	}

	@Override
	public void updateSubSlacks(Transformation t) throws IloException {

		Exchange e = (Exchange)t;
		int c1 = e.id[0];
		int c2 = e.id[1];
		
		int posC1 = currentSet().inC[c1];
		int posC2 = currentSet().inC[c2];
		
		/* Ensure that this transformation will not be performed a second time */
		exchange[c2][c1] = worstValue;
		exchange[c1][c2] = worstValue;
		/* If c1 was in the cycle, c2 took its place */
		if(posC1 != -1){
			
			/* Update the score */
			updateScore(posC1, c1, c2, size);
			
		}
		
		/* If c1 was in the cycle, c2 took its place */
		if(posC2 != -1){
			
			/* Update the score */
			updateScore(posC2, c2, c1, size);
		
		}
		
	}
	
	/**
	 * Update the xiC scores after a node of the cycle was changed
	 * @param positionInC Position of the node in C which has been changed
	 * @param previousNode Node at the position prior to the transformation
	 * @param newNode Node at the position after the transformation
	 * @throws IloException 
	 */
	public void updateScore(int positionInC, int previousNode, int newNode, 
			int sizeC) throws IloException{
		
		/* For each node */
		for(int i = 0 ; i < formulation.n() ; ++i){

			exchange[previousNode][i] = worstValue;
			exchange[i][previousNode] = worstValue;
			
			if(i != newNode && i != previousNode){

				double value = 
						- vg.getValue(formulation.edgeVar(i, newNode)) 
							+ vg.getValue(formulation.edgeVar(i, previousNode));
				
				xiC[i][(positionInC-2+sizeC)%sizeC] -= value;
				xiC[i][(positionInC-1+sizeC)%sizeC] += value;
				xiC[i][(positionInC+1)%sizeC] += value;
				xiC[i][(positionInC+2)%sizeC] -= value;
			}	
			
		}
		
		/* newNode can't be exchanged with one of its neighbors */
		TCCInequality tcc = currentSet(); 

		exchange[newNode][tcc.C.get((positionInC-2+sizeC)%sizeC)] = worstValue;
		exchange[newNode][tcc.C.get((positionInC-1+sizeC)%sizeC)] = worstValue;
		exchange[newNode][tcc.C.get((positionInC+1)%sizeC)] = worstValue;
		exchange[newNode][tcc.C.get((positionInC+2)%sizeC)] = worstValue;
		exchange[newNode][previousNode] = worstValue;

		exchange[tcc.C.get((positionInC-2+sizeC)%sizeC)][newNode] = worstValue;
		exchange[tcc.C.get((positionInC-1+sizeC)%sizeC)][newNode] = worstValue;
		exchange[tcc.C.get((positionInC+1)%sizeC)][newNode] = worstValue;
		exchange[tcc.C.get((positionInC+2)%sizeC)][newNode] = worstValue;
		exchange[previousNode][newNode] = worstValue;

//		xiC[newNode][(positionInC-2+sizeC)%sizeC] = worstValue;
//		xiC[newNode][(positionInC-1+sizeC)%sizeC] = worstValue;
//		xiC[newNode][(positionInC+1)%sizeC] = worstValue;
//		xiC[newNode][(positionInC+2)%sizeC] = worstValue;
		
		/* previousNode can still not be exchanged with its previous neighbors and
		 *  now it can't be exchange in position positionInC */
//		xiC[previousNode][positionInC] = worstValue;
	}

	public class Exchange extends Transformation{

		/** Id of the nodes concerned by the transformation:
		 * 	The ids are in [0;n[
		 */
		int[] id = new int[2];
		
		public Exchange(){
			super();
			id[0] = -1;
			id[1] = -1;		
		}
		
		/**
		 * Set the best transformation
		 * @param Score of this transformation
		 * @param id1 Id of the first node 
		 * @param id2 Id of the second node 
		 */
		public void set(double score, int id1, int id2){
			this.slack = score;
			id[0] = id1;
			id[1] = id2;
		}
		
		@Override
		public Exchange clone() {
			Exchange theClone = new Exchange();
			
			theClone.id = id.clone();
			
			return theClone;
		}

	}
	
	public TCCInequality currentSet(){
		return (TCCInequality)currentSets;
	}
	
	public void addToC(int c, int pos){
		currentSet().C.set(pos,c);
		currentSet().inC[c] = pos;	
	}

}

