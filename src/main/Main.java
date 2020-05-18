package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cplex.Cplex;
import cutting_plane.CP;
import formulation.MyPartition;
import formulation.Partition;
import formulation.MyParam;
import formulation.MyParam.Triangle;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;


/**
* The ExCC program aims at solving Correlation Clustering problem.
* It proposes 2 approaches to solve the problem:
* 	 Pure CPLEX approach and Cutting Planes approach.
* The Cutting Planes approach consists of 2 steps: 1) Root Relaxation, 2) Branch&Bound
* <p>
* 
* Some references:
* <ul>
* <li> Cartwright D, Harary F (1956) Structural balance:
* 	 a generalization of Heider’s theory. Psychol Rev 63:277-293 <\li>
* <li> Heider F (1946) Attitudes and cognitive organization. J Psychol 21:107-112 <\li>
* <\lu>
*/
public class Main {

	// I put this here:
	// https://www.ibm.com/support/knowledgecenter/SSSA5P_12.5.1/ilog.odms.ide.help/OPL_Studio/usroplide/topics/opl_ide_stats_MP_exam_log.html

	
	static String tempFile = "temp.txt";
	
	/**
	 * 
	 * It proposes 2 approaches to solve the problem: 
	 * 		Pure CPLEX approach and Cutting Planes approach.
	 * 
	 * Input parameters:
	 * <ul>
	 * <li> inFile (String): Input file path. </li>
	 * <li> outDir (String): Output directory path. Default "." 
	 * 		(i.e. the current directory). </li>
	 * <li> cp (Boolean): True if Cutting Plane approach will be used.
	 * 		 Default false. </li>
	 * <li> enumAll (Boolean): True if enumerating all optimal solutions is desired
	 * 		 in Pure CPLEX approach.
	 *      Not available in Cutting Plane approach. Default false. </li>
	 * <li> tilim (Integer): Time limit in seconds. It can be used only with
	 * 		 Cutting Plane approach. Default 3600s in Cutting Plane approach. </li>
	 *  <li> MaxTimeForRelaxationImprovement (Integer): Max time limit for relaxation improvement in the first phase of the Cutting Plane approach.
	 *  				This is independent of the time limit. If there is no considerable improvement for X seconds, it stops and passes to the 2nd phase.
	 *  				This parameter can be a considerable impact on the resolution time. For medium-sized instances (e.g. 50,60), 
	 *  				it might be beneficial to increase the value of this parameter (e.g. 1800 or 3600s). The default value is 600s.							
	 *  </li>
	 * <li> lazyInBB (Boolean): True if adding triangle constraints as lazy approach 
	 * 		during the Branch&Bound is desired. It is used in the Cutting Plane approach.
	 *      This will set automatically the number of threads to 1. Default false. </li>
	 * <li> userCutInBB (Boolean): True if adding user cuts during the Branch&Bound is desired.
	 * 		It is used in the Cutting Plane approach.
	 *      This will set automatically the number of threads to 1. Default false. </li>
	 * <li> verbose (Boolean): Default value is True. When True, it enables to display log outputs during the Cutting Plane approach.
	 * </ul>
	 * 
	 * So, to use Pure CPLEX approach, set 'cp' to false. Otherwise, true.
	 * The time limit option can be used only with Cutting Plane approach.
	 * 
	 * Example for Pure CPLEX approach:
	 * <pre>
	 * {@code
	 * ant clean compile jar
	 * ant -DinFile=data/signed.G -DoutDir=out/signed -Dcp=false -DenumAll=false run
	 * 
	 * java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio128/cplex/bin/x86-64_linux/ -DinFile="in/GRAPH.G" 
	 * 	-DoutDir=out -Dcp=false -Dtilim=-1 -DlazyInBB=false -DMaxTimeForRelaxationImprovement=60 -DuserCutInBB=false 
	 * 	-DenumAll=false -Dverbose=false -jar exe/cplex-partition.jar
	 * }
	 * </pre>
	 * 
	 * Example for Cutting Plane approach:
	 * <pre>
	 * {@code
	 * ant clean compile jar
	 * 
	 * ant ant -DinFile=data/signed.G -DoutDir=out/signed
	 * 	 -Dcp=true -Dtilim=3600 -DlazyInBB=false -DuserCutInBB=false run
	 * }
	 * </pre>
	 * 
	 * @param args  (Not used in this program. Instead, user parameters are obtained
	 * 	 through ant properties. See the build.xml for more details).
	 * 
	 * @throws FileNotFoundException.
	 * @throws UnknownObjectException. 
	 * @throws IloException.
	 */
	public static void main(String[] args) throws FileNotFoundException, UnknownObjectException, IloException {
		/* TODO use Cutting Plane approach without time limit */
		/* TODO When the size of the input graph is > 1000, change the hash function
		 * 	 in Edge class in formulation/Edge.java */
		/* TODO We may enumerate the other found feasible solutions in
		 * 	 Cutting Planes approach => sometimes it displays "2 solutions found" ..) */
		/* TODO We got "No solution exist" bug in Lazy callback in Branch&Bound step.
		 * 	 Why ? ==> some logs in my Leanote (but no input graph :/) */
		/* TODO we should add another Cutting Plane approach which uses only CPLEX methods. */
		/* TODO we may use 'localAdd() when adding user cuts to improve performance' */
		
		/* WARNING In cutting plane approach, it takes longer time than usual
		 * 	 as there is a time limit after the last improved relaxation */
		/* WARNING: Prior to the CPLEX version 12.7 (especially in 12.6),
		 * 	 lazy callback with MIP START has some bugs.
					When using newer version (12.7 or 12.8 it is okay)
		 */
		/* WARNING
		 * When the graph is incomplete and we use Cutting Planes approach,
		 * 	 we add constraints and cuts lazily.
		 * This causes Object not found exception from cplex.getValue() 
		 * 	since not all the edger variables are in the model or constraints.
		 * 
		 * I found this thread as a solution: 
		 * 			https://www.ibm.com/developerworks/community/forums/html/
		 * 			topic?id=4f8bc2e4-a514-48d3-af62-10b9f417516d
		 */
		
		int tilim = -1;
		boolean lazyInBB = false;
		boolean userCutInBB = false;
		String inputFilePath = "";
		String outputDirPath = ".";
		boolean isCP = false;
		boolean isEnumAll = false;
		int MaxTimeForRelaxationImprovement = -1;
		boolean verbose = true;
		
		
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
		
		// Those 4 options are  available with cutting plane approach
		if( isCP && !System.getProperty("tilim").equals("${tilim}") )
			tilim = Integer.parseInt(System.getProperty("tilim"));
		if( isCP && !System.getProperty("MaxTimeForRelaxationImprovement").equals("${MaxTimeForRelaxationImprovement}") )
			MaxTimeForRelaxationImprovement = Integer.parseInt(System.getProperty("MaxTimeForRelaxationImprovement"));
		
		
		if( !isEnumAll && isCP && !System.getProperty("lazyInBB").equals("${lazyInBB}") )
			lazyInBB = Boolean.valueOf(System.getProperty("lazyInBB"));
		if( !isEnumAll && isCP && !System.getProperty("userCutInBB").equals("${userCutInBB}") )
			userCutInBB = Boolean.valueOf(System.getProperty("userCutInBB"));
	
		if( isCP && !System.getProperty("verbose").equals("${verbose}") )
			verbose = Boolean.valueOf(System.getProperty("verbose"));
		
		System.out.println("===============================================");
		System.out.println("inputFilePath: " + inputFilePath);
		System.out.println("outputDirPath: " + outputDirPath);
		System.out.println("isCP: " + isCP);
		System.out.println("isEnumAll: " + isEnumAll);
		System.out.println("tilim: " + tilim + "s");
		System.out.println("MaxTimeForRelaxationImprovement: " + MaxTimeForRelaxationImprovement + "s");
		System.out.println("lazyInBB: " + lazyInBB);
		System.out.println("userCutInBB: " + userCutInBB);
		System.out.println("verbose: " + verbose);
		System.out.println("===============================================");

		
		createTempFileFromInput(inputFilePath);
		
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
					modFindIntSolution, reordering, tilim, outputDirPath, MaxTimeForRelaxationImprovement,
					isEnumAll, verbose);
			cp.solve();
//			cp.cpresult.log();
			
			// end =============================================================

		} else { // Pure Cplex approach
							
			// =================================================================

			myp = new MyParam(tempFile, cplex, Triangle.USE, userCutInBB);
//			MyPartition p = new MyPartition(myp);
			MyPartition p = (MyPartition)Partition.createPartition(myp);
			p.setLogPath(outputDirPath + "/logcplex.txt");
			p.setOutputDirPath(outputDirPath); // it writes all solutions into files

			if(isEnumAll) { // Enumerate all optimal solutions
				if(verbose)
					System.out.println("BEFORE POPULATE() in main");
				
				p.populate();
				
				if(verbose)
					System.out.println("AFTER POPULATE() in main");
				
			} 
			else { // Obtain only one optimal solution
					p.solve();
					p.retreiveClusters(); // 
					p.writeClusters(outputDirPath + "/ExCC-result.txt");	
			}

			
//			p.computeObjectiveValueFromSolution();


			// end =============================================================

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

}
