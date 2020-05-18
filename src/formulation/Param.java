package formulation;

import cplex.Cplex;

public class Param {
	
	/**
	 * Value added to all the weight of each edge in the graph
	 *  (enable to quickly change the value of all the edges of a graph
	 *   without generating a new input file)
	 */
	public double gapDiss = 0.0;

	/** Display cplex output */
	public boolean cplexOutput = false;
	
	/** Use cplex primal dual heuristic */
	public boolean useCplexPrimalDual = true;
	
	/** Use cplex automatic cuts */
	public boolean useCplexAutoCuts = true;
	
	/** Cplex time limit */
	public double tilim = -1;
	
	public boolean isInt = true;
	
	/** Input file which contains the weight value of the considered graph */
	public String inputFile = null;
	
	public Cplex cplex;
	
	public Param(String inputFile, Cplex cplex){
		this.inputFile = inputFile;
		this.cplex = cplex;
	}
	
	public Param(Param p){
		gapDiss = p.gapDiss;
		cplexOutput = p.cplexOutput;
		useCplexPrimalDual = p.useCplexPrimalDual;
		useCplexAutoCuts = p.useCplexAutoCuts;
		tilim = p.tilim;
		isInt = p.isInt;
		inputFile = p.inputFile;
		cplex = p.cplex;
	}
}
