package tsdb.dsl;

import java.util.Map;

public class FormulaSub extends FormulaBinary {
	public FormulaSub(Formula a, Formula b) {
		super(a, b);
	}
	@Override
	public Computation compile(Map<String, Integer> sensorMap) {
		return new Computation() {
			Computation x = a.compile(sensorMap);
			Computation y = b.compile(sensorMap);
			@Override
			public float eval(float[] data) {
				return x.eval(data) - y.eval(data);
			}
		};
	}
	@Override
	public String compileToString(Map<String, Integer> sensorMap) {
		String ja = a.compileToString(sensorMap);
		String jb = b.compileToString(sensorMap);
		return "("+ja+"-"+jb+")";
	}
}