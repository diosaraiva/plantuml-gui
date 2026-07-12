package com.diosaraiva.plantumlgui.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;

public class PlantUmlLayoutPanel extends JPanel {

	private final TitledBorder titledBorder;

	private Component inputHeader;
	private Component inputCenter;
	private Component inputSouth;

	private Component outputHeader;
	private Component outputCenter;
	private Component outputSouth;

	private double inputWeight = 0.4;

	public PlantUmlLayoutPanel(String title) {
		super(new BorderLayout());
		titledBorder = BorderFactory.createTitledBorder(title);
		setBorder(titledBorder);
	}

	/** Sets the three stacked components of the input (left) section. */
	public void setInput(Component header, Component center, Component south) {
		this.inputHeader = header;
		this.inputCenter = center;
		this.inputSouth = south;
		rebuild();
	}

	/** Sets the three stacked components of the output (right) section. */
	public void setOutput(Component header, Component center, Component south) {
		this.outputHeader = header;
		this.outputCenter = center;
		this.outputSouth = south;
		rebuild();
	}

	/** Horizontal weight (0..1) given to the input column; the rest goes to output. */
	public void setInputWeight(double weight) {
		this.inputWeight = Math.max(0.1, Math.min(0.9, weight));
		rebuild();
	}

	public void setTitle(String title) {
		titledBorder.setTitle(title);
		repaint();
	}

	private boolean hasInput() {
		return inputHeader != null || inputCenter != null || inputSouth != null;
	}

	private void rebuild() {
		removeAll();
		var output = section(outputHeader, outputCenter, outputSouth);
		if (hasInput()) {
			var input = section(inputHeader, inputCenter, inputSouth);
			var split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, input, output);
			split.setResizeWeight(inputWeight);
			split.setContinuousLayout(true);
			split.setBorder(null);
			// Position the divider once the split pane has a real width.
			split.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					split.setDividerLocation(inputWeight);
					split.removeComponentListener(this);
				}
			});
			add(split, BorderLayout.CENTER);
		} else {
			add(output, BorderLayout.CENTER);
		}
		revalidate();
		repaint();
	}

	private static JPanel section(Component header, Component center, Component south) {
		var panel = new JPanel(new BorderLayout(0, 4));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		if (header != null) { panel.add(header, BorderLayout.NORTH); }
		if (center != null) { panel.add(center, BorderLayout.CENTER); }
		if (south != null) { panel.add(south, BorderLayout.SOUTH); }
		return panel;
	}
}
