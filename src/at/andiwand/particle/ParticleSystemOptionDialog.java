package at.andiwand.particle;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public class ParticleSystemOptionDialog extends JFrame {
    private static final long serialVersionUID = 2126532009795721171L;

    private static void centerJFrame(JFrame frame) {
	Dimension desktop = Toolkit.getDefaultToolkit().getScreenSize();
	frame.setLocation((desktop.width - frame.getWidth()) / 2,
		(desktop.height - frame.getHeight()) / 2);
    }

    private final Object waitObject = new Object();

    private final JComboBox<DisplayMode> displayMode;
    private final JSpinner particleCount;
    private final JCheckBox calcRatio;
    private final JCheckBox fullscreen;
    private final JCheckBox vSync;

    public ParticleSystemOptionDialog(String title) throws LWJGLException {
	super(title);

	GroupLayout groupLayout = new GroupLayout(getContentPane());
	getContentPane().setLayout(groupLayout);

	groupLayout.setAutoCreateGaps(true);
	groupLayout.setAutoCreateContainerGaps(true);

	DisplayMode[] displayModes = Display.getAvailableDisplayModes();
	for (int i = 2; i < displayModes.length; i++) {
	    for (int j = i; j >= 1; j--) {
		if (displayModes[j - 1].getWidth() > displayModes[j].getWidth())
		    break;
		if (displayModes[j - 1].getWidth() == displayModes[j]
			.getWidth()) {
		    if (displayModes[j - 1].getHeight() > displayModes[j]
			    .getHeight())
			break;
		    if (displayModes[j - 1].getHeight() == displayModes[j]
			    .getHeight()) {
			if (displayModes[j - 1].getBitsPerPixel() > displayModes[j]
				.getBitsPerPixel())
			    break;
			if (displayModes[j - 1].getBitsPerPixel() == displayModes[j]
				.getBitsPerPixel()) {
			    if (displayModes[j - 1].getFrequency() >= displayModes[j]
				    .getFrequency())
				break;
			}
		    }
		}

		DisplayMode tmp = displayModes[j];
		displayModes[j] = displayModes[j - 1];
		displayModes[j - 1] = tmp;
	    }
	}
	displayMode = new JComboBox<DisplayMode>(displayModes);

	SpinnerNumberModel particleCountModel = new SpinnerNumberModel(1000000,
		0, Integer.MAX_VALUE, 1000000);
	particleCount = new JSpinner(particleCountModel);

	calcRatio = new JCheckBox("Calc Ratio", true);
	fullscreen = new JCheckBox("Fullscreen");
	vSync = new JCheckBox("VSync", true);

	JButton start = new JButton("Start");
	start.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		synchronized (waitObject) {
		    waitObject.notify();
		}
	    }
	});

	groupLayout.setHorizontalGroup(groupLayout.createParallelGroup()
		.addComponent(displayMode).addComponent(particleCount)
		.addComponent(calcRatio).addComponent(fullscreen)
		.addComponent(vSync)
		.addComponent(start, GroupLayout.Alignment.TRAILING));

	groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
		.addComponent(displayMode).addComponent(particleCount)
		.addComponent(calcRatio).addComponent(fullscreen)
		.addComponent(vSync).addComponent(start));

	pack();
	centerJFrame(this);
	setResizable(false);
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	setVisible(true);
    }

    public DisplayMode getDisplayMode() {
	return (DisplayMode) displayMode.getSelectedItem();
    }

    public int getParticleCount() {
	return (Integer) particleCount.getValue();
    }

    public boolean isCalcRatio() {
	return calcRatio.isSelected();
    }

    public boolean isFullscreen() {
	return fullscreen.isSelected();
    }

    public boolean isVSync() {
	return vSync.isSelected();
    }

    public void waitUntilCommit() {
	synchronized (waitObject) {
	    try {
		waitObject.wait();
	    } catch (InterruptedException e1) {
	    }
	}
    }
}