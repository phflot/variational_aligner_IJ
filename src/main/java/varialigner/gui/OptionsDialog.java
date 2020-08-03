package varialigner.gui;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.MessageDialog;
import ij.io.Opener;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.NativeType;
import varialigner.algorithm.AlignmentChannelOptions;
import varialigner.algorithm.AlignmentJob;
import varialigner.algorithm.AlignmentWorker;

/**
 * Main Dialog to select the solver and image parameters
 * 
 * @author Philipp Flotho
 * @param parent
 * @param images
 */
public class OptionsDialog extends GenericDialog {

	private static final long serialVersionUID = -1425554761163870030L;
	private final List<ImagePlusImg<? extends NativeType<?>, ?>> images;
	private final Frame parent;
	private int registrationQualitySetting = 1;
	private int nChannels = 0;
	private AlignmentJob alignmentJob;

	public static void main(String[] args) {
		
		ImageJ ij = new ImageJ();

		File file = new File("data/synth.tiff");
		final ImagePlus impCh1Shift = new Opener().openImage(file.getAbsolutePath());

		final List<ImagePlusImg<? extends NativeType<?>, ?>> images = new ArrayList<ImagePlusImg<? extends NativeType<?>, ?>>();

		images.add(ImagePlusImgs.from(impCh1Shift));

		impCh1Shift.setTitle("synth");

		impCh1Shift.show();

		new OptionsDialog(ij, images).showDialog();
	}

	public OptionsDialog(final Frame parent, List<ImagePlusImg<? extends NativeType<?>, ?>> images) {
		this(parent, images, new AlignmentJob());
	}

	public OptionsDialog(final Frame parent, List<ImagePlusImg<? extends NativeType<?>, ?>> images,
			AlignmentJob alignmentJob) {
		super("Line Scan Alignment", parent);
		this.images = images;
		this.parent = parent;
		this.alignmentJob = alignmentJob;

		setResizable(false);
		setOKLabel("Start");

		Panel channelPanel = new Panel();
		Button add_channel = new Button("add channel");
		GridBagConstraints c = new GridBagConstraints();
		channelPanel.setLayout(this.getLayout());
		addPanel(channelPanel);

		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.CENTER;
		channelPanel.add(add_channel, c);

		List<Label> labels = new LinkedList<Label>();
		List<JSpinner> weights = new LinkedList<JSpinner>();

		add_channel.addActionListener(new ActionListener() {
			@SuppressWarnings("rawtypes")
			@Override
			public void actionPerformed(ActionEvent e) {
				AddNewChannelDialog add_new_channel = new AddNewChannelDialog("channel " + ++nChannels, parent, images);
				add_new_channel.showDialog();
				if (add_new_channel.wasOKed()) {

					OptionsDialog this_tmp = OptionsDialog.this;

					if (!alignmentJob.add(add_new_channel.getRegistrationChannelOptions())) {
						new MessageDialog(parent, "Error adding channel",
								"Numer of Frames and dimensions in all sequences need to be identical!");
						return;
					}

					labels.add(
							new Label(add_new_channel.getRegistrationChannelOptions().getImg().getImagePlus().getTitle()
									+ ", ch" + (add_new_channel.getRegistrationChannelOptions().getChannel() + 1)));
					SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0.5f, 0.0f, 10.001f, 0.1);
					JSpinner spinner = new JSpinner(spinnerModel);

					// get the spinner text field and make it fire for manual input:

					JFormattedTextField spinnerText = (JFormattedTextField) spinner.getEditor().getComponent(0);
					((DefaultFormatter) spinnerText.getFormatter()).setCommitsOnValidEdit(true);

					add_new_channel.getRegistrationChannelOptions().addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							spinnerModel.setValue((double) add_new_channel.getRegistrationChannelOptions().getWeight());
						}
					});

					spinner.addChangeListener(new ChangeListener() {
						@Override
						public void stateChanged(ChangeEvent arg0) {
							add_new_channel.getRegistrationChannelOptions()
									.setWeight(spinnerModel.getNumber().floatValue());
						}
					});

					weights.add(spinner);

					int old_height = channelPanel.getHeight();

					channelPanel.removeAll();

					// c.anchor = GridBagConstraints.WEST;
					c.gridx = 0;
					c.gridy = 0;
					Iterator<JSpinner> weightIterator = weights.iterator();
					for (Label l : labels) {
						c.gridx = 0;
						channelPanel.add(l, c);
						c.gridx = 1;
						channelPanel.add(weightIterator.next(), c);

						c.gridy++;
					}
					c.gridy++;
					c.gridx = 0;
					channelPanel.add(add_channel, c);

					this_tmp.validate();
					this_tmp.setSize(this_tmp.getWidth(), this_tmp.getHeight() + channelPanel.getHeight() - old_height);
					this_tmp.validate();

					float weight = 1.0f / alignmentJob.size();
					for (AlignmentChannelOptions op : alignmentJob) {
						op.setWeight(weight);
					}
				}
			}
		});

		// Selection of alpha y:
		addNumericField("alpha", alignmentJob.getAlpha(), 2);
		final TextField alphay_field = ((TextField) super.getNumericFields().lastElement());
		alignmentJob.setAlpha(Float.parseFloat(alphay_field.getText()));
		alphay_field.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				alignmentJob.setAlpha(Float.parseFloat(alphay_field.getText()));
			}
		});

		String[] speed = new String[] { "fast", "balanced", "quality" };
		addChoice("Registration quality", speed, speed[registrationQualitySetting]);
		Choice qualityChoice = (Choice) super.getChoices().lastElement();
		qualityChoice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				registrationQualitySetting = qualityChoice.getSelectedIndex();
			}
		});

		JLabel projectLink = new JLabel("<html>Visit our <a href=''>project website</a><br /></html>");

		Panel projectLabelPanel = new Panel();
		projectLabelPanel.setLayout(this.getLayout());
		addPanel(projectLabelPanel);

		projectLabelPanel.add(projectLink);

		projectLink.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				try {

					Desktop.getDesktop().browse(new URI("https://phflot.github.io/variational_aligner/"));

				} catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
		});

	}

	@Override
	public synchronized void actionPerformed(final ActionEvent event) {
		super.actionPerformed(event);

		if (alignmentJob.isEmpty() && this.wasOKed()) {
			new MessageDialog((Frame) this.getParent(), "Need to select at least one channel",
					"Need to select at least one channel!");
			new OptionsDialog(parent, images).showDialog();
			return;
		}

		if (this.wasOKed()) {
			parseOptions();
			CompensationProgress progress_dialog = new CompensationProgress(IJ.getInstance(),
					alignmentJob.getNslices());
			AlignmentWorker engine = new AlignmentWorker(alignmentJob);
			engine.addPropertyChangeListener(progress_dialog);
			progress_dialog.setVisible(true);

			engine.execute();
		}
	}

	private void parseOptions() {
		switch (registrationQualitySetting) {
		case 0:
			alignmentJob.setIterations(20);
			break;
		case 1:
			alignmentJob.setIterations(70);
			break;
		case 2:
			alignmentJob.setIterations(100);
			alignmentJob.setEta(0.9f);
			break;
		}
	}
}
