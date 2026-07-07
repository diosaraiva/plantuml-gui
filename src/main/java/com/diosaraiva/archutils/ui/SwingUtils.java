package com.diosaraiva.archutils.ui;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;

// Reusable, dependency-free Swing helpers shared across the UI: listeners,
// menu/toolbar factories, clipboard and look-and-feel utilities.
public final class SwingUtils {

    private SwingUtils() { }

    // Platform menu-shortcut modifier (Cmd on macOS, Ctrl elsewhere).
    public static int menuShortcut() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    // DocumentListener that runs onChange for any edit.
    public static DocumentListener onDocumentChange(Runnable onChange) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { onChange.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { onChange.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
        };
    }

    // Configured JMenuItem; mnemonic/accelerator/action are optional.
    public static JMenuItem menuItem(String text, int mnemonic,
                                     KeyStroke accelerator, ActionListener action) {
        var item = new JMenuItem(text);
        if (mnemonic != 0) { item.setMnemonic(mnemonic); }
        if (accelerator != null) { item.setAccelerator(accelerator); }
        if (action != null) { item.addActionListener(action); }
        return item;
    }

    // Centered toolbar row so every panel toolbar looks and lays out alike.
    public static JPanel createToolBar() {
        return new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
    }

    // Shared toolbar button styling so all toolbar buttons are identical.
    public static JButton createToolButton(String text, String tooltip) {
        var button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    // Copies plain text to the system clipboard.
    public static void copyText(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    // Copies an image to the system clipboard.
    public static void copyImage(Image image) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new ImageTransferable(image), null);
    }

    // Opens a URL in the default browser; failures are logged, never thrown.
    public static void browse(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            System.err.println("Failed to open URL " + url + ": " + ex.getMessage());
        }
    }

    // Standard error dialog.
    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Makes a text-based component honour the current L&F font.
    public static void useUiFont(JComponent component) {
        component.putClientProperty(javax.swing.JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        component.setFont(UIManager.getFont("Label.font"));
    }

    // Applies a look and feel and refreshes every open window.
    public static void applyLookAndFeel(String className) throws Exception {
        UIManager.setLookAndFeel(className);
        refreshAllWindows();
    }

    // Applies a font family to all UI defaults, preserving each font's style/size.
    public static void applyFontFamily(String family) {
        for (var key : new ArrayList<>(UIManager.getDefaults().keySet())) {
            if (UIManager.get(key) instanceof Font font) {
                UIManager.put(key, new FontUIResource(family, font.getStyle(), font.getSize()));
            }
        }
        refreshAllWindows();
    }

    // Rebuilds the component tree of every open window (after theme/font change).
    public static void refreshAllWindows() {
        for (var window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }

    // Exposes an Image to the clipboard via the image DataFlavor.
    private record ImageTransferable(Image image) implements Transferable {
        @Override public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }
        @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }
        @Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}