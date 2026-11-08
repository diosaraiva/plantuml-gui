package com.diosaraiva.plantumlgui.ui.plantuml;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.diosaraiva.plantumlgui.service.AppSettings;
import com.diosaraiva.plantumlgui.service.ArchimatePlantUmlConverter;
import com.diosaraiva.plantumlgui.service.PlantUmlFormat;
import com.diosaraiva.plantumlgui.service.PlantUmlRenderer;
import com.diosaraiva.plantumlgui.util.Background;
import com.diosaraiva.plantumlgui.util.FileNames;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;

// Wires input, output and footer together and owns every render/export workflow.
@SuppressWarnings("serial")
public final class PlantUmlPanel extends JPanel {

    private static final double INPUT_WEIGHT = 0.4;
    private static final int DEFAULT_PREVIEW_DELAY_MS = 800;

    private final PlantUmlInputPanel inputPanel = new PlantUmlInputPanel();
    private final PlantUmlOutputPanel outputPanel =
            new PlantUmlOutputPanel(this::renderPreview, this::fireEditStateChanged);
    private final PlantUmlFooterPanel footerPanel = new PlantUmlFooterPanel();

    // Coalesces keystrokes into a single render while auto preview is on.
    private final Timer previewTimer = new Timer(
            AppSettings.getInt(AppSettings.PREVIEW_DELAY_MS, DEFAULT_PREVIEW_DELAY_MS), e -> renderPreview());

    private record ExportResult(File output, File preview) { }

    private Runnable editStateListener = () -> { };

    // Which side the user worked in last; decides whether Copy and Select All target the console.
    private boolean outputActive;

    // Only the newest preview may reach the screen; slower, older renders are dropped.
    private int renderGeneration;

    public PlantUmlPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        previewTimer.setRepeats(false);

        add(buildSplit(), BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);

        footerPanel.onExportDiagram(e -> exportDiagram());
        footerPanel.onFormatChanged(e -> footerPanel.setTargetFileExtension(footerPanel.getSelectedFormat()));
        footerPanel.onCopyImage(e -> copyImageToClipboard());
        footerPanel.setCopyImageEnabled(false);

        inputPanel.addCodeDocumentListener(SwingUtils.onDocumentChange(this::restartPreviewTimer));
        inputPanel.addPreviewButtonListener(e -> renderPreview());
        inputPanel.addAutoPreviewListener(e -> {
            if (inputPanel.isAutoPreviewEnabled()) { renderPreview(); }
        });
        inputPanel.setEditStateListener(this::fireEditStateChanged);
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addPropertyChangeListener("permanentFocusOwner", e -> trackActiveSide(e.getNewValue()));

