package com.diosaraiva.archutils.ui;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Factory that builds the application menu bar.
 */
public final class MenuBarFactory {

    private MenuBarFactory() { }

    public static JMenuBar create(MainFrame frame) {
        JMenuBar bar = new JMenuBar();
        bar.add(createFileMenu(frame));
        bar.add(createEditMenu(frame));
        bar.add(createServicesMenu(frame));
        bar.add(createSettingsMenu(frame));
        bar.add(createHelpMenu(frame));
        return bar;
    }

    private static JMenu createFileMenu(MainFrame frame) {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        JMenuItem open = new JMenuItem("Open");
        open.setMnemonic(KeyEvent.VK_O);
        open.addActionListener(e -> onOpenFile(frame));
        menu.add(open);

        menu.addSeparator();

        JMenuItem quit = new JMenuItem("Quit");
        quit.setMnemonic(KeyEvent.VK_Q);
        quit.addActionListener(e -> System.exit(0));
        menu.add(quit);
        return menu;
    }

    private static void onOpenFile(MainFrame frame) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open PlantUML File");
        chooser.setFileFilter(new FileNameExtensionFilter("PlantUML files (*.puml)", "puml"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                frame.showPanel(frame.getPlantUmlPanel());
                frame.getPlantUmlPanel().getInputPanel().setCode(content);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(frame,
                        "Failed to open file: " + ex.getMessage(),
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Builds the Edit menu (Undo / Redo / Copy to Clipboard) wired to the
     * PlantUML input editor. Undo and Redo items enable and disable
     * dynamically based on the editor's undo history.
     */
    private static JMenu createEditMenu(MainFrame frame) {
        JMenu menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);

        PlantUmlInputPanel input = frame.getPlantUmlPanel().getInputPanel();
        int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenuItem undo = new JMenuItem("Undo");
        undo.setMnemonic(KeyEvent.VK_U);
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut));
        undo.addActionListener(e -> input.undo());
        menu.add(undo);

        JMenuItem redo = new JMenuItem("Redo");
        redo.setMnemonic(KeyEvent.VK_R);
        redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut | InputEvent.SHIFT_DOWN_MASK));
        redo.addActionListener(e -> input.redo());
        menu.add(redo);

        menu.addSeparator();

        JMenuItem copyText = new JMenuItem("Copy Text");
        copyText.setMnemonic(KeyEvent.VK_T);
        copyText.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcut));
        copyText.addActionListener(e -> input.copyToClipboard());
        menu.add(copyText);

        JMenuItem copyImage = new JMenuItem("Copy Preview Image");
        copyImage.setMnemonic(KeyEvent.VK_I);
        copyImage.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcut | InputEvent.SHIFT_DOWN_MASK));
        copyImage.addActionListener(e -> frame.getPlantUmlPanel().copyImageToClipboard());
        menu.add(copyImage);

        JMenuItem paste = new JMenuItem("Paste");
        paste.setMnemonic(KeyEvent.VK_P);
        paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcut));
        paste.addActionListener(e -> input.paste());
        menu.add(paste);

        // Keep Undo/Redo enablement in sync with the editor's history.
        Runnable sync = () -> {
            undo.setEnabled(input.canUndo());
            redo.setEnabled(input.canRedo());
        };
        input.addUndoStateListener(sync);
        sync.run();

        return menu;
    }

    private static JMenu createServicesMenu(MainFrame frame) {
        JMenu menu = new JMenu("Services");
        menu.setMnemonic(KeyEvent.VK_V);
        JMenuItem plantUml = new JMenuItem("PlantUML");
        plantUml.addActionListener(e -> frame.showPanel(frame.getPlantUmlPanel()));
        menu.add(plantUml);
        JMenuItem csv = new JMenuItem("CSV/JSON/MD");
        csv.addActionListener(e -> frame.showPanel(frame.getCsvPanel()));
        menu.add(csv);
        return menu;
    }

    private static JMenu createSettingsMenu(MainFrame frame) {
        JMenu menu = new JMenu("Settings");
        menu.setMnemonic(KeyEvent.VK_S);

        JMenu themeMenu = new JMenu("Theme");
        themeMenu.setMnemonic(KeyEvent.VK_T);

        ButtonGroup group = new ButtonGroup();
        String currentLaf = UIManager.getLookAndFeel().getClass().getName();

        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(info.getName());
            item.setSelected(info.getClassName().equals(currentLaf));
            item.addActionListener(e -> switchLookAndFeel(info.getClassName(), frame));
            group.add(item);
            themeMenu.add(item);
        }

        menu.add(themeMenu);
        return menu;
    }

    private static void switchLookAndFeel(String className, MainFrame frame) {
        try {
            UIManager.setLookAndFeel(className);
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
            frame.pack();
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(frame,
                    "Failed to switch theme: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JMenu createHelpMenu(MainFrame frame) {
        JMenu menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        JMenuItem about = new JMenuItem("About");
        about.setMnemonic(KeyEvent.VK_A);
        about.addActionListener(e -> new AboutDialog(frame).setVisible(true));
        menu.add(about);
        return menu;
    }
}
