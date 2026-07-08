package com.diosaraiva.archutils.plantuml.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.diosaraiva.archutils.i18n.I18n;
import com.diosaraiva.archutils.plantuml.PlantUmlConsole;
import com.diosaraiva.archutils.ui.ConsoleView;

public final class JavaConsoleWindow extends JFrame {

    private static JavaConsoleWindow instance;

    private final ConsoleView console = new ConsoleView(
            null, "gconsole.refresh.tooltip", "gconsole.clean.tooltip",
            this::refresh, () -> PlantUmlConsole.global().clear());

    private final Consumer<String> feed = chunk -> {
        if (chunk == null) { console.setContent(""); } else { console.append(chunk); }
    };

    private JavaConsoleWindow() {
        super(I18n.get("gconsole.title"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        add(console, BorderLayout.CENTER);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                PlantUmlConsole.global().removeListener(feed);
            }
        });
    }

    public static void open() {
        SwingUtilities.invokeLater(() -> {
            if (instance == null) { instance = new JavaConsoleWindow(); }
            instance.refresh();
            PlantUmlConsole.global().removeListener(instance.feed);
            PlantUmlConsole.global().addListener(instance.feed);
            instance.setVisible(true);
            instance.toFront();
            instance.requestFocus();
        });
    }

    private void refresh() {
        console.setContent(PlantUmlConsole.global().getText());
    }
}
