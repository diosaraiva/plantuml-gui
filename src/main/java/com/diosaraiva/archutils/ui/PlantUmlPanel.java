package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.service.PlantUmlService;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Coordinator panel that wires input, live preview and export.
 * <p>
 * Live preview: as the user types, a debounced timer triggers a background
 * render into the {@code temp/} folder and refreshes the preview panel.
 * <p>
 * Export: target file, format selection and the Export button live in
 * {@link ExportDiagramPanel}, independent of the preview.
 */
public class PlantUmlPanel extends JPanel {

    /** Delay (ms) after the last keystroke before the live preview fires. */
    private static final int PREVIEW_DELAY_MS = 800;

    private final PlantUmlInputPanel inputPanel;
    private final ExportDiagramPanel exportPanel;
    private final DiagramPreviewPanel previewPanel;
    private final Timer previewTimer;

    public PlantUmlPanel() {
        String defaultTarget = resolveDefaultTarget("png");
        inputPanel = new PlantUmlInputPanel();
        exportPanel = new ExportDiagramPanel(defaultTarget);
        previewPanel = new DiagramPreviewPanel();

        // Debounce timer – fires once after the user stops typing
        previewTimer = new Timer(PREVIEW_DELAY_MS, e -> onLivePreview());
        previewTimer.setRepeats(false);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ---------- centre: input | preview ----------
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, previewPanel);
        splitPane.setResizeWeight(0.35);
        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                splitPane.setDividerLocation(0.35);
                splitPane.removeComponentListener(this);
            }
        });
        add(splitPane, BorderLayout.CENTER);

        // ---------- south: export panel ----------
        add(exportPanel, BorderLayout.SOUTH);

        // ---------- wiring ----------
        exportPanel.onExportDiagram(e -> onExportDiagram());
        exportPanel.onFormatChanged(e -> onFormatChanged());
        exportPanel.onCopyImage(e -> onCopyImageToClipboard());
        exportPanel.setCopyImageEnabled(false);

        // Live preview: listen to every code change
        inputPanel.addCodeDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { restartPreviewTimer(); }
            @Override public void removeUpdate(DocumentEvent e)  { restartPreviewTimer(); }
            @Override public void changedUpdate(DocumentEvent e) { restartPreviewTimer(); }
        });

        // Pre-render the initial sample so the preview is ready on startup
        SwingUtilities.invokeLater(this::onLivePreview);
    }

    // -------------------- live preview --------------------

    private void restartPreviewTimer() {
        previewTimer.restart();
    }

    private void onLivePreview() {
        String code = inputPanel.getCode();
        if (code.isEmpty()) {
            previewPanel.showMessage("PlantUML code is empty.");
            return;
        }
        previewPanel.showMessage("Rendering preview...");
        final String tempDir = resolveTempDir();

        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                return PlantUmlService.renderPreview(code, tempDir);
            }

            @Override
            protected void done() {
                try {
                    File preview = get();
                    if (preview != null && preview.isFile()) {
                        previewPanel.showDiagram(preview);
                        exportPanel.setCopyImageEnabled(previewPanel.getCurrentImage() != null);
                    } else {
                        previewPanel.showMessage("Preview generation returned no image.");
                        exportPanel.setCopyImageEnabled(false);
                    }
                } catch (Exception ex) {
                    previewPanel.showMessage("Preview error: " + ex.getMessage());
                    exportPanel.setCopyImageEnabled(false);
                }
            }
        }.execute();
    }

    // -------------------- export --------------------

    private void onFormatChanged() {
        exportPanel.setTargetFileExtension(exportPanel.getSelectedFormat());
    }

    private void onExportDiagram() {
        String code = inputPanel.getCode();
        String target = exportPanel.getTargetFile();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "PlantUML code is empty.", "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (target.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Target file path is empty.", "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        previewPanel.showMessage("Exporting diagram...");
        final String tempDir = resolveTempDir();

        new SwingWorker<File[], Void>() {
            @Override
            protected File[] doInBackground() throws Exception {
                PlantUmlService.render(code, target);
                File output = new File(target);
                File preview = null;
                if (target.toLowerCase().endsWith(".svg")) {
                    preview = PlantUmlService.renderPreview(code, tempDir);
                }
                return new File[]{output, preview};
            }

            @Override
            protected void done() {
                try {
                    File[] result = get();
                    previewPanel.showDiagram(result[0], result[1]);
                    exportPanel.setCopyImageEnabled(previewPanel.getCurrentImage() != null);
                    JOptionPane.showMessageDialog(PlantUmlPanel.this,
                            "Diagram exported successfully:\n" + result[0].getAbsolutePath(),
                            "Export Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    previewPanel.showMessage("Export error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(PlantUmlPanel.this,
                            "Failed to export diagram:\n" + ex.getMessage(),
                            "Export Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Copies the currently rendered diagram image to the system clipboard.
     * If no diagram has been rendered yet, a brief status message is shown
     * instead of failing silently.
     */
    private void onCopyImageToClipboard() {
        BufferedImage image = previewPanel.getCurrentImage();
        if (image == null) {
            previewPanel.showMessage("No diagram to copy yet. Generate a preview first.");
            return;
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new ImageTransferable(image), null);
    }

    // -------------------- helpers --------------------
    private static String resolveTempDir() {
        return System.getProperty("user.dir") + File.separator + "temp";
    }

    private static String resolveDefaultTarget(String ext) {
        String userDir = System.getProperty("user.dir");
        return userDir + File.separator + "output"
                + File.separator + "target." + ext;
    }

    public PlantUmlInputPanel getInputPanel() { return inputPanel; }
}
