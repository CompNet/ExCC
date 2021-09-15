package inequality_family;

import java.util.ArrayList;

import formulation.interfaces.IFEdgeV;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import variable.VariableGetter;

@SuppressWarnings("serial")
public class STInequality extends AbstractInequality<IFEdgeV>{

	public ArrayList<Integer> S = new ArrayList<Integer>();
	public ArrayList<Integer> T = new ArrayList<Integer>();

	public boolean[] inS;
	public boolean[] inT;

	public STInequality(IFEdgeV formulation){
		super(formulation, IFEdgeV.class);
		
		S = new ArrayList<Integer>();
		T = new ArrayList<Integer>();

		inS = new boolean[formulation.n()];
		inT = new boolean[formulation.n()];
	}

	@Override
	public Range createRange() {

		try {

			IloLinearNumExpr expr = formulation.getCplex().linearNumExpr();

			for(int s = 0 ; s < S.size() ; ++s){

				for(int s2 = s+1 ; s2 < S.size() ; ++s2)
					expr.addTerm(-1.0, this.formulation.edgeVar(S.get(s),S.get(s2)));


				for(int t = 0 ; t < T.size() ; ++t)
					expr.addTerm(1.0, this.formulation.edgeVar(S.get(s),T.get(t)));

			}

			for(int t = 0 ; t < T.size() ; ++t)
				for(int t2 = t+1 ; t2 < T.size() ; ++t2)
					expr.addTerm(-1.0, this.formulation.edgeVar(T.get(t),T.get(t2)));

//			System.out.println("s: " + set.S);
//			System.out.println("t: " + set.T);
//			System.out.println("score: " + evaluateSets(set) + "\n");
			return new Range(expr, (double)S.size());
		} catch (IloException e) {
			e.printStackTrace();
			return null;
		}

	}
	@Override
	public STInequality clone() {

		STInequality clone = new STInequality(formulation);

		for(int i = 0 ; i < formulation.n() ; ++i){
			clone.inS[i] = inS[i];
			clone.inT[i] = inT[i];
		}

		for(int i = 0 ; i < S.size() ; ++i)
			clone.S.add(new Integer(S.get(i)));

		for(int i = 0 ; i < T.size() ; ++i)
			clone.T.add(new Integer(T.get(i)));

		return clone;
	}

	@Override
	protected double evaluate(VariableGetter vg) throws IloException  {

		double result = 0.0;

		for(int s = 0 ; s < S.size() ; ++s){

			for(int t = 0 ; t < T.size() ; ++t)
				result += vg.getValue(formulation.edgeVar(S.get(s), T.get(t)));

			for(int s2 = s+1 ; s2 < S.size() ; ++s2)
				result -= vg.getValue(formulation.edgeVar(S.get(s), S.get(s2)));
		}

		for(int t = 0 ; t < T.size() ; ++t)
			for(int t2 = t+1 ; t2 < T.size() ; ++t2)
				result -= vg.getValue(formulation.edgeVar(T.get(t), T.get(t2)));

		return result;
	}

	@Override
	public double getSlack(VariableGetter vg) throws IloException   {
		return S.size() - this.evaluate(vg);
	}

}
