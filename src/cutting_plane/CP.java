package cutting_plane;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;

import callback.cut_callback.CutCallback_all;
import callback.cut_callback.FastCutCallback;
import callback.lazy_callback.LazyCBTriangle;

import formulation.Partition;
import formulation.MyPartition;
import formulation.MyParam;
import formulation.MyParam.Triangle;
import formulation.interfaces.IFEdgeV;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import inequality_family.AbstractInequality;
import mipstart.AbstractMIPStartGenerate;
import mipstart.PrimalHeuristicRounding;
import mipstart.SolutionManager;
import separation.SeparationSTGrotschell;
import separation.SeparationSTKL;
import separation.SeparationSTLabbe;
import separation.SeparationTCCKLFixedSize;
import separation.SeparationTriangle;

public class CP extends AbstractCuttingPlane<MyPartition>{

	int MAX_CUT;
	boolean userCutInBB;
	
	/**
	 * 
	 * @param p  partition parameter object
	 * @param MAX_CUT  the number of user cuts to be added (among all)
	 * @param modFindIntSolution  the frequency of applying primal heuristic during the iterations.
	 *  For instance, if mod=2, primal heuristic is applied once in every 2 subsequent iterations
	 * @param reordering  ordering separation algorithms ==> OBSOLETE
	 * @param tilim -1 if there is no limit on the time ; a time in seconds otherwise
	 * @throws IloException 
	 */
	public CP(MyParam p, int MAX_CUT, int modRemovingUntightCuts, int modFindIntSolution,
			boolean reordering, double tilim, String outputDir, int MaxTimeForRelaxationImprovement, boolean isEnumAll_,
			boolean verbose_)
			throws IloException {
		super(p, modRemovingUntightCuts, modFindIntSolution, reordering, tilim, outputDir, MaxTimeForRelaxationImprovement,
				isEnumAll_, verbose_);
		
		this.userCutInBB = p.userCutInBB;
		formulation = (MyPartition)Partition.createPartition(p);
		
		try
	    {
			formulation.setOutputDirPath(outputDir);
			formulation.setLogPath(outputDir + "/logcplex.txt");
	    }
	    catch (FileNotFoundException e)
	    {
	    	System.err.println("File not found exception caught: " + e);
	    }
		
		this.MAX_CUT = MAX_CUT;
	}

	
	/** 
	 * Adds separation algorithms (which generates inequalities) into array
	 *  in order that they are used during Root Relaxation approach.
	 * 
	 * @param remaining_time
	 * @param mipStart  the best feasible solution obtained in the Root Relaxation part
	 */
	@Override
	public void createSeparationAlgorithm() {
		boolean toAddInBB;
		boolean isQuick;

		/* Grotschell ST */
		toAddInBB = true;
		isQuick = true;
		sep.add(new CP_Separation<IFEdgeV>(
				new SeparationSTGrotschell(formulation, formulation.variableGetter(), MAX_CUT),
				toAddInBB,
				isQuick
				));

		/* Labbe ST */
		toAddInBB = true;
		isQuick = true;
		sep.add(new CP_Separation<IFEdgeV>(
				new SeparationSTLabbe(formulation, formulation.variableGetter()),
				toAddInBB,
				isQuick
				));
			

		/* Triangle inequalities */
		MyParam rp = (MyParam)this.formulation.p;

		/* If the triangle inequalities are:
		 * - not in cutting plane model 
		 * - not generated lazily in the cutting plane step 
		 * - not contained in the branch and cut model */ 
		if(rp.triangle == Triangle.USE_LAZY_IN_BC_ONLY) {
			toAddInBB = false; // TODO true ?
			isQuick = true;
			sep.add(new CP_Separation<IFEdgeV>(
					new SeparationTriangle(formulation, formulation.variableGetter(), MAX_CUT),
					toAddInBB, isQuick
					));
		}

		
		/* If the triangle inequalities are:
		 * - not in cutting plane model 
		 * - not generated lazily in the cutting plane step 
		 * - contained in the branch and cut model */ 
		else if(rp.triangle == Triangle.USE_IN_BC_ONLY) {
			toAddInBB = false;
			isQuick = true;
			sep.add(new CP_Separation<IFEdgeV>(
					new SeparationTriangle(formulation, formulation.variableGetter(), MAX_CUT),
					toAddInBB,
					isQuick
					));
		}

		/* Kernighan-Lin ST and ?? inequalities */
		toAddInBB = true;
		isQuick = false;
		sep.add(new CP_Separation<IFEdgeV>(
				new SeparationSTKL(formulation, formulation.variableGetter(), 5, true),
				toAddInBB,
				isQuick
				));
		toAddInBB = true;
		isQuick = false;
		sep.add(new CP_Separation<IFEdgeV>(
				new SeparationTCCKLFixedSize(formulation, formulation.variableGetter(), 2, null, true),
				toAddInBB,
				isQuick
				));

	}

	
	
	
	/** 
	 * Creates Integer formulation and provides it with the best feasible solution
	 *  obtained during the Cutting Planes approach.
	 * If Lazy callback or User Cuts are allowed in this Branch&Bound, 
	 * CPLEX solves it with 1 thread. Otherwise, use the maximal number of threads
	 * This Branch&Bound part is handled entirely by CPLEX (as a blackbox function,
	 *  as contrary to the previous Root Relaxation part)
	 * If time limit is specified in input parameters and the integer optimal solution
	 *  is reached before time limit, the solution is written into file
	 * 
	 * @param remaining_time
	 * @param mipStart  the best feasible solution obtained in the Root Relaxation part
	 */
	@Override
	public void findAllSolutionsAfterCP() {

		/* Get the tight constraints */
		ArrayList<AbstractInequality<? extends IFormulation>> ineq =  this.getTightConstraints();
//		ArrayList<Abstract_Inequality> ineq = this.getAllConstraints();

		// indicate that the formulation/solution will be integer during the Branch&Bound
		formulation.p.isInt = true;
		formulation.p.cplexOutput = true;
		
		String outputDirPath = formulation.getOutputDirPath();
		
		try {
			/* Create the partition with integer variables */
			formulation = ((MyPartition)Partition.createPartition((MyParam)formulation.p));
			formulation.setOutputDirPath(outputDirPath);
//			formulation = new MyPartition((RepParam)formulation.p);
			

			/* Add the previously tight constraints to the formulation */
			for(AbstractInequality<? extends IFormulation> i : ineq){

				i.setFormulation(formulation);
				try {
					formulation.getCplex().addRange(i.createRange());
				} catch (IloException e) {
					e.printStackTrace();
				}
			}

//			cpresult.time = - formulation.getCplex().getCplexTime();
//			formulation.getCplex().solve();
//			cpresult.time += formulation.getCplex().getCplexTime();

				if(this.verbose)
					System.out.println("out: " + formulation.getOutputDirPath());
			
				double GAP = 0.5;
			
				/* source: https://www.ibm.com/support/knowledgecenter/
				 * 			SS9UKU_12.5.0/com.ibm.cplex.zos.help/UsrMan/topics/
				 * 			discr_optim/soln_pool/18_howTo.html */
				// to enumerate all optimal solutions, use those parameters
				
				// gap from the obj value of the optimal solution
				formulation.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Pool.AbsGap, GAP);
				// For the value 4: the algorithm generates all solutions to your model
				formulation.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Pool.Intensity, 4);
				// 2100000000 is used as a high value
				formulation.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Limits.Populate, 10000);

				if(this.verbose)
					System.out.println("----- 1st populate -----");
				
				long start = System.currentTimeMillis();
				boolean isOk = formulation.getCplex().iloCplex.populate();
				long end = System.currentTimeMillis();
				
				if(this.verbose){
					System.out.println("cplex is ok: " + isOk);
					System.out.println("cplex status: " + formulation.getCplex().iloCplex.getCplexStatus());
				}
				
