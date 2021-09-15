package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import callback.branch_callback.BranchCbCountNodes;
import callback.branch_callback.BranchDisplayInformations;
import callback.branch_callback.BranchFirstNearZero;
import callback.branch_callback.BranchFurtherFromInteger;
import callback.branch_callback.BranchTriangleConstraints;
import cplex.Cplex;
import cutting_plane.CP;
import formulation.MyPartition;
import formulation.Partition;
import formulation.Edge;
import formulation.MyParam;
import formulation.MyParam.Triangle;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;
import mipstart.AbstractMIPStartGenerate;
import mipstart.PrimalHeuristicRounding;
import mipstart.SolutionManager;
import variable.VariableLister;
import variable.VariableLister.VariableListerException;


/**
* The ExCC program aims at solving Correlation Clustering problem.
* There are two strategies: obtaining a single vs. all optimal solution(s).
* When all optimal partitions of a given signed graph are required, we call this method "OneTreeCC".
* Each mentioned strategy above can be performed in two different ways:
* 	- Branch&Bound (B&B)
* 	- Branch&Cut (B&C)
* In both methods, we first create the ILP model based on the vertex pair formulation, where there are (n*(n-1)/2) variables.
* In B&B, we just let Cplex solve it with B&B.
* In B&C, we first perform a root relaxation and find violated valid inequalities through a Cutting Plane (CP) method. 
* 	After have found sufficient valid inequalities, we insert them into the model and then let Cplex solve it, as in B&B.
* In general, B&C is much preferable than B&B, since it drastically reduces the execution time.
* 
* This program also allows to solve the CC problem by importing a LP file, where a ILP model is already recorded in a previous run.
* 	The advantage of doing it is that if we provide ExCC with a ILP model containing violated valid inequalities found during a CP method, 
* 	then it amounts to skip the CP phase of the B&C method. So, it directly proceeds to the second phase: branching. This allows to gain a considerable amount of time.
* 
* 
* Some references for the CC problem:
* <ul>
* <li> Cartwright D, Harary F (1956) Structural balance:
* 	 a generalization of Heider’s theory. Psychol Rev 63:277-293 <\li>
* <li> Heider F (1946) Attitudes and cognitive organization. J Psychol 21:107-112 <\li>
* <li> N. Arınık & R. Figueiredo & V. Labatut Efficient Enumeration of Correlation Clustering Optimal Solution Space (submitted). Journal of Global Optimization (2021). <\li>
* <\lu>
*/
public class MainExCC {

	
	static String tempFile = "temp.txt";
	
