package tsdb.dsl.formula;

import java.util.Map;

import tsdb.dsl.BooleanFormulaVisitor1;
import tsdb.dsl.Environment;
import tsdb.dsl.computation.BooleanComputation;
import tsdb.dsl.computation.Computation;

public class BooleanFormulaEqual extends BooleanFormulaAtomicBinary {
	public BooleanFormulaEqual(Formula a, Formula b) {
		super(a, b);
	}
	@Override
	public BooleanComputation compile(Environment env) {
		return new BooleanComputation() {
			Computation x = a.compile(env);
			Computation y = b.compile(env);
			@Override
			public boolean eval(long timestamp, float[] data) {
				return x.eval(timestamp, data) == y.eval(timestamp, data);
			}
		};
	}
	@Override
	public String compileToString(Environment env) {
		String ja = a.compileToString(env);
		String jb = b.compileToString(env);
		return "("+ja+"=="+jb+")";
	}
	@Override
	public BooleanFormula not() {
		return new BooleanFormulaNotEqual(a, b);
	}
	@Override
	public <T> T accept(BooleanFormulaVisitor1<T> visitor) {
		return visitor.visitEqual(this);
	}
}