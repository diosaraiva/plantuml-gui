package com.diosaraiva.archutils.ui;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.diosaraiva.archutils.ui.SwingUtils.menuItem;
import static com.diosaraiva.archutils.ui.SwingUtils.menuShortcut;

/**
 * Factory that builds the application menu bar.
 */
public final class MenuBarFactory {

    /** Font families offered in Settings, filtered to those actually installed. */
    private static final List<String> FONT_CHOICES = List.of(
            "Dialog", "SansSerif", "Serif", "Monospaced",
            "Arial", "Helvetica", "Verdana", "Tahoma",
            "Times New Roman", "Courier New", "Menlo", "Consolas");

    /** A named window size offered in the Settings ▸ Window submenu. */
    private record Resolution(int width, int height) {
        String label() {
            return width + " x " + height;
        }
    }

    /** Preset window sizes offered in Settings ▸ Window. */
    private static final List<Resolution> RESOLUTIONS = List.of(
            new Resolution(1024, 600),
            new Resolution(1280, 720),
            new Resolution(1366, 768),
            new Resolution(1440, 900),
            new Resolution(1600, 900),
            new Resolution(1920, 1080));

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
        JMenu menu = menu("File", KeyEvent.VK_F);
        menu.add(menuItem("Open", KeyEvent.VK_O, null, e -> onOpenFile(frame)));
        menu.addSeparator();
        menu.add(menuItem("Quit", KeyEvent.VK_Q, null, e -> System.exit(0)));
        return menu;
    }

    private static void onOpenFile(MainFrame frame) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open PlantUML File");
        chooser.setFileFilter(new FileNameExtensionFilter("PlantUML files (*.puml)", "puml"));
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            String content = Files.readString(chooser.getSelectedFile().toPath());
            frame.showPanel(frame.getPlantUmlPanel());
            frame.getPlantUmlPanel().getInputPanel().setCode(content);
        } catch (Exception ex) {
            SwingUtils.showError(frame, "Failed to open file: " + ex.getMessage());
        }
    }

    /**
     * Builds the Edit menu (Undo / Redo / Copy Text / Copy Image / Paste) wired
     * to the PlantUML editor. Undo and Redo enable and disable dynamically.
     */
    private static JMenu createEditMenu(MainFrame frame) {
        JMenu menu = menu("Edit", KeyEvent.VK_E);
        PlantUmlInputPanel input = frame.getPlantUmlPanel().getInputPanel();
        int mod = menuShortcut();

        JMenuItem undo = menuItem("Undo", KeyEvent.VK_U,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod), e -> input.undo());
        JMenuItem redo = menuItem("Redo", KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod | InputEvent.SHIFT_DOWN_MASK), e -> input.redo());
        menu.add(undo);
        menu.add(redo);
        menu.addSeparator();
        menu.add(menuItem("Copy Text", KeyEvent.VK_T,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, mod), e -> input.copyToClipboard()));
        menu.add(menuItem("Copy Image", KeyEvent.VK_I,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, mod | InputEvent.SHIFT_DOWN_MASK),
                e -> frame.getPlantUmlPanel().copyImageToClipboard()));
        menu.add(menuItem("Paste", KeyEvent.VK_P,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, mod), e -> input.paste()));

        Runnable sync = () -> {
            undo.setEnabled(input.canUndo());
            redo.setEnabled(input.canRedo());
        };
        input.addUndoStateListener(sync);
        sync.run();
        return menu;
    }

    private static JMenu createServicesMenu(MainFrame frame) {
        JMenu menu = menu("Services", KeyEvent.VK_V);
        menu.add(menuItem("PlantUML", 0, null, e -> frame.showPanel(frame.getPlantUmlPanel())));
        menu.add(menuItem("CSV/JSON/MD", 0, null, e -> frame.showPanel(frame.getCsvPanel())));
        return menu;
    }

    private static JMenu createSettingsMenu(MainFrame frame) {
        JMenu menu = menu("Settings", KeyEvent.VK_S);
        menu.add(createThemeMenu(frame));
        menu.add(createFontMenu(frame));
        menu.add(createWindowMenu(frame));
        return menu;
    }

    /** Window submenu: pick a preset resolution and resize the main window. */
    private static JMenu createWindowMenu(MainFrame frame) {
        JMenu windowMenu = menu("Window", KeyEvent.VK_W);
        List<String> labels = RESOLUTIONS.stream().map(Resolution::label).toList();
        String current = frame.getWidth() + " x " + frame.getHeight();
        addRadioGroup(windowMenu, labels, labels.contains(current) ? current : null,
                label -> RESOLUTIONS.stream()
                        .filter(r -> r.label().equals(label))
                        .findFirst()
                        .ifPresent(r -> frame.applyResolution(r.width(), r.height())));
        return windowMenu;
    }

    private static JMenu createThemeMenu(MainFrame frame) {
        JMenu themeMenu = menu("Theme", KeyEvent.VK_T);
        String currentLaf = UIManager.getLookAndFeel().getClass().getName();
        UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();

        String[] names = new String[infos.length];
        String selected = null;
        for (int i = 0; i < infos.length; i++) {
            names[i] = infos[i].getName();
            if (infos[i].getClassName().equals(currentLaf)) {
                selected = names[i];
            }
        }
        addRadioGroup(themeMenu, Arrays.asList(names), selected, name -> {
            for (UIManager.LookAndFeelInfo info : infos) {
                if (info.getName().equals(name)) {
                    applyLookAndFeel(info.getClassName(), frame);
                    return;
                }
            }
        });
        return themeMenu;
    }

    /** Font submenu, mirroring the Theme submenu: pick a family, apply app-wide. */
    private static JMenu createFontMenu(MainFrame frame) {
        JMenu fontMenu = menu("Font", KeyEvent.VK_O);
        Set<String> installed = new LinkedHashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        List<String> choices = FONT_CHOICES.stream()
                .filter(f -> installed.contains(f) || isLogicalFont(f))
                .toList();
        String current = UIManager.getFont("Label.font").getFamily();
        addRadioGroup(fontMenu, choices, choices.contains(current) ? current : null,
                family -> {
                    SwingUtils.applyFontFamily(family);
                    frame.pack();
                });
        return fontMenu;
    }

    private static JMenu createHelpMenu(MainFrame frame) {
        JMenu menu = menu("Help", KeyEvent.VK_H);
        menu.add(menuItem("About", KeyEvent.VK_A, null,
                e -> new AboutDialog(frame).setVisible(true)));
        return menu;
    }

    // -------------------- helpers --------------------

    private static JMenu menu(String text, int mnemonic) {
        JMenu menu = new JMenu(text);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    /** Adds a mutually-exclusive set of radio items, invoking {@code onSelect} on click. */
    private static void addRadioGroup(JMenu menu, List<String> labels,
                                      String selected, Consumer<String> onSelect) {
        ButtonGroup group = new ButtonGroup();
        for (String label : labels) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
            item.setSelected(label.equals(selected));
            item.addActionListener(e -> onSelect.accept(label));
            group.add(item);
            menu.add(item);
        }
    }

    private static boolean isLogicalFont(String family) {
        return switch (family) {
            case "Dialog", "DialogInput", "SansSerif", "Serif", "Monospaced" -> true;
            default -> false;
        };
    }

    private static void applyLookAndFeel(String className, MainFrame frame) {
        try {
            SwingUtils.applyLookAndFeel(className);
            frame.pack();
        } catch (Exception ex) {
            SwingUtils.showError(frame, "Failed to switch theme: " + ex.getMessage());
        }
    }
}
