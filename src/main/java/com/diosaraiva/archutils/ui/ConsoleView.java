package com.diosaraiva.archutils.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.diosaraiva.archutils.i18n.I18n;

// Reusable console component: centered Refresh/Clean toolbar over a monospaced,
// non-editable, scrollable text area. Shared by the PlantUML Console tab and the
// standalone Java Console window; behaviour is parameterized by the handlers.
// All mutators are EDT-safe.
public final class ConsoleView extends JPanel {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextArea area = new JTextArea();
    private final JButton refreshButton;
    private final JButton cleanButton;
    private final TitledBorder titledBorder;
    private final String titleKey;
    private final String refreshTipKey;
    private final String cleanTipKey;

    // titleKey may be null for no titled border. onRefresh/onClean may be null;
    // Clean always clears the area first, then runs onClean (e.g. to also clear a
    // shared buffer).
    public ConsoleView(String titleKey, String refreshTipKey, String cleanTipKey,
                       Runnable onRefresh, Runnable onClean) {
        super(new BorderLayout());
        this.titleKey = titleKey;
        this.refreshTipKey = refreshTipKey;
        this.cleanTipKey = cleanTipKey;
        this.titledBorder = titleKey == null ? null
                : BorderFactory.createTitledBorder(I18n.get(titleKey));
        this.refreshButton = SwingUtils.createToolButton(
                I18n.get("console.refresh"), I18n.get(refreshTipKey));
        this.cleanButton = SwingUtils.createToolButton(
                I18n.get("console.clean"), I18n.get(cleanTipKey));

        if (titledBorder != null) { setBorder(titledBorder); }
        if (onRefresh != null) { refreshButton.addActionListener(e -> onRefresh.run()); }
        cleanButton.addActionListener(e -> {
            clearArea();
            if (onClean != null) { onClean.run(); }
        });

        var toolBar = SwingUtils.createToolBar();
        toolBar.add(refreshButton);
        toolBar.add(cleanButton);
        add(toolBar, BorderLayout.NORTH);

        area.setEditable(false);
        area.setLineWrap(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    // Re-applies localized chrome after a runtime language change.
    public void applyLanguage() {
        if (titledBorder != null) { titledBorder.setTitle(I18n.get(titleKey)); }
        refreshButton.setText(I18n.get("console.refresh"));
        refreshButton.setToolTipText(I18n.get(refreshTipKey));
        cleanButton.setText(I18n.get("console.clean"));
        cleanButton.setToolTipText(I18n.get(cleanTipKey));
        repaint();
    }

    public void setRefreshEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> refreshButton.setEnabled(enabled));
    }

    // Appends a timestamped header plus optional body, scrolling to the bottom.
    public void appendBlock(String header, String body) {
        SwingUtilities.invokeLater(() -> {
            var sb = new StringBuilder("[").append(LocalTime.now().format(TS)).append("] ")
                    .append(header).append(System.lineSeparator());
            if (body != null && !body.isBlank()) {
                sb.append(body.strip()).append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            appendRaw(sb.toString());
        });
    }

    // Appends raw text as-is (used by the live Java Console feed).
    public void append(String text) {
        SwingUtilities.invokeLater(() -> appendRaw(text));
    }

    // Replaces the whole content (used to snapshot a buffer).
    public void setContent(String text) {
        SwingUtilities.invokeLater(() -> {
            area.setText(text);
            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    public void clearArea() {
        SwingUtilities.invokeLater(() -> area.setText(""));
    }

    private void appendRaw(String text) {
        area.append(text);
        area.setCaretPosition(area.getDocument().getLength());
    }
}
