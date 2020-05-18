package inequality_family;

import ilog.concert.IloLinearNumExpr;

import java.io.Serializable;

public class  Range implements Serializable{

	private static final long serialVersionUID = 549489333391755348L;
	public Double lbound = -Double.MAX_VALUE;
	public Double ubound = Double.MAX_VALUE;
	
	public IloLinearNumExpr expr;
	
	public Range(double lbound, IloLinearNumExpr expr){
		this.lbound = lbound;
		this.expr = expr;
	}
	
	public Range(IloLinearNumExpr expr, double ubound){
		this.expr = expr;
		this.ubound = ubound;
	}
	
	public Range(double lbound, IloLinearNumExpr expr, double ubound){
		this.lbound = lbound;
		this.expr = expr;
		this.ubound = ubound;
	}
	
}