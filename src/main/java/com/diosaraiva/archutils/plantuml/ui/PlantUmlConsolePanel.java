package com.diosaraiva.archutils.plantuml.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.diosaraiva.archutils.i18n.I18n;
import com.diosaraiva.archutils.plantuml.PlantUmlConsole;
import com.diosaraiva.archutils.ui.ConsoleView;

public final class PlantUmlConsolePanel extends JFrame {

    private static PlantUmlConsolePanel instance;

    private final ConsoleView console = new ConsoleView(
            null, "gconsole.refresh.tooltip", "gconsole.clean.tooltip",
            this::refresh, () -> PlantUmlConsole.global().clear());

    private final PlantUmlLayoutPanel card = new PlantUmlLayoutPanel(I18n.get("gconsole.card.title"));

    private final Consumer<String> feed = chunk -> {
        if (chunk == null) { console.setContent(""); } else { console.append(chunk); }
    };

    private PlantUmlConsolePanel() {
        super(I18n.get("gconsole.title"));
        // Keep capturing output in the background even after the window is closed.
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setSize(new Dimension(800, 500));
        setLocationRelativeTo(null);

        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        card.setOutput(new JLabel(I18n.get("gconsole.output.header")), console, null);
        add(card, BorderLayout.CENTER);

        // Attach the live feed immediately so output is captured from launch.
        refresh();
        PlantUmlConsole.global().addListener(feed);
    }

    /**
     * Creates the console window (if needed) and starts capturing JVM output in
     * the background, without showing the window. Must be called on the EDT.
     */
    public static void startBackground() {
        if (instance == null) { instance = new PlantUmlConsolePanel(); }
    }

    public static void open() {
        SwingUtilities.invokeLater(() -> {
            startBackground();
            instance.refresh();
            instance.setVisible(true);
            instance.toFront();
            instance.requestFocus();
        });
    }

    private void refresh() {
        console.setContent(PlantUmlConsole.global().getText());
    }
}
