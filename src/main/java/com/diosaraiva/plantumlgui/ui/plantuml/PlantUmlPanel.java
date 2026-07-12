package com.diosaraiva.plantumlgui.ui.plantuml;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.diosaraiva.plantumlgui.Background;
import com.diosaraiva.plantumlgui.service.PlantUmlArchimateConverter;
import com.diosaraiva.plantumlgui.service.PlantUmlExporter;
import com.diosaraiva.plantumlgui.service.PlantUmlFormat;
import com.diosaraiva.plantumlgui.service.PlantUmlRenderer;
import com.diosaraiva.plantumlgui.ui.other.ConsoleView;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;

public class PlantUmlPanel extends JPanel {

    private static final int PREVIEW_DELAY_MS = 800;

    private final PlantUmlInputPanel inputPanel;
    private final PlantUmlExportPanel exportPanel;
    private final PlantUmlOutputPreviewPanel previewPanel;
    private final ConsoleView console;
    private final JTabbedPane tabs = new JTabbedPane();
    private final Timer previewTimer;
    private PlantUmlLayoutPanel card;

    private record ExportResult(File output, File preview) { }

    public PlantUmlPanel() {
        var defaultTarget = resolveDefaultTarget("png");
        inputPanel = new PlantUmlInputPanel();
        previewPanel = new PlantUmlOutputPreviewPanel();
        exportPanel = new PlantUmlExportPanel(defaultTarget);
        console = new ConsoleView("console.title", "console.refresh.tooltip",
                "console.clean.tooltip", this::onConsoleRefresh, null);

        previewTimer = new Timer(PREVIEW_DELAY_MS, e -> onLivePreview());
        previewTimer.setRepeats(false);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        tabs.addTab(I18n.get("tab.preview"), previewPanel);
        tabs.addTab(I18n.get("tab.console"), console);

        card = new PlantUmlLayoutPanel(I18n.get("card.plantuml.title"));
        card.setInput(inputPanel.getSamplesComponent(),
                inputPanel.getEditorComponent(),
                inputPanel.getControlsComponent());
        card.setOutput(null, tabs, null);
        card.setInputWeight(0.4);

        add(card, BorderLayout.CENTER);
        add(exportPanel, BorderLayout.SOUTH);

        exportPanel.onExportDiagram(e -> onExportDiagram());
        exportPanel.onFormatChanged(e -> onFormatChanged());
        exportPanel.onCopyImage(e -> copyImageToClipboard());
        exportPanel.setCopyImageEnabled(false);

        inputPanel.addCodeDocumentListener(SwingUtils.onDocumentChange(this::restartPreviewTimer));
        inputPanel.addPreviewButtonListener(e -> onLivePreview());
        inputPanel.addAutoPreviewListener(e -> {
            if (inputPanel.isAutoPreviewEnabled()) { onLivePreview(); }
        });

        SwingUtilities.invokeLater(() -> {
            onLivePreview();
            // Start the PlantUML console capturing output in the background at launch.
            runBackgroundConsoleCheck();
        });
    }

    private void restartPreviewTimer() {
        if (inputPanel.isAutoPreviewEnabled()) { previewTimer.restart(); }
    }

    private void onLivePreview() {
        var code = inputPanel.getCode();
        if (code.isEmpty()) {
            previewPanel.showMessage(I18n.get("plantuml.code.empty"));
            return;
        }
        previewPanel.showMessage(I18n.get("plantuml.preview.rendering"));
        var tempDir = resolveTempDir();

        Background.run(
                () -> PlantUmlRenderer.renderPreview(code, tempDir),
                this::showPreviewResult,
                ex -> {
                    previewPanel.showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
                    exportPanel.setCopyImageEnabled(false);
                });
    }

    private void showPreviewResult(File preview) {
        try {
            if (preview != null && preview.isFile()) {
                previewPanel.showDiagram(preview);
                exportPanel.setCopyImageEnabled(previewPanel.getCurrentImage() != null);
            } else {
                previewPanel.showMessage(I18n.get("plantuml.preview.noimage"));
                exportPanel.setCopyImageEnabled(false);
            }
        } catch (Exception ex) {
            previewPanel.showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
            exportPanel.setCopyImageEnabled(false);
        }
    }

    private void onConsoleRefresh() {
        var code = inputPanel.getCode();
        if (code.isEmpty()) {
            console.appendBlock(I18n.get("console.refresh.skipped"), I18n.get("console.source.empty"));
            return;
        }
        var tempDir = resolveTempDir();
        console.setRefreshEnabled(false);
        Background.run(
                () -> PlantUmlRenderer.compilePreview(code, tempDir),
                result -> {
                    var header = result.isSuccess()
                            ? I18n.get("console.compile.ok", result.exitCode())
                            : I18n.get("console.compile.fail", result.exitCode());
                    var body = result.output().isBlank() ? I18n.get("console.no.output") : result.output();
                    console.appendBlock(header, body);
                    console.setRefreshEnabled(true);
                },
                ex -> {
                    console.appendBlock(I18n.get("console.compile.error"), String.valueOf(ex.getMessage()));
                    console.setRefreshEnabled(true);
                });
    }

