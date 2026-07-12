package com.diosaraiva.archutils.plantuml.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.diosaraiva.archutils.i18n.I18n;

public class PlantUmlExportPanel extends JPanel {

    private static final Map<String, String> FORMATS = new LinkedHashMap<>();
    static {
        FORMATS.put("PNG", "png");
        FORMATS.put("SVG", "svg");
        FORMATS.put("PUML", "puml");
        FORMATS.put(I18n.get("format.archimate"), "xml");
    }

    private final JTextField targetFileField;
    private final JButton browseButton;
    private final JComboBox<String> formatCombo;
    private final JButton exportButton;
    private final JButton copyImageButton;
    private final JLabel targetFileLabel = new JLabel(I18n.get("export.targetFile"));
    private final JLabel formatLabel = new JLabel(I18n.get("export.format"));
    private final javax.swing.border.TitledBorder titledBorder =
            BorderFactory.createTitledBorder(I18n.get("export.panel.title"));

    public PlantUmlExportPanel(String defaultTargetFile) {
        targetFileField = new JTextField(20);
        targetFileField.setText(defaultTargetFile);
        browseButton = new JButton(I18n.get("export.browse"));
        formatCombo = new JComboBox<>(new DefaultComboBoxModel<>(
                FORMATS.keySet().toArray(new String[0])));
        formatCombo.setSelectedItem("PNG");
        exportButton = new JButton(I18n.get("export.button"));
        copyImageButton = new JButton(I18n.get("export.copy"));
        copyImageButton.setToolTipText(I18n.get("export.copy.tooltip"));
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(titledBorder);
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        add(targetFileLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 1);
        add(targetFileField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 1, 4, 6);
        browseButton.addActionListener(e -> onBrowse());
        add(browseButton, gbc);

        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridx = 3;
        add(formatLabel, gbc);
        gbc.gridx = 4;
        formatCombo.setToolTipText(I18n.get("export.format.tooltip"));
        add(formatCombo, gbc);

        gbc.gridx = 5;
        add(exportButton, gbc);

        gbc.gridx = 6;
        add(copyImageButton, gbc);
    }

    public void applyLanguage() {
        titledBorder.setTitle(I18n.get("export.panel.title"));
        targetFileLabel.setText(I18n.get("export.targetFile"));
        formatLabel.setText(I18n.get("export.format"));
        browseButton.setText(I18n.get("export.browse"));
        exportButton.setText(I18n.get("export.button"));
        copyImageButton.setText(I18n.get("export.copy"));
        copyImageButton.setToolTipText(I18n.get("export.copy.tooltip"));
        formatCombo.setToolTipText(I18n.get("export.format.tooltip"));
        repaint();
    }

    private void onBrowse() {
        var chooser = new JFileChooser();
        chooser.setDialogTitle(I18n.get("export.browse.title"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            targetFileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public void onExportDiagram(ActionListener listener) {
        exportButton.addActionListener(listener);
    }

    public void onCopyImage(ActionListener listener) {
        copyImageButton.addActionListener(listener);
    }

    public void setCopyImageEnabled(boolean enabled) {
        copyImageButton.setEnabled(enabled);
    }

    public void onFormatChanged(ActionListener listener) {
        formatCombo.addItemListener((ItemListener) e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                listener.actionPerformed(null);
            }
        });
    }

    public String getSelectedFormat() {
        var selected = formatCombo.getSelectedItem();
        return FORMATS.getOrDefault(selected, "png");
    }

    public boolean isArchimateSelected() {
        return "xml".equals(getSelectedFormat());
    }

    public String getTargetFile() {
        return targetFileField.getText().trim();
    }

    public void setTargetFileExtension(String ext) {
        var current = targetFileField.getText().trim();
        int dot = current.lastIndexOf('.');
        var base = dot > 0 ? current.substring(0, dot) : current;
        targetFileField.setText(base + "." + ext);
    }
}
