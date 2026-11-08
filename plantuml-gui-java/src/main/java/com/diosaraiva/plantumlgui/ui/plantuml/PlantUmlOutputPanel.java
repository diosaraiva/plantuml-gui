package com.diosaraiva.plantumlgui.ui.plantuml;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.diosaraiva.plantumlgui.service.PlantUmlFormat;
import com.diosaraiva.plantumlgui.service.PlantUmlRenderer.CompileResult;
import com.diosaraiva.plantumlgui.util.FileNames;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;

// Right-hand output: zoomable diagram preview plus the PlantUML console.
@SuppressWarnings("serial")
public final class PlantUmlOutputPanel extends JPanel {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int PREVIEW_TAB = 0;
    private static final int CONSOLE_TAB = 1;

    // CardLayout keys for the three possible preview contents.
    private static final String IMAGE_CARD = "image";
    private static final String TEXT_CARD = "text";
    private static final String MESSAGE_CARD = "message";

    private static final double ZOOM_STEP = 0.1;
    private static final double ZOOM_MIN = 0.1;
    private static final double ZOOM_MAX = 5.0;
    private static final int SCROLL_UNIT = 16;
    private static final int CONSOLE_FONT_SIZE = 12;

    private final JTabbedPane tabs = new JTabbedPane();

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final ImagePanel imagePanel = new ImagePanel();
    private final JScrollPane imageScroll = new JScrollPane(imagePanel);
    private final JTextArea textArea = new JTextArea();
    private final JLabel messageLabel = new JLabel("", JLabel.CENTER);
    private final JLabel zoomLabel = new JLabel("100%");

    private final JButton zoomInButton = SwingUtils.createToolButton("+", I18n.get("preview.zoom.in"));
    private final JButton zoomOutButton = SwingUtils.createToolButton("\u2212", I18n.get("preview.zoom.out"));
    private final JButton fitButton = SwingUtils.createToolButton("Fit", I18n.get("preview.zoom.fit"));
    private final JButton resetButton = SwingUtils.createToolButton("1:1", I18n.get("preview.zoom.reset"));

    private final JTextArea consoleArea = new JTextArea();
    private final JButton refreshButton =
            SwingUtils.createToolButton(I18n.get("console.refresh"), I18n.get("console.refresh.tooltip"));
    private final JButton cleanButton =
            SwingUtils.createToolButton(I18n.get("console.clean"), I18n.get("console.clean.tooltip"));

    public PlantUmlOutputPanel(Runnable onConsoleRefresh, Runnable onTabChanged) {
        super(new BorderLayout());
        refreshButton.addActionListener(e -> onConsoleRefresh.run());
        cleanButton.addActionListener(e -> clearConsole());

        tabs.addTab(I18n.get("tab.preview"), buildPreviewTab());
        tabs.addTab(I18n.get("tab.console"), buildConsoleTab());
        tabs.addChangeListener(e -> onTabChanged.run());
        add(tabs, BorderLayout.CENTER);
    }

    public BufferedImage getCurrentImage() { return imagePanel.getImage(); }

    public void showRendering() {
        showMessage(I18n.get("plantuml.preview.rendering"));
        setRefreshEnabled(false);
    }

    public void showMessage(String text) {
        messageLabel.setText(text);
        cards.show(cardPanel, MESSAGE_CARD);
    }

    // Shows the rendered image (when any) and always logs the compiler output.
    public void showCompileResult(CompileResult result) {
        try {
            if (result.previewImage() != null && result.previewImage().isFile()) {
                showImage(result.previewImage());
            } else {
                showMessage(I18n.get("plantuml.preview.noimage"));
            }
        } catch (IOException ex) {
            showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
        }
        var run = result.run();
        appendConsole(I18n.get(run.isSuccess() ? "console.compile.ok" : "console.compile.fail", run.exitCode()),
                run.output().isEmpty() ? I18n.get("console.no.output") : run.output());
        setRefreshEnabled(true);
    }

    public void showRenderError(Throwable ex) {
        showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
        appendConsole(I18n.get("console.compile.error"), String.valueOf(ex.getMessage()));
        setRefreshEnabled(true);
    }

    // SVG cannot be painted directly, so an optional PNG rendered from the same source stands in.
    public void showDiagram(File output, File preview) throws IOException {
        PlantUmlFormat format = PlantUmlFormat.fromExtension(FileNames.extension(output.getName()))
                .orElse(null);
        if (format == null) {
            showMessage(I18n.get("preview.unsupported", output.getName()));
            return;
        }
        switch (format) {
            case PNG -> showImage(output);
            case PUML, ARCHIMATE -> showText(output);
            case SVG -> {
                if (preview != null && preview.isFile()) {
                    showImage(preview);
                } else {
                    showMessage(I18n.get("preview.created", output.getAbsolutePath()));
                }
            }
        }
    }

    public void selectConsole() {
        tabs.setSelectedIndex(CONSOLE_TAB);
    }

    public boolean isConsoleSelected() { return tabs.getSelectedIndex() == CONSOLE_TAB; }

    public void copyConsoleToClipboard() { SwingUtils.copyText(consoleArea); }

    public void selectAllConsole() {
        selectConsole();
        consoleArea.requestFocusInWindow();
        consoleArea.selectAll();
    }

