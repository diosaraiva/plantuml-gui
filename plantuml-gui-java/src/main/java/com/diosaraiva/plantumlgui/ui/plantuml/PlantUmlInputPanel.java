package com.diosaraiva.plantumlgui.ui.plantuml;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;
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
    private Runnable editStateListener = () -> { };

    public PlantUmlInputPanel() {
        codeTextArea.setFont(SwingUtils.monospaced(SwingUtils.CODE_FONT_SIZE));
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
        codeTextArea.addCaretListener(e -> fireEditStateChanged());
        inputTabs.addChangeListener(e -> fireEditStateChanged());
        updateCounts();

        sampleList.setSelectedValue(DiagramSample.STDLIB_ARCHIMATE_APPLICATION, true);
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
        fireEditStateChanged();
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
        fireEditStateChanged();
    }

    public void redo() {
        if (undoManager.canRedo()) { undoManager.redo(); }
        fireEditStateChanged();
    }

    public boolean isCodeTabSelected() { return inputTabs.getSelectedIndex() == CODE_TAB; }

    public boolean hasSelection() {
        return codeTextArea.getSelectionStart() != codeTextArea.getSelectionEnd();
    }

    public void copyToClipboard() { SwingUtils.copyText(codeTextArea); }

    public void cut() {
        focusEditor();
        codeTextArea.cut();
    }

    public void paste() {
        focusEditor();
        codeTextArea.paste();
    }

    public void selectAll() {
        focusEditor();
        codeTextArea.selectAll();
    }

    // Single slot rather than a list: the menu bar is rebuilt on every language or size change.
    public void setEditStateListener(Runnable listener) {
        editStateListener = listener;
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
        SwingUtils.setFontSize(autoPreviewCheck, SMALL_FONT);
        autoPreviewCheck.addActionListener(e -> {
            AppSettings.set(AppSettings.AUTO_PREVIEW, autoPreviewCheck.isSelected());
            updatePreviewButtonState();
        });
        SwingUtils.setFontSize(countLabel, 10f);
        SwingUtils.setFontSize(previewButton, SMALL_FONT);

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
            fireEditStateChanged();
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

    private void focusEditor() {
        inputTabs.setSelectedIndex(CODE_TAB);
        codeTextArea.requestFocusInWindow();
    }

    private void fireEditStateChanged() {
        editStateListener.run();
    }

    // Gallery entries in display order; each file is <constant name in lower case>.puml under resources/plantuml/samples.
    private enum DiagramSample {
        UML_ACTIVITY("UML - Activity"),
        UML_CLASS("UML - Class"),
        UML_COMMUNICATION("UML - Communication"),
        UML_COMPONENT("UML - Component"),
        UML_COMPOSITE("UML - Composite Structure"),
        UML_DEPLOYMENT("UML - Deployment"),
        UML_INTERACTION("UML - Interaction Overview"),
        UML_OBJECT("UML - Object"),
        UML_PACKAGE("UML - Package"),
        UML_PROFILE("UML - Profile"),
        UML_SEQUENCE("UML - Sequence"),
        UML_STATE("UML - State"),
        UML_TIMING("UML - Timing"),
        UML_USE_CASE("UML - Use Case"),
        DIAGRAM_BOARD("Diagram - Board"),
        DIAGRAM_CHART("Diagram - Chart"),
        DIAGRAM_CHEN("Diagram - Chen ER"),
        DIAGRAM_DITAA("Diagram - Ditaa"),
        DIAGRAM_EBNF("Diagram - EBNF"),
        DIAGRAM_FILES("Diagram - Files"),
        DIAGRAM_GANTT("Diagram - Gantt"),
        DIAGRAM_INFORMATION_ENGINEERING("Diagram - Information Engineering"),
        DIAGRAM_JSON("Diagram - JSON"),
        DIAGRAM_MIND_MAP("Diagram - Mind Map"),
        DIAGRAM_NWDIAG("Diagram - Network (nwdiag)"),
        DIAGRAM_PACKETDIAG("Diagram - Packet (packetdiag)"),
        DIAGRAM_REGEX("Diagram - Regex"),
        DIAGRAM_SALT("Diagram - Salt (Wireframe)"),
        DIAGRAM_SUB_DIAGRAM("Diagram - Sub-Diagram"),
        DIAGRAM_WBS("Diagram - Work Breakdown Structure"),
        DIAGRAM_WIRE("Diagram - Wire"),
        DIAGRAM_YAML("Diagram - YAML"),
        MATH_ASCIIMATH("Math - AsciiMath"),
        MATH_JLATEXMATH("Math - JLaTeXMath"),
        STDLIB_ADAML("Stdlib - AdaML"),
        STDLIB_ARCHIMATE_APPLICATION("Stdlib - ArchiMate Application"),
        STDLIB_ARCHIMATE_BUSINESS("Stdlib - ArchiMate Business"),
        STDLIB_ARCHIMATE_IMPLEMENTATION("Stdlib - ArchiMate Implementation"),
        STDLIB_ARCHIMATE_LAYERED("Stdlib - ArchiMate Layered"),
        STDLIB_ARCHIMATE_MOTIVATION("Stdlib - ArchiMate Motivation"),
        STDLIB_ARCHIMATE_NESTING("Stdlib - ArchiMate Nesting"),
        STDLIB_ARCHIMATE_PHYSICAL("Stdlib - ArchiMate Physical"),
        STDLIB_ARCHIMATE_SEQUENCE("Stdlib - ArchiMate Sequence"),
        STDLIB_ARCHIMATE_STRATEGY("Stdlib - ArchiMate Strategy"),
        STDLIB_ARCHIMATE_TECHNOLOGY("Stdlib - ArchiMate Technology"),
        STDLIB_AWS("Stdlib - AWS (legacy aws)"),
        STDLIB_AWSLIB_OVERVIEW("Stdlib - AWS Overview"),
        STDLIB_AWSLIB_COMPUTE("Stdlib - AWS Compute"),
        STDLIB_AWSLIB_CONTAINERS("Stdlib - AWS Containers"),
        STDLIB_AWSLIB_DATA("Stdlib - AWS Data"),
        STDLIB_AWSLIB_DEVOPS("Stdlib - AWS DevOps"),
        STDLIB_AWSLIB_EVENT_DRIVEN("Stdlib - AWS Event-Driven"),
        STDLIB_AWSLIB_NETWORK("Stdlib - AWS Network"),
        STDLIB_AWSLIB_SECURITY("Stdlib - AWS Security"),
        STDLIB_AWSLIB_SERVERLESS("Stdlib - AWS Serverless"),
        STDLIB_AWSLIB20_VERSIONED("Stdlib - AWS v20 (pinned version)"),
        STDLIB_AZURE_OVERVIEW("Stdlib - Azure Overview"),
        STDLIB_AZURE_BIG_DATA("Stdlib - Azure Big Data"),
        STDLIB_AZURE_CQRS("Stdlib - Azure CQRS"),
        STDLIB_AZURE_DEVOPS("Stdlib - Azure DevOps"),
        STDLIB_AZURE_DISASTER_RECOVERY("Stdlib - Azure Disaster Recovery"),
        STDLIB_AZURE_EVENT_DRIVEN("Stdlib - Azure Event-Driven"),
        STDLIB_AZURE_HUB_SPOKE("Stdlib - Azure Hub and Spoke"),
        STDLIB_AZURE_HYBRID("Stdlib - Azure Hybrid Connectivity"),
        STDLIB_AZURE_LANDING_ZONE("Stdlib - Azure Landing Zone"),
        STDLIB_AZURE_MICROSERVICES("Stdlib - Azure Microservices"),
        STDLIB_AZURE_N_TIER("Stdlib - Azure N-tier"),
        STDLIB_AZURE_SECURITY("Stdlib - Azure Security"),
        STDLIB_AZURE_VIRTUAL_WAN("Stdlib - Azure Virtual WAN"),
        STDLIB_AZURE_WEB_QUEUE_WORKER("Stdlib - Azure Web-Queue-Worker"),
        STDLIB_BOOTSTRAP("Stdlib - Bootstrap Icons"),
        STDLIB_C4_COMPONENT("Stdlib - C4 Component"),
        STDLIB_C4_CONTAINER("Stdlib - C4 Container"),
        STDLIB_C4_CONTEXT("Stdlib - C4 Context"),
        STDLIB_C4_DEPLOYMENT("Stdlib - C4 Deployment"),
        STDLIB_C4_DYNAMIC("Stdlib - C4 Dynamic"),
        STDLIB_CLASSY("Stdlib - Classy"),
        STDLIB_CLASSY_C4("Stdlib - Classy-C4"),
        STDLIB_CLOUDINSIGHT("Stdlib - Cloudinsight"),
        STDLIB_CLOUDOGU("Stdlib - Cloudogu"),
        STDLIB_DOMAINSTORY("Stdlib - Domain Story"),
        STDLIB_EDGY("Stdlib - EDGY"),
        STDLIB_EDGY2("Stdlib - EDGY (edgy2 styling)"),
        STDLIB_EIP("Stdlib - Enterprise Integration Patterns"),
        STDLIB_ELASTIC("Stdlib - Elastic"),
        STDLIB_GCP("Stdlib - Google Cloud"),
        STDLIB_IBM("Stdlib - IBM Cloud"),
        STDLIB_K8S_AKS("Stdlib - k8s on Azure AKS"),
        STDLIB_KUBERNETES("Stdlib - Kubernetes Sprites"),
        STDLIB_LOGOS("Stdlib - Logos"),
        STDLIB_MATERIAL("Stdlib - Material Icons"),
        STDLIB_OFFICE("Stdlib - Office"),
        STDLIB_OSA("Stdlib - Open Security Architecture"),
        STDLIB_OSA2("Stdlib - Open Security Architecture 2"),
        STDLIB_TUPADR3("Stdlib - Tupadr3 Icon Fonts"),
        PREPROCESSING_CONDITIONS("Preprocessing - Conditions"),
        PREPROCESSING_FUNCTION("Preprocessing - Function"),
        PREPROCESSING_JSON("Preprocessing - JSON"),
        PREPROCESSING_PROCEDURE("Preprocessing - Procedure"),
        PREPROCESSING_WHILE("Preprocessing - While"),
        STYLE_CSS("Style - CSS"),
        CUSTOM_ARCHIMATE("Custom - ArchiMate Macros"),
        CUSTOM_MODULAR("Custom - Modular Include"),
        REFERENCE_ARCHIMATE_SPRITES("Reference - ArchiMate Sprites"),
        REFERENCE_COLORS("Reference - Colors"),
        REFERENCE_EMOJI("Reference - Emoji"),
        REFERENCE_FORMATS("Reference - Supported Formats"),
        REFERENCE_OPENICONIC("Reference - Open Iconic"),
        REFERENCE_SKINPARAMS("Reference - Skin Parameters"),
        REFERENCE_STDLIB("Reference - Standard Library"),
        LIST_COLORS("List - Colors"),
        LIST_EMOJI("List - Emoji"),
        LIST_OPENICONIC("List - Open Iconic"),
        LIST_SKINPARAMS("List - Skin Parameters"),
        LIST_SPRITES("List - Sprites"),
        LIST_STDLIB("List - Standard Library"),
        LIST_STDLIB_DETAILED("List - Standard Library (detailed)"),
        LIST_THEMES("List - Themes");

        private final String displayName;

        DiagramSample(String displayName) {
            this.displayName = displayName;
        }

        String fileName() { return name().toLowerCase(Locale.ROOT) + ".puml"; }

        @Override public String toString() { return displayName; }
    }
}
