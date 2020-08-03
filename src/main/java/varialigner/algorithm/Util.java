package varialigner.algorithm;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.convolution.Convolution;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.convolution.kernel.SeparableKernelConvolution;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.img.imageplus.IntImagePlus;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 
 * Class with various utility functions
 * 
 * @author Philipp Flotho
 *
 */

public class Util {

	public static final int BORDER_REFLECT_101 = 0;
	public static final int BORDER_CONSTANT = 1;

	public static final int INTERP_LINEAR = 0;

	private static final Convolution<NumericType<?>> dyConvolver = SeparableKernelConvolution
			.convolution1d(Kernel1D.centralAsymmetric(new double[] { -1.0f, 0.0f, 1.0f }), 1);
	private static final Convolution<NumericType<?>> dyyConvolver = SeparableKernelConvolution
			.convolution1d(Kernel1D.centralAsymmetric(new double[] { 1.0f, -2.0f, 1.0f }), 1);

	/**
	 * function that aligns given an input image @param img and displacement
	 * field @param v into @param target
	 */
	public static <T extends NumericType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>> void alignVertical(
			I img, PlanarImg<FloatType, FloatArray> v, I target) {

		NLinearInterpolatorFactory<T> interpolatorFactory = new NLinearInterpolatorFactory<>();
		RealRandomAccessible<T> interpolator = Views.interpolate(Views.extendZero(img), interpolatorFactory);

		int width = (int) img.dimension(0);

		Cursor<T> inputCursor = img.cursor();
		Cursor<T> outputCursor = target.cursor();

		RealRandomAccess<T> rra = interpolator.realRandomAccess();

		float[] vArr = v.getPlane(0).getCurrentStorageArray();

		while (inputCursor.hasNext()) {
			inputCursor.fwd();
			outputCursor.fwd();

			int xPos = inputCursor.getIntPosition(0);
			int yPos = inputCursor.getIntPosition(1);
			int idx = yPos * width + xPos;

			rra.setPosition(new double[] { xPos, (double) yPos + (double) vArr[idx],
					inputCursor.getDoublePosition(2) });
			outputCursor.get().set(rra.get());
		}
	}

	/**
	 * Functions to handle any imp file type
	 */
	public static <T extends NativeType<T> & ComplexType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>> ImagePlusImg<FloatType, FloatArray> imgToFloat(
			I img) {

		Cursor<T> inCursor = img.cursor();

		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);

		@SuppressWarnings("unchecked")
		ImagePlusImg<FloatType, FloatArray> out = (ImagePlusImg<FloatType, FloatArray>) new ImagePlusImgFactory<FloatType>(
				new FloatType()).create(dims);

		Cursor<FloatType> outCursor = out.cursor();

		while (inCursor.hasNext()) {
			inCursor.fwd();
			outCursor.fwd();

			outCursor.get().setReal(inCursor.get().getRealFloat());
		}

