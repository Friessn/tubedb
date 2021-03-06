package tsdb.dsl.computation;

import tsdb.util.Computation;

public class ComputationArctanDeg extends Computation {
	public final Computation a;
	public ComputationArctanDeg(Computation a) {
		this.a = a;
	}
	@Override
	public float eval(long timestamp, float[] data) {		
		return (float) Math.toDegrees(Math.atan(a.eval(timestamp, data)));
	}
}
