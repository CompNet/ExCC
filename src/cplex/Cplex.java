package cplex;

import callback.cut_callback.CallbackRootRelaxation;
import callback.presolve_callback.CallBackPresolveInfo;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BooleanParam;
import ilog.cplex.IloCplex.ControlCallback;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.ParameterSet;
import ilog.cplex.IloCplex.UnknownObjectException;
import inequality_family.Range;

public class Cplex {

	public IloCplex iloCplex;

	public boolean isSolved = false;

	public final double  PRECISION = 0.0000001;
	
	public Cplex() {
		start();
	}

	public void removeAutomaticCuts(){
		try {
			iloCplex.setParam(IloCplex.DoubleParam.CutsFactor, 0.0);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public void turnOffCPOutput(){
		/* Turn off cplex output */
		iloCplex.setOut(null);
		iloCplex.setWarning(null);
//		System.out.println("Warning: Cplex.java decomment turnOffCPOutput()");

	}

	public void onlyDisplayRootRelaxation() {

		//	cplex.setParam(IloCplex.IntParam.AggCutLim, -1);
		CallbackRootRelaxation rootCB = new CallbackRootRelaxation(true);

		removeAutomaticCuts();
		turnOffCPOutput();

		try {
			iloCplex.use(rootCB);
		} catch (IloException e) {
			e.printStackTrace();
		}

	}


	public void displayPresolveInfo() {

		//cplex.setParam(IloCplex.IntParam.AggCutLim, -1);
		CallBackPresolveInfo presolveCB = new CallBackPresolveInfo();
		try {
			iloCplex.use(presolveCB);
		} catch (IloException e) {
			e.printStackTrace();
		}

	}

	public void displayObjectiveFunction(){
		try {
			if(isSolved)
				System.out.println("Cplex found a result of: " + iloCplex.getObjValue());
		} catch (IloException e) {
			e.printStackTrace();
		}	
	}

	public void end(){
		iloCplex.end();
	}

	public void turnOffPrimalDualReduction() {
		try {
			iloCplex.setParam(IloCplex.IntParam.Reduce, 0);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}


	public double solve() throws IloException{
		double time = -iloCplex.getCplexTime();		
		iloCplex.solve();
		return time + iloCplex.getCplexTime();
	}

	public void clearCallback(){
		try {
			iloCplex.clearCallbacks();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public void use(ControlCallback ucc){
		try {
			iloCplex.use(ucc);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public void start(){
		try {
			iloCplex = new IloCplex();
			iloCplex.setParam(IntParam.ClockType, 2);

			/* Min gap under which cplex consider that the optimal solution is found */
			//			cplex.setParam(DoubleParam.EpGap, 1E-13);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public double getNnodes() {
		return iloCplex.getNnodes();
	}

	public double getObjValue() throws IloException {
		return iloCplex.getObjValue();
	}

	public double getBestObjValue() throws IloException {
		return iloCplex.getBestObjValue();
	}

	public int getNcuts(int cuttype) {
		try {
			return iloCplex.getNcuts(cuttype);
		} catch (IloException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public double getCplexTime() {
		try {
			return iloCplex.getCplexTime();
		} catch (IloException e) {
			e.printStackTrace();
			return -1.0;
		}
	}

	public IloRange le(IloLinearNumExpr expr, double ubound) throws IloException {
		return iloCplex.le(expr, ubound);
	}

	public IloRange ge(IloLinearNumExpr expr, double lbound) throws IloException  {
		return iloCplex.ge(expr, lbound);
	}

	public IloRange range(double bound, IloNumExpr expr, double bound2) throws IloException  {
		return iloCplex.range(bound, expr, bound2);
	}

	public IloRange addRange(double lbound, IloLinearNumExpr expr, double ubound) throws IloException{
		return iloCplex.addRange(lbound, expr, ubound);
	}


	public IloLinearNumExpr linearNumExpr(){

		try {
			return iloCplex.linearNumExpr();
		} catch (IloException e) {
			e.printStackTrace();
			return null;
		}
	}

	public IloRange addRange(Range r) throws IloException{
		return iloCplex.addRange(r.lbound, r.expr, r.ubound);
	}

	//	public IloRange addLazyRange(Range r){
	//		try {
	//			return cplex.addLazyConstraint(cplex.range(r.lbound, r.expr, r.ubound));
	//		} catch (IloException e) {
	//			e.printStackTrace();
	//			return null;
	//		}
	//	}

	public IloRange addLe(IloLinearNumExpr expr, double bound) throws IloException {
		return iloCplex.addLe(expr, bound);
	}

	public IloRange addGe(IloLinearNumExpr expr, double bound) throws IloException {
		return iloCplex.addGe(expr, bound);
	}

	public IloRange eq(double e, IloNumVar iloNumVar) throws IloException  {
		return iloCplex.eq(e,  iloNumVar);
	}

	public IloRange eq(double e, IloLinearNumExpr iloNumExpr) throws IloException  {
		return iloCplex.eq(e,  iloNumExpr);
	}

	public void setParam(DoubleParam p, double value) {
		try {
			iloCplex.setParam(p, value);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public void setParam(BooleanParam p, boolean value) {
		try {
			iloCplex.setParam(p, value);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public void setParam(IntParam p, int value) {
		try {
			iloCplex.setParam(p, value);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public double getParam(DoubleParam p) throws IloException {
		return iloCplex.getParam(p);
	}

	public ParameterSet getParameterSet() {
		try {
			return iloCplex.getParameterSet();
		} catch (IloException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setParameterSet(ParameterSet s) {
		try {
			iloCplex.setParameterSet(s);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	public void setDefaults() {
		try {
			iloCplex.setDefaults();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}



	public double getSlack(IloRange r) throws UnknownObjectException, IloException {
		return iloCplex.getSlack(r);
	}

	public void remove(IloRange r) {
		try {
			iloCplex.remove(r);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addMIPStart(IloNumVar[] var, double[] val) {
		try {
			iloCplex.addMIPStart(var, val);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}	

}