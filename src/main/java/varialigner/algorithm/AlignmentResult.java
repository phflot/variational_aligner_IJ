package varialigner.algorithm;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Structure to store the results after alignment
 * 
 * @author Philipp Flotho
 *
 */
public class AlignmentResult<T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>> {

	private final ImagePlusImg<T, A> aligned;
	private final PlanarImg<FloatType, FloatArray> v;

	public AlignmentResult(ImagePlusImg<T, A> aligned, PlanarImg<FloatType, FloatArray> v) {
		this.aligned = aligned;
		this.v = v;
	}

	public ImagePlusImg<T, A> getRegistered() {
		return aligned;
	}

	public PlanarImg<FloatType, FloatArray> getW() {
		return v;
	}
}