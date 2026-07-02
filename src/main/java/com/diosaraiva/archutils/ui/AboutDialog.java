package com.diosaraiva.archutils.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * About dialog displaying application information.
 */
public class AboutDialog extends JDialog {

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

        JLabel descLabel = new JLabel(
                "<html><center>Architecture utilities application.<br>"
                + "Provides tools for generating PlantUML diagrams<br>"
                + "and other architecture-related utilities.</center></html>");
        descLabel.setHorizontalAlignment(JLabel.CENTER);
        infoPanel.add(descLabel, BorderLayout.CENTER);

        JLabel versionLabel = new JLabel("Version 1.1.1");
        versionLabel.setHorizontalAlignment(JLabel.CENTER);
        infoPanel.add(versionLabel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
    }
}
