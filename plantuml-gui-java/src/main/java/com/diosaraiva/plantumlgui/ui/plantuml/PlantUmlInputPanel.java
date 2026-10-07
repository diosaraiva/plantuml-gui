package com.diosaraiva.plantumlgui.ui.plantuml;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;

import com.diosaraiva.plantumlgui.service.AppSettings;
import com.diosaraiva.plantumlgui.service.SampleLoader;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;
import com.diosaraiva.plantumlgui.util.TextLineNumber;

// Left-hand input: the code editor, the sample gallery and the auto-preview controls.
@SuppressWarnings("serial")
public final class PlantUmlInputPanel extends JPanel {

    private static final int CODE_TAB = 0;
    private static final int SAMPLES_TAB = 1;
    private static final Font EDITOR_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    private static final float SMALL_FONT = 11f;

    private final JTabbedPane inputTabs = new JTabbedPane();
    private final JTextArea codeTextArea = new JTextArea(10, 20);
    private final JList<DiagramSample> sampleList = new JList<>(DiagramSample.values());
    private final JLabel countLabel = new JLabel("", JLabel.RIGHT);
    private final JCheckBox autoPreviewCheck = new JCheckBox(I18n.get("input.autoPreview"),
            AppSettings.getBoolean(AppSettings.AUTO_PREVIEW, true));
    private final JButton previewButton = new JButton(I18n.get("input.preview"));
    private final JPanel controlsBar = new JPanel(new BorderLayout(8, 0));

    private final UndoManager undoManager = new UndoManager();
    private final List<Runnable> undoStateListeners = new ArrayList<>();