//				System.out.println("----- 2nd populate -----");
//				start = System.currentTimeMillis();
//				isOk = formulation.getCplex().iloCplex.populate();
//				end = System.currentTimeMillis();
//				System.out.println("cplex is ok: " + isOk);
//				System.out.println("cplex status: " + formulation.getCplex().iloCplex.getCplexStatus());
//				
//				System.out.println("----- 3rd populate -----");
//				start = System.currentTimeMillis();
//				isOk = formulation.getCplex().iloCplex.populate();
//				end = System.currentTimeMillis();
//				System.out.println("cplex is ok: " + isOk);
//				System.out.println("cplex status: " + formulation.getCplex().iloCplex.getCplexStatus());
				
							
				NumberFormat formatter = new DecimalFormat("#0.00000");
				if(this.verbose)
					System.out.print("Execution time is "
							+ formatter.format((end - start) / 1000d) + " seconds");
				
				if (isOk) {
					if(this.verbose){
			            System.out.println("Solution status = " + formulation.getCplex().iloCplex.getStatus());
			            System.out.println("Incumbent objective value  = "
			                               + formulation.getCplex().iloCplex.getObjValue());
					}
		
		            /* Get the number of solutions in the solution pool */		
		            int numsol = formulation.getCplex().iloCplex.getSolnPoolNsolns();
		            if(this.verbose)
		            	System.out.println("The solution pool contains " + numsol +
		                               " solutions.");
		            
		            // -------------------------------------------------------------
		            /* Since GAP=0.5, there will be some sub-optimal olsutions, we need not to choose them.
		             * So, determine best optimal solutions in the pool, get their indexes */
		            HashSet<Integer> opt = formulation.determineBestSolsFromPool();
		            
		            /* extract the optimal solutions from the pool and write them into files */
		            int h = 0;    // cumulative index of optimal solutions
		            for (int k : opt) {  // for each index of an optimal solution ...
		            	if(this.verbose)
			                System.out.println("Solution #" + h 
			                   + " (obj value = " + formulation.getCplex().iloCplex.getObjValue(k) + "):");
		
		                formulation.retreiveClusters(k);
		            	String filename = formulation.getOutputDirPath() + "/sol" + h + ".txt";
		            	formulation.writeClusters(filename);
		            	
		            	h = h + 1;
		            }
		            // -------------------------------------------------------------
				}
		

		} catch (IloException e) {
			e.printStackTrace();
		}

	}
	
	
	
	/** 
	 * Creates Integer formulation and provides it with the best feasible solution
	 *  obtained during the Cutting Planes approach.
	 * If Lazy callback or User Cuts are allowed in this Branch&Bound, 
	 * CPLEX solves it with 1 thread. Otherwise, use the maximal number of threads
	 * This Branch&Bound part is handled entirely by CPLEX (as a blackbox function,
	 *  as contrary to the previous Root Relaxation part)
	 * If time limit is specified in input parameters and the integer optimal solution
	 *  is reached before time limit, the solution is written into file
	 * 
	 * @param remaining_time
	 * @param mipStart  the best feasible solution obtained in the Root Relaxation part
	 */
	@Override
	public void findIntSolutionAfterCP(double remaining_time, SolutionManager mipStart) {
		System.out.println("Remaining time for b&c: " + remaining_time);

		/* Get the tight constraints */
		ArrayList<AbstractInequality<? extends IFormulation>> ineq =  this.getTightConstraints();
//		ArrayList<Abstract_Inequality> ineq = this.getAllConstraints();

		// indicate that the formulation/solution will be integer during the Branch&Bound
		formulation.p.isInt = true;
		
		formulation.p.cplexOutput = true;
		if(remaining_time != -1)
			formulation.p.tilim = remaining_time;
		
		try {
			/* Create the partition with integer variables */
			formulation = ((MyPartition)Partition.createPartition((MyParam)formulation.p));
//			formulation = new MyPartition((RepParam)formulation.p);
			
			if(mipStart != null){
				try {
					if(this.verbose)
						System.out.println("!!! MIP START eval:"+mipStart.evaluate()+" !!!");
					
					mipStart.updateFormulationAndVariables(formulation);

					mipStart.setVar();
					formulation.getCplex().addMIPStart(mipStart.var, mipStart.val);
					
					if(this.verbose)
						System.out.println("!!!!!!!!!!MIP START DONE!!!!!!!");
				} catch (IloException e) {
					e.printStackTrace();
				}		
			}

			/* Add the previously tight constraints to the formulation */
			for(AbstractInequality<? extends IFormulation> i : ineq){

				i.setFormulation(formulation);
				try {
					formulation.getCplex().addRange(i.createRange());
				} catch (IloException e) {
					e.printStackTrace();
				}
			}

			CutCallback_all acc = null;
//			FastCutCallback acc = null;
			if(this.userCutInBB) {
				acc = new CutCallback_all(formulation, 500);
//				acc = new FastCutCallback(formulation, 500);
				formulation.getCplex().use(acc);
			}
			

			cpresult.time = - formulation.getCplex().getCplexTime();

			formulation.getCplex().solve();
			cpresult.time += formulation.getCplex().getCplexTime();
			cpresult.getResults(formulation, acc, false);
			
			if(this.verbose){
				System.out.println("bestInt = " + formulation.getCplex().getObjValue());
				System.out.println("bestRelax. = " + formulation.getCplex().getBestObjValue());
			}
			
//			formulation.retreiveClusters();
//			formulation.displayClusters();
//			formulation.writeClusters(outputDir + "/result.txt");
			
		} catch (IloException e) {
			e.printStackTrace();
		}

	}

	
	public SolutionManager getMIPStart() throws IloException{

		PrimalHeuristicRounding mipGetter = new PrimalHeuristicRounding(formulation);

		return mipGetter.generateMIPStart();
	}


	/**
	 * Test if the current solution of the formulation is integer
	 * 
	 * @return True if the current solution is integer; false otherwise
	 */
	public boolean isInteger(){

		boolean result = true;

		try {

			int i = 0;
			while(i < formulation.n && result){

				int j = i+1;
				while(result && j < formulation.n){	
					
					IloNumVar x_ij = formulation.edgeVar(i,j);
					if(!isInteger(formulation.variableGetter().getValue(x_ij)))
						result = false;	
					++j;
				}

				++i;
			}
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
		}	

		return result;
	}

	
	@Override
	public AbstractMIPStartGenerate initializeMIPStartGenerator() {
		return new PrimalHeuristicRounding(formulation);	
	}

	
	@Override
	public MyPartition getFormulation() {
		return formulation;
	}
	
	
	



}
