package results;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;


public class ComputeResults {

	public static int defaultnm = 7;
	public static int defaultnM = 20;
	public static int defaultkm = 2;
	public static int defaultkM = 10;
	

	public static String[][] meanRatio(double[][][] up, double[][][] down, int signNb){
		return meanRatio(up, down, signNb, defaultnm, defaultnM, defaultkm, defaultkM);
	}
		

	public static String[][] meanRatio(double[][][] up, double[][][] down, int signNb,
			int nm, int nM, int km, int kM){
		
		int sizen = nM + 1;
		int sizek = kM +1;
		
		String [][] output = new String[sizen][sizek];
		
		for(int n = nm ; n <= nM ; ++n )
			for(int k = km ; k <= Math.min(kM,n-1) ;  ++k)
				output[n][k] = meanRatio(up[n][k], down[n][k], signNb);
		
		return output;
		
	}
	
	/**
	 * Compute the mean ratio of one array over another.
	 * @param up Array up in the ratio
	 * @param down Array down in the ratio
	 * @param sigNb Number of significant number in the result
	 * @return A string with the value if all the values of the table are >= 0.0; "-" otherwise
	 */
	public static String meanRatio(double[] up, double[] down, int sigNb){

		NumberFormat nf = NumberFormat.getInstance() ;
		nf.setMinimumFractionDigits(sigNb);
		nf.setMaximumFractionDigits(sigNb);
		
		String output;
		
		double result = 0.0;
		
		Integer size = Math.min(up.length, down.length);
//	System.out.println(size);	
		for(int i = 0 ; i < size ; ++i){
//System.out.println(result + " " + up[i] + " " + down[i] );			
			if(up[i] != -1.0 && down[i] != -1.0 && result != -1.0)
				result += up[i]/(down[i]+1E-10);
			else
				result = -1.0;
		}
		
		if(result == -1.0)
			output = "-";
		else{
			result /= size.doubleValue();
			output = nf.format(result);
//			System.out.println(result);
//			System.out.println();
		}
		
		return output;
		
	}

	public static String[][] meanImprovement(double[][][] ref, double[][][] compared, int signNb){
		return meanImprovement(ref, compared, signNb, defaultnm, defaultnM, defaultkm, defaultkM);
	}
	
	public static String[][] meanImprovement(double[][][] ref, double[][][] compared,
			int signNb, int nm, int nM, int km, int kM){
		
		int sizen = nM + 1;
		int sizek = kM +1;

		String [][] output = new String[sizen][sizek];
		
		for(int n = nm ; n <= nM ; ++n )
			for(int k = km ; k <= Math.min(kM,n-1) ;  ++k){
				output[n][k] = meanImprovement(ref[n][k], compared[n][k], signNb);
//System.out.println(output[n][k]);		
			}
		return output;
		
	}
	
	/**
	 * Compute the mean improvement of one array over another.
	 * @param ref Reference array
	 * @param comparedTo Compared array
	 * @param sigNb Number of significant number in the result
	 * @return A string with the value if all the values of the table are >= 0.0; "-" otherwise
	 */
	public static String meanImprovement(double[] ref, double[] comparedTo, int sigNb){

		NumberFormat nf = NumberFormat.getInstance() ;
		nf.setMinimumFractionDigits(sigNb);
		nf.setMaximumFractionDigits(sigNb);
//		NumberFormat nf0 = NumberFormat.getInstance() ;
//		nf0.setMinimumFractionDigits(0);
//		nf0.setMaximumFractionDigits(0);
//		NumberFormat nf1 = NumberFormat.getInstance() ;
//		nf1.setMinimumFractionDigits(1);
//		nf1.setMaximumFractionDigits(1);
		
		String output;
		
		double result = 0.0;
		
		int size = Math.min(ref.length, comparedTo.length);
		
		for(int i = 0 ; i < size ; ++i){
			
			if(ref[i] != -1.0 && comparedTo[i] != -1.0 && result != -1.0)
				result += improvement(ref[i], comparedTo[i]);
			else{
				result = -1.0;
				System.out.println("ComputeResults : meanImprovement : error value equal to -1.0");
			}
		}
		
		if(result == -1.0)
			output = "-";
		else{
			result *= 100.0/size;
//			if(result >= 10.0)
//				output = nf0.format(result);
//			else
//				output = nf1.format(result);
			output = nf.format(result);
		}
		
		return output;
		
	}
	
