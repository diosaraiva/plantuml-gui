package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.i18n.I18n;
import com.diosaraiva.archutils.plantuml.PlantUmlConsole;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

// Standalone window mirroring the whole application's System.out/err, captured
// by the always-on PlantUmlConsole tee (installed at startup). While visible it
// subscribes for live chunks; Refresh re-pulls the full buffer, Clean resets it.
// A single reusable instance is kept so repeated opens focus the same window.
public final class GlobalConsoleWindow extends JFrame {

    private static GlobalConsoleWindow instance;

    private final JTextArea outputArea = new JTextArea();
    // Live feed: a null chunk means the buffer was cleared elsewhere.
    private final Consumer<String> feed = chunk -> SwingUtilities.invokeLater(() -> {
        if (chunk == null) {
            outputArea.setText("");
        } else {
            outputArea.append(chunk);
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        }
    });

    private GlobalConsoleWindow() {
        super(I18n.get("gconsole.title"));
        initComponents();
    }

    // Opens (or focuses) the shared console window on the EDT.
    public static void open() {
        SwingUtilities.invokeLater(() -> {
            if (instance == null) {
                instance = new GlobalConsoleWindow();
            }
            instance.subscribeAndSnapshot();
            instance.setVisible(true);
            instance.toFront();
            instance.requestFocus();
        });
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        var toolBar = SwingUtils.createToolBar();
        var refreshButton = SwingUtils.createToolButton(
                I18n.get("console.refresh"), I18n.get("gconsole.refresh.tooltip"));
        refreshButton.addActionListener(e -> refresh());
        var cleanButton = SwingUtils.createToolButton(
                I18n.get("console.clean"), I18n.get("gconsole.clean.tooltip"));
        cleanButton.addActionListener(e -> clean());
        toolBar.add(refreshButton);
        toolBar.add(cleanButton);
        add(toolBar, BorderLayout.NORTH);

        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // Stop receiving live output once hidden to avoid leaking the listener.
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                PlantUmlConsole.global().removeListener(feed);
            }
        });
    }

    // Loads the historical buffer, then attaches the live feed for new output.
    private void subscribeAndSnapshot() {
        refresh();
        PlantUmlConsole.global().removeListener(feed);
        PlantUmlConsole.global().addListener(feed);
    }

    // Re-pulls the full captured buffer into the view.
    private void refresh() {
        outputArea.setText(PlantUmlConsole.global().getText());
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void clean() {
        PlantUmlConsole.global().clear();
        outputArea.setText("");
    }
}