	/**
	 * 
	 * Input parameters:
	 * <ul>
	 * <li> inFile (String): Input file path. </li>
	 * <li> outDir (String): Output directory path. Default "." 
	 * 		(i.e. the current directory). </li>
	 * <li> cp (Boolean): True if B&C (i.e. Cutting Plane approach) will be used.
	 * 		 Default false. </li>
	 * <li> enumAll (Boolean): True if enumerating all optimal solutions is desired. Default false. </li>
	 * <li> tilim (Integer): Time limit in seconds for the whole program. </li>
	 * <li> tilimForEnumAll (Integer): Time limit in seconds for the second phase of the OneTreeCC method. 
	 * 			This is useful when doing a benchmarking with EnumCC. </li>
	 * <li> solLim (Integer): max number of optimal solutions to be discovered when OneTreeCC is called.
	 * 						This can be useful when there are a huge number of optimal solutions, e.g. 50,000. </li>
	 * <li> MaxTimeForRelaxationImprovement (Integer): Max time limit for relaxation improvement in the first phase of the Cutting Plane method.
	 *  				This is independent of the time limit. If there is no considerable improvement for X seconds, it stops and passes to the 2nd phase, which is branching.
	 *  				This parameter can be a considerable impact on the resolution time. For medium-sized instances (e.g. 50,60), 
	 *  				it might be beneficial to increase the value of this parameter (e.g. 1800 or 3600s). The default value is 600s.	
	 *  				Moreover, it might be beneficial to decrease the default value to 30s or 60s if the graph is easy to solve or 
	 *  				the number of vertices is below 28.				
	 * </li>
	 * <li> lazyInBB (Boolean): Used only for B&C method. True if adding lazily triangle constraints (i.e. lazy callback approach) in the branching phase. If it is False,
	 * 						 the whole set of triangle constraints is added before branching. Based on our experiments, we can say that
	 * 						the lazy callback approach is not preferable over the default approach. Default false. </li>
	 * <li> userCutInBB (Boolean): Used only for B&C method. True if adding user cuts during the branching phase of the B&C method or in B&B method is desired.
	 * 		 Based on our experiments, we can say that it does not yield any advantage, and it might even slow down the optimization process. Default false. </li>
	 * <li> nbThread (Integer): Default value is the max number of CPU minus 1.
	 * <li> verbose (Boolean): Default value is True. When True, it enables to display log outputs during the Cutting Plane approach.
	 * <li> initMembershipFilePath (String): Default value is "". It allows to import an already known solution into the optimization process. 
	 * 										Since we solve a minimization problem, the imbalance value of the imported solution is served as the upper bound.
	 * 										It is usually beneficial to use this option, when we possess some good-quality heuristics. </li>
	 * <li> LPFilePath (String): Default value is "". It allows to import a LP file, corresponding to a ILP formulation. Remark: such a file can be obtained through Cplex by doing 'exportModel'. </li>
	 * <li> onlyFractionalSolution (boolean): Default value is False. It allows to run only the cutting plane method in B&C, 
	 * 											so the program does not proceed to the branching phase </li>
	 * <li> fractionalSolutionGapPropValue (Double): It allows to limit the gap value to some proportion value during the cutting plane method in B&C.
	 * 												it can be useful when we solve an easy graph. Hence, we do not spent much time by obtaining very tiny improvement 
	 * 												when the solution is already close to optimality. </li>
	 * </ul>
	 * 
	 * When to use B&B approach, set 'cp' to false. Otherwise, true.
	 * 
	 * Example for B&B:
	 * <pre>
	 * {@code
	 * ant clean compile jar
	 * ant -DinFile=data/signed.G -DoutDir=out/signed -Dcp=false -DenumAll=false run
	 * 
	 * java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio201/cplex/bin/x86-64_linux/ 
	 * 	-DinFile=data/net.G -DoutDir=out/net -Dcp=false -Dtilim=3600 -DlazyInBB=false -DuserCutInBB=false 
	 * 	-DenumAll=false -DinitMembershipFilePath="" -DnbThread=2 -Dverbose=true -DLPFilePath="" 
	 * 	-DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 
	 * 	-DMaxTimeForRelaxationImprovement=20 -DtilimForEnumAll=-1 -DsolLim=1 -jar exe/ExCC.jar
	 * }
	 * </pre>
	 * 
	 * Example for B&C:
	 * <pre>
	 * {@code
	 * ant clean compile jar
	 * 
	 * ant -DinFile=data/signed.G -DoutDir=out/signed -Dcp=true -Dtilim=3600 
	 * 	 -DlazyInBB=false -DuserCutInBB=false -DenumAll=false -Dverbose=true run
	 * 
	 * java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio201/cplex/bin/x86-64_linux/ 
	 * 	-DinFile=data/net.G -DoutDir=out/net -Dcp=true -Dtilim=3600 -DlazyInBB=false -DuserCutInBB=false 
	 * 	-DenumAll=false -DinitMembershipFilePath="" -DnbThread=2 -Dverbose=true -DLPFilePath="" 
	 * 	-DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 
	 * 	-DMaxTimeForRelaxationImprovement=20 -DtilimForEnumAll=-1 -DsolLim=1 -jar exe/ExCC.jar
	 * 
	 * }
	 * </pre>
	 * Example for enumerating all optimal solutions through B&C:
	 * <pre>
	 * {@code
	 * ant clean compile jar
	 * 
	 * ant -DinFile=data/signed.G -DoutDir=out/signed -Dcp=true -Dtilim=3600 
	 * 	 -DlazyInBB=false -DuserCutInBB=false -DenumAll=true -Dverbose=true run
	 * 
	 * java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio201/cplex/bin/x86-64_linux/ 
	 * 	-DinFile=data/net.G -DoutDir=out/net -Dcp=true -Dtilim=3600 -DlazyInBB=false -DuserCutInBB=false 
	 * 	-DenumAll=true -DinitMembershipFilePath="" -DnbThread=2 -Dverbose=true -DLPFilePath="" 
	 * 	-DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 
	 * 	-DMaxTimeForRelaxationImprovement=-1 -DtilimForEnumAll=-1 -DsolLim=-1 -jar exe/ExCC.jar
	 * 
	 * }
	 * </pre>
	 * 
	 * 
	 * @param args  (Not used in this program. Instead, user parameters are obtained
	 * 	 through ant properties. See the build.xml for more details).
	 * @throws VariableListerException 
	 * @throws IOException 
	 * 
	 * @throws FileNotFoundException.
	 * @throws UnknownObjectException. 
	 * @throws IloException.
	 */
	public static void main(String[] args) throws UnknownObjectException, IloException, VariableListerException, IOException {
		/* TODO When the size of the input graph is > 1000, change the hash function
		 * 	 in Edge class in formulation/Edge.java */
		/* TODO we may use 'localAdd()' when adding user cuts to improve performance */
		
		/* WARNING In cutting plane approach, it takes longer time than usual
		 * 	 as there is a time limit after the last improved relaxation */

		int tilim = -1;
		int tilimForEnumAll = -1;
		int solLim = -1;
		boolean lazyInBB = false;
		boolean userCutInBB = false;
		String inputFilePath = "";
		String outputDirPath = ".";
		boolean isCP = false;
		boolean isEnumAll = false;
		int MaxTimeForRelaxationImprovement = -1;
		boolean verbose = true;
		String initMembershipFilePath = "";
		String LPFilePath = "";
		int nbThread = 1;
		boolean onlyFractionalSolution = false;
		double fractionalSolutionGapPropValue = -1.0; // 0.01 => 1 percent gap
		
		
		if( !System.getProperty("inFile").equals("${inFile}") )
			inputFilePath = System.getProperty("inFile");
		else {
			System.out.println("input file is not specified. Exit");
			return;
		}
		
		if( !System.getProperty("outDir").equals("${outDir}") )
			outputDirPath = System.getProperty("outDir");
		if( !System.getProperty("cp").equals("${cp}") )
			isCP = Boolean.valueOf(System.getProperty("cp"));
		if( !System.getProperty("enumAll").equals("${enumAll}") )
			isEnumAll = Boolean.valueOf(System.getProperty("enumAll"));
		
		if( !System.getProperty("tilim").equals("${tilim}") )
			tilim = Integer.parseInt(System.getProperty("tilim"));
		
		if( !System.getProperty("tilimForEnumAll").equals("${tilimForEnumAll}") )
			tilimForEnumAll = Integer.parseInt(System.getProperty("tilimForEnumAll"));
		
		if( !System.getProperty("solLim").equals("${solLim}") )
			solLim = Integer.parseInt(System.getProperty("solLim"));
		
		
		// Those 3 options are available with cutting plane approach

		if( isCP && !System.getProperty("MaxTimeForRelaxationImprovement").equals("${MaxTimeForRelaxationImprovement}") )
			MaxTimeForRelaxationImprovement = Integer.parseInt(System.getProperty("MaxTimeForRelaxationImprovement"));
		
		
		if( !isEnumAll && isCP && !System.getProperty("lazyInBB").equals("${lazyInBB}") )
			lazyInBB = Boolean.valueOf(System.getProperty("lazyInBB"));
		if( !isEnumAll && isCP && !System.getProperty("userCutInBB").equals("${userCutInBB}") )
			userCutInBB = Boolean.valueOf(System.getProperty("userCutInBB"));
	
		if( isCP && !System.getProperty("verbose").equals("${verbose}") )
			verbose = Boolean.valueOf(System.getProperty("verbose"));
		
		System.out.println(System.getProperty("initMembershipFilePath"));
		if( !System.getProperty("initMembershipFilePath").equals("${initMembershipFilePath}") ) // it is not usefull
			initMembershipFilePath = System.getProperty("initMembershipFilePath");

		
		if( !System.getProperty("LPFilePath").equals("${LPFilePath}") )
			LPFilePath = System.getProperty("LPFilePath");
		else {
			System.out.println("LPFilePath file is not specified.");
		}
		
		
		if( !System.getProperty("nbThread").equals("${nbThread}") )
			nbThread = Integer.parseInt(System.getProperty("nbThread"));


		if( !System.getProperty("onlyFractionalSolution").equals("${onlyFractionalSolution}") )
			onlyFractionalSolution = Boolean.valueOf(System.getProperty("onlyFractionalSolution"));
		else {
			System.out.println("onlyFractionalSolution is not specified. The default value is false, i.e. performing B&B after cutting plane phase");
		}
		
		if( !System.getProperty("fractionalSolutionGapPropValue").equals("${fractionalSolutionGapPropValue}") )
			fractionalSolutionGapPropValue = Double.parseDouble(System.getProperty("fractionalSolutionGapPropValue"));
		else {
			System.out.println("fractionalSolutionGapPropValue is not specified. The default value is -1.0");
		}
		
		System.out.println("===============================================");
		System.out.println("inputFilePath: " + inputFilePath);
		System.out.println("outputDirPath: " + outputDirPath);
		System.out.println("isCP: " + isCP);
		System.out.println("isEnumAll: " + isEnumAll);
		System.out.println("tilim: " + tilim + "s");
		System.out.println("tilimForEnumAll: " + tilimForEnumAll + "s");
		System.out.println("solLim: " + solLim);
		System.out.println("MaxTimeForRelaxationImprovement: " + MaxTimeForRelaxationImprovement + "s");
		System.out.println("lazyInBB: " + lazyInBB);
		System.out.println("userCutInBB: " + userCutInBB);
		System.out.println("nbThread: " + nbThread);
		System.out.println("verbose: " + verbose);
		System.out.println("initMembershipFilePath: " + initMembershipFilePath);
		System.out.println("LPFilePath: " + LPFilePath);
		System.out.println("onlyFractionalSolution: " + onlyFractionalSolution);
		System.out.println("fractionalSolutionGapPropValue: " + fractionalSolutionGapPropValue);
		System.out.println("===============================================");

		
		createTempFileFromInput(inputFilePath);
		int n = 0; // number of nodes
		
		// ------------------------------------------
		
		int[] initMembership = null;
		if(!initMembershipFilePath.equals("")){
			// initMembership
			
			try {
				n = getNbLinesInFile(initMembershipFilePath);
				initMembership = readMembership(initMembershipFilePath, n);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		// -----------------------------------------

		MyParam myp = null;
		Cplex cplex = new Cplex(); // start
		cplex.setParam(IntParam.ClockType, 2);

		
		if(isCP) { // Cutting Plane approach
			
			// =================================================================
			
			if(lazyInBB)
				myp = new MyParam(tempFile, cplex, Triangle.USE_LAZY_IN_BC_ONLY, userCutInBB);
			else
				myp = new MyParam(tempFile, cplex, Triangle.USE_IN_BC_ONLY, userCutInBB);
		
//			myp.cplexOutput = false;
			myp.useCplexPrimalDual = true;
			myp.useCplexAutoCuts = true;
			myp.tilim = tilim;
			myp.userCutInBB = userCutInBB;
			
			int MAXCUT = 500;
			int minimalTimeBeforeRemovingUntightCuts = 1;
			int modFindIntSolution = 5;
			
			
			/* 'reordering' is not important if 'isQuick' is set to TRUE
			 *  for Separation methods in cutting planes */
			boolean reordering = false;
			CP cp = new CP(myp, MAXCUT, minimalTimeBeforeRemovingUntightCuts,
					modFindIntSolution, reordering, tilim, tilimForEnumAll, solLim, outputDirPath, MaxTimeForRelaxationImprovement,
					isEnumAll, verbose, initMembership, onlyFractionalSolution, fractionalSolutionGapPropValue);

			cp.solve();
//			cp.cpresult.log();
			
			// end =============================================================

		} else { // B&B approach
			System.out.println("B&B approach");			
			// =================================================================
			
			
			if(!LPFilePath.equals("") && !initMembershipFilePath.equals("") && initMembership != null){
				cplex.iloCplex.importModel(LPFilePath);
				MyPartition p;
				myp = new MyParam(tempFile, cplex, Triangle.USE_IN_BC_ONLY, userCutInBB);
				myp.setStatusReadLPModelFromFile(true);
				myp.tilim = tilim;
				p = (MyPartition) Partition.createPartition(myp);
				p.setLogPath(outputDirPath + "/logcplex.txt");
				

				AbstractMIPStartGenerate mipStartGenerator = new PrimalHeuristicRounding(p);
				SolutionManager mipStart = mipStartGenerator.loadIntSolution(initMembership);
				mipStart.setVar();
				p.getCplex().iloCplex.addMIPStart(mipStart.var, mipStart.val);
				////p.getCplex().setParam(IloCplex.DoubleParam.TiLim, 1000);
				p.setOutputDirPath(outputDirPath); // it writes all solutions into files

				
				if(isEnumAll) { // Enumerate all optimal solutions
					p.populate(tilim, tilimForEnumAll, solLim);
				}
				else {
					p.solve();
					
					p.retreiveClusters(); // 
					p.writeClusters(outputDirPath + "/sol0.txt");	
				}
				
				
			} else {
			
				MyPartition p;
				myp = new MyParam(tempFile, cplex, Triangle.USE, userCutInBB);
				myp.tilim = tilim;
				p = (MyPartition) Partition.createPartition(myp);
				
				p.setLogPath(outputDirPath + "/logcplex.txt");
				p.setOutputDirPath(outputDirPath); // it writes all solutions into files
				
				p.getCplex().setParam(IloCplex.Param.Threads, nbThread);

				
				
				if(!initMembershipFilePath.equals("") && initMembership != null) {
					AbstractMIPStartGenerate mipStartGenerator = new PrimalHeuristicRounding(p);
					SolutionManager mipStart = mipStartGenerator.loadIntSolution(initMembership);
					mipStart.setVar();
					p.getCplex().iloCplex.addMIPStart(mipStart.var, mipStart.val);
				}
				
	
				if(isEnumAll) { // Enumerate all optimal solutions
					if(verbose)
						System.out.println("BEFORE POPULATE() in main");
					
					p.populate(tilim, tilimForEnumAll, solLim);
					
					if(verbose)
						System.out.println("AFTER POPULATE() in main");
					
				} 
				else { // Obtain only one optimal solution
						p.solve();
						p.retreiveClusters(); // 
						p.writeClusters(outputDirPath + "/sol0.txt");	
				}
	
				
	//			p.computeObjectiveValueFromSolution();
	
	
				// end =============================================================

			
			}
		}
		
		cplex.end(); // end

	}

	
	
	
	
	/**
	 * This method reads input graph file, then stocks it as weighted adjacency matrix, 
	 * finally writes the graph in lower triangle format into a temp file.
	 * 
	 * @param filename  input graph filename
	 * @return 
	 */
	private static void createTempFileFromInput(String fileName) {
		
		  double[][] weightedAdjMatrix = null;
		  
		// =====================================================================
		// read input graph file
		// =====================================================================
		try{
		  InputStream  ips=new FileInputStream(fileName);
		  InputStreamReader ipsr=new InputStreamReader(ips);
		  BufferedReader   br=new
		  BufferedReader(ipsr);
		  String ligne;
		  
		  ligne = br.readLine();
		  
		  /* Get the number of nodes from the first line */
		  int n = Integer.parseInt(ligne.split("\t")[0]);
		  

		  weightedAdjMatrix = new double[n][n];
		  if(weightedAdjMatrix[0][0] != 0.0d)
			  System.out.println("Main: Error default value of doubles");
		  
		  /* For all the other lines */
		  while ((ligne=br.readLine())!=null){
			  
			  String[] split = ligne.split("\t");
			  
			  if(split.length >= 3){
				  int i = Integer.parseInt(split[0]);
				  int j = Integer.parseInt(split[1]);
				  double v = Double.parseDouble(split[2]);
				  weightedAdjMatrix[i][j] = v;
				  weightedAdjMatrix[j][i] = v;
			  }
			  else
				  System.err.println("All the lines of the input file must contain three values" 
						+ " separated by tabulations"
						+ "(except the first one which contains two values).\n"
				  		+ "Current line: " + ligne);
		  }
		  br.close();
		}catch(Exception e){
		  System.out.println(e.toString());
		}
		// end =================================================================


		// =====================================================================
		// write into temp file (in lower triangle format)
		// =====================================================================
		if(weightedAdjMatrix != null){
			 try{
			     FileWriter fw = new FileWriter(tempFile, false);
			     BufferedWriter output = new BufferedWriter(fw);

			     for(int i = 1 ; i < weightedAdjMatrix.length ; ++i){
			    	 String s = "";
			    	 
			    	 for(int j = 0 ; j < i ; ++j) // for each line, iterate over columns
			    		 s += weightedAdjMatrix[i][j] + " ";

			    	 s += "\n";
			    	 output.write(s);
			    	 output.flush();
			     }
			     
			     output.close();
			 }
			 catch(IOException ioe){
			     System.out.print("Erreur in reading input file: ");
			     ioe.printStackTrace();
			 }

		}
		// end =================================================================

	}
	
	
	
	/**
	 * read a solution from file
	 * 
	 */
	public static int[] readMembership(String inputFilePath, int n){
		int[] membership_ = new int[n];
		
		try{
			InputStream  ips = new FileInputStream(inputFilePath);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			  
			for(int i=0; i<n; i++){ // for each node
				line = br.readLine();
				membership_[i] = Integer.parseInt(line);	
			}
			
			line = br.readLine();
			br.close();
			
			// verify that the file we just read corresponds to a correct nb node
			if(line != null){
				return(null);
			}
		
		}catch(Exception e){
		  System.out.println(e.toString());
		  return(null);
		}
		
		return(membership_);
	}

	
	public static int getNbLinesInFile(String filepath) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		int lines = 0;
		while (reader.readLine() != null) lines++;
		reader.close();
		return(lines);
	}
	
	
	 public static TreeSet<ArrayList<Integer>> getMIPStartSolutionInArrayFormat(int[] membership){
	    	int n = membership.length;
	    	int nbCluster=0;
			for(int i=0; i<n; i++){
				if(membership[i]>nbCluster)
					nbCluster = membership[i];
			}
			
			TreeSet<ArrayList<Integer>> orderedClusters = new TreeSet<ArrayList<Integer>>(
					new Comparator<ArrayList<Integer>>(){
						// descending order by array size
						@Override
						public int compare(ArrayList<Integer> o1, ArrayList<Integer> o2) {
							int value=-1;
							if(o1.size() < o2.size())
								value = 1;
//							else if(o1.size() < o2.size())
//									value = -1;
							return value;
						}
					}
			);

			
	    	ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>(nbCluster);
			for(int i=1; i<=nbCluster; i++) // for each cluster
				clusters.add(new ArrayList<Integer>());
			for(int i=0; i<n; i++) // for each node
				clusters.get(membership[i]-1).add(i); // membership array has values starting from 1
			
			for(int i=1; i<=nbCluster; i++){ // for each cluster
				ArrayList<Integer> newCluster = clusters.get(i-1);
				orderedClusters.add(newCluster);
			}
			

			return(orderedClusters);
	    }
	 
	 
	 
}
