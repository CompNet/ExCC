package formulation;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import callback.lazy_callback.LazyCBTriangle;
import formulation.MyParam.Triangle;
import formulation.interfaces.IFEdgeVEdgeW;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;
// import ilog.cplex.IloCplex.DoubleParam;
// import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;
// import inequality_family.Range;
import inequality_family.Triangle_Inequality;
import variable.VariableLister;
import variable.VariableLister.VariableListerException;


/**
 * Program which use cplex to solve a k-partition problem of n nodes (0, 1, ...,
 * n-1)
 * 
 * 
 * The 0-1 variables are:
 * 
 * - xi,j = 1 if the edge (i,j) is in the partition; 0 otherwise
 * 
 * - xi = 1 if i the representative of its cluster (i.e. the point with lower
 * index); 0 otherwise
 * 
 * 
 * 
 * Three families of constraints are considered:
 * 
 * - triangular inequalities (if i is with j and k, j and k are together)
 * 
 * - upper representative (no more than 1 representative by cluster)
 * 
 * - lower representative (at least one representative by cluster)
 * 
 * @author zach
 * 
 */
public class MyPartition extends Partition implements IFEdgeVEdgeW{


	/**
	 * Representative variables Array of n-3 elements. v_rep[i] contains the
	 * value of xi+3 (1=0..n-4)
	 */
	public IloNumVar[] v_rep;

	public MyPartition(MyParam rp) throws IloException, VariableListerException{
		this(readDissimilarityInputFile(rp), rp);
	}

	public static boolean test = true;
	
