package separation;
import java.util.ArrayList;
import java.util.Random;

import formulation.interfaces.IFEdgeV;
import ilog.concert.IloException;
import inequality_family.AbstractInequality;
import inequality_family.STInequality;
import variable.VariableGetter;


/**
 * Use Kernighan-Lin type of algorithm to find violated ST-inequalities violated
 *  (with S and T two disjoint subsets such that 0<|S|<|T|):
 * x(S:T) - x(S) - x(T) <= |S|
 * 
 * Let U be the set of nodes which are not in S or T.
 * For three given subsets S,T and U, we consider the following transformations:
 * - move a vertex from one set to another;
 * - exchange the set of two vertices which are in different sets.
 *  
 *  Repeat several iterations of
 * 		Randomly generate subset S and T.
 * 		do a phase:
 * 			Apply transformations until no transformation remain
 * 			 (a transformation can only happened one time).
 * 		until the best inequality found in the phase is better than the best found
 * 		 in the previous phase  
 * 		Add the best inequality found.
 * 
 * @author zach
 *
 */
public class SeparationSTKL extends AbstractKLSeparation<IFEdgeV>{
	
	public boolean ratioEvaluation = false;

	/* Contain for each vertex i, the sum_s_in_S x(i,s) and sum_t_in_T x(i,t) */
	private double[] xiS;
	private double[] xiT;
	
	/* Value of the left hand side of the current ST-cut (i.e.: x(S,T) - x(S) - x(T)) */
	public double leftHandSide;
	
	/* New value of the right hand after the transformation is applied */
	public double rightHandSideChange;
	
	/** Array which contain the cost to exchange two nodes
	 * 
	 * exchange[i][j] is equal worstValue if the transformation have already been done
	 *  or if the nodes i and j have been in the same set
	 */
	double[][] exchange;
	
	/** Array which contain the cost to move one node from one set to another 
	 * - move[i][0] : cost to move i to S 
	 * - move[i][1] : cost to move i to T 
	 * - move[i][2] : cost to move i to U 
	 * 
	 * move[i][j] is equal to worstValue if the transformation have already been done 
	 * or if the node i started in the set j. 
	 */
	double[][] move;
		
	public SeparationSTKL(IFEdgeV formulation, VariableGetter vg, int iterations,
			boolean stopIteratingAfterCutFound) {
		super("ST_KL", formulation, vg, iterations, stopIteratingAfterCutFound);
		
	}
	
	@Override
	public void setSets(AbstractInequality<IFEdgeV> sets) {
		currentSets = ((STInequality)sets).clone();
	}

	@Override
	public void updateSets(Transformation tr) {
		if(tr instanceof Exchange){
			Exchange e = (Exchange)tr;
			
			switch(e.type){
			case 0: 
				removeFromS(e.id[0]); addToT(e.id[0]);
				removeFromT(e.id[1]); addToS(e.id[1]);
				break;
			case 1: 
				removeFromS(e.id[0]); addToS(e.id[1]);
				break;
			case 2: 
				removeFromT(e.id[0]); addToT(e.id[1]);
				break;
			}

			leftHandSide -= exchange[e.id[0]][e.id[1]];
			
			exchange[e.id[0]][e.id[1]] = worstValue;
			exchange[e.id[1]][e.id[0]] = worstValue;
			
		}
		else{
			
			Move m = (Move)tr;
			
			leftHandSide -= move[m.id][m.to];
			
			switch(m.from){
			case 0: removeFromS(m.id); move[m.id][0] = worstValue; break;
			case 1: removeFromT(m.id); move[m.id][1] = worstValue; break;
			case 2: move[m.id][2] = worstValue; break;
			}
			
			switch(m.to){
			case 0: addToS(m.id); move[m.id][0] = worstValue; break;
			case 1: addToT(m.id); move[m.id][1] = worstValue; break;
			case 2: move[m.id][2] = worstValue; break;
			}
		}
				
	}

