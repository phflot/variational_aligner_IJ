package varialigner;

import ij.IJ;
import ij.WindowManager;
import ij.gui.MessageDialog;
import ij.plugin.*;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.NativeType;
import varialigner.gui.*;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Main PlugIn class
 * 
 * @author Philipp Flotho
 *
 */
public class LineScanAligner implements PlugIn {

	@Override
	public void run(String arg) {

		final int[] imageIDX = WindowManager.getIDList();
		final int nImages = WindowManager.getImageCount();

		final List<ImagePlusImg<? extends NativeType<?>, ?>> images = new ArrayList<ImagePlusImg<? extends NativeType<?>, ?>>(
				nImages);

		for (int i = 0; i < nImages; i++) {
			images.add(i, ImagePlusImgs.from(WindowManager.getImage(imageIDX[i])));
		}

		if (images.isEmpty())
			new MessageDialog(null, "no open images",
					"At least one image needs to be open!");
		else
			new OptionsDialog(IJ.getInstance(), images).showDialog();
	}
}
