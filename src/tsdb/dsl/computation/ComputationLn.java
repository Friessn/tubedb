package tsdb.dsl.computation;

import tsdb.util.Computation;

public class ComputationLn extends Computation {
	public final Computation a;
	public ComputationLn(Computation a) {
		this.a = a;
	}
	@Override
	public float eval(long timestamp, float[] data) {
		return (float) Math.log(a.eval(timestamp, data));
	}
}