	public static <T> void printDoubleTable(String file){
		
		printDoubleTable(unserialize(file, double[][][].class));
		
	}
	
	public static void printDoubleTable(double[][][] table){

		NumberFormat nf = NumberFormat.getInstance() ;
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(1);
				
		for(int n = 7 ; n < table.length ; n++){
			
			System.out.print("n : " + n + "\t\t");
			
			for(int k = 2 ; k < n ; ++k){
				
				double value_nk = 0.0;
				
				for(int i = 0 ; i < table[7][2].length ; ++i){
					
					value_nk += table[n][k][i];
					
				}
				
				value_nk /= table[7][2].length;
				System.out.print(nf.format(value_nk) + "\t");
				
			}
			
			System.out.println();
		}
	}
	
	public static void printDoubleTable(String[][] table, boolean isLatex){
				
		for(int n = 7 ; n < table.length ; n++){
			
			if(isLatex)
				System.out.println("\\textbf{" + n + "} & ");
			else
				System.out.print("n : " + n + "\t\t");
			
			for(int k = 2 ; k <= Math.min(n-1, defaultkM) ; ++k){
				if("-0,0".equals(table[n][k]))
					table[n][k] = "0,0 ";
				
				if(isLatex)
					System.out.print(table[n][k] + " & ");
				else
					System.out.print(table[n][k] + "\t");
				
			}
			
			if(isLatex)
				System.out.println("\\\\");
			else
				System.out.println();
		}
	}
	
	public static double[][][] getGap(double[][][] bestInt, double[][][] bestRelax){

		int d1 = Math.min(bestInt.length, bestRelax.length);
		int d2 = Math.min(bestInt[0].length, bestRelax[0].length);
		int d3 = Math.min(bestInt[0][0].length, bestRelax[0][0].length);
		
		double[][][] output = new double[d1][d2][d3];

		for(int i = 0 ; i < d1 ; ++i)
			for(int j = 0 ; j < d2 ; ++j)
				for(int k = 0 ; k < d3 ; ++k){
					output[i][j][k] = improvement(bestRelax[i][j][k], bestInt[i][j][k]);
					
				}
		
		return output;
		
	}
	
	public static String get3DTables(ArrayList<double[][][]> time, ArrayList<double[][][]> branch,
			ArrayList<double[][][]> cut, ArrayList<double[][][]> bestInt, 
			ArrayList<double[][][]> bestRelax, ArrayList<String> solutionHeaders){
		
		ArrayList<ArrayList<double[][][]>> al_al = new ArrayList<ArrayList<double[][][]>>();
		
		for(int t = 0 ; t < time.size() ; ++t){
			ArrayList<double[][][]> al = new ArrayList<double[][][]>();
			al.add(time.get(t));
			al.add(branch.get(t));
			al.add(cut.get(t));
			al.add(getGap(bestInt.get(t), bestRelax.get(t)));
			
			al_al.add(al);
		}
		
		ArrayList<Integer> precision = new ArrayList<Integer>();
		precision.add(0);
		precision.add(0);
		precision.add(0);
		precision.add(2);
		
		
		return get3DTables(al_al, precision, solutionHeaders);
		
	}
	
	
	
