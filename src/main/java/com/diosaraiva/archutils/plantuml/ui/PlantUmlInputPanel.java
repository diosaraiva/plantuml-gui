package com.diosaraiva.archutils.plantuml.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;

import com.diosaraiva.archutils.ui.SwingUtils;
import com.diosaraiva.archutils.ui.TextLineNumber;
import com.diosaraiva.archutils.util.SampleLoader;

public class PlantUmlInputPanel extends JPanel {

    private final JComboBox<DiagramSample> sampleCombo;
    private final JTextArea codeTextArea;
    private final JLabel countLabel = new JLabel();
    private final JLabel samplesLabel = new JLabel(com.diosaraiva.archutils.i18n.I18n.get("input.samples"));
    private final JCheckBox autoPreviewCheck =
            new JCheckBox(com.diosaraiva.archutils.i18n.I18n.get("input.autoPreview"), true);
    private final JButton previewButton =
            new JButton(com.diosaraiva.archutils.i18n.I18n.get("input.preview"));
    private final javax.swing.border.TitledBorder titledBorder =
            BorderFactory.createTitledBorder(com.diosaraiva.archutils.i18n.I18n.get("input.title"));
    private final UndoManager undoManager = new UndoManager();
    private final List<Runnable> undoStateListeners = new ArrayList<>();

    public PlantUmlInputPanel() {
        sampleCombo = new JComboBox<>(DiagramSample.values());
        sampleCombo.setSelectedItem(DiagramSample.SEQUENCE);
        codeTextArea = new JTextArea(10, 20);
        codeTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(titledBorder);
        GridBagConstraints gbc = createGbc();

        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(samplesLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        sampleCombo.addActionListener(e -> loadSample());
        add(sampleCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        codeTextArea.setLineWrap(true);
        codeTextArea.setWrapStyleWord(false);
        JScrollPane scrollPane = new JScrollPane(codeTextArea);

        scrollPane.setRowHeaderView(new TextLineNumber(codeTextArea));
        add(scrollPane, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(createBottomBar(), gbc);

        initUndo();
        initCountLabel();
        loadSample();
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));

        autoPreviewCheck.setFont(autoPreviewCheck.getFont().deriveFont(Font.PLAIN, 11f));
        autoPreviewCheck.addActionListener(e -> updatePreviewButtonState());
        bar.add(autoPreviewCheck, BorderLayout.WEST);

        countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 10f));
        countLabel.setHorizontalAlignment(JLabel.RIGHT);
        bar.add(countLabel, BorderLayout.CENTER);

        previewButton.setFont(previewButton.getFont().deriveFont(Font.PLAIN, 11f));
        bar.add(previewButton, BorderLayout.EAST);