	@Override
	public void initializeSets() {
		
		currentSets = new STInequality(formulation);
		
		Random random = new Random();
		
		/* Create S and T randomly */
		for(int i = 0 ; i < formulation.n() ; ++i){
			
			switch(random.nextInt(3)){
				case 0: 
					addToS(i);
					currentSet().inT[i] = false;
					break;
				case 1: 
					addToT(i);
					currentSet().inS[i] = false;
					break;
				case 2:
					currentSet().inS[i] = false;
					currentSet().inT[i] = false;
					break;
			}
		}
		
		/* Ensure that |S|<|T| */
		if(currentSet().S.size() >= currentSet().T.size()){
			
			if(currentSet().S.size() == currentSet().T.size()){

				/* If |S| = |T| > 0 */
				if(currentSet().S.size() > 0){
					
					/* Add an element of S in T */
					int s_id = random.nextInt(currentSet().S.size());
					int s = currentSet().S.get(s_id);
					currentSet().S.remove(s_id);
					currentSet().inS[s] = false;
					addToT(s);
					
				}
				
				/* If |S| = |T| = 0 */
				else{
					
					/* Add one element of U in T */
					int u_id = random.nextInt(formulation.n());
					addToT(u_id);
					
				}
					
			}
			
			/* If |S| > |T| */
			else{
				
				/* Exchange S and T */
				ArrayList<Integer> al_temp = currentSet().S;
				currentSet().S = currentSet().T;
				currentSet().T = al_temp;
				
				boolean[] temp = currentSet().inS;
				currentSet().inS = currentSet().inT;
				currentSet().inT = temp;
				
			}
		}
		
		/* If |S| = 0 */
		if(currentSet().S.size() == 0){
			
			/* If |T| = 1 */
			if(currentSet().T.size() == 1){
				
				/* Add one node in T */
				int t = random.nextInt(formulation.n());
				if(currentSet().inT[t])
					t=(t+1)%formulation.n();
				
				addToT(t);
				
			}
			
			/* Add one node in S (one node which is not in T) */
			int s = random.nextInt(this.formulation.n());
			
			if(currentSet().T.size() < this.formulation.n())
				while(currentSet().inT[s])
					s=(s+1)%this.formulation.n();
			else
				removeFromT(s);
			
			addToS(s);
			
		}
		
	}