	public static String get3DTables(CP_ResultParamToDisplay param, Result[][][] result,
			int[] nValue, int[] kValue, String[] solutionHeader){
		
		
		String output = "";
		
		/* Check the data */
		boolean isValid = true;
		
		if(result.length != nValue.length){
			System.err.println("Invalid dimensions for result and nValue");
			isValid = false;
		}
		
		if(result[0][0].length != solutionHeader.length){
			System.err.println("Invalid dimensions for result and solutionHeader");
			isValid = false;
		}
		
		boolean[] isCpResult = new boolean[result[0][0].length];
		
		for(int i = 0 ; i < result.length ; ++i){
			
			if(result[i].length != kValue.length){				
				System.err.println("Invalid dimensions for result and kValue (" + i + ")");
				isValid = false;
			}
				
			for(int j = 0 ; j < result[0].length ; ++j){
				if(result[i][j].length != result[0][0].length){				
					System.err.println("Invalid third dimension of result"
									+" (" + i + "," + j + ")");
					isValid = false;
				}
				
				for(int k = 0 ;  k < result[0][0].length ; ++k){
					if(j == 0){
						if(result[i][j][k] instanceof CPResult)
							isCpResult[k] = true;
						else
							isCpResult[k] = false;
					}
					
					else{
						if(result[i][j][k] instanceof CPResult && !isCpResult[k] 
							|| !(result[i][j][k] instanceof CPResult) && isCpResult[k]){
							isValid = false;
							System.err.println("<result> inconsistent. For a given i and j," 
									+" all the <result[i][j][k]> must be of the same class"
									+ "(i.e.: all Result or all CP_Result) (k : " + k + ")");
						}
					}
					
				}
				
			}
		}
		
		if(isValid){
			
			/* Create the different format to print the results (from 0 to 4 digits after the comma) */
			ArrayList<NumberFormat> al_nf = new ArrayList<NumberFormat>();
			for(int i = 0 ; i < 4 ; ++i){
				NumberFormat nf = NumberFormat.getInstance() ;
				nf.setMinimumFractionDigits(i);
				nf.setMaximumFractionDigits(i);
				
				al_nf .add(nf);
			}
			
			/* Find the number of columns for Result and Cp_result */
			ArrayList<String> resultHeader = new ArrayList<String>();
			ArrayList<String> cp_header = new ArrayList<String>();
			
			if(param.cp_time != -1)
				cp_header.add("CP time (s)");
			
			if(param.time != -1)
				resultHeader.add("BB time (s)");
			
			if(param.cp_first_relaxation != -1)
				cp_header.add("CP 1st Relax");
			
			if(param.firstRelaxation != -1)
				resultHeader.add("1st BB Relax");

			if(param.bestRelaxation != -1)
				resultHeader.add("Best Relax");
			
			if(param.bestInt != -1)
				resultHeader.add("Best Int");
			
			if(param.gap != -1)
				resultHeader.add("Gap");
			
			if(param.node != -1)
				resultHeader.add("Node");
			
			if(param.cpCutNb != -1)
				cp_header.add("CP cut");			
			
			if(param.cutNb != -1)
				resultHeader.add("BB Cut Nb");
			
			if(param.cp_iteration != -1)
				cp_header.add("CP it.");
			
			if(param.iterationNb != -1)
				resultHeader.add("BB It.");
			
			if(param.mod_remove != -1)
				cp_header.add("Mod remove");
			
			if(param.separationTime != -1)
				resultHeader.add("Sep. time (s)");
			
			String col_format = "|@{\\hspace{0.2cm}}l@{\\hspace{0.2cm}}l@{\\hspace{0.2cm}}|";
			
			/* Create the latex format of the columns */
			for(int i = 0 ; i < result[0][0].length ; ++i){
				if(isCpResult[i]){
					int s = cp_header.size() + resultHeader.size();
					col_format += "*{" + s + "}{p{0.7cm}@{\\hspace{0.2cm}}}|";
				}
				else
					col_format += "*{" + resultHeader.size() + "}{p{0.7cm}@{\\hspace{0.2cm}}}|";
			}
		
			output = "\\documentclass[15pt , a4paper]{article}\n\\"
			+ "usepackage[french]{babel}\n\\usepackage [utf8] {inputenc}\n"
					+"\\usepackage{vmargin}\n\\usepackage{array}\n"
					+"\\usepackage{booktabs}\n"
					+"\\setmarginsrb{.5cm}{0cm}{0cm}{.5cm}{.5cm}{.5cm}{.5cm}{.5cm}\n"
					+ "\\begin{document}\\setlength{\\extrarowheight}{1pt}\n"
					+"\\begin{tiny}\\hspace{-0.5cm}"
					+"\\begin{tabular}{" + col_format + "}\\toprule\n";

			String colSeparator = " & ";
			String endOfLine1 = "\\\\\\hline\n";
			String endOfLine2 = "\\\\\n";
			
			/* Display the solution headers  on the first line */
			output += colSeparator;
			
			for(int i = 0 ; i < result[0][0].length ; ++i){
				
				int colNb = resultHeader.size();
				
				if(isCpResult[i])
					colNb += cp_header.size();
				
				output += colSeparator + "\\multicolumn{" + colNb + "}{c|}{"
							+ solutionHeader[i] + "}";
				
			}
			
			output += endOfLine1;
			
			/* Display the header of each column on the second line */
			output += "n" + colSeparator + "K";
			
			for(int i = 0 ; i < result[0][0].length ; ++i){
								
				if(isCpResult[i])
					for(String s : cp_header)
						output += colSeparator + s;
				
				for(String s : resultHeader)
					output += colSeparator + s;
			}
			
			output += endOfLine1;
			
			/* Display the results */
			
			/* For each n */
			for(int n = 0 ; n < nValue.length ; ++n){
				
				/* For each K */
				for(int k = 0 ; k < kValue.length ; ++k){
					
					/* Display k and n */
					output += nValue[n] + colSeparator;
					output += kValue[k];

					/* For each instance */
					for(int i = 0 ; i < result[0][0].length ; ++i){

//						/* n and K are not displayed, just display two tabs */
//						output += colSeparator + colSeparator;
						
//						output += i + "\t";
						Result res = result[n][k][i];
						CPResult cpres = null;
						
						if(isCpResult[i])
							cpres = (CPResult)res;
						
						if(param.cp_time != -1 && isCpResult[i])
							output += colSeparator + al_nf.get(param.cp_time).format(cpres.cp_time);
						
						if(param.cp_first_relaxation != -1 && isCpResult[i])
							output += colSeparator + al_nf.get(
									param.cp_first_relaxation).format(cpres.cp_first_relaxation
									);
						
						if(param.cpCutNb != -1 && isCpResult[i]){
							
							int nb = 0;
							
							for(int j = 0 ; j < cpres.cpCutNb.size() ; ++j)
								nb += cpres.cpCutNb.get(j).cutNb;
							
							output += colSeparator + al_nf.get(param.cpCutNb).format(nb);
							
						}
						
						if(param.cp_iteration != -1 && isCpResult[i])
							output += colSeparator + al_nf.get(
										param.cp_iteration).format(cpres.cp_iteration);
						
						if(param.mod_remove != -1 && isCpResult[i])
							output += colSeparator + al_nf.get(
									param.mod_remove).format(cpres.mod_remove);
						
						if(param.time != -1){

							if(res.time != -1)
								output += colSeparator + al_nf.get(
										param.time).format(res.time);
							else
								output += colSeparator + "-";
						}
						
						
						if(param.firstRelaxation != -1)
							output += colSeparator + al_nf.get(
									param.firstRelaxation).format(res.firstRelaxation);
						
						if(param.bestRelaxation != -1)
							output += colSeparator + al_nf.get(
									param.bestRelaxation).format(res.bestRelaxation);
						
						if(param.bestInt != -1)
							output += colSeparator + al_nf.get(
									param.bestInt).format(res.bestInt);
						
						if(param.gap != -1){
							
							double gap = 0;
							
							/* If bestRelaxation = -1 the cutting plane gave an integer solution */
							if(res.bestRelaxation != -1)
								gap = ComputeResults.improvement(res.bestRelaxation, res.bestInt);
							
							String s = "";
							if(gap < 1E-4)
								s = "-";
							else
								s = al_nf.get(param.gap).format(gap*100);
							
							output += colSeparator + s ;
						}
						
						if(param.node != -1)
							output += colSeparator + al_nf.get(param.node).format(res.node);
						
						if(param.cutNb != -1){
							int nb = 0;
							for(int j = 0 ; j < res.cplexCutNb.size() ; ++j)
								nb += res.cplexCutNb.get(j).cutNb;

							for(int j = 0 ; j < res.userCutNb.size() ; ++j)
								nb += res.userCutNb.get(j).cutNb;
								
							output += colSeparator + al_nf.get(param.cutNb).format(nb);
						}
						
						//TODO Comment this if?
						if(param.iterationNb != -1)
							output += colSeparator + al_nf.get(
									param.iterationNb).format(res.iterationNb);
						
						if(param.separationTime != -1)
							output += colSeparator + al_nf.get(
									param.separationTime).format(res.separationTime);
						
					}

					/* If this is the last couple K for this n, display a line */
					if(k == kValue.length - 1)
						output += endOfLine1;
					else
						output += endOfLine2;
					
				}
			}
						
			output += "\\end{tabular}\n\\end{tiny}\n\\end{document}";		
		}
		
		output = output.replace('\u00a0', ' ');
		return output;
		
	}
 	
