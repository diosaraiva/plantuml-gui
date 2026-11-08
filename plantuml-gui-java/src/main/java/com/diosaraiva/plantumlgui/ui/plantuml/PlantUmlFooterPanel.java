package com.diosaraiva.plantumlgui.ui.plantuml;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.File;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.diosaraiva.plantumlgui.service.AppSettings;
import com.diosaraiva.plantumlgui.service.PlantUmlFormat;
import com.diosaraiva.plantumlgui.service.PlantUmlRenderer;
import com.diosaraiva.plantumlgui.ui.MainFrame;
import com.diosaraiva.plantumlgui.util.FileNames;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;

// Footer tabs: Export (target file + format) and Config (PlantUML JAR, save/restore settings).
// Nothing here or in the Settings menu touches java_config.ini until Save is pressed.
@SuppressWarnings("serial")
public final class PlantUmlFooterPanel extends JTabbedPane {

    private static final int EXPORT_TAB = 0;
    private static final int CONFIG_TAB = 1;

    // Appended to the Config tab title while edits are pending.
    private static final String UNSAVED_MARKER = " *";

    private final JLabel targetFileLabel = new JLabel(I18n.get("export.targetFile"));
    private final JTextField targetFileField = new JTextField(20);
    private final JButton browseButton = new JButton(I18n.get("export.browse"));
    private final JLabel formatLabel = new JLabel(I18n.get("export.format"));
    private final JComboBox<PlantUmlFormat> formatCombo =
            new JComboBox<>(new DefaultComboBoxModel<>(PlantUmlFormat.values()));
    private final JButton exportButton = new JButton(I18n.get("export.button"));
    private final JButton copyImageButton = new JButton(I18n.get("export.copy"));

    private final JLabel jarPathLabel = new JLabel(I18n.get("config.jarPath"));
    private final JTextField jarPathField = new JTextField(24);
    private final JButton jarBrowseButton = new JButton(I18n.get("export.browse"));
    private final JButton saveButton = new JButton(I18n.get("config.save"));
    private final JButton discardButton = new JButton(I18n.get("config.discard"));
    private final JButton defaultButton = new JButton(I18n.get("config.default"));