	@Override
	public void initializeSubSlacks() throws IloException {
		
		/* Set the impossible transformations to worstValue */
		
		/* S -> S and S <-> S */
		for(int s = 0 ; s < currentSet().S.size() ; ++s){
			
			int s_id = currentSet().S.get(s);
			move[s_id][0] = worstValue;
			
			for(int s2 = s+1 ; s2 < currentSet().S.size() ; ++s2){
				
				int s2_id = currentSet().S.get(s2);
				exchange[s_id][s2_id] = worstValue;
				exchange[s2_id][s_id] = worstValue;
				
			}
			
		}

		/* T -> T and T <-> T */
		for(int t = 0 ; t < currentSet().T.size() ; ++t){

			int t_id = currentSet().T.get(t);
			move[t_id][1] = worstValue;
			
			for(int t2 = t+1 ; t2 < currentSet().T.size() ; ++t2){
				
				int t2_id = currentSet().T.get(t2);
				exchange[t_id][t2_id] = worstValue;
				exchange[t2_id][t_id] = worstValue;
				
			}
			
		}
		
		/* U -> U and U <-> U */
		for(int u = 0 ; u < formulation.n() ; ++u){
			if(!currentSet().inS[u] && !currentSet().inT[u]){
				
				move[u][2] = worstValue;
				
				for(int u2 = u+1 ; u2 < formulation.n() ; ++u2){
					
					if(!currentSet().inS[u2] && !currentSet().inT[u2]){

						exchange[u][u2] = worstValue;
						exchange[u2][u] = worstValue;
						
					}
				}				
			}
		}
		
		/* 
		 * Let s,t,u be any vertices respectively in S,T and U.
		 * Let S2=S-s and T2=T-t.
		 * 
		 *  Ex: cost of S -> T
		 * 		If s goes from S to T, the slack of the ST-inequality
		 * 			 (|S| - x(S,T) + x(S) + x(T))
		 * 		 is modified by 2 (x(formulation,T) - x(formulation,S2) + 1  since 
		 * 		|S2| - x(S2, T+s) + x(T+s) + x(S2) -  =
		 * 			 |S| - x(S,T) + x(T) + x(S) -  2 (x(formulation, S2) - x(formulation,T)) - 1
		 * 
		 * The cost of the transformations are:
		 * 		1 : S -> T  : 2(x(formulation,T ) - x(formulation,S2)) - 1
		 * 		2 : S -> U  :   x(formulation,T ) - x(formulation,S2)  - 1
		 * 		3 : T -> S  : 2(x(t,S ) - x(t,T2)) + 1
		 * 		4 : T -> U  :   x(t,S) - x(t,T2)
		 * 		5 : U -> S  :   x(u,S ) - x(u,T )  + 1
		 * 		6 : U -> T  :   x(u,T ) - x(u,S )
		 * 		7 : S <-> T : 2(x(formulation,T2) + x(t,S2)  - x(t,T2) - x(formulation,S2))
		 * 		8 : S <-> U :   x(u,S2) + x(formulation,T )  - x(formulation,S2) - x(u,T )  
		 * 		9 : T <-> U :   x(u,T2) + x(t,S )  - x(t,T2) - x(u,S ) 
		 */
		
		/* To avoid repeating loops, we keep in memory for each 
		 * 		- s : x(formulation,S2) and x(formulation,T )
		 * 		- t : x(t,S ) and x(t,T2)
		 * 		- u : x(u,S ) and x(u,T )
		 */
		xiS = new double[formulation.n()];
		xiT  = new double[formulation.n()];
		
		/* Compute xsS and xsT */
		for(int s = 0 ; s < currentSet().S.size() ; s++){
			
			int s_id = currentSet().S.get(s);
			xiS[s_id] = 0;
			xiT[s_id] = 0;
			
			for(int s2 = 0 ; s2 < currentSet().S.size() ; ++s2)
				if(s != s2){
					xiS[s_id] += this.vg.getValue(formulation.edgeVar(s_id,currentSet().S.get(s2)));
				}
			
			for(int t = 0 ; t < currentSet().T.size() ; ++t){
				xiT[s_id] += this.vg.getValue(formulation.edgeVar(s_id,currentSet().T.get(t)));
			}
			
		}
		
		/* Compute xtT and xtS */
		for(int t = 0 ; t < currentSet().T.size() ; t++){
			
			int t_id = currentSet().T.get(t);
			xiT[t_id] = 0;
			xiS[t_id] = 0;
			
			for(int t2 = 0 ; t2 < currentSet().T.size() ; ++t2)
				if(t!= t2){
					xiT[t_id] += vg.getValue(formulation.edgeVar(t_id,currentSet().T.get(t2)));
				}
			
			for(int s = 0 ; s < currentSet().S.size() ; ++s){
				xiS[t_id] += this.vg.getValue(formulation.edgeVar(t_id, currentSet().S.get(s)));
			}
			
		}
		
		leftHandSide = 0.0;
		
		for(int s = 0 ; s < currentSet().S.size() ; ++s){
			
			for(int t = 0 ; t < currentSet().T.size() ; ++t)
				leftHandSide += this.vg.getValue(
						formulation.edgeVar(currentSet().S.get(s), currentSet().T.get(t))
						);
			
			for(int s2 = s+1 ; s2 < currentSet().S.size() ; ++s2)
				leftHandSide -= this.vg.getValue(
						formulation.edgeVar(currentSet().S.get(s), currentSet().S.get(s2))
						);
		}
		
		for(int t = 0 ; t < currentSet().T.size() ; ++t)
			for(int t2 = t+1 ; t2 < currentSet().T.size() ; ++t2)
				leftHandSide -= this.vg.getValue(
						formulation.edgeVar(currentSet().T.get(t), currentSet().T.get(t2))
						);
		

		/* Compute xuS and xuT */
		for(int u = 0 ; u < formulation.n() ; u++){
			
			if(!currentSet().inS[u] && !currentSet().inT[u]){
			
				xiT[u] = 0;
				xiS[u] = 0;
				
				for(int s = 0 ; s < currentSet().S.size() ; ++s)
					xiS[u] += this.vg.getValue(formulation.edgeVar(u, currentSet().S.get(s)));
				
				for(int t = 0 ; t < currentSet().T.size() ; ++t)
					xiT[u] += vg.getValue(formulation.edgeVar(u, currentSet().T.get(t)));
			}
			
		}
	}