	public static String get3DTables(ArrayList<ArrayList<double[][][]>> tables,
			ArrayList<Integer> sig, ArrayList<String> solutionHeader){

		String cf1 = "*{4}{l@{\\hspace{0.3cm}}}|";
		String colFormat = "|ll|" + cf1 + cf1 + cf1 + cf1 + "l";
		String prefix = "\\documentclass[15pt , a4paper]{article}\n"
				+"\\usepackage[french]{babel}\n\\usepackage [utf8] {inputenc}\n"
				+"\\usepackage{vmargin}\n\\usepackage{array}\n"
				+"\\usepackage{booktabs}\n"
				+"\\setmarginsrb{.5cm}{0cm}{0cm}{.5cm}{.5cm}{.5cm}{.5cm}{.5cm}\n"
				+"\\begin{document}\\setlength{\\extrarowheight}{1pt}\n"
				+"\\begin{tiny}\\begin{tabular}{" + colFormat + "}\\toprule\n";
		String suffix = "\\end{tabular}\n\\end{tiny}\n\\end{document}";

		String colSeparator = " & ";
		String endOfLine1 = "\\\\\\hline\n";
//		String endOfLine2 = "\\\\\n";
		
		String output = prefix;
		String[] nValue = new String[4];
		nValue[0] = "20";
		nValue[1] = "25";
		nValue[2] = "30";
		nValue[2] = "35";
		
		String[] kValue = new String[4];
		kValue[0] = "4";
		kValue[1] = "6";
		kValue[2] = "8";
		kValue[3] = "10";
		
		ArrayList<NumberFormat> al_nf = new ArrayList<NumberFormat>();
		for(int i = 0 ; i < sig.size() ; ++i){
			NumberFormat nf = NumberFormat.getInstance() ;
			nf.setMinimumFractionDigits(sig.get(i));
			nf.setMaximumFractionDigits(sig.get(i));
			
			al_nf .add(nf);
		}
		
		
		ArrayList<String> headerTables = new ArrayList<String>();
		headerTables.add("Time");
		headerTables.add("Branch");
		headerTables.add("Cut");
		headerTables.add("Gap");
		
		boolean valid = true;
		
		for(int i = 0 ; i < tables.size() ; ++i)
			if(tables.get(i).size() != sig.size())
				valid = false;
		
		if(headerTables.size() != sig.size() || solutionHeader.size() != tables.size())
			valid = false;
		
				
		if(valid){
			
//			double[][][] firstTable = tables.get(0).get(0);

			/* Display the solution headers */
			output += colSeparator + colSeparator;
			for(int i = 0 ; i < tables.size() ; ++i){
				output += solutionHeader.get(i);
				for(int k = 0 ; k < tables.get(0).size() ; ++k)
					output += colSeparator;
			}
			
			
			/* For each column display the header */
			output += endOfLine1 + "n" + colSeparator + "K" + colSeparator;
			for(int j = 0 ; j < tables.size() ; ++j)
				for(int i = 0 ; i < headerTables.size() ; ++i){
					output += headerTables.get(i) +colSeparator;
				}
			
			output += endOfLine1;
			output += suffix;
		}
		else
			System.out.println("Invalid size");
		
		return output;
		
	}
	
	

