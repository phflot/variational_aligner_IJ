package varialigner.algorithm;

/**
 * 
 * Class to store the solver specific parameters
 * 
 * @author Philipp Flotho
 *
 */
public class DispSolverOptions {
	public int iterations = 50;
	public int updateLag = 5;
	public int levels = 100;
	public float alpha = 0.5f;
	public float[] aData = new float[] { 0.5f, 0.5f };
	public float aSmooth = 0.5f;
	public float eta = 0.9f;
	public float sigma = 2.0f;

	public DispSolverOptions(int iterations, int updateLag, int levels, float alpha, float[] a_data, float a_smooth,
			float eta, float sigma) {
		this.iterations = iterations;
		this.updateLag = updateLag;
		this.levels = levels;
		this.alpha = alpha;
		this.aData = a_data;
		this.aSmooth = a_smooth;
		this.eta = eta;
		this.sigma = sigma;
	}

	public DispSolverOptions() {
	}

	public String printOptions() {
		String options = "iterations = " + iterations + "\n" + "levels = " + levels + "\n" + "alpha = " + alpha + "\n"
				+ "eta = " + eta;
		return options;
	}
}