	@Override
	public void initializeTransformationArrays() {
		exchange = new double[formulation.n()][formulation.n()];
		move = new double[formulation.n()][3];
	}

	@Override
	public void computeTransformationSlacks() throws IloException {

		Exchange bestNextExchange = new Exchange();
		Move bestNextMove = new Move();
		STInequality set = currentSet();

		rightHandSideChange = 0.0;
		double tempRHSC = 0.0;
		double bestFrac = -Double.MAX_VALUE;
		
		for(int i = 0 ; i < formulation.n() ; ++i){

			/* If i is in S: Compute transformations 1,2 and 7 */ 
			if(set.inS[i]){
				
				/* 1 : S -> T  : 2(x(formulation,T) - x(formulation,S2)) - 1*/
				if(move[i][1] != worstValue){
					move[i][1] = 2 * (- xiS[i] + xiT[i]);
					tempRHSC = -1.0;
					
					double frac;
					
//					if(ratioEvaluation)
						frac = (leftHandSide - move[i][1] + 1E-6) / (set.S.size() + tempRHSC);
//					else
//						frac = (leftHandSide - move[i][1] + 1E-6) - (set.S.size() + tempRHSC);
					
					if(frac > bestFrac && set.S.size() > 1){
			
						bestFrac = frac;
						rightHandSideChange = tempRHSC;
						bestNextMove.set(move[i][1] + rightHandSideChange, i, 0, 1);
					}
				
				}
				
			 	/* 2 : S -> U  :   - x(formulation,S2) + x(formulation,T ) - 1*/
				if(move[i][2] != worstValue){
					move[i][2] = - xiS[i] + xiT[i];
					tempRHSC = -1.0;

					double frac;
					
//					if(ratioEvaluation)
						frac = (leftHandSide - move[i][2] + 1E-6) / (set.S.size() + tempRHSC);
//					else
//						frac = (leftHandSide - move[i][2] + 1E-6) - (set.S.size() + tempRHSC);
					
					if(frac > bestFrac && set.S.size() > 1){
						bestFrac = frac;
						rightHandSideChange = tempRHSC;
						bestNextMove.set(move[i][2] + rightHandSideChange, i, 0, 2);
					}
				}
				
				for(int t = 0 ; t < set.T.size() ; t++){
					
					int t_id = set.T.get(t);
					
					/* 7 : S <-> T : -2(x(t,T2) + x(formulation,S2)) - x(formulation,T2) - x(t,S2) */
					if(exchange[i][t_id] != worstValue){
						double xst = vg.getValue(formulation.edgeVar(i, t_id));
						exchange[i][t_id] = - 2*(xiT[t_id] + xiS[i] - (xiT[i]-xst) - (xiS[t_id]-xst));
						exchange[t_id][i] = exchange[i][t_id];

						double frac;
						
//						if(ratioEvaluation)
							frac = (leftHandSide - exchange[t_id][i] + 1E-6) / (set.S.size());
//						else
//							frac = (leftHandSide - exchange[t_id][i] + 1E-6) - (set.S.size());

						if(frac > bestFrac){
							bestFrac = frac;
							rightHandSideChange = 0.0;
							bestNextExchange.set(exchange[t_id][i], i, t_id, 0);
						}
					}
				}
				
			}
			
			/* If i is in T: Compute transformations 3 and 4 */
			else if(set.inT[i]){
				
				/* 3 : T -> S  : - 2(x(t,T2) - x(t,S )) + 1*/
				if(move[i][0] != worstValue){
					move[i][0] = - 2 * (xiT[i] - xiS[i]);
					tempRHSC = 1.0;

					double frac;
					
//					if(ratioEvaluation)
						frac = (leftHandSide - move[i][0] + 1E-6) / (set.S.size() + tempRHSC);
//					else
//						frac = (leftHandSide - move[i][0] + 1E-6) - (set.S.size() + tempRHSC);
					
					if(frac > bestFrac && set.T.size() - 1 > set.S.size() + 1){
						bestFrac = frac;
						rightHandSideChange = tempRHSC;
						bestNextMove.set(move[i][0] + rightHandSideChange, i, 1, 0);
					}
				}
				
			 	/* 4 : T -> U  :    - x(t,T2) + x(t,S ) */
				if(move[i][2] != worstValue){
					move[i][2] = - xiT[i] + xiS[i];


					double frac;
					
//					if(ratioEvaluation)
						frac = (leftHandSide - move[i][2] + 1E-6) / (set.S.size());
//					else
//						frac = (leftHandSide - move[i][2] + 1E-6) - (set.S.size());
					
					if(frac > bestFrac && set.T.size() - 1 > set.S.size()){
						bestFrac = frac;
						rightHandSideChange = 0.0;
						bestNextMove.set(move[i][2], i, 1, 2);
					}
				}
			}

			/* If i is in U: Compute transformations 5,6,8 and 9*/
			else{
				
				/* 5 : U -> S  :   - x(u,T ) + x(u,S ) + 1*/
				if(move[i][0] != worstValue){
					move[i][0] = - xiT[i] + xiS[i];
					tempRHSC = 1.0;

					double frac;
					
//					if(ratioEvaluation)
						frac = (leftHandSide - move[i][0] + 1E-6) / (set.S.size() + tempRHSC);
//					else
//						frac = (leftHandSide - move[i][0] + 1E-6) - (set.S.size() + tempRHSC);
					
					if(frac > bestFrac && set.S.size() + 1 < set.T.size()){
						bestFrac = frac;
						rightHandSideChange = tempRHSC;
						bestNextMove.set(move[i][0] + rightHandSideChange, i, 2, 0);
					}
				}
				
				/* 6 : U -> T  :   - x(u,S ) + x(u,T ) */
				if(move[i][1] != worstValue){
					move[i][1] = - xiS[i] + xiT[i];

					double frac;
					
//					if(ratioEvaluation)
						frac = (leftHandSide - move[i][1] + 1E-6) / (set.S.size());
//					else
//						frac = (leftHandSide - move[i][1] + 1E-6) - (set.S.size());
					
					if(frac > bestFrac){
						bestFrac = frac;
						rightHandSideChange = 0.0;
						bestNextMove.set(move[i][1], i, 2, 1);
					}
				}
				
				for(int s = 0 ; s < set.S.size() ; ++s){
					int s_id = set.S.get(s);
					double xus = this.vg.getValue(formulation.edgeVar(i, s_id));

					/* 8 : S <-> U :   - x(u,T ) -  x(formulation,S2) + x(formulation,T ) + x(u,S2) */
					if(exchange[i][s_id] != worstValue){
						exchange[i][s_id] = - xiT[i] - xiS[s_id] + xiT[s_id] + (xiS[i]-xus);	
						exchange[s_id][i] = exchange[i][s_id];

						double frac;
						
//						if(ratioEvaluation)
							frac = (leftHandSide - exchange[s_id][i] + 1E-6) / (set.S.size());
//						else
//							frac = (leftHandSide - exchange[s_id][i] + 1E-6) - (set.S.size());
						
						if(frac > bestFrac){
							bestFrac = frac;
							rightHandSideChange = 0.0;
							bestNextExchange.set(exchange[s_id][i], s_id, i, 1);
						}
					}
				}
				
				for(int t = 0 ; t < set.T.size() ; ++t){
					int t_id = set.T.get(t);
					double xut = vg.getValue(formulation.edgeVar(i, t_id));

					/* 9 : T <-> U :   - x(u,S ) - x(t,T2) + x(t,S ) + x(u,T2) */
					if(exchange[i][t_id] != worstValue){
						exchange[i][t_id] = - xiS[i] - xiT[t_id] + xiS[t_id] + (xiT[i]-xut);
						exchange[t_id][i] = exchange[i][t_id];

						double frac;
						
//						if(ratioEvaluation)
							frac = (leftHandSide - exchange[t_id][i] + 1E-6) / (set.S.size());
//						else
//							frac = (leftHandSide - exchange[t_id][i] + 1E-6) - (set.S.size());

						if(frac > bestFrac){
							bestFrac = frac;
							rightHandSideChange = 0.0;
							bestNextExchange.set(exchange[t_id][i], t_id, i, 2);
						}
					}
					
				}
				
			}
			
		}
		
		if(bestNextExchange.slack < bestNextMove.slack)
			bestNextTransformation = bestNextExchange;
		else
			bestNextTransformation = bestNextMove;
	
		
	}