		return out;
	}

	public static <T extends NativeType<T> & ComplexType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>> ImagePlusImg<FloatType, FloatArray> imgToFloatNormalize(
			I img, float minValue, float maxValue) {

		Cursor<T> inCursor = img.cursor();

		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);

		@SuppressWarnings("unchecked")
		ImagePlusImg<FloatType, FloatArray> out = (ImagePlusImg<FloatType, FloatArray>) new ImagePlusImgFactory<FloatType>(
				new FloatType()).create(dims);

		Cursor<FloatType> outCursor = out.cursor();

		while (inCursor.hasNext()) {
			inCursor.fwd();
			outCursor.fwd();

			outCursor.get().setReal((inCursor.get().getRealFloat() - minValue) / (maxValue - minValue));
		}

		return out;
	}

	/**
	 * Uses the first to channels for a color mapping
	 * 
	 * @param <I>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> IntImagePlus<ARGBType> getVisualization(I img,
			int redLineIdx) {

		ImagePlusImgFactory<ARGBType> factory = new ImagePlusImgFactory<ARGBType>(new ARGBType());

		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);
		int width = (int) dims[0];
		int height = (int) dims[1];
		int n_channels = (int) dims[2];

		I imgLow = (I) img.factory().create(img);

		Gauss3.gauss(new double[] { 4, 4, 0 }, Views.extendBorder(img), imgLow);

		I scaled = (I) img.factory().create(img);

		IntStream.rangeClosed(0, n_channels - 1).parallel().forEach(n -> {

			float[] imgPtr = img.getPlane(n).getCurrentStorageArray();
			float[] imgLowPtr = imgLow.getPlane(n).getCurrentStorageArray();
			float[] scaledPtr = scaled.getPlane(n).getCurrentStorageArray();

			float min = Float.MAX_VALUE;
			float max = Float.MIN_VALUE;
			float tmp;

			for (int i = 0; i < imgPtr.length; i++) {
				tmp = imgLowPtr[i];
				if (tmp < min)
					min = tmp;
				if (tmp > max)
					max = tmp;
			}

			for (int i = 0; i < imgPtr.length; i++) {
				tmp = (imgPtr[i] - min) / max;
				if (tmp < 0)
					tmp = 0;
				if (tmp > 1)
					tmp = 1;
				scaledPtr[i] = tmp;
			}
		});

		IntImagePlus<ARGBType> out = (IntImagePlus<ARGBType>) factory.create(new int[] { width, height });

		int[] colorData = out.getPlane(0).getCurrentStorageArray();
		float[] ch1 = scaled.getPlane(0).getCurrentStorageArray();
		int idx;

		if (n_channels == 1) {
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					idx = j * width + i;
					colorData[idx] = ((int) (255 * ch1[idx]) & 0x0000FF) << 16
							| ((int) (255 * ch1[idx]) & 0x0000FF) << 8 | ((int) (255 * ch1[idx]) & 0x0000FF);
					if ((redLineIdx - i + 1) < 3 && (redLineIdx - i + 1) >= 0)
						colorData[idx] = 255 << 16;
				}
			}
		} else {
			float[] ch2 = scaled.getPlane(1).getCurrentStorageArray();
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					idx = j * width + i;
					colorData[idx] = ((int) (255 * ch1[idx]) & 0x0000FF) << 16
							| ((int) (255 * (0.5 * ch1[idx] + 0.5 * ch2[idx])) & 0x0000FF) << 8
							| ((int) (255 * ch2[idx]) & 0x0000FF);
					if ((redLineIdx - i + 1) < 3 && (redLineIdx - i + 1) >= 0)
						colorData[idx] = 255 << 16;
				}
			}
		}

		return out;
	}

	public static <T extends ComplexType<T>, I extends IterableInterval<T>> float getMin(I img) {

		Cursor<T> inCursor = img.cursor();

		float min = Float.MAX_VALUE;

		while (inCursor.hasNext()) {
			inCursor.fwd();

			float tmp = inCursor.get().getRealFloat();
			min = tmp < min ? tmp : min;
		}

		return min;
	}

	public static <T extends ComplexType<T>, I extends IterableInterval<T>> float getMax(I img) {

		Cursor<T> inCursor = img.cursor();

		float max = Float.MIN_VALUE;

		while (inCursor.hasNext()) {
			inCursor.fwd();

			float tmp = inCursor.get().getRealFloat();
			max = tmp > max ? tmp : max;
		}

		return max;
	}

	public static <T extends ComplexType<T>, I extends IterableInterval<T>> float getMean(I img) {

		Cursor<T> inCursor = img.cursor();

		float mean = 0.0f;
		int counter = 0;

		while (inCursor.hasNext()) {
			inCursor.fwd();

			mean += inCursor.get().getRealFloat();
			counter++;
		}

		return mean / counter;
	}

	/**
	 * generates the
	 * 
	 * @param <I>
	 * @param img
	 * @param prog between zero and one
	 * @return
	 */
	// TODO: implement once the rest works
	public static <I extends PlanarImg<FloatType, FloatArray>> I getProgressViz(I img, float prog, float a, float b) {
		return null;
	}

	/**
	 * returns the mean along the second dimension
	 * 
	 * @param <I>
	 * @param img
	 * @return
	 */
	public static <I extends PlanarImg<FloatType, FloatArray>> I meanY(I img) {
		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);

		int width = (int) dims[0];

		return meanY(img, 0, width);
	}

	/**
	 * returns the mean along the second dimension between index @param start
	 * and @param end
	 * 
	 * @param <I>
	 * @param img
	 * @return
	 */
	public static <I extends PlanarImg<FloatType, FloatArray>> I meanY(I img, int start, int end) {

		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);

		int width = (int) dims[0];
		int height = (int) dims[1];

		@SuppressWarnings("unchecked")
		I out = (I) img.factory().create(1, dims[1], img.numSlices());

		float saver = 1.0f / Math.min((width - start), (end - start));

		// IntStream.rangeClosed(0, img.numSlices() - 1).parallel().forEach(n -> {
		for (int k = 0; k < img.numSlices(); k++) {
			int idx;
			float[] mean_slice = out.getPlane(k).getCurrentStorageArray();
			for (int j = 0; j < height; j++) {
				mean_slice[j] = 0;
			}

			float[] imgPtr = img.getPlane(k).getCurrentStorageArray();

			for (int i = start; i < width & i < end; i++) {
				for (int j = 0; j < height; j++) {
					idx = j * width + i;
					mean_slice[j] += saver * imgPtr[idx];
				}
			}
		}
		// });

		return out;
	}

	/**
	 * Set of functions that can be used in the same way as similarly named opencv
	 * functions
	 */
	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I resize(I img, double scalingFactor) {

		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);

		for (int i = 0; i <= 2; i++) {
			dims[i] = Math.round(dims[i] * scalingFactor);
		}

		I out = (I) img.factory().create(dims);

		resize(img, out, scalingFactor, scalingFactor);
		return out;
	}

	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I resize(I img, int[] dims) {

		int[] dimsFinal = dims;
		if (dims.length < img.numDimensions()) {
			dimsFinal = new int[img.numDimensions()];
			for (int i = 0; i < dimsFinal.length; i++) {
				if (i < dims.length)
					dimsFinal[i] = dims[i];
				else
					dimsFinal[i] = (int) img.dimension(i);
			}
		}

		I out = (I) img.factory().create(dimsFinal);
		double scalingFactorX = (double) dims[0] / (double) img.dimension(0);
		double scalingFactorY = (double) dims[1] / (double) img.dimension(1);

		resize(img, out, scalingFactorX, scalingFactorY);

		return out;
	}

	@SuppressWarnings("unchecked")
	public static <T extends NumericType<T>, I extends Img<T>> void resize(I img, I target, double scalingFactorX,
			double scalingFactorY) {

		long[] dims = new long[img.numDimensions()];
		img.dimensions(dims);
		I imgLowpass = (I) img.copy();

		double sigmaX = 0.0;
		if (scalingFactorX < 1)
			sigmaX = 1 / (2 * scalingFactorX);
		double sigmaY = 0.0;
		if (scalingFactorY < 1)
			sigmaY = 1 / (2 * scalingFactorY);
		Gauss3.gauss(new double[] { sigmaX, sigmaY }, Views.extendBorder(img), imgLowpass);

		NLinearInterpolatorFactory<T> interpolatorFactory = new NLinearInterpolatorFactory<>();
		RealRandomAccessible<T> interpolator = Views.interpolate(Views.extendZero(imgLowpass), interpolatorFactory);

		Cursor<T> outputCursor = target.cursor();

		RealRandomAccess<T> rra = interpolator.realRandomAccess();

		while (outputCursor.hasNext()) {
			outputCursor.fwd();

			double xPos = (outputCursor.getIntPosition(0)) / scalingFactorX;
			double yPos = (outputCursor.getIntPosition(1)) / scalingFactorY;

			rra.setPosition(new double[] { xPos, yPos, outputCursor.getDoublePosition(2) });

			outputCursor.get().set(rra.get());
		}
	}

	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I copyMakeBorderY(I img, int border) {

		long width = img.dimension(0);
		long height = img.dimension(1);
		int nChannels = img.numSlices();

		I out = (I) img.factory().create(width, height + 2, nChannels);

		copyMakeBorder(img, out, border);

		return out;
	}

	public static <T extends NumericType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>> void copyMakeBorder(
			I img, I target, int border) {

		ExtendedRandomAccessibleInterval<T, I> imgIn = Views.extendZero(img);
		switch (border) {
		case BORDER_REFLECT_101:
			imgIn = Views.extendMirrorSingle(img);
			break;
		}

		RandomAccess<T> ra = imgIn.randomAccess();
		Cursor<T> cursor = target.localizingCursor();

		int n_dims = imgIn.numDimensions();

		while (cursor.hasNext()) {
			cursor.fwd();

			if (n_dims == 3) {
				ra.setPosition(
						new int[] { cursor.getIntPosition(0), cursor.getIntPosition(1) - 1, cursor.getIntPosition(2) });
				cursor.get().set(ra.get());
			} else {
				ra.setPosition(new int[] { cursor.getIntPosition(0), cursor.getIntPosition(1) - 1 });
				cursor.get().set(ra.get());
			}
		}
	}

	public static <T extends FloatType, I extends IterableInterval<T> & RandomAccessibleInterval<T>> void dividePut(
			I img, float a) {

		Cursor<T> cursor = img.cursor();

		while (cursor.hasNext()) {
			cursor.fwd();
			T tmp = cursor.get();
			tmp.set(tmp.getRealFloat() / a);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends NumericType<T>, I extends Img<T>> I add(I img, I img2) {

		I out = (I) img.factory().create(img);
		addPut(out, img2);
		return out;
	}

	/**
	 * adds the two images into the first
	 * 
	 * @param <T>
	 * @param <I>
	 * @param img
	 * @param img2
	 */
	public static <T extends NumericType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>> void addPut(
			I img, I img2) {

		Cursor<T> cursor = img.cursor();
		Cursor<T> cursor2 = img2.cursor();

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor2.fwd();
			T tmp = cursor.get();
			tmp.add(cursor2.get());
		}
	}

	/**
	 * adds the two images into the first
	 * 
	 * @param <T>
	 * @param <I>
	 * @param img
	 * @param img2
	 */
	public static <T extends NumericType<T>, I extends IterableInterval<T> & RandomAccessibleInterval<T>> void subtractPut(
			I img, I img2) {

		Cursor<T> cursor = img.cursor();
		Cursor<T> cursor2 = img2.cursor();

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor2.fwd();
			T tmp = cursor.get();
			tmp.sub(cursor2.get());
		}
	}

	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I medianBlur5y(I img) {
		/*
		 * median filter along second dim of size 5
		 */
		I out = (I) img.factory().create(img);

		for (int c = 0; c < img.numSlices(); c++) {

			float[] f = img.getPlane(c).getCurrentStorageArray();
			float[] out_array = out.getPlane(c).getCurrentStorageArray();
			float[] tmp = new float[5];
			int nx = (int) img.dimension(0);
			int ny = (int) img.dimension(1);
			int idx, idx_kernel;

			for (int i = 2; i < nx - 2; i++) {
				for (int j = 2; j < ny - 2; j++) {
					idx = j * nx + i;
					for (int iy = -2; iy <= 2; iy++) {
						idx_kernel = (j + iy) * nx + i;
						tmp[iy + 2] = f[idx_kernel];
					}
					Arrays.sort(tmp);
					out_array[idx] = tmp[3];
				}
			}
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I imgradientY(I img, float hy) {
		I out = (I) img.factory().create(img);
		imgradientY(img, out, hy);
		return out;
	}

	public static <T extends NumericType<?>, I extends Img<FloatType>> void imgradientY(RandomAccessibleInterval<T> in,
			I out, float hy) {

		dyConvolver.process(Views.extendMirrorSingle(in), out);

		Cursor<FloatType> cursor = out.cursor();
		float tmp = 1.0f / (2.0f * hy);
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.get().mul(tmp);
		}
	}

	@SuppressWarnings("unchecked")
	public static <I extends PlanarImg<FloatType, FloatArray>> I imgradientYY(I img, float hy) {
		I out = (I) img.factory().create(img);
		imgradientYY(img, out, hy);

		return out;
	}

	public static <T extends NumericType<?>, I extends Img<FloatType>> void imgradientYY(RandomAccessibleInterval<T> in,
			I out, float hy) {

		dyyConvolver.process(Views.extendMirrorSingle(in), out);

		long[] dims = new long[out.numDimensions()];
		int y;
		out.dimensions(dims);

		Cursor<FloatType> cursor = out.cursor();
		float tmp = 1.0f / (hy * hy);
		while (cursor.hasNext()) {
			cursor.fwd();
			y = cursor.getIntPosition(1);
			if (y == 0 || y == dims[1] - 1)
				cursor.get().mul(0.0f);
			else
				cursor.get().mul(tmp);
		}
	}

	/**
	 * Utility functions for vector fields:
	 */
	public static <T extends NumericType<?>, I extends PlanarImg<FloatType, FloatArray>> float getMeanMagnitude(I w) {

		float[] u = w.getPlane(0).getCurrentStorageArray();
		float[] v = w.getPlane(1).getCurrentStorageArray();

		float tmp;
		float sum = 0;
		for (int i = 0; i < u.length; i++) {
			tmp = u[i] * u[i] + v[i] * v[i];
			tmp = tmp < 0 ? 0 : tmp;
			tmp = (float) Math.sqrt(tmp);
			sum += tmp;
		}

		return sum / u.length;
	}

	public static <T extends NumericType<?>, I extends PlanarImg<FloatType, FloatArray>> float getMaxMagnitude(I w) {

		float[] u = w.getPlane(0).getCurrentStorageArray();
		float[] v = w.getPlane(1).getCurrentStorageArray();

		float tmp;
		float maxMag = Float.MIN_VALUE;
		for (int i = 0; i < u.length; i++) {
			tmp = u[i] * u[i] + v[i] * v[i];
			tmp = tmp < 0 ? 0 : tmp;
			tmp = (float) Math.sqrt(tmp);
			maxMag = tmp > maxMag ? tmp : maxMag;
		}

		return maxMag / u.length;
	}

	public static class StopwatchTimer {
		private long startTime;

		public StopwatchTimer() {
			this.tic();
		}

		public void tic() {
			startTime = System.currentTimeMillis();
		}

		public double toc() {
			long elapsed = System.currentTimeMillis() - startTime;
			return (double) elapsed / 1000;
		}

		public void tocMsg(String msg) {
			System.out.println(msg + toc() + "s");
		}
	}

}