package results;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;

import callback.cut_callback.AbstractCutCallback;
import formulation.Partition;
import formulation.interfaces.IFEdgeVEdgeW;
import formulation.interfaces.IFormulation;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import separation.AbstractSeparation;


/**
 * Results of an execution of cplex
 * @author zach
 *
 */
public class Result implements Serializable{

	private static final long serialVersionUID = 6331586870193467691L;
	
	public int n;

	public double bestInt;
	public double bestRelaxation;
	
	public double time;
	public double node;
	public ArrayList<Cut> cplexCutNb = new ArrayList<Cut>();
	public ArrayList<Cut> userCutNb = new ArrayList<Cut>();
	
	/* Number of time our separation algorithm is called */
	public int iterationNb;
	public double separationTime;
	public double firstRelaxation = -1.0;
	
	public void solveAndGetResults(IFEdgeVEdgeW formulation, boolean log){
		solveAndGetResults(formulation, null, log);
	}
	
//	public void solveAndGetResults(int i, Abstract_CutCallback ucc, boolean log){
//		solveAndGetResults(i, ucc.formulation, ucc, log);
//	}
	
	public void solveAndGetResults(IFEdgeVEdgeW formulation, AbstractCutCallback ucc, boolean log){

		try {
			time = formulation.getCplex().solve();
			getResults(formulation, ucc, log);
		} catch (IloException e) {
			e.printStackTrace();
		}

	}
	
	public void getResults(IFEdgeVEdgeW formulation,
			AbstractCutCallback ucc, boolean log) {
		
		try{
			
			n = formulation.n();
			
			node = formulation.getCplex().getNnodes();
			System.out.println("Result, nb of nodes: " + formulation.getCplex().getNnodes());

			if(ucc != null && ucc.sep != null){

				ArrayList<AbstractSeparation<?>> al_as = ucc.sep;				
				separationTime = ucc.time;
				iterationNb = ucc.iterations;
				firstRelaxation = ucc.root_relaxation;

				for(int j = 0 ; j < al_as.size() ; ++j)
					if(al_as.get(j).added_cuts > 0)
						userCutNb.add(new Cut(al_as.get(j).name, al_as.get(j).added_cuts));
				
			}
			else{
				separationTime = -1.0;
				iterationNb = -1;
				firstRelaxation = -1.0;
			}
			
			bestInt = formulation.getCplex().getObjValue();
			bestRelaxation = formulation.getCplex().getBestObjValue();
		
			countCplexCuts(formulation);
				
		} catch (IloException e) {
			e.printStackTrace();
		}
		
		if(log)
			log();	
		
	}

	public void log(){

		NumberFormat nf0 = NumberFormat.getInstance() ;
		nf0.setMinimumFractionDigits(0);
		nf0.setMaximumFractionDigits(0);

		NumberFormat nf1 = NumberFormat.getInstance() ;
		nf1.setMinimumFractionDigits(1);
		nf1.setMaximumFractionDigits(1);

		NumberFormat nf2 = NumberFormat.getInstance() ;
		nf2.setMinimumFractionDigits(2);
		nf2.setMaximumFractionDigits(2);
		
		String log = "----\n";
		log += "date: " + ComputeResults.getDate();
		
		log += "(n): (";

		log += n + ")\n";
		
		log += "\ttime:\t\t" + nf0.format(time) + "s\n\tnode:\t\t" + nf0.format(node) + "n\n";

		log += "\tbest relax.:\t" + nf1.format(bestRelaxation) + "\n"; 
		
		log += "\tbest int.:\t" + nf0.format(bestInt) + "\n"; 

		if(firstRelaxation != -1.0)
			log += "\tfirst relax.:\t" + nf1.format(firstRelaxation) + "\n";
		
		log += "\tgap:\t\t" + nf2.format(ComputeResults.improvement(bestRelaxation, bestInt)) + "%\n"; 

		if(separationTime != -1.0)
			log += "\tsep. time:\t" + nf1.format(separationTime) + "s\n";
		
		if(iterationNb != -1.0)
			log += "\tsep. it. nb.:\t" + iterationNb + "\n"; 
		
		if(this.userCutNb.size() > 0 || this.cplexCutNb.size() > 0)
			log += "\tcuts:\n";
		for(int j = 0 ; j < this.userCutNb.size() ; ++j)
			log += "\t\t" + userCutNb.get(j).cutName + " cut:\t" + userCutNb.get(j).cutNb + "\n";
		
		for(int j = 0 ; j < this.cplexCutNb.size() ; ++j)
			log += "\t\t" + cplexCutNb.get(j).cutName + " cut:\t" + cplexCutNb.get(j).cutNb + "\n";

		ComputeResults.writeInFile("log.txt", log, true);
		System.out.println(log);
	}
	
