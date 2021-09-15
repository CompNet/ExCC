package cutting_plane;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

import formulation.Param;
import formulation.Edge;
import formulation.MyPartition;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import inequality_family.AbstractInequality;
import mipstart.AbstractMIPStartGenerate;
import mipstart.SolutionManager;
import results.CPResult;
import results.ComputeResults;
import variable.VariableLister.VariableListerException;



/**
 * 
 * 
 * @param DEFAULT_MAX_TIME_FOR_RELAXATION_IMPROVEMENT  default time to be waited
 * 			 after the last relaxation improvement time. After this time, Root 
 * 			 Relax. stops, and Branch&Bound starts.
 * 			 This is used when time limit is specified.
 * @param GAP_IMPROVEMENT_TRHESHOLD  Relative gap comparison value. If there is
 * 			 'GAP_IMPROVEMENT_TRHESHOLD' gap between the previous relaxation and
 * 			 the last one, it is considered improved.
 * @param MIN_REMAINING_TIME  minimal
 * 			 amount of time to use primal heuristic in force at each iteration.
 * 			 This is used when time limit is specified
 * 
 * @param sep  separation methods (each one generates cuts)
 * @param cpresult  an object that records some result information (cutting plane time, etc.)
 * @param formulation  formulation object that allows accessing 'cplex' object

 */
public abstract class AbstractCuttingPlane<Formulation extends IFormulation> {
	
	boolean isEnumAll = false;
	
	// ===================================================================
	// constants
	public double eps = 1E-6;
	double oneMeps = 1.0 - eps;
	
	int DEFAULT_MAX_TIME_FOR_RELAXATION_IMPROVEMENT = 600; // 600 secs = 10 mins
	double GAP_IMPROVEMENT_TRHESHOLD = 1E-2; // TODO is this a good value ?
	int MIN_REMAINING_TIME = 200;
	
	// Look at also those methods
	//   => setMaxTimeForRelaxationImprovement()
	//   => setTimeLimitForCuttingPlanes()
	// ===================================================================
	
	
	public ArrayList<CP_Separation<?>> sep = new ArrayList<>();

	public CPResult cpresult;

	Integer modFindIntSolution = Integer.MAX_VALUE;

	public Formulation formulation;
	boolean reordering;
	double tilim;
	String outputDir;
	
	int[] initialPartitionMembership;
	
	// ============================================================
	/* Maximal time in seconds given to the cutting plane step to improve
	 *  the relaxation by at least 0.01%
	 * After that the cutting plane step is stopped (and the branch and cut takes place)
	 */
	double maxTimeForRelaxationImprovement;	
	double minimalTimeBeforeRemovingUntightCuts = Double.MAX_VALUE;
	double lastRelaxationImprovTime;
	double lastRemovingCutsTime;
	boolean MaxTimeForRelaxationImprovementReached = false;
	boolean isUntightIneqRemovingReady = false;

	double gap = Double.MAX_VALUE;
	double lastImprovedRelaxation = -Double.MAX_VALUE;
	double cptilim = -1;
	boolean verbose = true;
	
	boolean is40percentOptimalityGap = false;
	boolean is30percentOptimalityGap = false;
	boolean is20percentOptimalityGap = false;
	boolean is10percentOptimalityGap = false;
	boolean is5percentOptimalityGap = false;

	
	boolean onlyFractionalSolution = false;
	double fractionalSolutionGapPropValue = -1.0; // not specific gap value, when there is no improvement

	// ============================================================
	
	 
	