    public PlantUmlFooterPanel() {
        // Combo shows the localised label while the enum constant stays the value.
        formatCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                Object text = value instanceof PlantUmlFormat format ? format.label() : value;
                return super.getListCellRendererComponent(list, text, index, selected, focused);
            }
        });

        browseButton.addActionListener(e -> chooseFile(targetFileField, I18n.get("export.browse.title"), null));
        jarBrowseButton.addActionListener(e -> chooseFile(jarPathField, I18n.get("config.browse.title"),
                new FileNameExtensionFilter(I18n.get("config.jar.filter"), "jar")));
        saveButton.addActionListener(e -> onSave());
        discardButton.addActionListener(e -> onDiscard());
        defaultButton.addActionListener(e -> onRestoreDefaults());

        applySettingsToFields();
        addTab(I18n.get("export.panel.title"), buildExportTab());
        addTab(I18n.get("config.tab"), buildConfigTab());
        applyLanguage();

        // The Settings menu edits the same in-memory values, so track them from here too.
        AppSettings.addChangeListener(this::updateUnsavedState);
        updateUnsavedState();
    }

    public PlantUmlFormat getSelectedFormat() {
        return (PlantUmlFormat) formatCombo.getSelectedItem();
    }

    public String getTargetFile() {
        return targetFileField.getText().trim();
    }

    public void setTargetFileExtension(PlantUmlFormat format) {
        targetFileField.setText(FileNames.withExtension(getTargetFile(), format.extension()));
    }

    public void onExportDiagram(ActionListener listener) {
        exportButton.addActionListener(listener);
    }

    public void onCopyImage(ActionListener listener) {
        copyImageButton.addActionListener(listener);
    }

    public void onFormatChanged(ActionListener listener) {
        formatCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                listener.actionPerformed(null);
            }
        });
    }

    public void setCopyImageEnabled(boolean enabled) {
        copyImageButton.setEnabled(enabled);
    }

    public void applyLanguage() {
        setTitleAt(EXPORT_TAB, I18n.get("export.panel.title"));
        targetFileLabel.setText(I18n.get("export.targetFile"));
        formatLabel.setText(I18n.get("export.format"));
        formatCombo.setToolTipText(I18n.get("export.format.tooltip"));
        browseButton.setText(I18n.get("export.browse"));
        exportButton.setText(I18n.get("export.button"));
        copyImageButton.setText(I18n.get("export.copy"));
        copyImageButton.setToolTipText(I18n.get("export.copy.tooltip"));
        jarPathLabel.setText(I18n.get("config.jarPath"));
        jarBrowseButton.setText(I18n.get("export.browse"));
        saveButton.setText(I18n.get("config.save"));
        discardButton.setText(I18n.get("config.discard"));
        discardButton.setToolTipText(I18n.get("config.discard.tooltip"));
        defaultButton.setText(I18n.get("config.default"));
        defaultButton.setToolTipText(I18n.get("config.default.tooltip"));
        updateUnsavedState();
        repaint();
    }

    // Marks the Config tab and retargets the Save/Discard affordances to the pending state.
    private void updateUnsavedState() {
        boolean dirty = AppSettings.isDirty();
        setTitleAt(CONFIG_TAB, I18n.get("config.tab") + (dirty ? UNSAVED_MARKER : ""));
        setToolTipTextAt(CONFIG_TAB, dirty ? I18n.get("config.unsaved.tooltip") : null);
        saveButton.setToolTipText(dirty
                ? I18n.get("config.save.pending") : I18n.get("config.save.tooltip"));
        discardButton.setEnabled(dirty);
    }

    private JPanel buildExportTab() {
        var panel = newTabPanel();
        var gbc = baseConstraints();
        addRow(panel, gbc, targetFileLabel, targetFileField, browseButton);
        gbc.gridx++;
        panel.add(formatLabel, gbc);
        gbc.gridx++;
        panel.add(formatCombo, gbc);
        gbc.gridx++;
        panel.add(exportButton, gbc);
        gbc.gridx++;
        panel.add(copyImageButton, gbc);
        return panel;
    }

    private JPanel buildConfigTab() {
        var panel = newTabPanel();
        var gbc = baseConstraints();
        addRow(panel, gbc, jarPathLabel, jarPathField, jarBrowseButton);
        gbc.gridx++;
        panel.add(saveButton, gbc);
        gbc.gridx++;
        panel.add(discardButton, gbc);
        gbc.gridx++;
        gbc.insets = new Insets(4, 1, 4, 6);
        panel.add(defaultButton, gbc);
        return panel;
    }

    // Both tabs start with the same label + stretching field + browse button trio.
    private static void addRow(JPanel panel, GridBagConstraints gbc,
                               JLabel label, JTextField field, JButton browse) {
        gbc.gridx = 0;
        panel.add(label, gbc);

        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 1);
        panel.add(field, gbc);

        gbc.gridx++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 1, 4, 6);
        panel.add(browse, gbc);
    }

    private static JPanel newTabPanel() {
        var panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        return panel;
    }

    private static GridBagConstraints baseConstraints() {
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    // Collects the editable fields and writes everything to java_config.ini.
    // This is the only action in the app that persists settings.
    private void onSave() {
        String path = jarPathField.getText().trim();
        AppSettings.setJarPath(path.equals(PlantUmlRenderer.bundledJarPath()) ? "" : path);
        AppSettings.set(AppSettings.EXPORT_TARGET, relativeToOutputDir(getTargetFile()));
        AppSettings.set(AppSettings.EXPORT_FORMAT, getSelectedFormat().name());
        AppSettings.save();
        SwingUtils.showInfo(this, I18n.get("config.saved.title"),
                I18n.get("config.saved.msg", AppSettings.file()));
    }

    // Throws away pending edits by re-reading the file, then re-applies what was stored.
    private void onDiscard() {
        if (!SwingUtils.confirm(this, I18n.get("config.discard.title"),
                I18n.get("config.discard.confirm", AppSettings.file()))) {
            return;
        }
        AppSettings.discardChanges();
        applySettingsToFields();
        applySettingsToWindow();
    }

    // Overwrites java_config.ini with the bundled defaults and re-applies them live.
    private void onRestoreDefaults() {
        if (!SwingUtils.confirm(this, I18n.get("config.default.title"),
                I18n.get("config.default.confirm", AppSettings.file()))) {
            return;
        }
        AppSettings.resetToDefaults();
        applySettingsToFields();
        applySettingsToWindow();
        SwingUtils.showInfo(this, I18n.get("config.default.title"),
                I18n.get("config.default.msg", AppSettings.file()));
    }

    private void applySettingsToFields() {
        jarPathField.setText(effectiveJarPath());
        targetFileField.setText(configuredTargetFile());
        formatCombo.setSelectedItem(
                PlantUmlFormat.fromName(AppSettings.get(AppSettings.EXPORT_FORMAT), PlantUmlFormat.PNG));
    }

    private void applySettingsToWindow() {
        try {
            SwingUtils.applyLookAndFeel(AppSettings.get(AppSettings.THEME));
        } catch (Exception ex) {
            SwingUtils.showError(this, I18n.get("app.title"), I18n.get("theme.switch.failed", ex.getMessage()));
        }
        SwingUtils.applyFontFamily(AppSettings.get(AppSettings.FONT));
        I18n.setLocale(AppSettings.getLanguage());

        if (SwingUtilities.getWindowAncestor(this) instanceof MainFrame frame) {
            frame.applyResolution(AppSettings.windowWidth(), AppSettings.windowHeight());
            frame.reloadLanguage();
        }
    }

    private void chooseFile(JTextField field, String title, FileNameExtensionFilter filter) {
        SwingUtils.chooseFile(this, title, filter).ifPresent(file -> field.setText(file.getAbsolutePath()));
    }

    // Config value is either a name relative to the output dir or an absolute path.
    private static String configuredTargetFile() {
        Path configured = Path.of(AppSettings.get(AppSettings.EXPORT_TARGET));
        return configured.isAbsolute()
                ? configured.toString() : AppSettings.outputDir().resolve(configured).toString();
    }

    // Inverse of configuredTargetFile(): keep paths short when inside the output dir.
    private static String relativeToOutputDir(String target) {
        String prefix = AppSettings.outputDir() + File.separator;
        return target.startsWith(prefix) ? target.substring(prefix.length()) : target;
    }

    private static String effectiveJarPath() {
        String custom = AppSettings.getJarPath();
        return custom.isEmpty() ? PlantUmlRenderer.bundledJarPath() : custom;
    }
}