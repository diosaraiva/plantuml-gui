package com.diosaraiva.archutils.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * About dialog displaying application information.
 */
public class AboutDialog extends JDialog {

    private static final String REPO_URL = "https://github.com/diosaraiva/arch-utils";

    public AboutDialog(JFrame parent) {
        super(parent, "About Arch Utils", true);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel infoPanel = new JPanel(new BorderLayout(8, 8));

        JLabel titleLabel = new JLabel("Arch Utils");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        infoPanel.add(titleLabel, BorderLayout.NORTH);

        infoPanel.add(createDescriptionPane(), BorderLayout.CENTER);

        JLabel versionLabel = new JLabel("Version 1.1.1");
        versionLabel.setHorizontalAlignment(JLabel.CENTER);
        infoPanel.add(versionLabel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
    }

    /** HTML description with a clickable repository link, using the UI font. */
    private JEditorPane createDescriptionPane() {
        JEditorPane pane = new JEditorPane("text/html",
                "<html><center>Architecture utilities application.<br>"
                + "Provides tools for generating PlantUML diagrams<br>"
                + "and other architecture-related utilities.<br><br>"
                + "<a href=\"" + REPO_URL + "\">" + REPO_URL + "</a>"
                + "</center></html>");
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