    public void appendConsole(String header, String body) {
        SwingUtilities.invokeLater(() -> {
            var sb = new StringBuilder("[").append(LocalTime.now().format(TIMESTAMP)).append("] ")
                    .append(header).append(System.lineSeparator());
            if (body != null && !body.isBlank()) {
                sb.append(body.strip()).append(System.lineSeparator());
            }
            consoleArea.append(sb.append(System.lineSeparator()).toString());
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    public void applyLanguage() {
        tabs.setTitleAt(PREVIEW_TAB, I18n.get("tab.preview"));
        tabs.setTitleAt(CONSOLE_TAB, I18n.get("tab.console"));
        zoomInButton.setToolTipText(I18n.get("preview.zoom.in"));
        zoomOutButton.setToolTipText(I18n.get("preview.zoom.out"));
        fitButton.setToolTipText(I18n.get("preview.zoom.fit"));
        resetButton.setToolTipText(I18n.get("preview.zoom.reset"));
        refreshButton.setText(I18n.get("console.refresh"));
        refreshButton.setToolTipText(I18n.get("console.refresh.tooltip"));
        cleanButton.setText(I18n.get("console.clean"));
        cleanButton.setToolTipText(I18n.get("console.clean.tooltip"));
        repaint();
    }

    private JPanel buildPreviewTab() {
        SwingUtils.setFontSize(zoomLabel, 11f);
        zoomInButton.addActionListener(e -> zoomBy(ZOOM_STEP));
        zoomOutButton.addActionListener(e -> zoomBy(-ZOOM_STEP));
        resetButton.addActionListener(e -> setZoom(1.0));
        fitButton.addActionListener(e -> fitToWindow());

        var zoomBar = SwingUtils.createToolBar();
        zoomBar.add(zoomOutButton);
        zoomBar.add(zoomLabel);
        zoomBar.add(zoomInButton);
        zoomBar.add(fitButton);
        zoomBar.add(resetButton);

        // Ctrl/Cmd + wheel zooms; a plain wheel keeps scrolling.
        imageScroll.addMouseWheelListener(e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                e.consume();
                zoomBy(e.getWheelRotation() < 0 ? ZOOM_STEP : -ZOOM_STEP);
            }
        });
        imageScroll.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT);
        imageScroll.getHorizontalScrollBar().setUnitIncrement(SCROLL_UNIT);

        textArea.setEditable(false);
        textArea.setFont(SwingUtils.monospaced(SwingUtils.CODE_FONT_SIZE));

        cardPanel.add(imageScroll, IMAGE_CARD);
        cardPanel.add(new JScrollPane(textArea), TEXT_CARD);
        cardPanel.add(messageLabel, MESSAGE_CARD);

        var panel = new JPanel(new BorderLayout());
        panel.add(cardPanel, BorderLayout.CENTER);
        panel.add(zoomBar, BorderLayout.SOUTH);
        showMessage(I18n.get("preview.none"));
        return panel;
    }

    private JPanel buildConsoleTab() {
        consoleArea.setEditable(false);
        consoleArea.setLineWrap(true);
        consoleArea.setFont(SwingUtils.monospaced(CONSOLE_FONT_SIZE));

        var toolBar = SwingUtils.createToolBar();
        toolBar.add(refreshButton);
        toolBar.add(cleanButton);

        var panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(consoleArea), BorderLayout.CENTER);
        panel.add(toolBar, BorderLayout.SOUTH);
        return panel;
    }

    private void showImage(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            showMessage(I18n.get("preview.unreadable", file.getName()));
            return;
        }
        imagePanel.setImage(image);
        cards.show(cardPanel, IMAGE_CARD);
        // Viewport size is only known after layout, so fit on the next EDT pass.
        SwingUtilities.invokeLater(() -> {
            fitToWindow();
            imageScroll.getVerticalScrollBar().setValue(0);
            imageScroll.getHorizontalScrollBar().setValue(0);
        });
    }

    private void showText(File file) throws IOException {
        textArea.setText(Files.readString(file.toPath()));
        textArea.setCaretPosition(0);
        cards.show(cardPanel, TEXT_CARD);
    }

    private void zoomBy(double delta) {
        setZoom(imagePanel.getScale() + delta);
    }

    private void setZoom(double scale) {
        double clamped = Math.min(Math.max(scale, ZOOM_MIN), ZOOM_MAX);
        imagePanel.setScale(clamped);
        zoomLabel.setText(Math.round(clamped * 100) + "%");
        imageScroll.repaint();
    }

    private void fitToWindow() {
        BufferedImage image = imagePanel.getImage();
        int width = imageScroll.getViewport().getWidth();
        int height = imageScroll.getViewport().getHeight();
        if (image == null || width <= 0 || height <= 0) {
            return;
        }
        setZoom(Math.min((double) width / image.getWidth(), (double) height / image.getHeight()));
    }

    private void setRefreshEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> refreshButton.setEnabled(enabled));
    }

    private void clearConsole() {
        SwingUtilities.invokeLater(() -> consoleArea.setText(""));
    }

    // Draws a scaled image centred in the viewport.
    @SuppressWarnings("serial")
    private static final class ImagePanel extends JPanel {

        private static final Dimension EMPTY_SIZE = new Dimension(100, 100);

        private BufferedImage image;
        private double scale = 1.0;

        ImagePanel() {
            setBackground(Color.WHITE);
        }

        BufferedImage getImage() { return image; }

        void setImage(BufferedImage image) {
            this.image = image;
            setScale(1.0);
        }

        double getScale() { return scale; }

        void setScale(double scale) {
            this.scale = scale;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            if (image == null) {
                return EMPTY_SIZE;
            }
            return new Dimension((int) Math.ceil(image.getWidth() * scale),
                    (int) Math.ceil(image.getHeight() * scale));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                return;
            }
            Dimension size = getPreferredSize();
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.drawImage(image, Math.max(0, (getWidth() - size.width) / 2),
                        Math.max(0, (getHeight() - size.height) / 2), size.width, size.height, null);
            } finally {
                g2.dispose();
            }
        }
    }
}