	public static <T> T unserialize(String file, Class<T> type){
		
		T results = null;
		
		try {
		      FileInputStream fichier = null;
	    	  fichier = new FileInputStream(file);
		      
		      ObjectInputStream ois = new ObjectInputStream(fichier);
		      
		      results = type.cast(ois.readObject());
		      
		      ois.close();
		} 
	    catch (java.io.IOException e) {
	      e.printStackTrace();
	    }
	    catch (ClassNotFoundException e) {
	      e.printStackTrace();
	    }
 		
		return results;
	}
	
	public static void serialize(Object d, String file){
		
	    try {
		      FileOutputStream fichier = new FileOutputStream(file);
		      ObjectOutputStream oos = new ObjectOutputStream(fichier);
		      oos.writeObject(d);
		      oos.flush();
		      oos.close();
//		      System.out.println("serialize");
		    }
		    catch (java.io.IOException e) {
		      e.printStackTrace();
		    }
		
	}

	public static double improvement(double ref, double betterValue){

		return Math.abs(betterValue-ref)/Math.abs(1E-10 + ref);
	}
	
	public static double improvement2(double ref, double betterValue){

		return Math.abs((betterValue-ref))/(1E-10 + ref);
	}
	
	public static String getOutput(ArrayList<String[][]> values, String header){
		return getOutput(values, header, defaultnm, defaultnM, defaultkm, defaultkM);
	}

