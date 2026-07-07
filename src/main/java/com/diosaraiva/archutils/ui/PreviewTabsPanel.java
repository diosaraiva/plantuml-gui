package com.diosaraiva.archutils.ui;

import com.diosaraiva.archutils.i18n.I18n;

import javax.swing.JTabbedPane;

// Tabbed container: the diagram Preview tab (unchanged rendering) plus a Console
// tab for PlantUML compilation output. Tab titles are localized.
public class PreviewTabsPanel extends JTabbedPane {

    private final DiagramPreviewPanel previewPanel = new DiagramPreviewPanel();
    private final ConsolePanel consolePanel = new ConsolePanel();

    public PreviewTabsPanel() {
        addTab(I18n.get("tab.preview"), previewPanel);
        addTab(I18n.get("tab.console"), consolePanel);
    }

    // Re-titles the tabs and forwards the language change to both children.
    public void applyLanguage() {
        setTitleAt(0, I18n.get("tab.preview"));
        setTitleAt(1, I18n.get("tab.console"));
        previewPanel.applyLanguage();
        consolePanel.applyLanguage();
    }

    /** @return the embedded diagram preview panel. */
    public DiagramPreviewPanel getPreviewPanel() { return previewPanel; }

    /** @return the embedded console panel. */
    public ConsolePanel getConsolePanel() { return consolePanel; }

    /** Selects the Console tab. */
    public void showConsole() { setSelectedComponent(consolePanel); }

    /** Selects the Preview tab. */
    public void showPreview() { setSelectedComponent(previewPanel); }
}