	@Override
	public void updateSubSlacks(Transformation tr) throws IloException {
		/*
		 * idS Id of the node that left the set S during the transformation (-1 if there is none).
		 * idT Id of the node that left the set T during the transformation (-1 if there is none).
		 * idU Id of the node that left the set U during the transformation (-1 if there is none).
		 */
		int idS = -1;
		int idT = -1;
		int idU = -1;
		
		/* If the transformation is a move */
		if(tr instanceof Move){
			
			Move m = (Move)tr;
			
			switch(m.from){
			case 0: idS = m.id; break;
			case 1: idT = m.id; break;
			case 2: idU = m.id; break;
			}
						
		}
		
		/* If the transformation is an exchange */
		else{
			
			Exchange e = (Exchange)tr;
			
			switch(e.type){
			case 0: idS = e.id[0]; idT = e.id[1]; break;
			case 1: idS = e.id[0]; idU = e.id[1]; break;
			case 2: idT = e.id[0]; idU = e.id[1]; break;
			}
		}
		
		STInequality set = currentSet();
		
		/* Update xiS and xiT
		 * and set the slack of the transformations which are now impossible to worstValue */
		
		/* If a node was removed from S */
		if(idS != -1){
			
			for(int i = 0 ; i < formulation.n() ; ++i)
				if(i != idS)
					xiS[i] -= vg.getValue(formulation.edgeVar(i, idS));
			
			/* If idS was added in T */
			if(set.inT[idS]){
				for(int i = 0 ; i < formulation.n() ; ++i)
					if(i != idS)
						xiT[i] += vg.getValue(formulation.edgeVar(i, idS));
				
				for(int t = 0 ; t < set.T.size() ; ++t){
					exchange[set.T.get(t)][idS] = worstValue;
					exchange[set.T.get(t)][idS] = worstValue;
				}
			}
			
			/* If idS was added in U */
			else{
				for(int u = 0 ; u < formulation.n() ; ++u){
					
					/* If u is in U */
					if(!set.inS[u] && !set.inT[u]){
						exchange[idS][u] = worstValue;
						exchange[u][idS] = worstValue;
					}
				}
			}
			
		}

		/* If a node was removed from T */
		if(idT != -1){
			
			for(int i = 0 ; i < formulation.n() ; ++i)
				if(i != idT)
					xiT[i] -= vg.getValue(formulation.edgeVar(i, idT));

			/* If idT was added in S */
			if(set.inS[idT]){
				for(int i = 0 ; i < formulation.n() ; ++i)
					if(i != idT)
						xiS[i] += vg.getValue(formulation.edgeVar(i, idT));

				for(int s = 0 ; s < set.S.size() ; ++s){
					exchange[set.S.get(s)][idT] = worstValue;
					exchange[set.S.get(s)][idT] = worstValue;
				}
			}

			/* If idT was added in U */
			else{
				for(int u = 0 ; u < formulation.n() ; ++u){
					
					/* If u is in U */
					if(!set.inS[u] && !set.inT[u]){
						exchange[idT][u] = worstValue;
						exchange[u][idT] = worstValue;
					}
				}
			}
			
		}

		/* If a node was removed from U */
		if(idU != -1){
			
			if(set.inS[idU]){
				for(int i = 0 ; i < formulation.n() ; ++i)
					if(i != idU)
						xiS[i] += vg.getValue(formulation.edgeVar(i, idU));

				for(int s = 0 ; s < set.S.size() ; ++s){
					exchange[set.S.get(s)][idU] = worstValue;
					exchange[set.S.get(s)][idU] = worstValue;
				}
			}
			
			if(set.inT[idU]){
				for(int i = 0 ; i < formulation.n() ; ++i)
					if(i != idU)
						xiT[i] += vg.getValue(formulation.edgeVar(i, idU));

				for(int t = 0 ; t < set.T.size() ; ++t){
					exchange[set.T.get(t)][idU] = worstValue;
					exchange[set.T.get(t)][idU] = worstValue;
				}
			}
			
		}
	}
		