        SwingUtilities.invokeLater(this::renderPreview);
    }

    public PlantUmlInputPanel getInputPanel() { return inputPanel; }

    public boolean canCut() {
        return !isConsoleTargeted() && inputPanel.isCodeTabSelected() && inputPanel.hasSelection();
    }

    public boolean canCopyText() { return isConsoleTargeted() || inputPanel.isCodeTabSelected(); }

    public boolean canPaste() { return !isConsoleTargeted(); }

    public void cut() { inputPanel.cut(); }

    public void copyText() {
        if (isConsoleTargeted()) {
            outputPanel.copyConsoleToClipboard();
        } else {
            inputPanel.copyToClipboard();
        }
    }

    public void paste() { inputPanel.paste(); }

    public void selectAll() {
        if (isConsoleTargeted()) {
            outputPanel.selectAllConsole();
        } else {
            inputPanel.selectAll();
        }
    }

    // Single slot rather than a list: the menu bar is rebuilt on every language or size change.
    public void setEditStateListener(Runnable listener) {
        editStateListener = listener;
        listener.run();
    }

    public boolean copyImageToClipboard() {
        var image = outputPanel.getCurrentImage();
        if (image == null) {
            outputPanel.showMessage(I18n.get("copy.none"));
            return false;
        }
        SwingUtils.copyImage(image);
        return true;
    }

    public void applyLanguage() {
        inputPanel.applyLanguage();
        outputPanel.applyLanguage();
        footerPanel.applyLanguage();
    }

    private JSplitPane buildSplit() {
        var split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                section(inputPanel.getEditorComponent(), inputPanel.getControlsComponent()),
                section(outputPanel, null));
        split.setResizeWeight(INPUT_WEIGHT);
        split.setContinuousLayout(true);
        split.setBorder(null);
        // The divider can only be positioned once the split pane has a real width.
        split.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                split.setDividerLocation(INPUT_WEIGHT);
                split.removeComponentListener(this);
            }
        });
        return split;
    }

    private static JPanel section(Component center, Component south) {
        var panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        panel.add(center, BorderLayout.CENTER);
        if (south != null) { panel.add(south, BorderLayout.SOUTH); }
        return panel;
    }

    // The console wins while it is showing and was used last, or while the code editor is hidden.
    private boolean isConsoleTargeted() {
        return outputPanel.isConsoleSelected() && (outputActive || !inputPanel.isCodeTabSelected());
    }

    private void trackActiveSide(Object focusOwner) {
        if (!(focusOwner instanceof Component owner)) {
            return;
        }
        if (SwingUtilities.isDescendingFrom(owner, outputPanel)) {
            outputActive = true;
        } else if (SwingUtilities.isDescendingFrom(owner, inputPanel.getEditorComponent())) {
            outputActive = false;
        } else {
            return;
        }
        fireEditStateChanged();
    }

    private void fireEditStateChanged() {
        editStateListener.run();
    }

    private void restartPreviewTimer() {
        if (inputPanel.isAutoPreviewEnabled()) { previewTimer.restart(); }
    }

    private void renderPreview() {
        int generation = ++renderGeneration;
        String code = inputPanel.getCode();
        if (code.isEmpty()) {
            outputPanel.showMessage(I18n.get("plantuml.code.empty"));
            return;
        }
        outputPanel.showRendering();
        Background.run(() -> PlantUmlRenderer.compilePreview(code),
                result -> {
                    if (generation != renderGeneration) { return; }
                    outputPanel.showCompileResult(result);
                    footerPanel.setCopyImageEnabled(outputPanel.getCurrentImage() != null);
                },
                ex -> {
                    if (generation != renderGeneration) { return; }
                    outputPanel.showRenderError(ex);
                    footerPanel.setCopyImageEnabled(false);
                });
    }

    private void exportDiagram() {
        String code = inputPanel.getCode();
        String target = footerPanel.getTargetFile();
        PlantUmlFormat format = footerPanel.getSelectedFormat();

        if (code.isEmpty()) {
            showError(I18n.get("plantuml.code.empty"));
        } else if (target.isEmpty()) {
            showError(I18n.get("export.target.empty"));
        } else if (format == PlantUmlFormat.ARCHIMATE) {
            exportArchimate(code, FileNames.withExtension(target, format.extension()));
        } else {
            exportDiagram(code, new File(target), format);
        }
    }

    private void exportDiagram(String code, File output, PlantUmlFormat format) {
        outputPanel.showMessage(I18n.get("export.exporting"));
        Background.run(
                () -> {
                    PlantUmlRenderer.export(code, output, format);
                    // SVG is not paintable, so render a throw-away PNG to show in the preview.
                    File preview = format == PlantUmlFormat.SVG
                            ? PlantUmlRenderer.compilePreview(code).previewImage() : null;
                    return new ExportResult(output, preview);
                },
                result -> {
                    try {
                        outputPanel.showDiagram(result.output(), result.preview());
                        footerPanel.setCopyImageEnabled(outputPanel.getCurrentImage() != null);
                        SwingUtils.showInfo(this, I18n.get("export.success.title"),
                                I18n.get("export.success.msg", result.output().getAbsolutePath()));
                    } catch (Exception ex) {
                        outputPanel.showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
                    }
                },
                ex -> {
                    outputPanel.showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
                    showError(I18n.get("export.fail.msg", ex.getMessage()));
                });
    }

    private void exportArchimate(String code, String targetPath) {
        File output = new File(targetPath);
        String modelName = FileNames.baseName(output);
        outputPanel.selectConsole();
        outputPanel.appendConsole(I18n.get("archimate.export.started"),
                I18n.get("archimate.export.converting"));

        Background.run(
                () -> {
                    var result = ArchimatePlantUmlConverter.convert(code, modelName);
                    result.model().writeTo(output);
                    return result;
                },
                result -> {
                    List<String> warnings = result.warnings();
                    outputPanel.appendConsole(I18n.get("archimate.export.finished"),
                            archimateSummary(output, result, warnings));
                    SwingUtils.showInfo(this, I18n.get("export.success.title"),
                            I18n.get("archimate.export.msg", output.getAbsolutePath())
                                    + (warnings.isEmpty() ? ""
                                            : I18n.get("archimate.export.warnings", warnings.size())));
                },
                ex -> {
                    outputPanel.appendConsole(I18n.get("archimate.export.failed"),
                            String.valueOf(ex.getMessage()));
                    showError(I18n.get("archimate.export.failmsg", ex.getMessage()));
                });
    }

    private static String archimateSummary(File output, ArchimatePlantUmlConverter.Result result,
            List<String> warnings) {
        var sb = new StringBuilder(I18n.get("archimate.export.summary", output.getAbsolutePath(),
                result.model().getElementCount(), result.model().getRelationshipCount()));
        warnings.forEach(w -> sb.append(System.lineSeparator()).append("  - ").append(w));
        return sb.toString();
    }

    private void showError(String message) {
        SwingUtils.showError(this, I18n.get("export.error.title"), message);
    }
}