package varialigner.gui;

import java.awt.Choice;
import java.awt.Frame;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.List;

import ij.gui.GenericDialog;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.type.NativeType;
import varialigner.algorithm.AlignmentChannelOptions;

/**
 * Dialog that adds a new channel for the registration
 * 
 * @author Philipp Flotho
 *
 */
public class AddNewChannelDialog extends GenericDialog {

	private static final long serialVersionUID = 2206943449958360921L;

	@SuppressWarnings("rawtypes")
	private AlignmentChannelOptions registrationChannelOptions;
	final Choice ch_choice;

	@SuppressWarnings("rawtypes")
	public AlignmentChannelOptions getRegistrationChannelOptions() {
		return registrationChannelOptions;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public AddNewChannelDialog(String title, Frame parent, List<ImagePlusImg<? extends NativeType<?>, ?>> images) {
		super(title, parent);

		setOKLabel("Add");
		setResizable(false);

		registrationChannelOptions = new AlignmentChannelOptions();

		int n_images = images.size();
		String[] open_images = new String[n_images];
		for (int i = 0; i < n_images; i++) {
			open_images[i] = images.get(i).getImagePlus().getShortTitle();
		}

		// Selection of the input image channel (first 10?)
		int n_channels = images.get(0).numSlices();
		String[] n_channels_text = new String[n_channels];
		for (int i = 0; i < n_channels; i++) {
			n_channels_text[i] = String.valueOf(i + 1);
		}

		// Selection of the input image:
		addChoice("Image", open_images, open_images[0]);
		final Choice imp_choice = (Choice) this.getChoices().lastElement();
		addChoice("Channel", n_channels_text, n_channels_text[0]);
		ch_choice = (Choice) this.getChoices().lastElement();

		ch_choice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				registrationChannelOptions.setChannel(ch_choice.getSelectedIndex());
			}
		});

		registrationChannelOptions.setChannel(0);
		registrationChannelOptions.setImg(images.get(imp_choice.getSelectedIndex()));
		imp_choice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {

				int idx = imp_choice.getSelectedIndex();

				registrationChannelOptions.setImg(images.get(idx));

				int n_channels = images.get(idx).numSlices();
				ch_choice.removeAll();
				for (int i = 0; i < n_channels; i++) {
					ch_choice.add(String.valueOf(i + 1));
				}

				ch_choice.select(0);
			}
		});

		// Selection of sigma x
		addNumericField("sigma x", registrationChannelOptions.getSigma()[0], 2);
		final TextField sigmax_field = ((TextField) super.getNumericFields().lastElement());
		registrationChannelOptions.getSigma()[0] = Float.parseFloat(sigmax_field.getText());
		sigmax_field.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationChannelOptions.getSigma()[0] = Float.parseFloat(sigmax_field.getText());
			}
		});

		// Selection of sigma y
		addNumericField("sigma y", registrationChannelOptions.getSigma()[1], 2);
		final TextField sigmay_field = ((TextField) super.getNumericFields().lastElement());
		registrationChannelOptions.getSigma()[1] = Float.parseFloat(sigmay_field.getText());
		sigmay_field.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				registrationChannelOptions.getSigma()[1] = Float.parseFloat(sigmay_field.getText());
			}
		});
	}
}