	public class Exchange extends Transformation{

		/** Id of the nodes concerned by the transformation:
		 * 	The ids are in [0;n[
		 */
		int[] id = new int[2];
		
		/**
		 * Type of exchange:
		 * 	- 0: S <-> T (id[0] in S and id[1] in T)
		 * 	- 1: S <-> U (id[0] in S and id[1] in U)
		 * 	- 2: T <-> U (id[0] in T and id[1] in U)
		 */
		int type;
		
		public Exchange(){
			super();
			id[0] = -1;
			id[1] = -1;
			type = -1;
		}
		
		/**
		 * Set the best transformation
		 * @param Slack of this transformation
		 * @param id1 Id of the first node
		 * @param id2 Id of the second node 
		 */
		public void set(double slack, int id1, int id2, int type){
			this.slack = slack;
			id[0] = id1;
			id[1] = id2;
			this.type = type;
		}
		
		@Override
		public Exchange clone() {
			Exchange theClone = new Exchange();
			
			theClone.id = id.clone();
			theClone.type = this.type;
			
			return theClone;
		}
		
	}
	
	public class Move extends Transformation{

		/** Id of the node concerned by the transformation:
		 * 	The id is in [0;n[
		 */
		int id;
		
		/**
		 * Set in which the node is
		 * 	- 0: S
		 * 	- 1: T
		 * 	- 2: U
		 */
		int from;
		
