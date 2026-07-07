package com.diosaraiva.archutils.ui;

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

import com.diosaraiva.archutils.i18n.I18n;

/**
 * About dialog displaying application information.
 */
public class AboutDialog extends JDialog {

    private static final String REPO_URL = "https://github.com/diosaraiva/arch-utils";
    private static final String VERSION = "1.1.1";

    public AboutDialog(JFrame parent) {
        super(parent, I18n.get("about.title"), true);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        var infoPanel = new JPanel(new BorderLayout(8, 8));

        var titleLabel = new JLabel(I18n.get("about.title"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        infoPanel.add(titleLabel, BorderLayout.NORTH);

        infoPanel.add(createDescriptionPane(), BorderLayout.CENTER);

        var versionLabel = new JLabel(I18n.get("about.version", VERSION));
        versionLabel.setHorizontalAlignment(JLabel.CENTER);
        infoPanel.add(versionLabel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.CENTER);

        var okButton = new JButton(I18n.get("about.ok"));
        okButton.addActionListener(e -> dispose());
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
    }

    /** HTML description with a clickable repository link, using the UI font. */
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