	public static String getOutput(ArrayList<String[][]> values, String header,
			int nm, int nM, int km, int kM){
		
		String result = header + "\n";
		
		for(int n = nm ; n < nM + 1 ; ++n){
			
			result += "n: " + n + "\t";
			
			for(int k = km ; k <= Math.min(kM,n-1) ; ++k){
				for(int i = 0 ; i < values.size() - 1 ; ++i)
					result += values.get(i)[n][k] + "/";
				
				result += values.get(values.size()-1)[n][k] + "\t";
//				System.out.println(result);
			}
			result += "\n";
			
		}
		
		
		return result;
		
	}
	
	public static void writeInFile(String file, String content, boolean append){
		
		 try{
		     FileWriter fw = new
		     FileWriter(file, append);
		     BufferedWriter output = new BufferedWriter(fw);
		     output.write(content + "\n");
		     output.flush();
		     output.close();
		 }
		 catch(IOException ioe){
		     System.out.print("Erreur : ");
		     ioe.printStackTrace();
		 }
	}
	
	public static void log(String content){
		writeInFile("log.txt", content, true);
		System.out.println(content);
	}
	
	public static String getDate(){
		
		DateFormat mediumDateFormat = DateFormat.getDateTimeInstance(
				DateFormat.MEDIUM,
				DateFormat.MEDIUM);
		
		Date aujourdhui = new Date();
		return mediumDateFormat.format(aujourdhui);
	}
	
	/**
	 * Number of figures after the comma displayed for each parameter
	 *  (if -1 the parameter is not displayed)
	 *
	 */
	public class ResultParamToDisplay{
		
		int gap = 1;
		int time = 0;
		int node = 0;
		int cutNb = -1;
		int bestInt = -1;
		int firstRelaxation = -1;
		int bestRelaxation = -1;
		int iterationNb = -1;
		int separationTime = -1;
		
		public ResultParamToDisplay(){}

		public ResultParamToDisplay(int gap, int bestInt, int firstRelaxation, int bestRelaxation, 
				int time, int node, int cutNb, int iterationNb, int separationTime){

			this.gap = gap;
			this.bestInt = bestInt;
			this.firstRelaxation = firstRelaxation;
			this.bestRelaxation = bestRelaxation;
			this.time = time;
			this.node = node;
			this.cutNb = cutNb;
			this.iterationNb = iterationNb;
			this.separationTime = separationTime;
		}
		
	}
	
	public class CP_ResultParamToDisplay extends ResultParamToDisplay{
		
		int mod_remove = -1;
		int cp_first_relaxation = -1;
		int cp_time = 0;
		int cpCutNb = -1;
		int cp_iteration = -1;
		
		public CP_ResultParamToDisplay(){}
		
		public CP_ResultParamToDisplay(int gap, int bestInt, int firstRelaxation, int bestRelaxation, 
				int time, int node, int cutNb, int iterationNb, int separationTime,
				int mod_remove, int cp_first_relaxation, int cp_time, int cpCutNb, int cp_iteration){
			
			super(gap, bestInt, firstRelaxation, bestRelaxation, time, node, cutNb,
					iterationNb, separationTime);
			
			this.mod_remove = mod_remove;
			this.cp_first_relaxation = cp_first_relaxation;
			this.cp_time = cp_time;
			this.cpCutNb = cpCutNb;
			this.cp_iteration = cp_iteration;
			
		}
		
	}