        updatePreviewButtonState();
        return bar;
    }

    private void updatePreviewButtonState() {
        previewButton.setEnabled(!autoPreviewCheck.isSelected());
    }

    private void initCountLabel() {
        codeTextArea.getDocument().addDocumentListener(SwingUtils.onDocumentChange(this::updateCounts));
        updateCounts();
    }

    private void updateCounts() {
        int chars = codeTextArea.getDocument().getLength();
        int lines = codeTextArea.getLineCount();
        countLabel.setText(com.diosaraiva.archutils.i18n.I18n.get("input.counts", chars, lines));
    }

    public void applyLanguage() {
        titledBorder.setTitle(com.diosaraiva.archutils.i18n.I18n.get("input.title"));
        samplesLabel.setText(com.diosaraiva.archutils.i18n.I18n.get("input.samples"));
        autoPreviewCheck.setText(com.diosaraiva.archutils.i18n.I18n.get("input.autoPreview"));
        previewButton.setText(com.diosaraiva.archutils.i18n.I18n.get("input.preview"));
        updateCounts();
        repaint();
    }

    private void initUndo() {
        codeTextArea.getDocument().addUndoableEditListener(e -> {
            undoManager.addEdit(e.getEdit());
            fireUndoStateChanged();
        });

        int mod = SwingUtils.menuShortcut();
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod), "archutils-undo", this::undo);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mod), "archutils-redo", this::redo);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod | InputEvent.SHIFT_DOWN_MASK),
                "archutils-redo", this::redo);
    }

    private void bindKey(KeyStroke stroke, String actionKey, Runnable action) {
        codeTextArea.getInputMap().put(stroke, actionKey);
        codeTextArea.getActionMap().put(actionKey, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void loadSample() {
        DiagramSample sample = (DiagramSample) sampleCombo.getSelectedItem();
        if (sample == null) return;
        try {
            codeTextArea.setText(SampleLoader.load(sample.getFileName()));
            codeTextArea.setCaretPosition(0);
        } catch (Exception ex) {
            codeTextArea.setText("Error loading sample: " + ex.getMessage());
        }

        undoManager.discardAllEdits();
        fireUndoStateChanged();
    }

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        return gbc;
    }

    public String getCode() { return codeTextArea.getText().trim(); }

    public void setCode(String code) {
        codeTextArea.setText(code);
        codeTextArea.setCaretPosition(0);
        undoManager.discardAllEdits();
        fireUndoStateChanged();
    }

    public void addCodeDocumentListener(DocumentListener listener) {
        codeTextArea.getDocument().addDocumentListener(listener);
    }

    public boolean isAutoPreviewEnabled() {
        return autoPreviewCheck.isSelected();
    }

    public void addPreviewButtonListener(ActionListener listener) {
        previewButton.addActionListener(listener);
    }

    public void addAutoPreviewListener(ActionListener listener) {
        autoPreviewCheck.addActionListener(listener);
    }

    public boolean canUndo() { return undoManager.canUndo(); }

    public boolean canRedo() { return undoManager.canRedo(); }

    public void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
        fireUndoStateChanged();
    }

    public void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
        fireUndoStateChanged();
    }

    public void copyToClipboard() {
        String selected = codeTextArea.getSelectedText();
        SwingUtils.copyText((selected != null && !selected.isEmpty())
                ? selected : codeTextArea.getText());
    }

    public void paste() {
        codeTextArea.paste();
    }

    public void addUndoStateListener(Runnable listener) {
        undoStateListeners.add(listener);
    }

    private void fireUndoStateChanged() {
        for (Runnable r : undoStateListeners) {
            r.run();
        }
    }

    public enum DiagramSample {
        ACTIVITY("Activity", "activity.puml"),
        ARCHIMATE_APPLICATION("Archimate Application", "archimate_application.puml"),
        ARCHIMATE_BUSINESS("Archimate Business", "archimate_business.puml"),
        ARCHIMATE_IMPLEMENTATION("Archimate Implementation", "archimate_implementation.puml"),
        ARCHIMATE_LAYERED("Archimate Layered", "archimate_layered.puml"),
        ARCHIMATE_MOTIVATION("Archimate Motivation", "archimate_motivation.puml"),
        ARCHIMATE_PHYSICAL("Archimate Physical", "archimate_physical.puml"),
        ARCHIMATE_STRATEGY("Archimate Strategy", "archimate_strategy.puml"),
        ARCHIMATE_TECHNOLOGY("Archimate Technology", "archimate_technology.puml"),
        C4_COMPONENT("C4 Component", "c4_component.puml"),
        C4_CONTAINER("C4 Container", "c4_container.puml"),
        C4_CONTEXT("C4 Context", "c4_context.puml"),
        C4_DEPLOYMENT("C4 Deployment", "c4_deployment.puml"),
        CLASS("Class", "class.puml"),
        COMPONENT("Component", "component.puml"),
        DEPLOYMENT("Deployment", "deployment.puml"),
        DITAA("Ditaa", "ditaa.puml"),
        FILES("Files", "files.puml"),
        GANTT("Gantt", "gantt.puml"),
        JSON("JSON", "json.puml"),
        MINDMAP("Mind Map", "mindmap.puml"),
        OBJECT("Object", "object.puml"),
        SEQUENCE("Sequence", "sequence.puml"),
        STATE("State", "state.puml"),
        TIMING("Timing", "timing.puml"),
        USE_CASE("Use Case", "usecase.puml"),
        WBS("Work Breakdown Structure", "wbs.puml"),
        YAML("YAML", "yaml.puml"),
        CUSTOM_ARCHIMATE("Custom Archimate", "custom_archimate.puml"),
        CUSTOM_MODULAR("Custom Modular", "custom_modular.puml"),
        UTILS_SKINPARAMS("List Available [skinparams]", "util_skinparams.puml"),
        UTILS_SPRITES("List Available [sprites]", "util_sprites.puml"),
        UTILS_COLORS("List Available [colors]", "util_colors.puml"),
        UTILS_OPENICONIC("List Available [icons]", "util_openiconic.puml"),
        UTILS_CREOLE("List Available [creole]", "creole.puml");

        private final String displayName;
        private final String fileName;

        DiagramSample(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getFileName() { return fileName; }

        @Override public String toString() { return displayName; }
    }
}
