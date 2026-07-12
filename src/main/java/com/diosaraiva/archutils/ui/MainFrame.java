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

public class MainFrame extends JFrame {

    private static final int DEFAULT_WIDTH = 1024;
    private static final int DEFAULT_HEIGHT = 600;

    private final JPanel contentPanel;
    private final PlantUmlPanel plantUmlPanel;
    private final CsvPanel csvPanel;

    private int selectedWidth = DEFAULT_WIDTH;
    private int selectedHeight = DEFAULT_HEIGHT;

    public MainFrame() {
        super(I18n.get("app.title"));
        contentPanel = new JPanel(new BorderLayout());
        plantUmlPanel = new PlantUmlPanel();
        csvPanel = new CsvPanel();
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setLayout(new BorderLayout());

        contentPanel.add(plantUmlPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        setJMenuBar(MenuBarFactory.create(this));
        pack();
        setLocationRelativeTo(null);
    }

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

    public void showPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void applyResolution(int width, int height) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        int w = Math.min(width, screen.width);
        int h = Math.min(height, screen.height);
        selectedWidth = width;
        selectedHeight = height;
        setSize(w, h);
        setLocationRelativeTo(null);
        // Rebuild the menu so the Window menu reflects the new selection.
        setJMenuBar(MenuBarFactory.create(this));
        revalidate();
        repaint();
    }

    /** Width of the resolution currently chosen from the Window menu. */
    public int getSelectedWidth() { return selectedWidth; }

    /** Height of the resolution currently chosen from the Window menu. */
    public int getSelectedHeight() { return selectedHeight; }

    public PlantUmlPanel getPlantUmlPanel() { return plantUmlPanel; }

    public CsvPanel getCsvPanel() { return csvPanel; }
}
