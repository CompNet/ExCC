package formulation;

import cplex.Cplex;

public class PartitionParam extends Param{
	
	public boolean useNN_1 = false;
	
	/** Maximal number of nodes read into the input file */
	public int maxNumberOfNodes = -1;
	
	
	public PartitionParam(String inputFile, Cplex cplex){
		super(inputFile, cplex);
	}

	public PartitionParam(PartitionParam p){
		super(p);
		
		useNN_1 = p.useNN_1;
		maxNumberOfNodes = p.maxNumberOfNodes;
	}
}