    public PlantUmlInputPanel() {
        codeTextArea.setFont(EDITOR_FONT);
        codeTextArea.setLineWrap(true);
        codeTextArea.setWrapStyleWord(false);

        var editorScroll = new JScrollPane(codeTextArea);
        editorScroll.setRowHeaderView(new TextLineNumber(codeTextArea));

        sampleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sampleList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { loadSample(); }
        });

        inputTabs.addTab(I18n.get("input.tab.code"), editorScroll);
        inputTabs.addTab(I18n.get("input.tab.samples"), new JScrollPane(sampleList));

        buildControlsBar();
        initUndo();

        codeTextArea.getDocument().addDocumentListener(SwingUtils.onDocumentChange(this::updateCounts));
        updateCounts();

        sampleList.setSelectedValue(DiagramSample.ARCHIMATE_APPLICATION, true);
        inputTabs.setSelectedIndex(CODE_TAB);
    }

    public JComponent getEditorComponent() { return inputTabs; }

    public JComponent getControlsComponent() { return controlsBar; }

    public String getCode() { return codeTextArea.getText().trim(); }

    // Replaces the buffer and resets history; used by File > Open and the sample gallery.
    public void setCode(String code) {
        codeTextArea.setText(code);
        codeTextArea.setCaretPosition(0);
        undoManager.discardAllEdits();
        fireUndoStateChanged();
    }

    public void addCodeDocumentListener(DocumentListener listener) {
        codeTextArea.getDocument().addDocumentListener(listener);
    }

    public boolean isAutoPreviewEnabled() { return autoPreviewCheck.isSelected(); }

    public void addPreviewButtonListener(ActionListener listener) {
        previewButton.addActionListener(listener);
    }

    public void addAutoPreviewListener(ActionListener listener) {
        autoPreviewCheck.addActionListener(listener);
    }

    public boolean canUndo() { return undoManager.canUndo(); }

    public boolean canRedo() { return undoManager.canRedo(); }

    public void undo() {
        if (undoManager.canUndo()) { undoManager.undo(); }
        fireUndoStateChanged();
    }

    public void redo() {
        if (undoManager.canRedo()) { undoManager.redo(); }
        fireUndoStateChanged();
    }

    // Copies the selection, or the whole buffer when nothing is selected.
    public void copyToClipboard() {
        String selected = codeTextArea.getSelectedText();
        SwingUtils.copyText(selected == null || selected.isEmpty() ? codeTextArea.getText() : selected);
    }

    public void paste() { codeTextArea.paste(); }

    public void selectAll() {
        inputTabs.setSelectedIndex(CODE_TAB);
        codeTextArea.requestFocusInWindow();
        codeTextArea.selectAll();
    }

    public void addUndoStateListener(Runnable listener) {
        undoStateListeners.add(listener);
    }

    public void applyLanguage() {
        inputTabs.setTitleAt(CODE_TAB, I18n.get("input.tab.code"));
        inputTabs.setTitleAt(SAMPLES_TAB, I18n.get("input.tab.samples"));
        autoPreviewCheck.setText(I18n.get("input.autoPreview"));
        previewButton.setText(I18n.get("input.preview"));
        updateCounts();
        repaint();
    }

    private void buildControlsBar() {
        autoPreviewCheck.setFont(autoPreviewCheck.getFont().deriveFont(Font.PLAIN, SMALL_FONT));
        autoPreviewCheck.addActionListener(e -> {
            AppSettings.set(AppSettings.AUTO_PREVIEW, autoPreviewCheck.isSelected());
            updatePreviewButtonState();
        });
        countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 10f));
        previewButton.setFont(previewButton.getFont().deriveFont(Font.PLAIN, SMALL_FONT));

        controlsBar.add(autoPreviewCheck, BorderLayout.WEST);
        controlsBar.add(countLabel, BorderLayout.CENTER);
        controlsBar.add(previewButton, BorderLayout.EAST);
        updatePreviewButtonState();
    }

    // Manual rendering only makes sense while auto preview is off.
    private void updatePreviewButtonState() {
        previewButton.setEnabled(!autoPreviewCheck.isSelected());
    }

    private void updateCounts() {
        countLabel.setText(I18n.get("input.counts",
                codeTextArea.getDocument().getLength(), codeTextArea.getLineCount()));
    }

    private void initUndo() {
        codeTextArea.getDocument().addUndoableEditListener(e -> {
            undoManager.addEdit(e.getEdit());
            fireUndoStateChanged();
        });
        int mod = SwingUtils.menuShortcut();
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod), "undo", this::undo);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mod), "redo", this::redo);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod | InputEvent.SHIFT_DOWN_MASK), "redo", this::redo);
    }

    private void bindKey(KeyStroke stroke, String actionKey, Runnable action) {
        codeTextArea.getInputMap().put(stroke, actionKey);
        codeTextArea.getActionMap().put(actionKey, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void loadSample() {
        DiagramSample sample = sampleList.getSelectedValue();
        if (sample == null) {
            return;
        }
        try {
            setCode(SampleLoader.load(sample.fileName()));
        } catch (Exception ex) {
            setCode(I18n.get("sample.load.failed", ex.getMessage()));
        }
        inputTabs.setSelectedIndex(CODE_TAB);
    }

    private void fireUndoStateChanged() {
        undoStateListeners.forEach(Runnable::run);
    }

    // Gallery entries; the file name resolves under resources/plantuml/samples.
    public enum DiagramSample {
    	ARCHIMATE_APPLICATION("Archimate Application", "archimate_application.puml"),
    	ARCHIMATE_BUSINESS("Archimate Business", "archimate_business.puml"),
    	ARCHIMATE_IMPLEMENTATION("Archimate Implementation", "archimate_implementation.puml"),
    	ARCHIMATE_JUNCTIONS("Archimate Junctions", "archimate_junctions.puml"),
    	ARCHIMATE_LAYERED("Archimate Layered", "archimate_layered.puml"),
    	ARCHIMATE_MOTIVATION("Archimate Motivation", "archimate_motivation.puml"),
    	ARCHIMATE_PHYSICAL("Archimate Physical", "archimate_physical.puml"),
    	ARCHIMATE_SEQUENCE("Archimate Sequence", "archimate_sequence.puml"),
    	ARCHIMATE_STRATEGY("Archimate Strategy", "archimate_strategy.puml"),
    	ARCHIMATE_TECHNOLOGY("Archimate Technology", "archimate_technology.puml"),
    	BOARD("Board", "board.puml"),
    	C4_COMPONENT("C4 Component", "c4_component.puml"),
    	C4_CONTAINER("C4 Container", "c4_container.puml"),
    	C4_CONTEXT("C4 Context", "c4_context.puml"),
    	C4_DEPLOYMENT("C4 Deployment", "c4_deployment.puml"),
    	CSS_STYLING("CSS Styling", "css_styling.puml"),
    	CHART("Chart", "chart.puml"),
    	DITAA("Ditaa", "ditaa.puml"),
    	EBNF("EBNF", "ebnf.puml"),
    	FILES("Files", "files.puml"),
    	GANTT("Gantt", "gantt.puml"),
    	IE("Information Engineering", "ie.puml"),
    	JSON("JSON", "json.puml"),
    	MINDMAP("Mind Map", "mindmap.puml"),
    	NWDIAG("Network Diagram (nwdiag)", "nwdiag.puml"),
    	PACKETDIAG("Packet Diagram", "packetdiag.puml"),
    	REGEX("Regex", "regex.puml"),
    	SALT("Salt (Wireframe)", "salt.puml"),
    	SUBDIAGRAM("Sub-Diagram", "subdiagram.puml"),
    	TIMING("Timing", "timing.puml"),
    	UML_ACTIVITY("UML Activity", "uml_activity.puml"),
    	UML_CLASS("UML Class", "uml_class.puml"),
    	UML_COMPONENT("UML Component", "uml_component.puml"),
    	UML_DEPLOYMENT("UML Deployment", "uml_deployment.puml"),
    	UML_OBJECT("UML Object", "uml_object.puml"),
    	UML_SEQUENCE("UML Sequence", "uml_sequence.puml"),
    	UML_STATE("UML State", "uml_state.puml"),
    	UML_USE_CASE("UML Use Case", "uml_usecase.puml"),
    	WBS("Work Breakdown Structure", "wbs.puml"),
    	WIRE("Wire", "wire.puml"),
    	YAML("YAML", "yaml.puml"),
    	CONFIG_COLORS("Config Colors", "config_colors.puml"),
    	CONFIG_EMOJI26("Config Emoji 26", "config_emoji26.puml"),
    	CONFIG_LISTOPENICONIC("Config List Open Iconic", "config_listopeniconic.puml"),
    	CONFIG_LISTSPRITES("Config List Sprites", "config_listsprites.puml"),
    	CONFIG_NESTING("Config Nesting", "config_nesting.puml"),
    	CONFIG_SKINPARAMETERS("Config Skin Parameters", "config_skinparameters.puml"),
    	CONFIG_STANDARLIBRARIES("Config Standard Libraries", "config_stdlib.puml"),
    	CONFIG_SUPPORTEDFORMATS("Config Supported Formats", "config_supportedformats.puml"),
    	CUSTOM_ARCHIMATE("Custom Archimate", "custom_archimate.puml"),
    	CUSTOM_MODULAR("Custom Modular", "custom_modular.puml"),
    	//CUSTOM_MODULAR_REF("Custom Modular Reference", "custom_modular_ref.puml"),
    	UTILS_COLORS("List Available [colors]", "util_colors.puml"),
    	UTILS_EMOJI("List Available [emoji]", "util_emoji.puml"),
    	UTILS_OPENICONIC("List Available [icons]", "util_openiconic.puml"),
    	UTILS_SKINPARAMS("List Available [skinparams]", "util_skinparams.puml"),
    	UTILS_SPRITES("List Available [sprites]", "util_sprites.puml"),
    	UTILS_THEMES("List Available [themes]", "util_themes.puml");

        private final String displayName;
        private final String fileName;

        DiagramSample(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String fileName() { return fileName; }

        @Override public String toString() { return displayName; }
    }
}