    /** Runs a background PlantUML compilation so its output is captured in the console at launch. */
    public void runBackgroundConsoleCheck() {
        onConsoleRefresh();
    }

    private void onFormatChanged() {
        exportPanel.setTargetFileExtension(exportPanel.getSelectedFormat());
    }

    private void onExportDiagram() {
        var code = inputPanel.getCode();
        var target = exportPanel.getTargetFile();
        if (code.isEmpty()) { showError(I18n.get("plantuml.code.empty")); return; }
        if (target.isEmpty()) { showError(I18n.get("export.target.empty")); return; }
        if (exportPanel.isArchimateSelected()) { onExportArchimate(code, target); return; }

        previewPanel.showMessage(I18n.get("export.exporting"));
        var tempDir = resolveTempDir();
        var format = PlantUmlFormat.fromExtension(exportPanel.getSelectedFormat())
                .orElse(PlantUmlFormat.PNG);

        Background.run(
                () -> {
                    var output = new File(target);
                    PlantUmlExporter.export(code, output, format);

                    File preview = format == PlantUmlFormat.SVG
                            ? PlantUmlRenderer.renderPreview(code, tempDir) : null;
                    return new ExportResult(output, preview);
                },
                result -> {
                    try {
                        previewPanel.showDiagram(result.output(), result.preview());
                        exportPanel.setCopyImageEnabled(previewPanel.getCurrentImage() != null);
                        JOptionPane.showMessageDialog(this,
                                I18n.get("export.success.msg", result.output().getAbsolutePath()),
                                I18n.get("export.success.title"), JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        previewPanel.showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
                    }
                },
                ex -> {
                    previewPanel.showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
                    JOptionPane.showMessageDialog(this, I18n.get("export.fail.msg", ex.getMessage()),
                            I18n.get("export.error.title"), JOptionPane.ERROR_MESSAGE);
                });
    }

    private void onExportArchimate(String code, String target) {
        var path = target.toLowerCase().endsWith(".xml") ? target : target + ".xml";
        var output = new File(path);
        var modelName = deriveModelName(output);
        tabs.setSelectedComponent(console);
        console.appendBlock(I18n.get("archimate.export.started"), I18n.get("archimate.export.converting"));

        Background.run(
                () -> {
                    var result = PlantUmlArchimateConverter.convert(code, modelName);
                    result.model().writeTo(output);
                    return result;
                },
                result -> {
                    var warnings = result.warnings();
                    var sb = new StringBuilder("Wrote ").append(output.getAbsolutePath())
                            .append(System.lineSeparator())
                            .append("Elements: ").append(result.model().getElementCount())
                            .append(", Relationships: ").append(result.model().getRelationshipCount());
                    for (var w : warnings) {
                        sb.append(System.lineSeparator()).append("  - ").append(w);
                    }
                    console.appendBlock(I18n.get("archimate.export.finished"), sb.toString());
                    var extra = warnings.isEmpty() ? ""
                            : I18n.get("archimate.export.warnings", warnings.size());
                    JOptionPane.showMessageDialog(this,
                            I18n.get("archimate.export.msg", output.getAbsolutePath()) + extra,
                            I18n.get("export.success.title"), JOptionPane.INFORMATION_MESSAGE);
                },
                ex -> {
                    console.appendBlock(I18n.get("archimate.export.failed"), String.valueOf(ex.getMessage()));
                    JOptionPane.showMessageDialog(this, I18n.get("archimate.export.failmsg", ex.getMessage()),
                            I18n.get("export.error.title"), JOptionPane.ERROR_MESSAGE);
                });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message,
                I18n.get("export.error.title"), JOptionPane.ERROR_MESSAGE);
    }

    private static String deriveModelName(File file) {
        var name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public boolean copyImageToClipboard() {
        var image = previewPanel.getCurrentImage();
        if (image == null) {
            previewPanel.showMessage(I18n.get("copy.none"));
            return false;
        }
        SwingUtils.copyImage(image);
        return true;
    }

    public void applyLanguage() {
        card.setTitle(I18n.get("card.plantuml.title"));
        tabs.setTitleAt(0, I18n.get("tab.preview"));
        tabs.setTitleAt(1, I18n.get("tab.console"));
        inputPanel.applyLanguage();
        exportPanel.applyLanguage();
        previewPanel.applyLanguage();
        console.applyLanguage();
    }

    private static String resolveTempDir() {
        return System.getProperty("user.dir") + File.separator + "temp";
    }

    private static String resolveDefaultTarget(String ext) {
        return System.getProperty("user.dir") + File.separator + "output"
                + File.separator + "target." + ext;
    }

    public PlantUmlInputPanel getInputPanel() { return inputPanel; }
}