	/**
	 * 
	 * 
	 * @param p  partition parameter object
	 * @param minimalTimeBeforeRemovingUntightCuts  time to be waited since the last removing
	 * 		So, untight cuts are not in general removed juste after the cut generation => delay
	 * @param reordering  if TRUE, reorder the separation method during cut generation => OBSOLETE
	 * @param modFindIntSolution  the frequency of applying primal heuristic during the iterations.
	 * @param tilim  time limit for the whole program (Root Relaxation + Branch&Bound)
	 * @param outputDir  output directory where the result files will be saved
	 */
	public AbstractCuttingPlane(Param p, int minimalTimeBeforeRemovingUntightCuts,
			int modFindIntSolution, boolean reordering, double tilim, String outputDir,
			int MaxTimeForRelaxationImprovement, boolean isEnumAll_, boolean verbose_,
			int[] initialPartitionMembership_, boolean onlyFractionalSolution_, double fractionalSolutionGapPropValue_)
					throws IloException{

		p.isInt = false;
		cpresult = new CPResult();
		cpresult.setLogPath(outputDir);
		this.minimalTimeBeforeRemovingUntightCuts = minimalTimeBeforeRemovingUntightCuts;
		this.reordering = reordering;
		this.tilim = tilim;
		this.modFindIntSolution = modFindIntSolution;
		this.outputDir = outputDir;
		this.isEnumAll = isEnumAll_;
		this.verbose = verbose_;
		this.initialPartitionMembership = initialPartitionMembership_;
		
		setMaxTimeForRelaxationImprovement(MaxTimeForRelaxationImprovement);
		if(tilim > 0) { // if time limit is provided by user
			setTimeLimitForCuttingPlanes();
		}
		
		this.onlyFractionalSolution = onlyFractionalSolution_;
		this.fractionalSolutionGapPropValue = fractionalSolutionGapPropValue_;
	}

	
	
	public abstract Formulation getFormulation();

	
	