	public MyPartition(double objectif[][], MyParam rp) throws IloException, VariableListerException {
		super(rp);

		this.d = objectif;
		this.n = d.length;

		if(rp instanceof MyParam)
			this.p = new MyParam(rp);
			

		
		if(rp.getStatusReadLPModelFromFile() && rp.cplex.iloCplex.getNintVars()>0){ // if there are already variables, this means that we create an object from lp file
			System.out.println("!!!!!!!!!!!!!!!!! LP read model");
			
			IloNumVar[] vars = VariableLister.parse(getCplex().iloCplex);
			v_edge = new IloNumVar[n][];
			for (int b = 0 ; b < n; ++b)
				v_edge[b] = new IloNumVar[n];
			
			for(int a=0; a<vars.length; a++){
				IloNumVar var = vars[a];
				String[] parts = var.getName().split("_");
				int i = Integer.parseInt(parts[1]);
				int j = Integer.parseInt(parts[2]);
				v_edge[i][j] = var;
				v_edge[j][i] = var;
			}
			
			System.out.println("rp.tilim: " + rp.tilim);
			
			if(rp.tilim != -1)
				getCplex().setParam(IloCplex.DoubleParam.TiLim, Math.max(10,rp.tilim));

//			getCplex().setParam(DoubleParam.WorkMem, 256);
//			getCplex().setParam(DoubleParam.TreLim, 4000);
//			getCplex().setParam(IntParam.NodeFileInd, 3);
			
//			if(p.isInt == true)
//				getCplex().setParam(IloCplex.Param.Threads, 1);

//			getCplex().setParam(IloCplex.Param.Threads, 1);
			getCplex().setParam(IloCplex.Param.Threads, getCplex().iloCplex.getNumCores()-1);
			
			
		} else {
			System.out.println("!!!!!!!!!!!!!!!!! else");
			
			if(!rp.cplexOutput)
				getCplex().turnOffCPOutput();

			if(!rp.useCplexAutoCuts)
				getCplex().removeAutomaticCuts();

			if(!rp.useCplexPrimalDual)
				getCplex().turnOffPrimalDualReduction();

		
			try {
	
				/* Create the model */
				getCplex().iloCplex.clearModel();
				getCplex().iloCplex.clearCallbacks();
	
				/* Reinitialize the parameters to their default value */
				getCplex().setDefaults();
				
				System.out.println("rp.tilim: " + rp.tilim);
	
				if(rp.tilim != -1)
					getCplex().setParam(IloCplex.DoubleParam.TiLim, Math.max(10,rp.tilim));
	
	//			getCplex().setParam(DoubleParam.WorkMem, 256);
	//			getCplex().setParam(DoubleParam.TreLim, 4000);
	//			getCplex().setParam(IntParam.NodeFileInd, 3);
				
	//			if(p.isInt == true)
	//				getCplex().setParam(IloCplex.Param.Threads, 1);
	
	//			getCplex().setParam(IloCplex.Param.Threads, 1);
				getCplex().setParam(IloCplex.Param.Threads, getCplex().iloCplex.getNumCores()-1);
				
				
				if(p.isInt == true) {
					// source: https://www.ibm.com/developerworks/community/forums/html/topic?id=813d0940-702e-45ad-ba85-bf3cfb994a9b
					//		=>   If you know that info is not needed and you will always make the branching decision, then you should set
					//				the variable selection parameter to the computationally simplest setting, probably minimum or maximum integrality violation
	//				getCplex().setParam(IloCplex.Param.MIP.Strategy.VariableSelect, 1); // Branch on variable with maximum infeasibility
					
					
					
					// https://www.ibm.com/support/knowledgecenter/SSSA5P_12.7.1/ilog.odms.cplex.help/CPLEX/Parameters/topics/PreDual.html
	//				getCplex().iloCplex.setParam(IloCplex.Param.Preprocessing.Dual, 1); // this is a useful technique for problems with more constraints than variables.
					//getCplex().iloCplex.setParam(IloCplex.Param.MIP.Strategy.KappaStats, 1);
	
	
	
					// https://perso.ensta-paris.fr/~diam/ro/online/cplex/cplex1271/CPLEX/Parameters/topics/VarSel.html
					//getCplex().setParam(IloCplex.Param.MIP.Strategy.VariableSelect, 3); // strong branching
					////getCplex().setParam(IloCplex.Param.MIP.Strategy.VariableSelect, 4); // reduced pseudo cost branching for a bit faster process
					////getCplex().iloCplex.setParam(IloCplex.Param.Emphasis.MIP, 1); // to reduce the time spent at root node
	//				getCplex().iloCplex.setParam(IloCplex.Param.MIP.Display, 5); // max info in log file
	
	                //getCplex().iloCplex.setParam(IloCplex.Param.MIP.Strategy.RINSHeur, 25);
	//               getCplex().iloCplex.setParam(IloCplex.Param.MIP.Strategy.Probe, 3);
	                //getCplex().iloCplex.setParam(IloCplex.Param.Preprocessing.Symmetry, 4);
	                //// https://www.ibm.com/support/knowledgecenter/SSSA5P_12.7.1/ilog.odms.cplex.help/CPLEX/Parameters/topics/Symmetry.html
	                ////getCplex().iloCplex.setParam(IloCplex.Param.MIP.Strategy.Branch, -1); // -1 down, 1 up
	                ////getCplex().iloCplex.setParam(IloCplex.Param.MIP.PolishAfter.Time, 90); // https://www.ibm.com/developerworks/community/forums/html/topic?id=989e4d95-9f51-45d3-b2e6-bac23d5a9387
	                //getCplex().iloCplex.setParam(IloCplex.Param.MIP.Cuts.Disjunctive, 2);
				}
				
				/* Create the variables */
				createVariables();
				createObjectiveFunction();
				createConstraints(rp.triangle);

//			    ArrayList<int[]> clusterings = new ArrayList<>();
//			    for(int lowerBound : rp.clusteringsByLowerBoundMap.keySet()){
//				    for(int[] clustering : rp.clusteringsByLowerBoundMap.get(lowerBound)){
//					    createDistinctSolutionConstraint(clustering, lowerBound);
//					    clusterings.add(clustering);
//				    }
//			    }
//
//			    Clustering c = new Clustering(clusterings.get(0), -1); // we just need 1 optimal clustering to compute optimal objective value of CC
//			    c.computeImbalance(d);
//			    double optimalObjectiveValue = c.getImbalance();
//			    createOptimalityConstraint(optimalObjectiveValue);
				
				//createOptimalityConstraint(618);
	
				//Turn off preprocessing
	//			cplex.setParam(IloCplex.BooleanParam.PreInd, false);
	
			} catch (IloException e) {
				System.err.println("Concert exception caught: " + e);
				e.printStackTrace();
				System.exit(0);
			}
		
		
		}
	}
	
	
	public Set<Edge> getEdges() {
		Set<Edge> edges = new HashSet<>();
		
		for(int i=1; i<n; i++){
			for(int j=0; j<i; j++){
				Edge e = new Edge(i, j);
				e.setWeight(d[i][j]);
				edges.add(e);
			}
		}
		
		return(edges);
	}
	
	
	public int[] retreiveEdgeVariables()
			throws UnknownObjectException, IloException {
		int[] edgeVars = new int[this.n()*(this.n()-1)/2];

		/* For each edge */
		int k=0;
		for(Edge e : this.getEdges()) {
			double value = cvg.getValue(this.edgeVar(e.getSource(), e.getDest()));
			if(value > 1E-4)
				edgeVars[k++] = 1;
			else
				edgeVars[k++] = 0;
		}
			
		return(edgeVars);
	}

	

