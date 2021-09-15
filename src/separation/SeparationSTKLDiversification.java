package separation;
import java.util.ArrayList;
import java.util.Random;

import formulation.interfaces.IFEdgeV;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import inequality_family.AbstractInequality;
import inequality_family.STInequality;
import variable.VariableGetter;

public class SeparationSTKLDiversification extends SeparationSTKL{

	public SeparationSTKLDiversification(IFEdgeV formulation, VariableGetter vg,
			int iterations, boolean stopIteratingAfterCutFound) {
		super(formulation, vg, iterations, stopIteratingAfterCutFound);
	}
	
	/**
	 * Current node which is forced to be in S
	 */
	public int cn1;
	
	/**
	 * Current node which is forced to be in T
	 */
	public int cn2;
	
	public ArrayList<AbstractInequality<? extends IFormulation>> separate() {
				
		ArrayList<AbstractInequality<? extends IFormulation>> r = new ArrayList<>();
		
		for(cn1 = 0 ; cn1 < formulation.n() ; ++cn1)
			for(cn2 = cn1+1 ; cn2 < formulation.n() ; ++cn2){
//System.out.println("cn1,cn2: " + cn1 + "," + cn2);				
				r.addAll(super.separate());
				
			}

		return r;
	}
	
	@Override
	public void initializeSets(){

		currentSets = new STInequality(formulation);
		STInequality set = (STInequality)currentSets;
		
		Random random = new Random();
		
		/* Create S and T randomly */
		for(int i = 0 ; i < formulation.n() ; ++i){
			
			switch(random.nextInt(3)){
				case 0: 
					addToS(i);
					set.inT[i] = false;
					break;
				case 1: 
					addToT(i);
					set.inS[i] = false;
					break;
				case 2:
					set.inS[i] = false;
					set.inT[i] = false;
					break;
			}
		}
		
		/* Ensure that cn1 is in S */
		if(!set.inS[cn1]){
			
			if(set.inT[cn1])
				removeFromT(cn1);
			
			addToS(cn1);
		}
		
		/* Ensure that cn2 is in T */
		if(!set.inT[cn2]){
			
			if(set.inS[cn2])
				removeFromS(cn2);
			
			addToT(cn2);
		}
		
		/* Ensure that |S|<|T| */
		if(set.S.size() >= set.T.size()){
			
			if(set.S.size() == set.T.size()){

				/* If |S| = |T| */
				
				/* Add an element of S in T */
				int s_id = random.nextInt(set.S.size());
				int s = set.S.get(s_id);
				
				/* If s is cn1 take the next in S */
				if(s == cn1){
					s_id = (s_id + 1)%set.S.size();
					s = set.S.get(s_id);
				}
				
				set.S.remove(s_id);
				set.inS[s] = false;
				addToT(s);
					
			}
			
			/* If |S| > |T| */
			else{
 				
				/* Exchange S and T */
				ArrayList<Integer> al_temp = set.S;
				set.S = set.T;
				set.T = al_temp;
				
				boolean[] temp = set.inS;
				set.inS = set.inT;
				set.inT = temp;
				
				/* Move cn1 to S */
				removeFromT(cn1);					
				addToS(cn1);
				
				/* Move cn2 to T */
				removeFromS(cn2);
				addToT(cn2);
				
			}
		}		
		
	}
	
	@Override
	public void initializeSubSlacks() throws IloException {
		super.initializeSubSlacks();

		STInequality cs = new STInequality(formulation);
		
		/* Prevent cn1 and cn2 from moving to another set */
		move[cn1][0] = -Double.MAX_VALUE;
		move[cn1][1] = -Double.MAX_VALUE;
		move[cn1][2] = -Double.MAX_VALUE;
		
		move[cn2][0] = -Double.MAX_VALUE;
		move[cn2][1] = -Double.MAX_VALUE;
		move[cn2][2] = -Double.MAX_VALUE;
		
		/* Prevent i to be exchanged with cn1 and cn2
		 *  (only allow to exchange cn1 and cn2 together) */
		for(int i = 0 ; i< formulation.n() ; ++i){
			
			if(i != cn1 
					|| (cs.inS[i] && cs.inT[cn2])
					|| (cs.inT[i] && cs.inS[cn2])){
				exchange[i][cn2] = -Double.MAX_VALUE;
				exchange[cn2][i] = -Double.MAX_VALUE;
			}
			
			if(i != cn2 
					|| (cs.inS[i] && cs.inT[cn1])
					|| (cs.inT[i] && cs.inS[cn1])){
				exchange[i][cn1] = -Double.MAX_VALUE;
				exchange[cn1][i] = -Double.MAX_VALUE;
			}
			
		}
		
	}

}