		/**
		 * Set in which the node will be after the move
		 */
		int to;
		
		public Move(){
			super();
			id = -1;
			from = -1;
			to = -1;
		}
		
		/**
		 * Set the best transformation
		 * @param Slack of this transformation
		 * @param id Id of the node
		 * @param from Set in which the node is
		 * @param to Set in which the node will be after the move
		 */
		public void set(double slack, int id, int from, int to){
			this.slack = slack;
			this.id = id;
			this.from = from;
			this.to = to;
		}
		
		@Override
		public Move clone() {
			Move theClone = new Move();
			
			theClone.id = id;
			theClone.from = this.from;
			theClone.to = to;
			
			return theClone;
		}
		
	}
	
	public STInequality currentSet(){
		return (STInequality)currentSets;
	}
	
	/**
	 * Remove an element of S (the element is identified by its id in [0,n[)
	 * @param s Element of S to remove
	 */
	public void removeFromS(int s){
		currentSet().S.remove(new Integer(s));
		currentSet().inS[s] = false;
	}
	
	/**
	 * Remove an element of T (the element is identified by its id in [0,n[)
	 * @param t Element of T to remove
	 */
	public void removeFromT(int t){
		currentSet().T.remove(new Integer(t));
		currentSet().inT[t] = false;
	}
	
	/**
	 * Add an element in S (the element is identified by its id in [0,n[)
	 * @param s Element to add in S
	 */
	public void addToS(int s){
		currentSet().S.add(s);
		currentSet().inS[s] = true;
	}
	
	/**
	 * Add an element in T (the element is identified by its id in [0,n[)
	 * @param t Element to add in T
	 */
	public void addToT(int t){
		currentSet().T.add(t);
		currentSet().inT[t] = true;
	}

}