	public void createConstraints(Triangle triangle) throws IloException{

		/*
		 * Add triangular constraints : xi,j + xi,k - xj,k <= 1
		 * 
		 * - if i is with j and k, then j and k are together
		 */
		if(triangle == Triangle.USE
				|| (triangle == Triangle.USE_IN_BC_ONLY && p.isInt == true)){
			createTriangleConstraints();
//			System.out.println("\n!!Add triangle constraints to the model");
			
			// TODO: DENEME
//			int cost = 61;
//			createCostConstraint(cost);
		}
		else if(triangle == Triangle.USE_LAZY
				|| (triangle == Triangle.USE_LAZY_IN_BC_ONLY && p.isInt == true)){
			System.out.println("\n!!Add lazy CB in BC");
			getCplex().use(new LazyCBTriangle(this, 500));
		}
		else {
			System.out.println("\n!!Don't add triangle or lazy callback for triangles");
		}
		
		
//		IloNumVar var = this.edgeVar(0,11);
//		IloLinearNumExpr expr = this.getCplex().linearNumExpr();
//		expr.addTerm(-1.0, var);
//		this.getCplex().addLe(expr, -1.0); // force to be 1
//
//		var = this.edgeVar(62,66);
//		expr = this.getCplex().linearNumExpr();
//		expr.addTerm(-1.0, var);
//		this.getCplex().addLe(expr, -1.0); // force to be 1


//        String targets = "0,2;0,4;0,14;0,17;0,24";
//        String[] parts = targets.split(";");        
//
//        for(int a=0; a<parts.length; a++){
//            int i = Integer.parseInt(parts[a].split(",")[0]);
//            int j = Integer.parseInt(parts[a].split(",")[1]);
//
//            var = this.edgeVar(i,j);
//		    expr = this.getCplex().linearNumExpr();
//		    expr.addTerm(1.0, var);
//		    this.getCplex().addLe(expr, 0.0); // force to be 0
//        }

	}



	

