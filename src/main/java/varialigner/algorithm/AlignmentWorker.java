package varialigner.algorithm;

import java.beans.PropertyChangeSupport;
import java.util.LinkedList;
import java.util.stream.IntStream;

import javax.swing.SwingWorker;

import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Collects the options and images and handles the image registration calls per
 * frame
 * 
 * @author Philipp Flotho
 *
 */
public class AlignmentWorker extends SwingWorker<Void, Void> {

	private final DispSolver ofInstance;
	private final AlignmentJob registrationJob;
	@SuppressWarnings("unused")
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private int nProcessedFrames = 0;
	private int framesLeft;
	private long startTime;

	public AlignmentWorker(AlignmentJob registrationJob) {
		ofInstance = registrationJob.getOFinstance();
		this.registrationJob = registrationJob;
		framesLeft = registrationJob.getNslices();
	}

	private synchronized void processedFrameNotification() {
		nProcessedFrames++;
		framesLeft--;
		firePropertyChange("n_processed_frames", nProcessedFrames - 1, nProcessedFrames);
		firePropertyChange("frames_left", framesLeft + 1, framesLeft);
		double tmp = (double) (System.currentTimeMillis() - startTime) / 1000;
		firePropertyChange("elapsed", tmp - 1, tmp);
	}

	/**
	 * 
	 * 
	 * @author Philipp Flotho
	 *
	 * @param <T>
	 * @param <A>
	 */
	private class IntermediateStruct<T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>> {

		private final AlignmentChannelOptions<T, A> o;
		private final ImagePlusImg<T, A> registrationTarget;
		private final ImagePlusImg<T, A> imgLow;

		private final float minValue;
		private final float maxValue;
		private final int width;
		private final int height;
		// private final boolean isInplace;

		private final static int nSlices = 1;

		/**
		 * deprecated, inplace will not be supported for the 1D version
		 * 
		 * @return
		 */
		/*
		 * public boolean isInplace() { return isInplace; }
		 */

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		@SuppressWarnings("unused")
		public int getnSlices() {
			return nSlices;
		}

		public ImagePlusImg<T, A> getRegistrationTarget() {
			return registrationTarget;
		}

		@SuppressWarnings("unchecked")
		public IntermediateStruct(AlignmentChannelOptions<T, A> o) {
			this.o = o;

			long[] dimsL = new long[o.getImg().numDimensions()];
			o.getImg().dimensions(dimsL);
			int[] dims = new int[o.getImg().numDimensions()];
			int counter = 0;
			for (long l : dimsL) {
				dims[counter] = (int) Math.max(Math.min(l, Integer.MAX_VALUE), Integer.MIN_VALUE);
				counter++;
			}

			this.width = dims[0];
			this.height = dims[1];

			registrationTarget = (ImagePlusImg<T, A>) o.getImg().factory().create(width, height, 1);
			registrationTarget.setPlane(0, o.getImg().getPlane(o.getChannel()));

			ImagePlusImg<T, A> imgLow = (ImagePlusImg<T, A>) o.getImg().factory().create(width, height, 1);

			float[] s = o.getSigma();
			Gauss3.gauss(new double[] { s[0], s[1], s[2] }, Views.extendMirrorSingle(registrationTarget), imgLow);

			this.imgLow = imgLow;
			minValue = Util.getMin(imgLow);
			maxValue = Util.getMax(imgLow);

			/*
			 * this.isInplace = o.isInplace(); if (o.isInplace()) { registrationTarget =
			 * o.getImg(); } else { registrationTarget = (ImagePlusImg<T, A>)
			 * o.getImg().factory().create(dims); }
			 */
		}

		public ImagePlusImg<FloatType, FloatArray> getFrameLow() {

			@SuppressWarnings("unchecked")
			ImagePlusImg<T, A> imgTmp = (ImagePlusImg<T, A>) imgLow.factory().create(width, height);
			imgTmp.setPlane(0, imgLow.getPlane(0));

			ImagePlusImg<FloatType, FloatArray> frameOut = Util.imgToFloatNormalize(imgTmp, minValue, maxValue);

			return frameOut;
		}

		public ImagePlusImg<T, A> getFrame() {
			@SuppressWarnings("unchecked")
			ImagePlusImg<T, A> imgTmp = (ImagePlusImg<T, A>) imgLow.factory().create(width, height);
			imgTmp.setPlane(0, o.getImg().getPlane(o.getChannel()));

			return imgTmp;
		}
	}

	/**
	 * list where each element represents one channel
	 * 
	 * @author Philipp Flotho
	 *
	 * @param <T>
	 * @param <A>
	 */
	private class IntermediateStructs<T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>>
			extends LinkedList<IntermediateStruct<T, A>> {

		public ImagePlusImg<FloatType, FloatArray> getFramesLow() {

			@SuppressWarnings("unchecked")
			ImagePlusImg<FloatType, FloatArray> out = (ImagePlusImg<FloatType, FloatArray>) this.getFirst()
					.getFrameLow().factory()
					.create(this.getFirst().getWidth(), this.getFirst().getHeight(), this.size());

			for (int i = 0; i < this.size(); i++) {
				out.setPlane(i, this.get(i).getFrameLow().getPlane(0));
			}

			return out;
		}

		public ImagePlusImg<T, A> getFrames() {

			@SuppressWarnings("unchecked")
			ImagePlusImg<T, A> out = (ImagePlusImg<T, A>) this.getFirst().getFrame().factory()
					.create(this.getFirst().getWidth(), this.getFirst().getHeight(), this.size());

			for (int i = 0; i < this.size(); i++) {
				out.setPlane(i, this.get(i).getFrame().getPlane(0));
			}

			return out;
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -7248646765861061239L;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected Void doInBackground() {

		IntermediateStructs<?, ?> intermediateStructs = new IntermediateStructs();

		DispSolver dispInstance = registrationJob.getOFinstance();

		for (AlignmentChannelOptions<?, ?> o : registrationJob) {
			IntermediateStruct tmp = new IntermediateStruct(o);
			intermediateStructs.add(tmp);
		}

		ImagePlusImg<FloatType, FloatArray> ref = Util.meanY(intermediateStructs.getFramesLow(), 200, 2000);

		ImagePlusImg<FloatType, FloatArray> dataWeightArray = registrationJob.getDataWeightArray();

		startTime = System.currentTimeMillis();

		ImagePlusImg<FloatType, FloatArray> img = intermediateStructs.getFramesLow();
		ImagePlusImg registrationTargets = intermediateStructs.getFrames();

		AlignmentResult registrationResult = dispInstance.compensate(img, ref, dataWeightArray, registrationTargets);

		for (int i = 0; i < intermediateStructs.size(); i++) {
			IntermediateStruct s = intermediateStructs.get(i);
			s.getRegistrationTarget().setPlane(0, registrationResult.getRegistered().getPlane(i));
		}

		IntStream.rangeClosed(0, registrationJob.getNslices() - 1).parallel().forEach(n -> {
			processedFrameNotification();
		});

		long elapsed = System.currentTimeMillis() - startTime;
		firePropertyChange("final_time", 0, (double) elapsed / 1000);

		firePropertyChange("settings", "", this.ofInstance.options.printOptions());

		for (int i = 0; i < intermediateStructs.size(); i++) {

			IntermediateStruct s = intermediateStructs.get(i);
			ImageJFunctions.show(s.registrationTarget, "registered ch " + i);
		}

		return null;
	}
}
