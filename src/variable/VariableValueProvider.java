package variable;

/**
 * Object which is able to provide the value of a given variable.
 * There are typically two types of VariableValueProvider:
 * - the formulations: provides the value of a variable in the best known feasible solution;
 * - the callbacks: provides the value of a variable in the current best known relaxed solution.
 * 
 * @author Zacharie ALES
 *
 */
public interface VariableValueProvider {
	public VariableGetter variableGetter();
}