	public static String get3DHistogram(Result[][][] result, int[] nValue,
			int[] kValue, String[] solutionHeader) {

		String output = "";
		
		/* Check the data */
		boolean isValid = true;
		
		if(result.length != nValue.length){
			System.err.println("Invalid dimensions for result and nValue");
			isValid = false;
		}
		
		if(result[0][0].length != solutionHeader.length){
			System.err.println("Invalid dimensions for result and solutionHeader");
			isValid = false;
		}
		
		boolean[] isCpResult = new boolean[result[0][0].length];
		
		for(int i = 0 ; i < result.length ; ++i){
			
			if(result[i].length != kValue.length){				
				System.err.println("Invalid dimensions for result and kValue (" + i + ")");
				isValid = false;
			}
				
			for(int j = 0 ; j < result[0].length ; ++j){
				if(result[i][j].length != result[0][0].length){				
					System.err.println("Invalid third dimension of result (" + i + "," + j + ")");
					isValid = false;
				}
				
				for(int k = 0 ;  k < result[0][0].length ; ++k){
					if(j == 0){
						if(result[i][j][k] instanceof CPResult)
							isCpResult[k] = true;
						else
							isCpResult[k] = false;
					}
					
					else{
						if(result[i][j][k] instanceof CPResult && !isCpResult[k] 
							|| !(result[i][j][k] instanceof CPResult) && isCpResult[k]){
							isValid = false;
							System.err.println("<result> inconsistent. For a given i and j,"
									+" all the <result[i][j][k]> must be of the same class"
									+" (i.e.: all Result or all CP_Result) (k : " + k + ")");
						}
					}
					
				}
				
			}
		}
		
		if(isValid){
			
			/* Create the different format to print the results (from 0 to 4 digits after the comma) */
			ArrayList<NumberFormat> al_nf = new ArrayList<NumberFormat>();
			for(int i = 0 ; i < 4 ; ++i){
				NumberFormat nf = NumberFormat.getInstance() ;
				nf.setMinimumFractionDigits(i);
				nf.setMaximumFractionDigits(i);
				
				al_nf .add(nf);
			}
			
			String col_format = "@{\\hspace{0.2cm}}m{0.5cm}@{\\hspace{0.2cm}}";
			
			/* Create the latex format of the columns */
			for(int i = 0 ; i < result[0][0].length ; ++i){
					col_format += "*{" + "1" + "}{c@{\\hspace{0.2cm}}}";
			}
		
			output = "\\documentclass[15pt , a4paper]{article}\n"
					+"\\usepackage[french]{babel}\n\\usepackage [utf8] {inputenc}\n"
					+"\\usepackage{pgfplots, pgfplotstable}\n\\usepackage{vmargin}\n"
					+"\\usepackage{array}\n\\usepackage{booktabs}\n"
					+"\\setmarginsrb{.5cm}{0cm}{0cm}{.5cm}{.5cm}{.5cm}{.5cm}{.5cm}\n"
					+"\\definecolor{zgreen}{RGB}{0,131,144}\n"
					+"\\begin{document}\\setlength{\\extrarowheight}{1pt}\n"
					+"\\begin{tiny}\\hspace{-0.5cm}"
					+"\\begin{tabular}{" + col_format + "}\\toprule\n";

			String colSeparator = " & ";
			String endOfLine1 = "\\\\\\hline\n";
			
			/* Display the solution headers  on the first line */
			output += "n";
			
			for(int i = 0 ; i < result[0][0].length ; ++i){
				
				output += colSeparator +  solutionHeader[i];
				
			}
			
			output += endOfLine1;
			
			/* Display the results */
			
			double[] xCoordinates = new double[4];
			xCoordinates[0] = 1;
			xCoordinates[1] = 2.6;
			xCoordinates[2] = 4.2;
			xCoordinates[3] = 5.8;
			
			String[] color = new String[4];
			color[0] = "blue";
			color[1] = "red";
			color[2] = "zgreen";
			color[3] = "black";
			
			/* For each n */
			for(int n = 0 ; n < nValue.length ; ++n){

				output += nValue[n];
				
				/* For each instance */
				for(int i = 0 ; i < result[0][0].length ; ++i){
					
					output += "\n" + colSeparator + "\\scalebox{0.3}{"
							+"\\begin{tikzpicture}[baseline=(current bounding box.center)]";
					
					/* Draw a white rectangle of maximal size to ensure that
					 *  all the histogram have the same size */ 
					output += "\\draw [line width=14mm, color=white!70] plot [ycomb] coordinates {(1,3.6)};";
					
					output += "\\draw (0,0) grid[xstep=20,ystep=20] (7.2,3.6);";
					
					double[] yCoordinates = new double[4];
					
					/* For each K */
					for(int k = 0 ; k < kValue.length ; ++k){
					
						Result res = result[n][k][i];
						
						CPResult cpres = null;
						if(isCpResult[i])
							cpres = (CPResult)res;
						
						yCoordinates[k] = res.time;
						
						double v_cp = 0;
						
						if(cpres != null){
							v_cp = cpres.cp_time;
							yCoordinates[k] += v_cp;
						}
						
						yCoordinates[k] /= 1000;
						yCoordinates[k] = Math.min(yCoordinates[k], 3.6);
						v_cp /= 1000;
						
						output += "\\draw [line width=14mm, color="
								+ color[k] + "!70] plot [ycomb] coordinates {("
								+ xCoordinates[k] + "," + yCoordinates[k] +  ")};";

						double gap = 100 * ComputeResults.improvement(res.bestRelaxation, res.bestInt);
						if(gap >= 1 && yCoordinates[k] == 3.6){
							double y = 1.8;
							output += "\\draw[white](" + xCoordinates[k] + "," + y + ")node{"
							+"\\begin{Huge}\\textbf{" + Math.round(gap) + "}\\end{Huge}};";
						}
					}
					
					
					output += "\\end{tikzpicture}}";
					
				}
				
				
				output += endOfLine1;
			}
						
			output += "\\end{tabular}\n\\end{tiny}\n\\end{document}";		
		}
		
		output = output.replace('\u00a0', ' ');
		return output;

	}

	
	public static String doubleToString(double d, int digitAfter0){
		  double power = Math.pow(10.0, digitAfter0);
		  return ((Double)(Math.round(d*power)/power)).toString();
	}
	
