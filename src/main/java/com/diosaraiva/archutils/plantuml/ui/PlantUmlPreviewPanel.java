package com.diosaraiva.archutils.plantuml.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.diosaraiva.archutils.i18n.I18n;
import com.diosaraiva.archutils.ui.SwingUtils;

public class PlantUmlPreviewPanel extends JPanel {

    private static final String PNG_CARD = "png";
    private static final String PUML_CARD = "puml";
    private static final String MSG_CARD = "msg";

    private static final double ZOOM_STEP = 0.1;
    private static final double ZOOM_MIN = 0.1;
    private static final double ZOOM_MAX = 5.0;

    private final CardLayout cards;
    private final JPanel cardPanel;
    private final ImagePanel imagePanel;
    private final JScrollPane imageScroll;
    private final JTextArea pumlArea;
    private final JLabel msgLabel;
    private final JLabel zoomLabel;

    private JPanel zoomBar;

    private final JButton zoomInBtn = SwingUtils.createToolButton("+", I18n.get("preview.zoom.in"));
    private final JButton zoomOutBtn = SwingUtils.createToolButton("\u2212", I18n.get("preview.zoom.out"));
    private final JButton fitBtn = SwingUtils.createToolButton("Fit", I18n.get("preview.zoom.fit"));
    private final JButton resetBtn = SwingUtils.createToolButton("1:1", I18n.get("preview.zoom.reset"));

    public PlantUmlPreviewPanel() {
        cards = new CardLayout();
        cardPanel = new JPanel(cards);
        imagePanel = new ImagePanel();
        imageScroll = new JScrollPane(imagePanel);
        pumlArea = new JTextArea();
        msgLabel = new JLabel();
        zoomLabel = new JLabel("100%");
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        zoomBar = SwingUtils.createToolBar();
        zoomLabel.setFont(zoomLabel.getFont().deriveFont(Font.PLAIN, 11f));
        zoomBar.add(zoomOutBtn);
        zoomBar.add(zoomLabel);
        zoomBar.add(zoomInBtn);
        zoomBar.add(fitBtn);
        zoomBar.add(resetBtn);

        zoomInBtn.addActionListener(e -> zoom(ZOOM_STEP));
        zoomOutBtn.addActionListener(e -> zoom(-ZOOM_STEP));
        resetBtn.addActionListener(e -> setZoom(1.0));
        fitBtn.addActionListener(e -> fitToWindow());

        imageScroll.addMouseWheelListener(e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                e.consume();
                double delta = e.getWheelRotation() < 0 ? ZOOM_STEP : -ZOOM_STEP;
                zoom(delta);
            }
        });

        imageScroll.getVerticalScrollBar().setUnitIncrement(16);
        imageScroll.getHorizontalScrollBar().setUnitIncrement(16);
        cardPanel.add(imageScroll, PNG_CARD);

        pumlArea.setEditable(false);
        pumlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        cardPanel.add(new JScrollPane(pumlArea), PUML_CARD);

        msgLabel.setHorizontalAlignment(JLabel.CENTER);
        cardPanel.add(msgLabel, MSG_CARD);

        add(cardPanel, BorderLayout.CENTER);
        add(zoomBar, BorderLayout.SOUTH);

        showMessage(I18n.get("preview.none"));
    }

    public void applyLanguage() {
        zoomInBtn.setToolTipText(I18n.get("preview.zoom.in"));
        zoomOutBtn.setToolTipText(I18n.get("preview.zoom.out"));
        fitBtn.setToolTipText(I18n.get("preview.zoom.fit"));
        resetBtn.setToolTipText(I18n.get("preview.zoom.reset"));
        repaint();
    }

    public void showMessage(String text) {
        msgLabel.setText(text);
        cards.show(cardPanel, MSG_CARD);
    }

    public BufferedImage getCurrentImage() {
        return imagePanel.getImage();
    }

    public void showDiagram(File file) throws IOException {
        showDiagram(file, null);
    }

    public void showDiagram(File output, File preview) throws IOException {
        String name = output.getName().toLowerCase();
        if (name.endsWith(".svg")) {
            if (preview != null && preview.isFile()) {
                showPng(preview);
            } else {
                showMessage("SVG created: " + output.getAbsolutePath());
            }
        } else if (name.endsWith(".png")) {
            showPng(output);
        } else if (name.endsWith(".puml")) {
            showPuml(output);
        } else {
            showMessage("Unsupported format: " + name);
        }
    }

    private void showPng(File file) throws IOException {        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            showMessage("Could not load image: " + file.getName());
            return;
        }
        imagePanel.setImage(img);
        cards.show(cardPanel, PNG_CARD);

        SwingUtilities.invokeLater(() -> {
            fitToWindow();
            imageScroll.getVerticalScrollBar().setValue(0);
            imageScroll.getHorizontalScrollBar().setValue(0);
        });
    }

    private void showPuml(File file) throws IOException {
        String puml = new String(Files.readAllBytes(file.toPath()),
                StandardCharsets.UTF_8);
        pumlArea.setText(puml);
        pumlArea.setCaretPosition(0);
        cards.show(cardPanel, PUML_CARD);
    }

    private void zoom(double delta) {
        setZoom(imagePanel.getScale() + delta);
    }

    private void setZoom(double scale) {
        scale = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, scale));
        imagePanel.setScale(scale);
        zoomLabel.setText(Math.round(scale * 100) + "%");
        imagePanel.revalidate();
        imageScroll.repaint();
    }

    private void fitToWindow() {
        BufferedImage img = imagePanel.getImage();
        if (img == null) return;
        int vpW = imageScroll.getViewport().getWidth();
        int vpH = imageScroll.getViewport().getHeight();
        if (vpW <= 0 || vpH <= 0) return;
        double scaleX = (double) vpW / img.getWidth();
        double scaleY = (double) vpH / img.getHeight();
        setZoom(Math.min(scaleX, scaleY));
    }

    private static class ImagePanel extends JPanel {

        private BufferedImage image;
        private double scale = 1.0;

        ImagePanel() {
            setBackground(Color.WHITE);
        }

        void setImage(BufferedImage image) {
            this.image = image;
            this.scale = 1.0;
            revalidate();
            repaint();
        }

        BufferedImage getImage() { return image; }

        double getScale() { return scale; }

        void setScale(double scale) {
            this.scale = scale;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            if (image == null) {
                return new Dimension(100, 100);
            }
            int w = (int) Math.ceil(image.getWidth() * scale);
            int h = (int) Math.ceil(image.getHeight() * scale);
            return new Dimension(w, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            int w = (int) Math.ceil(image.getWidth() * scale);
            int h = (int) Math.ceil(image.getHeight() * scale);

            int x = Math.max(0, (getWidth() - w) / 2);
            int y = Math.max(0, (getHeight() - h) / 2);

            g2.drawImage(image, x, y, w, h, null);
            g2.dispose();
        }
    }
}
