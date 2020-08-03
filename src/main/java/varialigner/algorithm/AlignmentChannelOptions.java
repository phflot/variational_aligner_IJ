package varialigner.algorithm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;

/**
 * 
 * Class that stores information about an image channel for registration
 * 
 * @author Philipp Flotho
 *
 */
public class AlignmentChannelOptions<T extends NativeType<T> & ComplexType<T>, A extends ArrayDataAccess<A>> {

	private ImagePlusImg<T, A> imp;
	private int channel;

	private float aData = 0.5f;
	private final float[] sigma = new float[] { 0.0f, 0.5f, 0.0f };
	private float weight = 1.0f;

	public float getAdata() {
		return aData;
	}

	public void setAdata(float aData) {
		this.aData = aData;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
		for (ActionListener l : actionListeners) {
			l.actionPerformed(new ActionEvent(this, 0, new String()));
		}
	}

	public float[] getSigma() {
		return sigma;
	}

	public AlignmentChannelOptions() {

	}

	public AlignmentChannelOptions(ImagePlusImg<T, A> imp) {
		this.imp = imp;
	}

	public AlignmentChannelOptions(ImagePlusImg<T, A> imp, float weight) {
		this.imp = imp;
		this.weight = weight;
	}

	@SuppressWarnings("unchecked")
	public ImagePlusImg<T, A> getImg() {
		ImagePlusImg<T, A> out = (ImagePlusImg<T, A>) imp.factory().create(imp.getHeight(), imp.getWidth());
		out.setPlane(0, imp.getPlane(this.channel));
		return imp;
	}

	public void setImg(ImagePlusImg<T, A> imp) {
		this.imp = imp;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	public int getChannel() {
		return this.channel;
	}

	private final List<ActionListener> actionListeners = new LinkedList<ActionListener>();

	public void addActionListener(ActionListener listener) {
		actionListeners.add(listener);
	}
}
