package varialigner.algorithm;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.img.imageplus.IntImagePlus;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * 
 * Class with the solver for the displacement estimation
 * 
 * @author Philipp Flotho
 *
 */
public class DispSolver {

	public final DispSolverOptions options;
	private final float OMEGA = 1.95f;
	private int iterations;
	private int updateLag;
	private int levels;
	private float alpha;
	private float[] aData;
	@SuppressWarnings("unused")
	private float aSmooth;
	private float eta;
	private final float eps = 0.00001f;

	public static void main(String[] args) {

	}

	public DispSolver() {
		this(new DispSolverOptions());
	}

	public DispSolver(DispSolverOptions options) {
		this.options = options;
		iterations = options.iterations;
		updateLag = options.updateLag;
		levels = options.levels;
		alpha = options.alpha;
		aData = options.aData;
		aSmooth = options.aSmooth;
		eta = options.eta;
	}

	public AlignmentResult<FloatType, FloatArray> compensate(PlanarImg<FloatType, FloatArray> img,
			PlanarImg<FloatType, FloatArray> ref, PlanarImg<FloatType, FloatArray> dataWeightVector) {

		return compensate(img, ref, dataWeightVector, img);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>> AlignmentResult<T, A> compensate(
			PlanarImg<FloatType, FloatArray> img, PlanarImg<FloatType, FloatArray> ref,
			PlanarImg<FloatType, FloatArray> dataWeightVector, PlanarImg<T, A> registrationTargets) {

		PlanarImg<FloatType, FloatArray> v = align(img, ref, dataWeightVector);

		int height = (int) img.dimension(1);

		Util.dividePut(v, -1.0f);

		ImagePlusImg registered = (ImagePlusImg) registrationTargets.factory().create(registrationTargets);

		Util.alignVertical(registrationTargets, v, registered);

		IntImagePlus<ARGBType> visualization = Util
				.getVisualization(Util.resize(Util.imgToFloat(registered), new int[] { 4 * height, height }), -20);
		ImageJFunctions.show(visualization, "False Color Scaled");

		AlignmentResult result = new AlignmentResult(registered, v);

		return result;
	}

	/**
	 * img and ref are both MatVectors with one element for each channel
	 * 
	 * @param img
	 * @param ref
	 */
	@SuppressWarnings({ "unchecked" })
	public <I extends PlanarImg<FloatType, FloatArray>> I align(I img, I ref, I dataWeightVector) {

		int width = (int) img.dimension(0);
		int height = (int) img.dimension(1);
		int nChannels = img.numSlices();

		// a_data should be float[n_channels]:
		if (aData.length < nChannels) {
			float[] a_data_old = aData;
			aData = new float[nChannels];
			for (int i = 0; i < nChannels; i++)
				aData[i] = a_data_old.length < i ? a_data_old[i] : a_data_old[0];
		}

		int idx, idxDown, idxUp, idxLeft, ndIdx;
		float wDown, wUp, wSum, denomV, numV;

		float[] weight = new float[nChannels];
		float sum = 0;
		for (int i = 0; i < nChannels; i++) {
			float[] tmp = dataWeightVector.getPlane(i).getCurrentStorageArray();
			weight[i] = tmp[0];
			sum += tmp[0];
		}
		for (int i = 0; i < nChannels; i++) {
			weight[i] /= sum;
		}

		int max_level = warpingDepth(height);

		I movingLow = (I) img.copy();
		I fixedLow = (I) ref.copy();

		I v = (I) new ImagePlusImgFactory<>(new FloatType()).create(width, height);
		I vTmp = (I) v.factory().create(v);

		I movingLevel = (I) movingLow.copy();
		I fixedLevel = (I) fixedLow.copy();
		I tmp = (I) movingLevel.copy();

		/* outer loop with the pyramid */
		for (int l = max_level; l >= 0; l--) {

			double scalingFactor = Math.pow(eta, l);

			int[] levelSize = new int[] { (int) (Math.round((double) width)),
					(int) (Math.round(scalingFactor * height)) };

			int nxLevel = levelSize[0];
			int nyLevel = levelSize[1] + 2;

			// rescaling the working copies of the images
			movingLevel = Util.resize(movingLow, levelSize);
			fixedLevel = Util.resize(fixedLow, new int[] { 1, levelSize[1] });

			float hy = (float) height / (float) levelSize[1];

			if (l == max_level) {
				vTmp = (I) v.factory().create(levelSize);

				float[] vTmpPtr = vTmp.getPlane(0).getCurrentStorageArray();
				for (int i = 0; i < vTmpPtr.length; i++) {
					vTmpPtr[i] = 0.0f;
				}
				tmp = movingLevel;

			} else {

				vTmp = Util.resize(v, levelSize);
			}

			I vScaled = (I) vTmp.factory().create(vTmp);
			float[] vScaledPtr = vScaled.getPlane(0).getCurrentStorageArray();
			float[] vTmpPtr = vTmp.getPlane(0).getCurrentStorageArray();

			for (int i = 0; i < vTmpPtr.length; i++) {
				vScaledPtr[i] = vTmpPtr[i] / (-hy);
			}

			tmp = (I) movingLevel.factory().create(movingLevel);
			Util.alignVertical(movingLevel, vScaled, tmp);

			vTmp = Util.copyMakeBorderY(vTmp, Util.BORDER_CONSTANT);

			I dv = (I) vTmp.factory().create(vTmp);

			float[] vPtr = vTmp.getPlane(0).getCurrentStorageArray();
			float[] dvPtr = dv.getPlane(0).getCurrentStorageArray();

			float[] psi = new float[nyLevel * nChannels];
			float[] psiSmooth = new float[nyLevel];
			for (int i = 0; i < nxLevel * nyLevel; i++) {
				dvPtr[i] = 0.0f;
			}
			for (int j = 0; j < nyLevel; j++) {
				psiSmooth[j] = 1.0f;
			}
			for (int i = 0; i < nyLevel * nChannels; i++) {
				psi[i] = 1.0f;
			}

			// preparing the derivatives:
			I fyRefImg = Util.imgradientY(fixedLevel, hy);
			fyRefImg = Util.copyMakeBorderY(fyRefImg, Util.BORDER_CONSTANT);
			I fyImg = Util.imgradientY(tmp, hy);
			fyImg = Util.copyMakeBorderY(fyImg, Util.BORDER_CONSTANT);
			I fyyImg = Util.imgradientYY(tmp, hy);
			fyyImg = Util.copyMakeBorderY(fyyImg, Util.BORDER_CONSTANT);
			fixedLevel = Util.copyMakeBorderY(fixedLevel, Util.BORDER_CONSTANT);

			float[] dvTmp = new float[nyLevel];
			// loop along the temporal dimension
			for (int i = 0; i < nxLevel; i++) {

				for (int j = 0; j < nyLevel; j++) {
					dvTmp[j] = 0.0f;
				}
				if (i > 0) {
					for (int j = 0; j < nyLevel; j++) {
						idx = j * nxLevel + i;
						idxLeft = j * nxLevel + i - 1;
						dvTmp[j] = dvPtr[idxLeft];
						psiSmooth[j] = 1.0f;
					}
					for (int j = 0; j < nyLevel * nChannels; j++) {
						psi[j] = 1.0f;
					}
				}

				int iterationCounter = 0;
				/* outer loop: */
				while (iterationCounter++ < iterations) {

					if (iterationCounter % updateLag == 0) {
						nonlinearityData(psi, fyImg, fyyImg, fixedLevel, dvTmp, nChannels, aData, i);
						// aSmooth is kept at 1 for now:
						// nonlinearitySmoothness(psiSmooth, dvTmp, vTmp, aSmooth, hy, i);
					}

					/* Gauss Seidel step: */
					for (int j = 1; j < nyLevel - 1; j++) {

						idx = j * nxLevel + i;
						idxDown = (j + 1) * nxLevel + i;
						idxUp = (j - 1) * nxLevel + i;

						wDown = j < nyLevel - 2 ? 0.5f * (psiSmooth[j] + psiSmooth[j + 1]) * alpha / (hy * hy) : 0;
						wUp = j > 1 ? 0.5f * (psiSmooth[j] + psiSmooth[j - 1]) * alpha / (hy * hy) : 0;
						wSum = wUp + wDown;

						// summing up the data terms
						denomV = 0;
						numV = 0;
						for (int k = 0; k < nChannels; k++) {
							ndIdx = j * nxLevel + i;

							float[] fy = fyImg.getPlane(k).getCurrentStorageArray();
							float[] fyy = fyyImg.getPlane(k).getCurrentStorageArray();
							float[] f_ref_y = fyRefImg.getPlane(k).getCurrentStorageArray();

							numV -= weight[k] * psi[j + k * (nyLevel)]
									* ((fy[ndIdx] * fyy[ndIdx]) - (f_ref_y[j] * fyy[ndIdx]));
							denomV += weight[k] * psi[j + k * (nyLevel)] * fyy[ndIdx] * fyy[ndIdx];
						}

						denomV += wSum;

						dvTmp[j] = (1 - OMEGA) * dvTmp[j] + OMEGA * (numV + wUp * (vPtr[idxUp] + dvTmp[j - 1])
								+ wDown * (vPtr[idxDown] + dvTmp[j + 1]) - wSum * (vPtr[idx])) / (denomV);
					} // GS step
				} // outer loop

				for (int j = 0; j < nyLevel; j++) {
					dvPtr[j * nxLevel + i] = dvTmp[j];
				}

			} // temporal dim loop

			Util.medianBlur5y(dv);
			Util.addPut(vTmp, dv);

			v = (I) vTmp.factory().create(new long[] { nxLevel, nyLevel - 2 });

			RandomAccessibleInterval<FloatType> vInner = Views.offsetInterval(vTmp, new long[] { 0, 1, 0 },
					new long[] { nxLevel, nyLevel - 2, 1 });

			Cursor<FloatType> wCursor = v.cursor();
			RandomAccess<FloatType> wInnerAccess = vInner.randomAccess();

			while (wCursor.hasNext()) {
				wCursor.fwd();
				wInnerAccess.setPosition(wCursor);
				wCursor.get().set(wInnerAccess.get());
			}
		} // end pyramid

		ImageJFunctions.show(v, "Displacement field");
		return v;
	}

	@SuppressWarnings({ "unused" })
	private <I extends PlanarImg<FloatType, FloatArray>> void nonlinearitySmoothness(float[] psiSmooth, float[] dv, I v,
			float a, float hy, int i) {

		int width = (int) v.dimension(0);
		int height = (int) v.dimension(1);

		float vFullTmp[] = new float[height];

		float vTmp[] = v.getPlane(0).getCurrentStorageArray();

		int idx;
		for (int j = 0; j < height; j++) {
			idx = j * width + i;
			vFullTmp[j] = vTmp[idx] + dv[j];
		}

		float tmp, vy;
		float factor = 1.0f / (2 * hy);
		for (int j = 1; j < height - 1; j++) {
			vy = vFullTmp[j + 1] - vFullTmp[j - 1];
			vy *= factor;
			tmp = vy * vy;
			tmp = tmp < 0 ? 0 : tmp;
			psiSmooth[j] = a * (float) Math.pow(tmp + eps, a - 1);
		}
	}

	private <I extends PlanarImg<FloatType, FloatArray>> void nonlinearityData(float[] psi, I fyImg, I fyyImg, I ref,
			float[] dv, int n_channels, float[] a, int i) {

		float tmp;

		int width = (int) fyyImg.dimension(0);
		int height = (int) fyyImg.dimension(1);
		int idx;

		for (int k = 0; k < n_channels; k++) {

			float refPtr[] = ref.getPlane(k).getCurrentStorageArray();
			float fy[] = fyImg.getPlane(k).getCurrentStorageArray();
			float fyy[] = fyyImg.getPlane(k).getCurrentStorageArray();

			for (int j = 0; j < height; j++) {

				idx = j * width + i;
				tmp = fy[idx] + fyy[idx] * dv[j] - refPtr[j];
				tmp = tmp < 0 ? 0 : tmp;
				psi[j + k * height] = a[k] * (float) Math.pow(tmp + eps, a[k] - 1);
			}
		}
	}

	@SuppressWarnings("unused")
	private int[] maxCC(float[] input, float ref, int width, int height) {

		int[] ccOut = new int[width];

		for (int i = 0; i < width; i++) {
			float[] slice = new float[height];
			for (int j = 0; j < height; j++) {
				slice[j] = input[j * width + i];
			}
			float tmp = Float.MIN_VALUE;
			for (int j1 = 0; j1 < height; j1++) {
				float cc = 0;
				for (int j2 = 0; j2 < height; j2++) {
					// cc +=
				}
			}
		}
		return null;
	}

	private int warpingDepth(int height) {

		float min_dim = height;
		int warpingdepth = 0;

		for (int i = 1; i < levels; i++) {
			warpingdepth = warpingdepth + 1;
			min_dim = min_dim * eta;
			if (Math.round(min_dim) < 50)
				break;
		}
		return warpingdepth;
	}
}