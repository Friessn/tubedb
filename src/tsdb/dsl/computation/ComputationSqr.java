package tsdb.dsl.computation;

import tsdb.util.Computation;

public class ComputationSqr extends Computation {
	public final Computation a;
	public ComputationSqr(Computation a) {
		this.a = a;
	}
	@Override
	public float eval(long timestamp, float[] data) {
		float x = a.eval(timestamp, data);
		return x*x;
	}
}