	public void displaySolution(){

//		if(isSolved)

		try {
			int l = 6;

			/* Display the edge variables different from 0 (<l> by line) */
			System.out.println(" ");
			System.out.println("Edges variables");
			displayEdgeVariables(l);

		} catch (UnknownObjectException e) {
			e.printStackTrace();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	
	
	
	
	/**
	 *
	 *
	 */
	void createDistinctSolutionConstraint(int[] prevEdgeVars, double lowerBound) throws IloException {
		// https://www.ibm.com/support/pages/using-cplex-examine-alternate-optimal-solutions
		IloLinearNumExpr expr = getCplex().linearNumExpr();
		
		int cardinalitySameCluster=0;
		int k=0;
		for(Edge e : this.getEdges()) {
			if(prevEdgeVars[k] == 1){ // same cluster, so x*(i,j)=1
				cardinalitySameCluster++;
				expr.addTerm(+1.0, edgeVar(e.getSource(), e.getDest()));
			}
			else {
				expr.addTerm(-1.0, edgeVar(e.getSource(), e.getDest()));
			}
			k++;
		}
		
		getCplex().addLe(expr, cardinalitySameCluster-lowerBound);
	}




/**
	 * Create the objective function
	 * 
	 * x_ij=1 means nodes i and j are ina common set
	 * 
	 * The objective function is:
	 * 	 min (  sum_{ (i,j) \in E^- } w_ij x_ij + sum_{ (i,j) \in E^+ } w_ij (1- x_ij)  )
	 * 
	 * But here, we exclude the constant term which is:
	 *  sum_{ (i,j) \in E^+ } w_ij.
	 * Because we will add it at the end
	 * So the objective function becomes: 
	 * 	min - sum_{ (i,j) \in E^+ } w_ij x_ij + sum_{ (i,j) \in E^- } w_ij x_ij
	 * Actually, they are equivalent
	 *
	 * @throws IloException
	 */
	void createObjectiveFunction() throws IloException {

		IloLinearNumExpr obj = getCplex().linearNumExpr();

		double sum = 0.0;
		for (int i = 1; i < n; ++i)
			for (int j = 0; j < i; ++j)
				if(d[i][j] > 0.0d){ // process positive edges
					obj.addTerm(-d[i][j], v_edge[i][j]);
					sum += d[i][j]; // constant term
				}
				else if(d[i][j] < 0.0d) // process negative edges
					obj.addTerm(Math.abs(d[i][j]), v_edge[i][j]);

//		cplex.addMinimize(obj);
		getCplex().iloCplex.addMinimize(getCplex().iloCplex.sum(obj, sum));

	}
	
	
	
	/**
	 * Create the optimality consraint
	 * 
	 * x_ij=1 means nodes i and j are in a common set
	 * 
	 * The objective function is:
	 * 	 min (  sum_{ (i,j) \in E^- } w_ij x_ij + sum_{ (i,j) \in E^+ } w_ij (1- x_ij)  )
	 * 
	 * But here, we exclude the constant term which is:
	 *  sum_{ (i,j) \in E^+ } w_ij.
	 * Because we will add it at the end
	 * So the objective function becomes: 
	 * 	min - sum_{ (i,j) \in E^+ } w_ij x_ij + sum_{ (i,j) \in E^- } w_ij x_ij
	 * Actually, they are equivalent
	 *
	 * @throws IloException
	 */
	public void createOptimalityConstraint(double upperBound) throws IloException {

		IloLinearNumExpr expr = getCplex().linearNumExpr();

		double sum = 0.0;
		for (int i = 1; i < n; ++i)
			for (int j = 0; j < i; ++j)
				if(d[i][j] > 0.0d){ // process positive edges
					expr.addTerm(-d[i][j], v_edge[i][j]);
					sum += d[i][j]; // constant term
				}
				else if(d[i][j] < 0.0d) // process negative edges
					expr.addTerm(Math.abs(d[i][j]), v_edge[i][j]);


		getCplex().addLe(expr, upperBound-sum);
	}



//	/**
//	 * 
//	 * @param solution 
//	 * 
//	 * @throws IloException
//	 */
//	void createCostConstraint(int cost) throws IloException {
//		// tutorial: https://www.ibm.com/support/knowledgecenter/en/SSSA5P_12.6.0/ilog.odms.cplex.help/CPLEX/GettingStarted/topics/tutorials/Java/create_model.html
//		IloLinearNumExpr obj = getCplex().linearNumExpr();
//
//		double sum = 0.0;
//		for (int i = 1; i < n; ++i)
//			for (int j = 0; j < i; ++j)
//				if(d[i][j] > 0.0d){ // process positive edges
//					obj.addTerm(-d[i][j], v_edge[i][j]);
//					sum += d[i][j]; // constant term
//				}
//				else if(d[i][j] < 0.0d) // process negative edges
//					obj.addTerm(Math.abs(d[i][j]), v_edge[i][j]);
//
//		getCplex().iloCplex.addLe(getCplex().iloCplex.sum(obj, sum), cost);
//	}



	/**
	 * Add : n-3 variables xi which represent the fact that i is representative
	 * of its cluster (i in [3,n-1]) n * n-1 / 2 variables xi,j (i in [0,n-2], j
	 * in [i+1,n-1])
	 */
	void createVariables() throws IloException {

		if(p.isInt)
			v_edge = new IloIntVar[n][];
		else
			v_edge = new IloNumVar[n][];


		/* Create the edge variables (lower triangular part of v_edge) */
		for (int i = 0 ; i < n; ++i){
			if(p.isInt)
				v_edge[i] = new IloIntVar[n];
			else
				v_edge[i] = new IloNumVar[n];

			getCplex().iloCplex.conversion(v_edge[i], IloNumVarType.Float);
			
			for(int j = 0 ; j < i ; ++j){
				if(p.isInt)
					v_edge[i][j] = getCplex().iloCplex.intVar(0, 1);
				else
					v_edge[i][j] = getCplex().iloCplex.numVar(0,1);
				
				v_edge[i][j].setName("x_" + i + "_" + j);
				
				// ==========================================
				
				/* Link the symetric variables to their equivalent in the lower triangular
					part of v_edge => Ex : v[1][0] = v[0][1] */
				v_edge[j][i] = v_edge[i][j];
			}
			
		}

		
		/* WORKAROUD: This workaround works all the time and does not duplicate
		 *  any edge variables in the model. Because, we do not add any variables
		 *   to the model if the graph is complete
		 * 
		 * When the graph is incomplete and we use Cutting Planes approach,
		 *  we add constraints and cuts lazily.
		 * This causes ObjectException from cplex.getValue() since not all the edge
		 *  variables are in the model or constraints in the beginning.
		 * I found this thread as a solution: 
		 * 			https://www.ibm.com/developerworks/community/forums/
		 * 			html/topic?id=4f8bc2e4-a514-48d3-af62-10b9f417516d
		 */
		for (int i = 0 ; i < n; ++i){
			
			for(int j = i+1 ; j < n ; ++j) {
				
				// if the edge does not exist in the graph, i.e the corresponding weight = 0
				if(d[i][j] == 0) { 
					getCplex().iloCplex.add(v_edge[i][j]); // add it to the model
					getCplex().iloCplex.add(v_edge[j][i]); // add it to the model
				}
			}
		}
		
	}

	
	
	
	/**
	 * Add triangular constraints : xi,j + xi,k - xj,k <= 1 - if i is with j and
	 * k, then j and k are together
	 * @param solution 
	 * 
	 * @throws IloException
	 */
	void createTriangleConstraints() throws IloException {
		
		for (int i = 0; i < n - 2; ++i)
			for (int j = i + 1; j < n - 1; ++j)
				for (int k = j + 1; k < n; ++k) {

					// IloCplex model = getCplex().iloCplex;
					// System.out.println("!!");
					getCplex().addRange(new Triangle_Inequality(this, i, j, k).createRange());
					getCplex().addRange(new Triangle_Inequality(this, j, i, k).createRange());

					IloLinearNumExpr expr3 = getCplex().linearNumExpr();
					expr3.addTerm(1.0, v_edge[k][i]);
					expr3.addTerm(1.0, v_edge[k][j]);
					
					expr3.addTerm(-1.0, v_edge[j][i]);
					getCplex().addLe(expr3, 1.0);
				}
	}
	
	
	



    /**
	 * Write the resulting graph into file. IT us useful for fractional edge variables
	 * 
	 * @param numberOfElementsByLine
	 *            Number of variables displayed by line
	 * @throws UnknownObjectException
	 * @throws IloException
	 * @throws IOException 
	 */
	public void writeEdgeVariablesIntoFile(String filePath, boolean keepOnlyExistingEdges)
			throws UnknownObjectException, IloException, IOException {
		System.out.println(filePath);
		String content2 = "";
		int nbEdges = 0;
		
		for(int i = 0; i < n; i++) {
			
			for(int j = i+1; j < n; j++) {
				boolean process = true;

				if(keepOnlyExistingEdges && d[i][j]==0)
					process = false;
//				if(!keepOnlyExistingEdges && value > 1E-4)
//					process = false;
				
				double value = cvg.getValue(v_edge[i][j]);
				
				//if(process && value > 1E-4){
						nbEdges++;
						content2 = content2 + i + "\t" + j + "\t" + value + "\n";
				//}
			}
		}
		
		String firstLine = this.n + "\t" + nbEdges + "\n";
		String content = firstLine + content2;

		BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
		writer.write(content);
		writer.close();
		 
	}


	public void computeObjectiveValueFromSolution() {

		try {

			double obj = 0.0;
			/*
			 * Display the representative variables different from 0 (<l> by
			 * line)
			 */
//			System.out.println(" ");
//			System.out.println("Representative variables");
//
//			for (int m = 0; m < n; ++m){
//				double value = cvg.getValue(v_rep[m]);
//				System.out.println(m + " : " + value);
//			}

			/* Display the edge variables different from 0 (<l> by line) */
			System.out.println(" ");
			System.out.println("Edges variables");

			/* While all the edges variables have not been displayed */
			for(int i = 1 ; i < n ; ++i)
				for(int j = 0 ; j < i ; ++j){

					double value = cvg.getValue(v_edge[i][j]);

//					System.out.println(i + "-" + j + " : " + value);
					if(value == 1.0){ // if they are in the same cluster
						if(d[i][j] < 0) // if negative
							obj += Math.abs(d[i][j]);
					} else {
						if(d[i][j] > 0) // if positive
							obj += d[i][j];
					}

				}

			System.out.println("Objective: "+ obj);

			System.out.println(" ");

		} catch (UnknownObjectException e) {
			e.printStackTrace();
		} catch (IloException e) {
			e.printStackTrace();
		}

	}
	
	
	
	
	
	public double solve(){
		try {
				
			double time = -getCplex().iloCplex.getCplexTime();		
			getCplex().iloCplex.solve();
			return time + getCplex().iloCplex.getCplexTime();
			
			
		} catch (IloException e) {
			e.printStackTrace();
			return -1.0;
		}
	}
	
	
	public double solve(ArrayList<int[]> prevEdgeVarsList){
		try {
			if(prevEdgeVarsList.size()>0){
			    for(int[] prevEdgeVars : prevEdgeVarsList){
				    createDistinctSolutionConstraint(prevEdgeVars, 1);
			    }
			}
			
			double time = -getCplex().iloCplex.getCplexTime();		
			getCplex().iloCplex.solve();
			return time + getCplex().iloCplex.getCplexTime();
			
			
		} catch (IloException e) {
			e.printStackTrace();
			return -1.0;
		}
	}
	
	
	
	
	// ======================================================================
	
	// To enumerate all optimal solution use this method instead of cplex.solve()
	/* source: https://github.com/AdrianBZG/IBM_ILOG_CPLEX_Examples/
	 * 			blob/master/java/src/examples/Populate.java */
	public void populate(long tilim, long tilimForEnumAll, int solLim) {
				
		/* set GAP to 0.5 instead of 0 for accepting rounding error
		 * source: 
		 * 		https://orinanobworld.blogspot.fr/2013/01/
		 * 		finding-all-mip-optima-cplex-solution.html
		 * */
		double GAP = 0.5;
		
		try {
			/* source: https://www.ibm.com/support/knowledgecenter/
			 * 			SS9UKU_12.5.0/com.ibm.cplex.zos.help/UsrMan/topics/
			 * 			discr_optim/soln_pool/18_howTo.html */
			// to enumerate all optimal solutions, use those parameters
			
			// gap from the obj value of the optimal solution
			getCplex().iloCplex.setParam(IloCplex.Param.MIP.Pool.AbsGap, GAP);
			// For the value 4: the algorithm generates all solutions to your model
			getCplex().iloCplex.setParam(IloCplex.Param.MIP.Pool.Intensity,4);
			// 2100000000 is used as a high value
			if(solLim>0){
				System.out.println("solution limit: " + solLim);
				getCplex().iloCplex.setParam(IloCplex.Param.MIP.Limits.Populate, solLim-1);
			}
			else // 2100000000 is used as a high value
				getCplex().iloCplex.setParam(IloCplex.Param.MIP.Limits.Populate, 2100000000);

			System.out.println("----- MIP solve -----");
			long startTime = System.currentTimeMillis();
			boolean isOk = getCplex().iloCplex.solve();
			long endTime = System.currentTimeMillis();
			float execTimeFirstPhase = (float) (endTime-startTime)/1000;
			System.out.println("cplex is ok: " + isOk + " with exec time: " + execTimeFirstPhase);
			System.out.println("cplex status: " + getCplex().iloCplex.getCplexStatus());
			
			double remainingTime = -1;
			if(tilim>0 && tilimForEnumAll<0)
				remainingTime = tilim-execTimeFirstPhase;
			
			System.out.println("----- populate -----");
			
			if(tilimForEnumAll>0) {
				System.out.println("tilim for enum all: " + tilimForEnumAll);
				getCplex().iloCplex.setParam(IloCplex.Param.TimeLimit, tilimForEnumAll);
			}
			else if(tilim>0){
				if(remainingTime<0)
					remainingTime = 1.0;
				System.out.println("reaminigTime: " + remainingTime);
				getCplex().iloCplex.setParam(IloCplex.Param.TimeLimit, remainingTime);
			}
			
			startTime = System.currentTimeMillis();
			isOk = getCplex().iloCplex.populate();
			endTime = System.currentTimeMillis();
			System.out.println("cplex is ok: " + isOk);
			System.out.println("cplex status: " + getCplex().iloCplex.getCplexStatus());
			
			String filename = getOutputDirPath() + "/exec-time-cplex.txt";
			NumberFormat formatter = new DecimalFormat("#0.00000");
			System.out.println("Execution time is "
					+ formatter.format((endTime - startTime) / 1000d) + " seconds");
			
			try{
				 BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
				 writer.write( formatter.format((endTime - startTime) / 1000d));
				 writer.close();
			 } catch(IOException ioe){
			     System.out.print("Erreur in writing output file: ");
			     ioe.printStackTrace();
			 }
			
			
			if (isOk) {
	            System.out.println("Solution status = " + getCplex().iloCplex.getStatus());
	            System.out.println("Incumbent objective value  = "
	                               + getCplex().iloCplex.getObjValue());
	          
	
	            /* Get the number of solutions in the solution pool */		
	            int numsol = getCplex().iloCplex.getSolnPoolNsolns();
	            System.out.println("The solution pool contains " + numsol +
	                               " solutions.");
	
	            
	            // -------------------------------------------------------------
	            /* Since GAP=0.5, there will be some sub-optimal solutions, we need not to choose them.
	             * So, determine best optimal solutions in the pool, get their indexes */
	            HashSet<Integer> opt = determineBestSolsFromPool();
	            
	            System.out.println("out: " + getOutputDirPath());
	            
	            /* extract the optimal solutions from the pool and write them into files */
	            int h = 0;    // cumulative index of optimal solutions
	            for (int k : opt) {  // for each index of an optimal solution ...
	                System.out.println("Solution #" + h 
	                   + " (obj value = " + getCplex().iloCplex.getObjValue(k) + "):");
	
	            	retreiveClusters(k);
	            	filename = getOutputDirPath() + "/" + "sol" + h + ".txt";
	            	writeClusters(filename);
	            	
	            	h = h + 1;
	            }
	            // -------------------------------------------------------------
	        }
		}
	    catch (IloException e) {
	         System.err.println("Concert exception caught: " + e);
	    }
	}

	
	
	
	
	
	public HashSet<Integer> determineBestSolsFromPool() {
		
		double TOL = 1e-5; // tolerance
		
		// Get the number of solutions in the pool.
	    int nsol = 0;
		try {
			nsol = getCplex().iloCplex.getSolnPoolNsolns();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    // Create a container for the indices of optimal solutions.
	    HashSet<Integer> opt = new HashSet<>();
	    double best = Double.POSITIVE_INFINITY;  // best objective value found
	    /*
	     * Check which pool solutions are truly optimal; if the pool capacity
	     * exceeds the number of optimal solutions, there may be suboptimal
	     * solutions lingering in the pool.
	     */
	    for (int i = 0; i < nsol; i++) {
	    	// Get the objective value of the i-th pool solution.
	    	double z;
			
	    	try {
				z = getCplex().iloCplex.getObjValue(i);
				
				/* retreive solutions from [z - TOL, z + TOL] where z is the best obj val.
				 * Note that the problem is a minimization problem */
				if (z < best - TOL) {
			        /*
			         * If this solution is better than the previous best, the previous
			         * solutions must have been suboptimal; drop them all and count this one.
			         */
			        best = z;
			        opt.clear();
			        opt.add(i);
			      } else if (z < best + TOL) {
			        /*
			         * If this solution is within rounding tolerance of optimal, count it.
			         */
			        opt.add(i);
			      }
			} catch (IloException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      
	    }
	    System.out.println("\n\nFound " + nsol + " solutions, of which "
	                       + opt.size() + " are optimal.");
	    
	    return opt;
	}
	
	

		
}
