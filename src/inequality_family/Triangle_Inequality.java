package inequality_family;

import formulation.interfaces.IFEdgeV;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import variable.VariableGetter;

@SuppressWarnings("serial")
public class Triangle_Inequality extends AbstractInequality<IFEdgeV> {

	public int s1;
	public int t1, t2;

	public Triangle_Inequality(IFEdgeV formulation, int s1, int t1, int t2) {
		super(formulation, IFEdgeV.class);
		
		this.s1 = s1;
		this.t1 = t1;
		this.t2 = t2;
	}
	
	@Override
	public Range createRange() {

		IloLinearNumExpr expr = formulation.getCplex().linearNumExpr();

		try {
			expr.addTerm(+1.0, formulation.edgeVar(s1, t1));
			expr.addTerm(+1.0, formulation.edgeVar(s1, t2));
			expr.addTerm(-1.0, formulation.edgeVar(t1, t2));

		} catch (IloException e) {
			e.printStackTrace();
		}

		return new Range(expr, 1.0);
	}

	@Override
	public AbstractInequality<IFEdgeV> clone() {

		return new Triangle_Inequality(formulation, s1, t1, t2);
	}

	@Override
	protected double evaluate(VariableGetter vg) throws IloException  {
		double result = vg.getValue(formulation.edgeVar(s1, t1));
		result += vg.getValue(formulation.edgeVar(s1, t2));
		result -= vg.getValue(formulation.edgeVar(t1, t2));

		return result;
	}

	@Override
	public double getSlack(VariableGetter vg) throws IloException   {
		return 1.0 - this.evaluate(vg);
	}

}
