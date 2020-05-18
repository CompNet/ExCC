package separation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import formulation.interfaces.IFEdgeV;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import inequality_family.AbstractInequality;
import inequality_family.STInequality;
import variable.VariableGetter;


public class SeparationSTGrotschell extends AbstractSeparation<IFEdgeV>{

	int MAXCUT;
	int MAXFOUND;

	public SeparationSTGrotschell(IFEdgeV formulation, VariableGetter vg, int MAXCUT) {
		super("ST_Grotschell", formulation, vg);
		
		this.MAXCUT = MAXCUT;
		MAXFOUND = 5*MAXCUT;
	}

	public TreeSet<GapSTInequality> foundIneq = new TreeSet<GapSTInequality>(
			new Comparator<GapSTInequality>(){
				
				@Override
				public int compare(GapSTInequality o1, GapSTInequality o2) {
					int value = o2.gap - o1.gap;
					if(value == 0)
						value = 1;
					return value;
				}
			}
	);

	@Override
	public ArrayList<AbstractInequality<? extends IFormulation>> separate(){
			
		foundIneq.clear();
			boolean heuristic1Over = false;
			int v = 0; 
			
			/* Search 2-partition inequalities such that S={v} (for all possible v) */
			while(foundIneq.size() < MAXFOUND && v < formulation.n()){
		
				/* Find neighbors j of v such that v_rep[v][j] != 0 and 1 */
				ArrayList<Integer> neighborV = new ArrayList<Integer>();
				
				for(int j = 0 ; j < v ; ++j){
					double value;
					try {
						value = vg.getValue(formulation.edgeVar(v,j));
						if(value != 0 && value != 1)
							neighborV.add(j);	
					} catch (IloException e) {
						e.printStackTrace();
					}		
				}
				
				for(int j = v+1 ; j < formulation.n() ; ++j){
					double value;
					try {
						value = vg.getValue(formulation.edgeVar(j,v));
						if(value != 0 && value != 1)
							neighborV.add(j);
					} catch (IloException e) {
						e.printStackTrace();
					}
				}
	
				Collections.shuffle(neighborV);
				
				try{
				if(neighborV.size() > 0){
					
					/* Add the first neighbor in a set T */
					ArrayList<Integer> T = new ArrayList<Integer>();
					T.add(neighborV.get(0));
		
					/* For each neighbor w */
					for(int i = 1 ; i < neighborV.size() ; ++i){
					
						Integer w = neighborV.get(i);
						boolean w_valid = true;
						Iterator<Integer> t = T.iterator();

						/* If we consider the first heuristic */
						if(!heuristic1Over){
							
							/* If all the elements t of T verify  v_edge[w][t] = 0 */
							while(t.hasNext() && w_valid)	
								if(vg.getValue(formulation.edgeVar(w,t.next())) < eps)
									w_valid = false;	
						}
						
						/* If we consider the second heuristic */
						else{
							
							double result = vg.getValue(formulation.edgeVar(w,v));
							
							/* If all the elements t of T verify  v_edge[w][t] = 0 */
							while(t.hasNext() && result > (0 + eps))	
								result -= vg.getValue(formulation.edgeVar(w,t.next()));
							
							if(result < (0 - eps))
								w_valid = false;
//							else
//								System.out.println("Second valid");
							
						}
							
						if(w_valid){
							
							/* Add w in T */
							T.add(w);
						}
						
					}
					
					ArrayList<Integer> S = new ArrayList<Integer>();
					S.add(v);
					
					GapSTInequality ineq = new GapSTInequality(S, T);
					
					/* If the inequality is violated */
					if(ineq.gap < -eps){
						
						foundIneq.add(ineq);
					}
				}
				}catch(IloException e){
					e.printStackTrace();
				}
				
				++v;
				
				/* If this is the end of the first heuristic and no cut has been found,
				 *  start the second */
				if(v == formulation.n() && !heuristic1Over && foundIneq.size() == 0){
					heuristic1Over = true;
					v = 0;
				}
			}
			


			ArrayList<AbstractInequality<? extends IFormulation>> returned = new ArrayList<>();
			
			Iterator<GapSTInequality> it = foundIneq.iterator();
			int nb = 0;
			
			while(it.hasNext() && nb < MAXCUT){
				returned.add(it.next());
				nb++;
			}
			
			return returned;
			
		}


	/**
	 * Represent a triangle inequality (x_s,t1 + x_s,t2 - x_t1,t2 <= 1)
	 * @author zach
	 *
	 */
	@SuppressWarnings("serial")
	public class GapSTInequality extends STInequality{
		
		/** Gap between the value of the inequality and it's upper bound (1) */
		public int gap;
		
		public GapSTInequality(ArrayList<Integer> s, ArrayList<Integer> t) throws IloException{
			super(SeparationSTGrotschell.this.formulation);
			S = s;
			T = t;
			gap = (int) (getSlack(vg) * 1000);
		}
		
	}

}
