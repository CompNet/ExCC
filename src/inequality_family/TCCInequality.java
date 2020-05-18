package inequality_family;

import java.util.ArrayList;

import formulation.interfaces.IFEdgeV;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import variable.VariableGetter;

@SuppressWarnings("serial")
public class TCCInequality extends AbstractInequality<IFEdgeV>{

	public ArrayList<Integer> C = new ArrayList<Integer>();
	public int[] inC;
	public int p;

	public TCCInequality(IFEdgeV formulation, int size) {
		super(formulation, IFEdgeV.class);
		
		inC = new int[formulation.n()];
		C = new ArrayList<Integer>();
		p = (size-1)/2;
	}
	
	@Override
	public Range createRange() {

		Range result = null;


		try {
			IloLinearNumExpr expr = formulation.getCplex().linearNumExpr();
//			System.out.println(C.size());
//			System.out.println(C);
			for(int c = 0 ; c < C.size(); ++c){
				expr.addTerm(+1.0, formulation.edgeVar(C.get(c),C.get((c+1)%C.size())));
				expr.addTerm(-1.0, formulation.edgeVar(C.get(c),C.get((c+2)%C.size())));
			}

			result = new Range(expr, p);

//			if(2*p + 1 != C.size()){
//				System.out.println("Taille C : " + C.size());
//				System.out.println(expr.toString() + " <= " + p);			
//				System.exit(0);
//			}
		} catch (IloException e) {
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public AbstractInequality<IFEdgeV> clone() {

		TCCInequality clone = new TCCInequality(formulation, C.size());

		for(int i = 0 ; i < inC.length ; ++i)
			clone.inC[i] = inC[i];

		for(int i = 0 ; i < C.size() ; ++i)
			clone.C.add(new Integer(C.get(i)));

		return clone;
	}

	@Override
	protected double evaluate(VariableGetter vg) throws IloException  {

		double result = 0.0;

		for(int i = 0 ; i < C.size() ; ++i){

			result += vg.getValue(formulation.edgeVar(C.get((i+1)%C.size()),C.get(i)));
			result -= vg.getValue(formulation.edgeVar(C.get((i+2)%C.size()),C.get(i)));

		}

		return result;
	}

	@Override
	public double getSlack(VariableGetter vg) throws IloException {
		return p - this.evaluate(vg);
	}

}
