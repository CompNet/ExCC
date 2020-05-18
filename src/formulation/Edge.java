package formulation;


/**
 * ID of an edge i,j (with i < j)
 * @author Zacharie ALES
 *
 */
public class Edge{
	int i;
	int j;
	int hashcode;

	public Edge(int i, int j){
		this.i=i;
		this.j=j;
		// TODO Is this hash code is good enough? it is ok up to 1000 nodes
		hashcode = 1000 * this.i + this.j;
	}

	@Override
	public int hashCode(){	
		return hashcode;
	}

	@Override
	public boolean equals(Object o){

		if (this==o)
			return true;
		if (o instanceof Edge) {
			Edge e = (Edge)o;

			return this.i == e.i 
					&& this.j == e.j;
		}
		return false;
	}
	
	public int getSource() {
		return this.i;
	}
	
	public int getDest() {
		return this.j;
	}
}