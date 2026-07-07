package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.archimate.PlantUmlArchimateConverter;
import com.diosaraiva.archutils.i18n.I18n;
import com.diosaraiva.archutils.plantuml.PlantUmlExporter;
import com.diosaraiva.archutils.plantuml.PlantUmlFormat;
import com.diosaraiva.archutils.plantuml.PlantUmlRenderer;
import com.diosaraiva.archutils.util.Background;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

// Coordinator panel wiring input, live preview and export. Live preview: a
// debounced timer renders into temp/ on a virtual thread and refreshes the
// preview. Export controls live in ExportDiagramPanel. UI code never touches
// PlantUML directly - it goes through the plantuml package.
public class PlantUmlPanel extends JPanel {

    // Idle delay after the last keystroke before the live preview fires.
    private static final int PREVIEW_DELAY_MS = 800;

    private final PlantUmlInputPanel inputPanel;
    private final ExportDiagramPanel exportPanel;
    private final PreviewTabsPanel previewTabs;
    private final DiagramPreviewPanel previewPanel;
    private final ConsolePanel consolePanel;
    private final Timer previewTimer;

    // Diagram export outcome; preview is non-null only for SVG (needs a PNG proxy).
    private record ExportResult(File output, File preview) { }

    public PlantUmlPanel() {
        var defaultTarget = resolveDefaultTarget("png");
        inputPanel = new PlantUmlInputPanel();
        exportPanel = new ExportDiagramPanel(defaultTarget);
        previewTabs = new PreviewTabsPanel();
        previewPanel = previewTabs.getPreviewPanel();
        consolePanel = previewTabs.getConsolePanel();

        previewTimer = new Timer(PREVIEW_DELAY_MS, e -> onLivePreview());
        previewTimer.setRepeats(false);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        var splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, previewTabs);
        splitPane.setResizeWeight(0.35);
        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                splitPane.setDividerLocation(0.35);
                splitPane.removeComponentListener(this);
            }
        });
        add(splitPane, BorderLayout.CENTER);
        add(exportPanel, BorderLayout.SOUTH);

        exportPanel.onExportDiagram(e -> onExportDiagram());
        exportPanel.onFormatChanged(e -> onFormatChanged());
        exportPanel.onCopyImage(e -> onCopyImageToClipboard());
        exportPanel.setCopyImageEnabled(false);

        inputPanel.addCodeDocumentListener(SwingUtils.onDocumentChange(this::restartPreviewTimer));
        inputPanel.addPreviewButtonListener(e -> onLivePreview());

        // Turning Auto Preview back on should refresh immediately.
        inputPanel.addAutoPreviewListener(e -> {
            if (inputPanel.isAutoPreviewEnabled()) {
                onLivePreview();
            }
        });

        consolePanel.onRefresh(e -> onConsoleRefresh());

        // Pre-render the initial sample so the preview is ready on startup.
        SwingUtilities.invokeLater(this::onLivePreview);
    }

    // -------------------- live preview --------------------

    private void restartPreviewTimer() {
        if (inputPanel.isAutoPreviewEnabled()) {
            previewTimer.restart();
        }
    }

    private void onLivePreview() {
        var code = inputPanel.getCode();
        if (code.isEmpty()) {
            previewPanel.showMessage(I18n.get("plantuml.code.empty"));
            return;
        }
        previewPanel.showMessage(I18n.get("plantuml.preview.rendering"));
        var tempDir = resolveTempDir();

        // Virtual thread avoids blocking the EDT during rendering.
        Background.run(
                () -> PlantUmlRenderer.renderPreview(code, tempDir),
                preview -> {
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
                },
                ex -> {
                    previewPanel.showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
                    exportPanel.setCopyImageEnabled(false);
                });
    }

    // -------------------- console --------------------

    // Re-runs the current source through a capturing compile and appends the
    // combined stdout+stderr (including syntax errors) to the console tab.
    private void onConsoleRefresh() {
        var code = inputPanel.getCode();
        if (code.isEmpty()) {
            consolePanel.appendOutput(I18n.get("console.refresh.skipped"),
                    I18n.get("console.source.empty"));
            return;
        }
        var tempDir = resolveTempDir();
        consolePanel.setRefreshEnabled(false);

        Background.run(
                () -> PlantUmlRenderer.compilePreview(code, tempDir),
                result -> {
                    var header = result.isSuccess()
                            ? I18n.get("console.compile.ok", result.exitCode())
                            : I18n.get("console.compile.fail", result.exitCode());
                    var body = result.output().isBlank()
                            ? I18n.get("console.no.output") : result.output();
                    consolePanel.appendOutput(header, body);
                    consolePanel.setRefreshEnabled(true);
                },
                ex -> {
                    consolePanel.appendOutput(I18n.get("console.compile.error"),
                            String.valueOf(ex.getMessage()));
                    consolePanel.setRefreshEnabled(true);
                });
    }

    // -------------------- export --------------------

    private void onFormatChanged() {
        exportPanel.setTargetFileExtension(exportPanel.getSelectedFormat());
    }

    private void onExportDiagram() {
        var code = inputPanel.getCode();
        var target = exportPanel.getTargetFile();
        if (code.isEmpty()) {
            showError(I18n.get("plantuml.code.empty"));
            return;
        }
        if (target.isEmpty()) {
            showError(I18n.get("export.target.empty"));
            return;
        }
        if (exportPanel.isArchimateSelected()) {
            onExportArchimate(code, target);
            return;
        }
        previewPanel.showMessage(I18n.get("export.exporting"));
        var tempDir = resolveTempDir();
        // Format is resolved from the target extension; PNG is the safe default.
        var format = PlantUmlFormat.fromExtension(exportPanel.getSelectedFormat())
                .orElse(PlantUmlFormat.PNG);

        Background.run(
                () -> {
                    var output = new File(target);
                    PlantUmlExporter.export(code, output, format);
                    // SVG has no inline raster, so render a PNG proxy for preview.
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
                    JOptionPane.showMessageDialog(this,
                            I18n.get("export.fail.msg", ex.getMessage()),
                            I18n.get("export.error.title"), JOptionPane.ERROR_MESSAGE);
                });
    }

    // Best-effort ArchiMate export: unmapped PlantUML lines are reported to the
    // console rather than guessed at (source may not be ArchiMate-aware).
    private void onExportArchimate(String code, String target) {
        var path = target.toLowerCase().endsWith(".xml") ? target : target + ".xml";
        var output = new File(path);
        var modelName = deriveModelName(output);
        previewTabs.showConsole();
        consolePanel.appendOutput(I18n.get("archimate.export.started"),
                I18n.get("archimate.export.converting"));

        Background.run(
                () -> {
                    var result = PlantUmlArchimateConverter.convert(code, modelName);
                    result.model().writeTo(output);
                    return result;
                },
                result -> {
                    var warnings = result.warnings();
                    var sb = new StringBuilder();
                    sb.append("Wrote ").append(output.getAbsolutePath())
                            .append(System.lineSeparator())
                            .append("Elements: ").append(result.model().getElementCount())
                            .append(", Relationships: ").append(result.model().getRelationshipCount());
                    if (!warnings.isEmpty()) {
                        sb.append(System.lineSeparator()).append("Warnings:");
                        for (var w : warnings) {
                            sb.append(System.lineSeparator()).append("  - ").append(w);
                        }
                    }
                    consolePanel.appendOutput(I18n.get("archimate.export.finished"), sb.toString());
                    var extra = warnings.isEmpty() ? ""
                            : I18n.get("archimate.export.warnings", warnings.size());
                    JOptionPane.showMessageDialog(this,
                            I18n.get("archimate.export.msg", output.getAbsolutePath()) + extra,
                            I18n.get("export.success.title"), JOptionPane.INFORMATION_MESSAGE);
                },
                ex -> {
                    consolePanel.appendOutput(I18n.get("archimate.export.failed"),
                            String.valueOf(ex.getMessage()));
                    JOptionPane.showMessageDialog(this,
                            I18n.get("archimate.export.failmsg", ex.getMessage()),
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

    // Copies the rendered image to the clipboard; shows a status message instead
    // of failing silently when nothing is rendered yet.
    private void onCopyImageToClipboard() {
        copyImageToClipboard();
    }

    /**
     * Copies the currently rendered diagram image to the system clipboard.
     *
     * @return {@code true} if an image was copied, {@code false} otherwise
     */
    public boolean copyImageToClipboard() {
        var image = previewPanel.getCurrentImage();
        if (image == null) {
            previewPanel.showMessage(I18n.get("copy.none"));
            return false;
        }
        SwingUtils.copyImage(image);
        return true;
    }

    // -------------------- helpers --------------------

    private static String resolveTempDir() {
        return System.getProperty("user.dir") + File.separator + "temp";
    }

    private static String resolveDefaultTarget(String ext) {
        var userDir = System.getProperty("user.dir");
        return userDir + File.separator + "output" + File.separator + "target." + ext;
    }

    public PlantUmlInputPanel getInputPanel() { return inputPanel; }

    // Propagates a runtime language change to all child components.
    public void applyLanguage() {
        inputPanel.applyLanguage();
        exportPanel.applyLanguage();
        previewTabs.applyLanguage();
    }
}