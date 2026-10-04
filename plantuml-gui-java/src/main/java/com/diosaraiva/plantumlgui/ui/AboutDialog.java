package com.diosaraiva.plantumlgui.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;

// Modal About box; VERSION is the single place the released app version is declared.
@SuppressWarnings("serial")
public final class AboutDialog extends JDialog {

    private static final String REPO_URL = "https://github.com/diosaraiva/plantuml-gui";
    private static final String VERSION = "1.6.1";

    public AboutDialog(JFrame parent) {
        super(parent, I18n.get("about.title"), true);
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        var info = new JPanel(new BorderLayout(8, 8));
        info.add(centeredLabel(I18n.get("about.title"), 18f), BorderLayout.NORTH);
        info.add(createDescriptionPane(), BorderLayout.CENTER);
        info.add(centeredLabel(I18n.get("about.version", VERSION), 0f), BorderLayout.SOUTH);
        add(info, BorderLayout.CENTER);

        var ok = new JButton(I18n.get("about.ok"));
        ok.addActionListener(e -> dispose());
        var buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(ok);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private static JLabel centeredLabel(String text, float boldSize) {
        var label = new JLabel(text, JLabel.CENTER);
        if (boldSize > 0f) {
            label.setFont(label.getFont().deriveFont(Font.BOLD, boldSize));
        }
        return label;
    }

    // HTML pane so the description can carry the clickable repository link.
    private JEditorPane createDescriptionPane() {
        var pane = new JEditorPane("text/html", I18n.get("about.description", REPO_URL));
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setCursor(new Cursor(Cursor.HAND_CURSOR));
        SwingUtils.useUiFont(pane);
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                SwingUtils.browse(e.getURL().toString());
            }
        });
        return pane;
    }
}