	/**
	 * Solve a formulation with the Cutting Plane approach.
	 * 
	 * However, this is a manual version. The author of the code source, Zacharie Ales,
	 * claims that CPLEX sometimes adds some extra functionalities that you may not want to add.
	 * In order to prevent from that, he decided to implement his own version of 
	 * Cutting Plane approach. 
	 * ==> TODO we should add another Cutting Plane approach which uses only CPLEX methods.
	 * 
	 * In this manual version: we do in multiple times until no cut generated or within time limit:
	 *  - solving root relaxation 
	 *  	=> i.e. computing continuous optimal solution (based on current user cuts)
	 *  - adding violated inequalities (i.e. user cuts)
	 *  	We do not add all the generated inequalities ==> we select only tight ones
	 *  - finding a feasible solution with primal heuristic regularly, then 
	 *  	saves the best one. This best feasible solution will be given to Branch&Bound step
	 *  	in case there is no integer solution which validates the generated inequalities during
	 *  	Cutting Plane approach
	 * 
	 * (warning: the variables in the formulation must be continuous, not integer!)
	 * 
	 * Some variables to b explained:
	 * 'toRemove': an array that contains untight violated inequalities
	 * 		 to be removed from CPLEX model
	 * 'toAdd': an array that contains all violated inequalities to be added
	 * 	 into CPLEX model
	 * 
	 * 
	 * @return
	 * @throws VariableListerException 
	 * @throws IloException 
	 */
	public double solve() throws VariableListerException, IloException{
		

		cpresult.cp_time = -formulation.getCplex().getCplexTime();
		
		this.formulation = getFormulation();
		this.lastRelaxationImprovTime = -formulation.getCplex().getCplexTime();
		this.lastRemovingCutsTime = -formulation.getCplex().getCplexTime();
		
		// =====================================================================
		createSeparationAlgorithm();		
		declareCplexAsRootRelaxation();
		// =====================================================================

		boolean isInteger = false;
		double last_cp_relaxation = -Double.MAX_VALUE;
		double cpTime; // temp variable
		SolutionManager bestMIP = null;
		double bestInt = Double.MAX_VALUE;
		
		try {
			// =================================================================
			solveRootRelaxation();
			// =================================================================

			cpresult.cp_first_relaxation = formulation.getCplex().getObjValue();
			cpresult.cp_iteration = 0;

			if(this.initialPartitionMembership != null){
				bestMIP = LoadInitialIntegerSolution();
				bestMIP.setMembership(this.initialPartitionMembership);
				bestInt = bestMIP.evaluate();
			}

			
			boolean cutFound = true;
			boolean optimumFound = false;
			boolean readyToRemove = false;
			
			double remainingTime = -1;
			if(tilim > 0) { // if time limit is provided by user
				/* Remaining cutting plane time */
				cpTime = formulation.getCplex().getCplexTime() + cpresult.cp_time;
				remainingTime = cptilim - (cpTime);
			}

			while((remainingTime > 0 || tilim == -1.0) && cutFound && !optimumFound 
					&& !MaxTimeForRelaxationImprovementReached){
				
				
				// =============================================================
				// init
				
				cutFound = false;
				ArrayList<AbstractInequality<? extends IFormulation>> toAdd = new ArrayList<>();
				ArrayList<IloRange> toRemove = new ArrayList<>();
				
				Iterator<CP_Separation<?>> algo = sep.iterator();
				while(algo.hasNext()) {
					/* No separation algorithm has yet found any cut at this iteration */
					algo.next().usedAtThisIteration = false;
				}
				// =============================================================
				
				
				// =============================================================
				/* Compute the relaxation */
				solveRootRelaxation();
				// =============================================================
													
				
				// =============================================================
				// Determine untight inequalities to be removed (removing part 1/2)
				boolean isTimeToRemove = checkIfTimeToRemoveUntightInequalities();
				if(isTimeToRemove){					
					ArrayList<IloRange> untightIneqs = determineUntightInequalities();
					toRemove.addAll(untightIneqs);
					readyToRemove = true;
				}
				// =============================================================


				last_cp_relaxation = formulation.getCplex().getObjValue();
				gap = ComputeResults.improvement(last_cp_relaxation,  bestInt);
				cpTime = formulation.getCplex().getCplexTime() + cpresult.cp_time;

				
				
				// --------------------------------------------------------
				boolean takeGraphSnapshot = false;
				double gapProp = (Math.round(100*gap)/100.0);
				if(!is40percentOptimalityGap && (gapProp<=0.4 && gapProp>0.3)){
					is40percentOptimalityGap = true;
					takeGraphSnapshot = true;
				} else if(!is30percentOptimalityGap && (gapProp<=0.3 && gapProp>0.2)){
					is30percentOptimalityGap = true;
					takeGraphSnapshot = true;
				} else if(!is20percentOptimalityGap && (gapProp<=0.2 && gapProp>=0.1)){
					is20percentOptimalityGap = true;
					takeGraphSnapshot = true;
				} else if(!is10percentOptimalityGap && (gapProp<=0.1 && gapProp>0.05)){
					is10percentOptimalityGap = true;
					takeGraphSnapshot = true;
				} else if(!is5percentOptimalityGap && (gapProp<=0.05 && gapProp>0.01)){
					is5percentOptimalityGap = true;
					takeGraphSnapshot = true;
				} 
				
				if(takeGraphSnapshot){
					try {
						((MyPartition) formulation).writeEdgeVariablesIntoFile(outputDir+"/fractionalGraph_gap="+gapProp+".G", false);
					} catch (IloException | IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					takeGraphSnapshot = false; // init
				}
				// --------------------------------------------------------
				
				
				if(gapProp <= fractionalSolutionGapPropValue) {
					if(this.verbose)
						System.out.println("The cutting plane method is stopped, since it reaches the threshold value for gap: " + fractionalSolutionGapPropValue);
					break;
				}
				
				
				if(this.verbose)
					System.out.println(Math.round(cpTime) + "s : ["
						+ Math.round(last_cp_relaxation) + ", " + Math.round(bestInt) + "] "
						+ (Math.round(100*gap)/100.0) + "%");

				/* Is the relaxation an integer solution? */ 
				isInteger = isInteger();
				
				
				
				/* If the solution is Integer, test if triangle constraints find
				 *  a violated inequality or not. If there is not any violation, 
				 *  Cutting Planes approach terminates since we find a feasible solution */
				if(isInteger){
					int sepId = getTriangleSeparationMethodId();
					ArrayList<AbstractInequality<? extends IFormulation>> violatedIneqs;
					violatedIneqs = checkLazilyIfIntegerSolutionValid(sepId);
					if(violatedIneqs.size() > 0) {
						if(!this.isEnumAll){ // do not add these triangle inequalities if the goal is to enumerate all solutions, we add all of them in 2nd phase
							tagInequality(violatedIneqs, sepId);
							toAdd.addAll(violatedIneqs);
						}
						cutFound = true;
					}
				}


				/* If the solution is not integer */
				// if an incumbent solution is already provided by ILSCC, then no need to perform a primal heuristic
				// ILSCC usually performs better than the primal heuristic that we employ here
				if(this.initialPartitionMembership == null && !this.isEnumAll && (!isInteger || !optimumFound)){

					if(remainingTime < MIN_REMAINING_TIME 
							|| cpresult.cp_iteration % modFindIntSolution == 0) {
						SolutionManager newMIP = findIntegerSolutionWithPrimalHeuristic();
						double newInt = newMIP.evaluate();
						
						if(this.verbose){
							System.out.println("------- newInt: " + newInt);
							System.out.println("------- bestInt: " + bestInt);
						}
						
						if(newInt < bestInt){
							bestInt = newInt;
							bestMIP = newMIP;
						}	
					}
					
				}
				
				
				double gapFromLastImprovedRelaxation = 
						ComputeResults.improvement(lastImprovedRelaxation, last_cp_relaxation);
				double time_since_last_improvement = 
						lastRelaxationImprovTime + formulation.getCplex().getCplexTime();
				
				if(this.verbose){
					System.out.println("\n remaining time for relaxation improvement time: "
							+ (Math.round(maxTimeForRelaxationImprovement) - Math.round(time_since_last_improvement)) + "s "
							+ ComputeResults.doubleToString(gapFromLastImprovedRelaxation, 4) + "%");
					System.out.println("gapFromLastImprovedRelaxation:"+gapFromLastImprovedRelaxation);
						
//					System.out.println("\n time/gap since last relaxation improvement: "
//							+ Math.round(time_since_last_improvement) + "s " 
//							+ ComputeResults.doubleToString(gapFromLastImprovedRelaxation, 4) + "%");
//					System.out.println("gapFromLastImprovedRelaxation:"+gapFromLastImprovedRelaxation);
				}
				
				if(gapFromLastImprovedRelaxation > GAP_IMPROVEMENT_TRHESHOLD){
					lastImprovedRelaxation = last_cp_relaxation;
					lastRelaxationImprovTime = -formulation.getCplex().getCplexTime();
				} 
				else if(time_since_last_improvement > maxTimeForRelaxationImprovement){
					MaxTimeForRelaxationImprovementReached = true;
					System.out.println("\n Max time for relaxation improvement reached (" 
					+ Math.round(maxTimeForRelaxationImprovement) + "s)");
				}

				
				
				if(tilim > 0) { // if time limit is provided by user
					/* Remaining cutting plane time */
					cpTime = formulation.getCplex().getCplexTime();
					remainingTime = cptilim - (cpTime + cpresult.cp_time);
				}
				
				
				
				
				// =============================================================
				// Generate user cuts
				if((remainingTime > 0 || tilim == -1.0) && !optimumFound && !MaxTimeForRelaxationImprovementReached){
					/* The 'generateUserCuts' method also stores the corresponding
					separation method id for generated cuts => tagInequality() */
					toAdd = generateUserCuts(remainingTime); 
					
					
					if(toAdd.size() != 0) {
						cutFound = true;
						addInequality(toAdd); // add directly to CPLEX model
						if(this.verbose)
							System.out.println("cut found !!!!!!");
					} else {
						if(this.verbose)
							System.out.println("cut not found !!!!!!");
					}
				}
				// =============================================================


				// =============================================================
				// Remove untight inequalities (removing part 2/2)
				if(readyToRemove && cutFound){					
					removeUntightInequalities(toRemove); // remove from CPLEX model
					lastRemovingCutsTime = -formulation.getCplex().getCplexTime();
					readyToRemove = false;
				}
				// =============================================================
				
				if(this.verbose){
					System.out.println("Optimum found: " + optimumFound);
					System.out.println("cutFound: " + cutFound);
					System.out.println("MaxTimeForRelaxationImprovementReached: "
							+ MaxTimeForRelaxationImprovementReached);
				}
				
				cpresult.cp_iteration++;
				if(this.verbose)
					System.out.println("cpresult.cp_iteration: " + cpresult.cp_iteration);
				
				
				// ----------------------------------
			} // end of while
			
			// -------------------
			if(!this.isEnumAll) 
				cpresult.gapAfterCp = gap;
			else
				cpresult.gapAfterCp  = -1;// no need for all enumeration version
			// -------------------
			
			
		} catch (IloException e1) {
			e1.printStackTrace();
		}

		cpresult.cp_time += formulation.getCplex().getCplexTime();
		if(this.verbose)
			System.out.println("\t" + Math.round(cpresult.cp_time) + "s "
					+ Math.round(cpresult.firstRelaxation));
		
		// =====================================================================
		// =====================================================================

		for(CP_Separation se : this.sep){
			if(this.verbose)
				System.out.println(se.se.name + " : " + (se.addedIneq.size() + se.removedIneq));
		}
		
		for(CP_Separation<?> s : sep)
			if(s.addedIneq.size() > 0)
				cpresult.cpCutNb.add(cpresult.new Cut(s.se.name, s.addedIneq.size()));
		
		
		
		if(isInteger){
			if(this.verbose)
				System.out.println("\nSolution is integer after cp");
			
			cpresult.bestRelaxation = -1.0;
			cpresult.time = 0.0;
			cpresult.node = 0;
			cpresult.separationTime = -1.0;
			cpresult.iterationNb = -1;
			
			if(this.isEnumAll && !onlyFractionalSolution){
				if(this.verbose)
					System.out.println("mod: enum all solutions");
				
				findAllSolutionsAfterCP(tilim == -1 ? -1 : tilim - cpresult.cp_time, bestMIP);
			}
			else if(!this.isEnumAll){
			    // ===============================================
			    // register the last graph snapshot before B&B
			    try {
				    ((MyPartition) formulation).writeEdgeVariablesIntoFile(outputDir+"/fractionalGraph.G", false);
			    } catch (IloException | IOException e1) {
				    // TODO Auto-generated catch block
				    e1.printStackTrace();
			    }
			    // ===============================================
			    
				((MyPartition) formulation).retreiveClusters();
				((MyPartition) formulation).displayClusters();
				//p.setOutputDirPath(this.outputDir);
				((MyPartition) formulation).writeClusters(this.outputDir + "/sol0.txt");
				
				RegisterLPmodelAfterCP();
				
				//formulation.computeObjectiveValueFromSolution(); // This does not give the correct value at this stage, do not use it here
				//formulation.computeObjectiveValueFromClusters();
			}
		}
		else if(!isInteger){
			if(this.verbose)
				System.out.println("\nSolution is not integer after cp");
			
			// ===============================================
			// register the last graph snapshot before B&B
			try {
				((MyPartition) formulation).writeEdgeVariablesIntoFile(outputDir+"/fractionalGraph.G", false);
			} catch (IloException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// ===============================================
			
			RegisterLPmodelAfterCP();
			
			if(!onlyFractionalSolution){
				
				if(!this.isEnumAll){
					if(this.verbose)
						System.out.println("mod: find only one solution");
					
					findIntSolutionAfterCP(tilim == -1 ? -1 : tilim - cpresult.cp_time, bestMIP);
					
					if(this.verbose)
						System.out.println("CP relaxation: " + last_cp_relaxation
								+ " BC relaxation: " + cpresult.bestRelaxation);
					
					if(cpresult.bestRelaxation < last_cp_relaxation || cpresult.bestRelaxation > 1E15)
						cpresult.bestRelaxation = last_cp_relaxation;
			
					try {
						cpresult.bestInt = formulation.getCplex().getObjValue();
					} catch (IloException e) {
						e.printStackTrace();
					}
					
//					MyPartition p = (MyPartition)formulation;				
//					p.retreiveClusters();
//					p.displayClusters();
//					//p.setOutputDirPath(this.outputDir);
//					p.writeClusters(this.outputDir + "/ExCC-result.txt");
		
					cpresult.firstRelaxation = last_cp_relaxation;
					
				} else {
					
					if(this.verbose)
						System.out.println("mod: enum all solutions");
					
					findAllSolutionsAfterCP(tilim == -1 ? -1 : tilim - cpresult.cp_time, bestMIP);
					
				}
				
			}
		}
		
		
		
		cpresult.log();


		return cpresult.cp_time + cpresult.time;
	}


	
	// =========================================================================
	// =========================================================================

	
	
	public abstract AbstractMIPStartGenerate initializeMIPStartGenerator();


	
	/**
	 * It binds the recently generated cuts to its cut family method.
	 * 
	 * @param r  recently generated user cuts
	 * @param idSep  id of a cut family method
	 * 
	 */
	public void tagInequality(ArrayList<AbstractInequality<? extends IFormulation>> r, int idSep){

		CP_Separation<? extends IFormulation> c = sep.get(idSep);

		for(AbstractInequality<? extends IFormulation> ri : r){
			c.addedIneq.add(ri);
		}

		c.se.added_cuts += r.size();
	}

	
	
	/**
	 * It adds generated cuts into the CPLEX model.
	 * 
	 * @param r  generated user cuts
	 * 
	 */
	public void addInequality(ArrayList<AbstractInequality<? extends IFormulation>> r){

		for(AbstractInequality<? extends IFormulation> ri : r)
			try {
				ri.ilorange = formulation.getCplex().addRange(ri.getRange());
			} catch (IloException e) {
				e.printStackTrace();
			}

	}


	/**
	 * It defines which separation method is used in both Root relaxation and Branch&Bound.
	 * 
	 */
	public abstract void createSeparationAlgorithm();

	
	
	/**
	 * Find an integer solution thanks to the best relaxation found by the cutting plane step.
	 * Warning : This method must update the secondStep_time attribute
	 * @throws VariableListerException 
	 * @throws IloException 
	 */
	public abstract void findIntSolutionAfterCP(double remaining_time, SolutionManager mipStart) throws VariableListerException, IloException;

	/**
	 * Find all solutions after the cutting plane step.
	 * Warning : 
	 * @throws VariableListerException 
	 */
	public abstract void findAllSolutionsAfterCP(double remaining_time, SolutionManager mipStart) throws VariableListerException;


	/**
	 * Test if the current solution of the formulation is integer
	 * @return True if the current solution is integer; false otherwise
	 */
	public abstract boolean isInteger();


	/**
	 * Test if the value is integer.
	 * In the Linear Relax. some edge variables might have values very close to 0 or to 1.
	 * Those values are considered as Integer.
	 * 
	 * @return True if the current solution is integer; false otherwise
	 */
	public boolean isInteger(double d){
		return d < eps || d > oneMeps; 
	}


	/**
	 * Looks for tight constraints among all. 
	 * 
	 * @return Array which contain tight constraints
	 */
	public ArrayList<AbstractInequality<?>> getTightConstraints(){

		ArrayList<AbstractInequality<?>> result = new ArrayList<>();

		for(CP_Separation<?> si : sep)
			if(si.toAddInBB){		
				for(AbstractInequality<?> i : si.addedIneq){					
					if(i.isTight(formulation.variableGetter())){		
						result.add(i);
					}
				}
			}

		return result;

	}	
	
	
	
	
	/* DO NOT DELETE: It might be used in 'findIntSolutionAfterCP()' in 'CP.java'
	instead of 'getTightConstraints()' */
	
//	/**
//	 * It returns all user cuts (except triangle constraints) generated until this time
//	 * 
//	 * @return result  All user cuts
//	 */
//	public ArrayList<AbstractInequality<?>> getAllConstraints(){
//
//		ArrayList<AbstractInequality<?>> result = new ArrayList<>();
//
//		for(CP_Separation<?> si : sep)
//			if(si.toAddInBB){		
//				for(AbstractInequality<?> i : si.addedIneq){				
//					result.add(i);
//				}
//			}
//
//		return result;
//
//	}
	
	

	// =========================================================================
	
	
	/**
	 * 
	 * When time limit is set, it assigns an improvement evaluation threshold 
	 * for Root Relaxation. If there is no improvement for some time in Root relaxation,
	 * it stops doing it, then it passes to Branch&Bound step. If time limit is 
	 * not set, the default value will be used, because after some time, 
	 * even tough there is an improvement, it is very tiny. So, it is better 
	 * to pass to Branch&Bound in this case. This default value is decided arbitrarily.
	 * Probably it is not the best value.
	 * 
	 * For example, if time limit is set to 3600s, this will be 360s
	 */
	public void setMaxTimeForRelaxationImprovement(int MaxTime) {
		if(MaxTime != -1) { // if time limit is provided by user
			
//			this.maxTimeForRelaxationImprovement = tilim/10.0;
			this.maxTimeForRelaxationImprovement = MaxTime;
        }
        else {
            this.maxTimeForRelaxationImprovement = DEFAULT_MAX_TIME_FOR_RELAXATION_IMPROVEMENT;
        }

//		} else if(tilim > 0 && MaxTime == -1) { // if time limit is provided by user
//			
//			this.maxTimeForRelaxationImprovement = DEFAULT_MAX_TIME_FOR_RELAXATION_IMPROVEMENT;
//
//		} else {
//			this.maxTimeForRelaxationImprovement = DEFAULT_MAX_TIME_FOR_RELAXATION_IMPROVEMENT;
//		}
		
	}
	
	
	
	
	/**
	 * 
	 * When time limit is set, this method decides how much time is dedicated
	 *  for Root Relaxation and Branch&Bound.
	 * For example, if time limit is set to 3600s, this will be 360s
	 * 
	 */
	public void setTimeLimitForCuttingPlanes() {	
		double cplex_min_time = tilim/10.0; 
		this.cptilim = tilim - cplex_min_time;
	}
	
	
	
	/**
	 * 
	 * It specifies the current problem as Root Relaxation.
	 * 'Root' means the main/master problem (i.e. without sub-problems as it is in Branch&Bound)
	 * 
	 */
	public void declareCplexAsRootRelaxation() {
		
		try {
			// '2' for Root Relaxation
			formulation.getCplex().iloCplex.setParam(IloCplex.IntParam.RootAlg, 2);
		} catch (IloException e1) {
			e1.printStackTrace();
		}

	}
	
	
	
	/**
	 * 
	 * It calls the 'solve' method of CPLEX. Since, this method is called after
	 *  the 'declareCplexAsRootRelaxation' method, Root Relaxation is specified.
	 *   So, this solve() is equivalent to solve Root Relaxation
	 * 
	 */
	public void solveRootRelaxation() throws IloException {
		formulation.getCplex().solve();
	}
	

	
	/**
	 * Checks if it is time to remove untight cuts.
	 * 
	 * @return TRUE if it is time to remove untight cuts.
	 */
	public boolean checkIfTimeToRemoveUntightInequalities() {
		
		boolean isOk = false;
		
		double cutsTime = formulation.getCplex().getCplexTime()+lastRemovingCutsTime;
		if(cutsTime > minimalTimeBeforeRemovingUntightCuts){
			isOk = true;
		}

		return isOk;
	}
	
	
	
	/**
	 * It determines untight inequalities and stores it in an array.
	 * 
	 * @return toRemove  array of inequalities to be removed
	 */
	public ArrayList<IloRange> determineUntightInequalities(){
		
		// =====================================================================
		
		ArrayList<IloRange> toRemove= new ArrayList<IloRange>();

		for(CP_Separation<?> si : sep)
			for(int i = si.addedIneq.size()-1 ; i >= 0 ; --i){
				AbstractInequality<?> ai = si.addedIneq.get(i);
				if(!ai.isTight(formulation.variableGetter())){
					toRemove.add(ai.ilorange);
					si.remove(i);
				}
			}
		
		return toRemove;
	}
	
	
	
	/**
	 * It removes inequalities (already added in previous iterations) from CPLEX model
	 * 
	 * @param toRemove  array of inequalities to be removed from CPLEX model
	 */
	public void removeUntightInequalities(ArrayList<IloRange> toRemove) {
		for(IloRange i: toRemove)
			formulation.getCplex().remove(i);
	}
	
	
	
	
	/**
	 * It obtains the method id of the Triangle Separation method.
	 * It is called during manual lazy callback in Root Relaxation
	 * ==> 'checkLazilyIfIntegerSolutionValid()'
	 * 
	 * @return methodId  the corresponding index
	 */
	public int getTriangleSeparationMethodId() {

		Iterator<CP_Separation<?>> it = sep.iterator();
		
		int methodId = -1;
		for(int i=0; i< sep.size(); i++){
			if(sep.get(i).se.name.equals("triangle iterative")) {
				methodId = i;
				break;
			}
		}
		
		return methodId;
	}
	
	
	
	/**
	 *  It generates "lazily" triangle inequalities, and checks if one of them is
	 *   not valid for the current integer solution. We would say that this is
	 *    sort of manual lazy callback, because this method is called whenever
	 *     a feasible solution is found
	 *  
	 *  @param sepId  method id of Triangle inequalities
	 *  @return violated inequalities if the solution valid. Otherwise, an empty array
	 */
	public ArrayList<AbstractInequality<? extends IFormulation>> checkLazilyIfIntegerSolutionValid(int sepId) {		
		ArrayList<AbstractInequality<? extends IFormulation>> toAdd = new ArrayList<>();
				
		cpresult.cpSepItTriangleNb++;
		double begin_timer = formulation.getCplex().getCplexTime();
		
		CP_Separation<? extends IFormulation> cp = sep.get(sepId);
		cp.usedAtThisIteration = true;			
		ArrayList<AbstractInequality<? extends IFormulation>> ineqs = cp.se.separate();

		if(ineqs.size() > 0){ // if there is any cut
			toAdd.addAll(ineqs);	
			
			if(this.verbose)
				System.out.print(" (" + ineqs.size() + ")");		
		}
		
		double end_timer = formulation.getCplex().getCplexTime();
		cpresult.cpSepTriangleTime += (end_timer-begin_timer);
		
		return toAdd;
	}
	
	
	
	/**
	 * 
	 * This method aims at loading an initial int solution, probablyt retreived from ILS or another meta-heuristic
	 * 
	 * @return new_mip  a feasible solution
	 * @throws IloException 
	 */
	public SolutionManager LoadInitialIntegerSolution() throws IloException {
				
		/*
		 When you provide a MIP start as data, CPLEX processes it before starting branch
		 and cut during an optimization. If one or more of the MIP starts define a solution,
		 CPLEX installs the best of these solutions as the incumbent solution. Having an
		 incumbent from the very beginning of branch and cut allows CPLEX to eliminate
		 portions of the search space and thus may result in smaller branch-and-cut trees.
		*/
		AbstractMIPStartGenerate mipStartGenerator = initializeMIPStartGenerator();
		SolutionManager new_mip = mipStartGenerator.loadIntSolution(this.initialPartitionMembership);
		
		return new_mip;
	}

	
	
	
	
	/**
	 * 
	 * This method aims at running a 'Primal heuristic' method to find a feasible solution.
	 * 
	 * @return new_mip  a feasible solution
	 */
	public SolutionManager findIntegerSolutionWithPrimalHeuristic() {
				
		if(this.verbose){
			System.out.println("-------  If the solution is not integer !!! ----------");
			System.out.println("------- cpresult.cp_iteration: " + cpresult.cp_iteration);
		}
		
		/*
		 When you provide a MIP start as data, CPLEX processes it before starting branch
		 and cut during an optimization. If one or more of the MIP starts define a solution,
		 CPLEX installs the best of these solutions as the incumbent solution. Having an
		 incumbent from the very beginning of branch and cut allows CPLEX to eliminate
		 portions of the search space and thus may result in smaller branch-and-cut trees.
		*/
		double begin_timer = formulation.getCplex().getCplexTime();
		AbstractMIPStartGenerate mipStartGenerator = initializeMIPStartGenerator();
		double end_timer = formulation.getCplex().getCplexTime();
		cpresult.cpRoundingTime += (end_timer-begin_timer);
		
		if(this.verbose)
			System.out.println("-- cpRoundingTime: " + (end_timer-begin_timer) + "s");
		
		SolutionManager new_mip = null;
		
		try {
			new_mip = mipStartGenerator.generateMIPStart();

		} catch (IloException e) {
			e.printStackTrace();
		}
		
		return new_mip;

	}

	
	
	/**
	 * It generates 'MAX_CUT' number of violated inequalities for each or some
	 *  (according to 'isQuick' parameter) cut generation family.
	 * if 'isQuick' is set to false, cut generation stops when a cut family
	 *  generates any cut (we wait its termination, then it stops)
	 * If 'isQuick' is set to true, cuts are generated for each cut family.
	 * When time limit used, time may be up (i.e. remaining time = 0) in this method.
	 * In this case, the methods stops after the current cut generation
	 *  terminates its job.
	 * 
	 * @param remainingTime it is used if time limit is set 
	 * @return toAdd  an array containing all violated inequalities
	 */
	public ArrayList<AbstractInequality<? extends IFormulation>> generateUserCuts(double remainingTime) {
		boolean cutFound = false;
		cpresult.cpSepItCutNb++;
		double begin_timer = formulation.getCplex().getCplexTime();
		int methodId = 0;
		
		ArrayList<AbstractInequality<? extends IFormulation>> toAdd = new ArrayList<>();

		/* While no cut is found and if there is still time */
		while((remainingTime > 0 || tilim == -1.0) && methodId < sep.size()){

			try {

				CP_Separation<?> sep_i = sep.get(methodId);

				/* If the separation method has not yet been used in this iteration */
				//						if(!sep_i.usedAtThisIteration){
				if(!sep_i.usedAtThisIteration && (!cutFound || sep_i.isQuick)){

					ArrayList<AbstractInequality<?>> r = sep_i.se.separate();

					if(r.size() > 0){
						tagInequality(r, methodId);
						toAdd.addAll(r);
						cutFound = true;

						if(this.verbose)
							System.out.print(" : " + sep.get(methodId).se.name);
						
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			if(tilim > 0) { // if time limit is provided by user
				/* Remaining cutting plane time */
				double cpTime = formulation.getCplex().getCplexTime() + cpresult.cp_time;
				remainingTime = cptilim - (cpTime);
			}
			methodId++;
		}
		
		double end_timer = formulation.getCplex().getCplexTime();
		cpresult.cpSepCutTime += (end_timer-begin_timer);
		
		return toAdd;
		
	}



	public abstract void RegisterLPmodelAfterCP() throws VariableListerException, IloException ;
		
	
	// public abstract void fixSelEdgeVarsToZero(ArrayList<Edge> filtEdges);
	
	
}

