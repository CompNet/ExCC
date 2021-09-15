package separation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

import formulation.interfaces.IFEdgeV;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import inequality_family.AbstractInequality;
import inequality_family.Triangle_Inequality;
import variable.VariableGetter;



/**
 * Separate the triangle inequalities (x_s,t1 + x_s,t2 - x_t1,t2 <= 1).
 * At each iteration a maximum of MAXCUT inequalities are added.
 * The triangle inequalities are tested until 5*MAXCUT violated inequalities are found.
 *  Then the MAXCUT most violated inequalities (inequalities with the larger gap)
 *   among the 5*MAXCUT are added.
 * An inequality is violated if x_s,t1 + x_s,t2 - x_t1,t2 > 1.
 * The gap is equal to: x_s,t1 + x_s,t2 - x_t1,t2 - 1.
 * 
 * @author zach
 *
 */
public class SeparationTriangle extends AbstractSeparation<IFEdgeV>{

	int MAXCUT;
	int MAXFOUND;

	boolean[][] inequalityAdded;

	public TreeSet<GapTriangleInequality> foundIneq = new TreeSet<GapTriangleInequality>(
			new Comparator<GapTriangleInequality>(){

				@Override
				public int compare(GapTriangleInequality o1, GapTriangleInequality o2) {
					int value = o2.gap - o1.gap;
					if(value == 0)
						value = 1;
					return value;
				}
			}
	);

	public SeparationTriangle(IFEdgeV formulation, VariableGetter vg, int MAXCUT) {
		super("triangle iterative", formulation, vg);
		
		this.MAXCUT = MAXCUT;
		MAXFOUND = 5*MAXCUT;

	}

	@Override
	public ArrayList<AbstractInequality<? extends IFormulation>> separate() {

		ArrayList<AbstractInequality<? extends IFormulation>> result = new ArrayList<>();

		foundIneq.clear();	
		int i = 0;


		Random random = new Random();

		int[] shuffle = new int[formulation.n()];
		for(int k = 0 ; k < shuffle.length ; ++k)
			shuffle[k] = k;

		for(int k = 0 ; k < shuffle.length ; ++k){

			int v = random.nextInt(shuffle.length);
			int v1 = shuffle[k];
			shuffle[k] = shuffle[v];
			shuffle[v] = v1;

		}



		while(i < formulation.n() && foundIneq.size() < MAXFOUND){
			int j = i+1;

			while(j < formulation.n()){

				int k = j+1;

				while(k < formulation.n() && foundIneq.size() < MAXFOUND){

					try {
						addIfGapNegative(shuffle[i],shuffle[j],shuffle[k]);
					} catch (IloException e) {
						e.printStackTrace();
					}
					try{
						addIfGapNegative(shuffle[j],shuffle[i],shuffle[k]);
					} catch (IloException e) {
						e.printStackTrace();
					}
					try{
						addIfGapNegative(shuffle[k],shuffle[i],shuffle[j]);
					} catch (IloException e) {
						e.printStackTrace();
					}

					++k;
				}

				++j;
			}

			++i;
		}

		Iterator<GapTriangleInequality> it = foundIneq.iterator();
		int nb = 0;

		while(it.hasNext() && nb < MAXCUT){
			result.add(it.next());
			nb++;
		}

		return result;
	}

	private void addIfGapNegative(int s, int t1, int t2) throws IloException {
		GapTriangleInequality ineq = new GapTriangleInequality(s, t1, t2);

		if(ineq.gap < -eps){
			foundIneq.add(ineq);
		}
	}

	/**
	 * Represent a triangle inequality (x_s,t1 + x_s,t2 - x_t1,t2 <= 1)
	 * @author zach
	 *
	 */
	@SuppressWarnings("serial")
	public class GapTriangleInequality extends Triangle_Inequality{
		
		/** Gap between the value of the inequality and it's upper bound (1) */
		public int gap;

		public GapTriangleInequality(int s, int t1, int t2) throws IloException{
			super(SeparationTriangle.this.formulation, s, t1, t2);
			gap = (int) (getSlack(vg) * 1000);
		}

	}

}
