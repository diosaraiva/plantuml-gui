package com.diosaraiva.archutils.ui;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;

/**
 * Small collection of reusable Swing helpers shared across the UI, kept static
 * and dependency-free so panels and factories stay concise.
 */
public final class SwingUtils {

    private SwingUtils() { }

    /** Platform menu-shortcut modifier (Cmd on macOS, Ctrl elsewhere). */
    public static int menuShortcut() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    /** Returns a {@link DocumentListener} that runs {@code onChange} for any edit. */
    public static DocumentListener onDocumentChange(Runnable onChange) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { onChange.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { onChange.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
        };
    }

    /** Builds a configured {@link JMenuItem}. Mnemonic/accelerator are optional. */
    public static JMenuItem menuItem(String text, int mnemonic,
                                     KeyStroke accelerator, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        if (mnemonic != 0) {
            item.setMnemonic(mnemonic);
        }
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        if (action != null) {
            item.addActionListener(action);
        }
        return item;
    }

    /** Copies plain text to the system clipboard. */
    public static void copyText(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }

    /** Copies an image to the system clipboard via {@link ImageTransferable}. */
    public static void copyImage(Image image) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new ImageTransferable(image), null);
    }

    /** Opens a URL in the default browser; failures are logged, never thrown. */
    public static void browse(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            System.err.println("Failed to open URL " + url + ": " + ex.getMessage());
        }
    }

    /** Shows a standard error dialog. */
    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Makes a text-based {@link JComponent} honour the current L&F font. */
    public static void useUiFont(JComponent component) {
        component.putClientProperty(javax.swing.JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        component.setFont(UIManager.getFont("Label.font"));
    }

    /** Applies a look and feel and refreshes every open window. */
    public static void applyLookAndFeel(String className) throws Exception {
        UIManager.setLookAndFeel(className);
        refreshAllWindows();
    }

    /** Applies a font family to all UI defaults, preserving each font's style/size. */
    public static void applyFontFamily(String family) {
        for (Object key : new ArrayList<>(UIManager.getDefaults().keySet())) {
            if (UIManager.get(key) instanceof Font font) {
                UIManager.put(key, new FontUIResource(family, font.getStyle(), font.getSize()));
            }
        }
        refreshAllWindows();
    }

    /** Rebuilds the component tree of every open window (after theme/font change). */
    public static void refreshAllWindows() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }
}