	private static class Coordinate{
		double x;
		double y;
		
		public Coordinate(double x, double y){
			this.x = x;
			this.y = y;
		}
		
		public String toString(){
			return "(" + x + "," + y + ")";
		}
		
		public double distanceTo(Coordinate c){
			return Math.sqrt((x-c.x)*(x-c.x)+(y - c.y)*(y - c.y));
		}
	}
	
	/**
	 * Convert an input file with euclidean coordinates into a dissimilarity matrix
	 * Only the lines containing at least three words separated by whitespaces are considered.
	 * @param input_path Input file with the euclidean coordinates
	 * @param output_path Output file with the dissimilarity matrix
	 * @param col_first_coordinate After splitting a line according to its spaces,
	 *  column in which the first coordinate is (e.g.: " test 1 3", is splitted 
	 *  in the four following columns numbered from 0 to 2, "", "test", "1", "3")
	 * @param col_second_coordinate
	 */
	public static void convert2DCoordinatesIntoDistanceInputFile(String input_path, 
			String output_path, int col_first_coordinate, int col_second_coordinate){
		
		ArrayList<Coordinate> coordinate = new ArrayList<Coordinate>();

		try{
		  InputStream  ips=new
		  FileInputStream(input_path);
		  InputStreamReader ipsr=new InputStreamReader(ips);
		  BufferedReader br = new BufferedReader(ipsr);
		  
		  String ligne;
		  while ((ligne=br.readLine())!=null){
			  
			  /* Split the string according to the whitespaces (the spaces are grouped together) */
			  String[] splited = ligne.split("\\s+");
			  
			  try{
				  
				  if(splited.length > 2)
					  coordinate.add(new Coordinate(
						   Double.parseDouble(splited[col_first_coordinate]),
						   Double.parseDouble(splited[col_second_coordinate])
					  ));
				  
//				  if(coordinate.size() > 0)
//					  System.out.println(coordinate.get(coordinate.size()-1));
			  }
			  catch(java.lang.NumberFormatException e){
				  /* Do nothing */
				  System.out.println("Invalid line: " + ligne);
			  }
			  
		  }
		  br.close();
		}catch(Exception e){
		  e.printStackTrace();
		}
		
		/* If coordinates have been found */
		if(coordinate.size() > 1){
			
			String content = "";
			
			/* For all couples of coordinates */
			for(int i = 1 ; i < coordinate.size() ; ++i){
				
				Coordinate ci = coordinate.get(i);
				
				for(int j = 0 ; j < i ; ++j){
					
					/* Compute the distance */
					Coordinate cj = coordinate.get(j);
					content += ci.distanceTo(cj) + " ";
				}
				content += "\n";
			}
			
			 try{
			     FileWriter fw = new
			     FileWriter(output_path, true);
			     BufferedWriter output = new BufferedWriter(fw);

			     output.write(content);
			     output.flush();
			     output.close();
			 }
			 catch(IOException ioe){
			     ioe.printStackTrace();
			 }

			
		}
		else
			System.out.println("Error: less than 1 coordinate found in file: " + input_path);
		
		
	}
	
	

	
}
