package com.diosaraiva.archutils.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.diosaraiva.archutils.dataexport.ui.CsvPanel;
import com.diosaraiva.archutils.i18n.I18n;
import com.diosaraiva.archutils.plantuml.ui.PlantUmlPanel;

// Main application window: hosts the service panels and the menu bar, and
// applies runtime resolution/language changes.
public class MainFrame extends JFrame {

    private final JPanel contentPanel;
    private final PlantUmlPanel plantUmlPanel;
    private final CsvPanel csvPanel;

    public MainFrame() {
        super(I18n.get("app.title"));
        contentPanel = new JPanel(new BorderLayout());
        plantUmlPanel = new PlantUmlPanel();
        csvPanel = new CsvPanel();
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1024, 600));
        setLayout(new BorderLayout());

        contentPanel.add(plantUmlPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        setJMenuBar(MenuBarFactory.create(this));
        pack();
        setLocationRelativeTo(null);
    }

    // Applies a runtime language change across the whole window: title, menu bar
    // and every panel's localized chrome, without losing editor/preview state.
    public void reloadLanguage() {
        SwingUtilities.invokeLater(() -> {
            setTitle(I18n.get("app.title"));
            setJMenuBar(MenuBarFactory.create(this));
            plantUmlPanel.applyLanguage();
            csvPanel.applyLanguage();
            revalidate();
            repaint();
        });
    }

    // Replaces the content area with the given panel.
    public void showPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // Resizes to the requested resolution, clamped to the usable screen, then
    // re-centres the window.
    public void applyResolution(int width, int height) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        int w = Math.min(width, screen.width);
        int h = Math.min(height, screen.height);
        setSize(w, h);
        setLocationRelativeTo(null);
    }

    public PlantUmlPanel getPlantUmlPanel() { return plantUmlPanel; }

    public CsvPanel getCsvPanel() { return csvPanel; }
}