	public void serialize(String file){
		
	    try {
		      FileOutputStream fichier = new FileOutputStream(file);
		      ObjectOutputStream oos = new ObjectOutputStream(fichier);
		      oos.writeObject(this);
		      oos.flush();
		      oos.close();
		    }
		    catch (java.io.IOException e) {
		      e.printStackTrace();
		    }
		
	}
	
	public static Result unserialize(String file){
		
		Result results = null;
		
		try {
		      FileInputStream fichier = null;
	    	  fichier = new FileInputStream(file);
		      
		      ObjectInputStream ois = new ObjectInputStream(fichier);
		      
		      results = (Result) ois.readObject();
		      
		      ois.close();
		      fichier.close();
		} 
	    catch (java.io.IOException e) {
	      e.printStackTrace();
	    }
	    catch (ClassNotFoundException e) {
	      e.printStackTrace();
	    }
 		
		return results;
	}	
	

	public class Cut implements Serializable{

		private static final long serialVersionUID = -6969643363994348665L;
		
		public String cutName;
		public int cutNb;
		
		public Cut(String name, int nb){
			cutName = name;
			cutNb = nb;
		}
		
	}
		
	public void countCplexCuts(IFormulation formulation) throws IloException{
	
		if(formulation.getCplex().getNcuts(IloCplex.CutType.CliqueCover) != 0)
			this.cplexCutNb.add(
				new Cut("CliqueCover", formulation.getCplex().getNcuts(IloCplex.CutType.CliqueCover))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.Cover) != 0)
			this.cplexCutNb.add(
				new Cut("Cover", formulation.getCplex().getNcuts(IloCplex.CutType.Cover))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.Disj) != 0)
			this.cplexCutNb.add(
				new Cut("Disj", formulation.getCplex().getNcuts(IloCplex.CutType.Disj))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.FlowCover) != 0)
			this.cplexCutNb.add(
				new Cut("FlowCover", formulation.getCplex().getNcuts(IloCplex.CutType.FlowCover))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.FlowPath) != 0)
			this.cplexCutNb.add(
				new Cut("FlowPath", formulation.getCplex().getNcuts(IloCplex.CutType.FlowPath))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.Frac) != 0)
			this.cplexCutNb.add(
				new Cut("Frac", formulation.getCplex().getNcuts(IloCplex.CutType.Frac))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.GUBCover) != 0)
			this.cplexCutNb.add(
				new Cut("GUBCover", formulation.getCplex().getNcuts(IloCplex.CutType.GUBCover))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.ImplBd) != 0)
			this.cplexCutNb.add(
				new Cut("ImplBd", formulation.getCplex().getNcuts(IloCplex.CutType.ImplBd))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.LocalCover) != 0)
			this.cplexCutNb.add(
				new Cut("LocalCover", formulation.getCplex().getNcuts(IloCplex.CutType.LocalCover))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.MCF) != 0)
			this.cplexCutNb.add(
				new Cut("MCF", formulation.getCplex().getNcuts(IloCplex.CutType.MCF))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.MIR) != 0)
			this.cplexCutNb.add(
				new Cut("MIR", formulation.getCplex().getNcuts(IloCplex.CutType.MIR))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.ObjDisj) != 0)
			this.cplexCutNb.add(
				new Cut("ObjDisj", formulation.getCplex().getNcuts(IloCplex.CutType.ObjDisj))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.SolnPool) != 0)
			this.cplexCutNb.add(
				new Cut("SolnPool", formulation.getCplex().getNcuts(IloCplex.CutType.SolnPool))
			);
	
		if(formulation.getCplex().getNcuts(IloCplex.CutType.Table) != 0)
			this.cplexCutNb.add(
				new Cut("Table", formulation.getCplex().getNcuts(IloCplex.CutType.Table))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.Tighten) != 0)
			this.cplexCutNb.add(
				new Cut("Tighten", formulation.getCplex().getNcuts(IloCplex.CutType.Tighten))
			);
		
		if(formulation.getCplex().getNcuts(IloCplex.CutType.ZeroHalf) != 0)
			this.cplexCutNb.add(
				new Cut("ZeroHalf", formulation.getCplex().getNcuts(IloCplex.CutType.ZeroHalf))
			);
	}
}
