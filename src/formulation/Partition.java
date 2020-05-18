package formulation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import cplex.Cplex;
import formulation.interfaces.IFEdgeV;
import formulation.interfaces.IFEdgeW;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.UnknownObjectException;
import variable.CplexVariableGetter;


public abstract class Partition implements IFEdgeV, IFEdgeW{

	String outputDirPath = ".";
	
	CplexVariableGetter cvg;
	
	public PartitionParam p;

	/* Number of points to cluster */
	public int n;

	public double[][] d;
	
	public Partition(PartitionParam p) {
		this.p = p;
	}

	
	public static Partition createPartition(Param param) throws IloException{

		Partition p = null;


		if(param instanceof MyParam){
			MyParam rp = (MyParam) param;
			p = new MyPartition(rp);
		}

		p.cvg = new CplexVariableGetter(p.getCplex());
		
		return p;
	}

	
	public List<TreeSet<Integer>> clusters; // output
	
	/**
	 * Edges variables Array of n elements. v_edge[0] is empty v_edge[i]
	 * contains an array of i variables (which corresponds to x0i, ..., xi-1,i)
	 * for i=1..n-1
	 */
	public IloNumVar[][] v_edge;
		
	/**
	 * Read a txt file which contains a low triangular matrix. This matrix
	 * represent the dissimilarity between the elements to partition:
	 * 
	 * - the line j must contains j double (the first one is the dissimilarity
	 * between the nodes 0 and j-1, the last one is the dissimilarity between
	 * j-1 and itself (should be zero))
	 * 
	 * @param dissimilarity_file
	 * @param max_number_of_nodes Maximum number of line read in the file 
	 * (i.e. maximum number of nodes considered in the problem) ; -1 if there is no limit
	 * @throws InvalidInputFileException
	 */
	static double[][] readDissimilarityInputFile(PartitionParam param)
			{

		double[][] d = null;
		
		ArrayList<String[]> al_dissimilarity = new ArrayList<String[]>();
		int j = 1;

		File f = new File(param.inputFile);
		if (!f.exists()) {
			System.err.println("The input file '" + param.inputFile
					+ "' does not exist.");
		}

		InputStream ips;
		try {
			ips = new FileInputStream(param.inputFile);
			InputStreamReader ipsr = new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			
			if(param.maxNumberOfNodes == -1)
				param.maxNumberOfNodes = Integer.MAX_VALUE;

			/* For each line */
			while ((line = br.readLine()) != null && j < param.maxNumberOfNodes) {
				String[] temp = line.split(" ");

				/* If the number of elements is valid */
				if (temp.length >= j)
					al_dissimilarity.add(j - 1, temp);
				else{
					System.err
					.println("Partition.java: Invalid dissimilarity input file." 
							+ "Error the line \""
							+ line
							+ "\" should contain at least "
							+ j
							+ " double separated by spaces.");
					System.exit(0);
				}

				++j;
			}

			br.close();

			int n = j;
	
			if (n == 0){
				System.err.println("The input file is empty.");
				System.exit(0);
			}
			else {
				d = new double[n][];

				d[0] = new double[n];
				/*
				 * for each line with non diagonal elements (i.e. line 1 to n-1
				 * of al_dissimilarity <-> line 2 to n of the file)
				 */
			
				for (j = 0; j < n-1; ++j) {

					String[] currentLine = al_dissimilarity.get(j);
					
					d[j+1] = new double[n];

					for (int i = 0; i <= j; ++i){
						d[j+1][i] = Double.parseDouble(currentLine[i])+ param.gapDiss;
						d[i][j+1] = d[j+1][i];
					}
				}
				
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return d;
	}
	
	@Override
	public double edgeWeight(int i, int j) {
		return d[i][j];
	}
	
	@Override
	public IloNumVar edgeVar(int i, int j) throws IloException {
		return v_edge[i][j];
	}
	
	
	
	
	public void setOutputDirPath(String _outputDirPath){
		this.outputDirPath = _outputDirPath;
	}
	
	public String getOutputDirPath(){
		return(this.outputDirPath);
	}
	
	
	

	/**
	 * Display the value of the edge variables which are equal to 1
	 * 
	 * @param numberOfElementsByLine
	 *            Number of variables displayed by line
	 * @throws UnknownObjectException
	 * @throws IloException
	 */
	public void displayEdgeVariables(int numberOfElementsByLine)
			throws UnknownObjectException, IloException {

		int i = 1; // Index of the edge first node
		int j = 0; // Index of the edge second node

		/* While all the edges variables have not been displayed */
		while (i != n) {
			
			int k = 0; // Id of the last element displayed on the line

			/*
			 * While the line is not over (i.e. while it does not contain l
			 * elements or reach the last edge variable)
			 */
			while (k < numberOfElementsByLine && i != n) {
	
					double value = cvg.getValue(v_edge[i][j]);

					if(value > 1E-4){
						System.out.print("x" + i + "-" + j + "(" + value + ")\t\t");
						++k;
					}
							

				if (j != i - 1)
					++j;
				else {
					++i;
					j = 0;
				}

			}

			System.out.println(" ");
		}

	}
	

	public int n() {
		return n;
	}



	
	@Override
	public Cplex getCplex() {
		return p.cplex;
	}

	@Override
	public CplexVariableGetter variableGetter() {
		return cvg;
	}
	
	
	
	
	public void setLogPath(String s) throws FileNotFoundException{
		OutputStream output = new FileOutputStream(s);
		getCplex().iloCplex.setOut(output);
	}
	
	
	
	// =========================================================================
	

	public List<TreeSet<Integer>> retreiveClusters(int solutionNo){
		
		List<TreeSet<Integer>> clusters = new ArrayList<TreeSet<Integer>>();
		
		/* Check if each point is a cluster of size > 1 */
		boolean[] tb = new boolean[n];

		/* For each edge */
		for(int i = 1 ; i < n ; ++i)
			for(int j = 0 ; j < i ; ++j){
				
				double value;
				try {
					if(solutionNo > 0) {
						IloNumVar[] currX = {v_edge[i][j]}; // an array with one element
						/* source: https://www.ibm.com/support/knowledgecenter/
											SSSA5P_12.8.0/ilog.odms.cplex.help/CPLEX/
											UsrMan/topics/APIs/Java/12_access_soln.html */
						double[] xsol = getCplex().iloCplex.getValues(currX, solutionNo);
						value = xsol[0];
					} else {
						value = getCplex().iloCplex.getValue(v_edge[i][j]);
					}
				} catch (UnknownObjectException e) {
					value = 0.0;
					e.printStackTrace();
				} catch (IloException e) {
					value = 0.0;
					e.printStackTrace();
				}
				
				/* If the edge is in a cluster */
				if(value > 1E-4){
					
					boolean clusterFound = false;
					tb[i] = true;
					tb[j] = true;
					
					for(TreeSet<Integer> cluster : clusters){
						
						if(!clusterFound){
							if(cluster.contains(i) || cluster.contains(j)){
								cluster.add(i);
								cluster.add(j);
								clusterFound = true;
							}
							
						}
					}
					
					/* If no cluster contains node i or node j, create a new one */
					if(!clusterFound){
						TreeSet<Integer> newCluster = new TreeSet<Integer>();
						newCluster.add(i);
						newCluster.add(j);
						clusters.add(newCluster);
					}
						
				}
			}
		
		/* Add the clusters reduced to one point */
		for(int i = 0 ; i < n ; i++)
			if(!tb[i]){
				TreeSet<Integer> newCluster = new TreeSet<>();
				newCluster.add(i);
				clusters.add(newCluster);
			}
				
		
		//before returning the result, set the "clusters" object
		setClusters(clusters);

		
		return(clusters);
	}

	
	
	
	public List<TreeSet<Integer>> retreiveClusters(){
		// -1: solution no is not specified
		return( retreiveClusters(-1) );
	}
	
	
	
	public List<TreeSet<Integer>> getClusters() {
		return(this.clusters);
	}
	
	
	public void setClusters(List<TreeSet<Integer>> clusters) {
		this.clusters = new ArrayList<TreeSet<Integer>>();
		
		for(TreeSet<Integer> cluster : clusters) {
			TreeSet<Integer> newCluster = new TreeSet<Integer>();
			// cloning tree into clinetree
			newCluster = (TreeSet)cluster.clone();
			this.clusters.add(newCluster);
		}
	}
	
	
	/**
	 * Display clusters
	 * 
	 */
	public void displayClusters(){
		clusters = getClusters();
		
		for(TreeSet<Integer> cluster : clusters)
			System.out.println(cluster);
	}
	
	
	/**
	 * Write a solution into file
	 * 
	 */
	public void writeClusters(String fileName){
		//String filepath = getOutputDirPath() + "/" + fileName;
		clusters = getClusters();
		
		String content = "";
		for(TreeSet<Integer> cluster : clusters) {
			if(!content.equals(""))
				content += "\n";
			content += cluster.toString();
		}
			
		try{
			 BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			 writer.write(content);
			 writer.close();
		 } catch(IOException ioe){
		     System.out.print("Erreur in writing output file: ");
		     ioe.printStackTrace();
		 }
	}
	
	
}
