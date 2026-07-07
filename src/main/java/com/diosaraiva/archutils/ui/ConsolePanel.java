package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.i18n.I18n;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Console tab showing PlantUML compilation output (stdout+stderr incl. syntax
// errors) for debugging .puml sources. Refresh re-runs the compile (wired by
// PlantUmlPanel); Clean clears the area. Uses the shared toolbar/button styling
// so it matches the Preview toolbar. All mutators are EDT-safe.
public class ConsolePanel extends JPanel {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextArea consoleArea = new JTextArea();
    private final JButton refreshButton = SwingUtils.createToolButton(
            I18n.get("console.refresh"), I18n.get("console.refresh.tooltip"));
    private final JButton cleanButton = SwingUtils.createToolButton(
            I18n.get("console.clean"), I18n.get("console.clean.tooltip"));
    private final TitledBorder titledBorder =
            BorderFactory.createTitledBorder(I18n.get("console.title"));

    public ConsolePanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(titledBorder);

        var toolBar = SwingUtils.createToolBar();
        cleanButton.addActionListener(e -> clear());
        toolBar.add(refreshButton);
        toolBar.add(cleanButton);
        add(toolBar, BorderLayout.NORTH);

        consoleArea.setEditable(false);
        consoleArea.setLineWrap(true);
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(consoleArea), BorderLayout.CENTER);
    }

    // Re-applies localized text; called after a runtime language change.
    public void applyLanguage() {
        titledBorder.setTitle(I18n.get("console.title"));
        refreshButton.setText(I18n.get("console.refresh"));
        refreshButton.setToolTipText(I18n.get("console.refresh.tooltip"));
        cleanButton.setText(I18n.get("console.clean"));
        cleanButton.setToolTipText(I18n.get("console.clean.tooltip"));
        repaint();
    }

    // Registers the handler invoked when the Refresh button is pressed.
    public void onRefresh(ActionListener listener) {
        refreshButton.addActionListener(listener);
    }

    // Enables/disables the Refresh button (e.g. while a compile is running).
    public void setRefreshEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> refreshButton.setEnabled(enabled));
    }

    // Appends a timestamped block and scrolls to the bottom. Safe off the EDT.
    public void appendOutput(String header, String body) {
        SwingUtilities.invokeLater(() -> {
            var sb = new StringBuilder();
            sb.append("[").append(LocalTime.now().format(TS)).append("] ")
                    .append(header).append(System.lineSeparator());
            if (body != null && !body.isBlank()) {
                sb.append(body.strip()).append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            consoleArea.append(sb.toString());
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    // Clears the console text area. Safe to call off the EDT.
    public void clear() {
        SwingUtilities.invokeLater(() -> consoleArea.setText(""));
    }
}