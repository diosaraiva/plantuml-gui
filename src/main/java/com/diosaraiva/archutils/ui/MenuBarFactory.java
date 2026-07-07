package com.diosaraiva.archutils.ui;

import static com.diosaraiva.archutils.ui.SwingUtils.menuItem;
import static com.diosaraiva.archutils.ui.SwingUtils.menuShortcut;

import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.diosaraiva.archutils.AppSettings;
import com.diosaraiva.archutils.i18n.I18n;
import com.diosaraiva.archutils.plantuml.ui.JavaConsoleWindow;

// Builds the application menu bar. All labels come from I18n; changing language
// rebuilds the bar in place so menus re-render without a restart.
public final class MenuBarFactory {

    // Font families offered in Settings, filtered to those actually installed.
    private static final List<String> FONT_CHOICES = List.of(
            "Dialog", "SansSerif", "Serif", "Monospaced",
            "Arial", "Helvetica", "Verdana", "Tahoma",
            "Times New Roman", "Courier New", "Menlo", "Consolas");

    // Language options shown as their own endonyms (proper nouns, not localized).
    private static final Map<String, Locale> LANGUAGES = new LinkedHashMap<>();
    static {
        LANGUAGES.put("English (US)", I18n.EN_US);
        LANGUAGES.put("Português (BR)", I18n.PT_BR);
        LANGUAGES.put("Español (ES)", I18n.ES_ES);
    }

    // A named window size offered in Settings > Window.
    private record Resolution(int width, int height) {
        String label() { return width + " x " + height; }
    }

    private static final List<Resolution> RESOLUTIONS = List.of(
            new Resolution(1024, 600),
            new Resolution(1280, 720),
            new Resolution(1366, 768),
            new Resolution(1440, 900),
            new Resolution(1600, 900),
            new Resolution(1920, 1080));

    private MenuBarFactory() { }

    public static JMenuBar create(MainFrame frame) {
        var bar = new JMenuBar();
        bar.add(createFileMenu(frame));
        bar.add(createEditMenu(frame));
        bar.add(createServicesMenu(frame));
        bar.add(createSettingsMenu(frame));
        bar.add(createHelpMenu(frame));
        return bar;
    }

    private static JMenu createFileMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.file"), KeyEvent.VK_F);
        menu.add(menuItem(I18n.get("menu.file.open"), KeyEvent.VK_O, null, e -> onOpenFile(frame)));
        menu.addSeparator();
        menu.add(menuItem(I18n.get("menu.file.quit"), KeyEvent.VK_Q, null, e -> System.exit(0)));
        return menu;
    }

    private static void onOpenFile(MainFrame frame) {
        var chooser = new JFileChooser();
        chooser.setDialogTitle(I18n.get("dialog.open.title"));
        chooser.setFileFilter(new FileNameExtensionFilter(I18n.get("dialog.open.filter"), "puml"));
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            var content = Files.readString(chooser.getSelectedFile().toPath());
            frame.showPanel(frame.getPlantUmlPanel());
            frame.getPlantUmlPanel().getInputPanel().setCode(content);
        } catch (Exception ex) {
            SwingUtils.showError(frame, I18n.get("file.open.failed", ex.getMessage()));
        }
    }

    // Edit menu wired to the PlantUML editor; Undo/Redo enable dynamically.
    private static JMenu createEditMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.edit"), KeyEvent.VK_E);
        var input = frame.getPlantUmlPanel().getInputPanel();
        int mod = menuShortcut();

        var undo = menuItem(I18n.get("menu.edit.undo"), KeyEvent.VK_U,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod), e -> input.undo());
        var redo = menuItem(I18n.get("menu.edit.redo"), KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod | InputEvent.SHIFT_DOWN_MASK), e -> input.redo());
        menu.add(undo);
        menu.add(redo);
        menu.addSeparator();
        menu.add(menuItem(I18n.get("menu.edit.copyText"), KeyEvent.VK_T,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, mod), e -> input.copyToClipboard()));
        menu.add(menuItem(I18n.get("menu.edit.copyImage"), KeyEvent.VK_I,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, mod | InputEvent.SHIFT_DOWN_MASK),
                e -> frame.getPlantUmlPanel().copyImageToClipboard()));
        menu.add(menuItem(I18n.get("menu.edit.paste"), KeyEvent.VK_P,
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
        var menu = menu(I18n.get("menu.services"), KeyEvent.VK_V);
        menu.add(menuItem(I18n.get("menu.services.plantuml"), 0, null,
                e -> frame.showPanel(frame.getPlantUmlPanel())));
        menu.add(menuItem(I18n.get("menu.services.csv"), 0, null,
                e -> frame.showPanel(frame.getCsvPanel())));
        return menu;
    }

    private static JMenu createSettingsMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.settings"), KeyEvent.VK_S);
        menu.add(createThemeMenu(frame));
        menu.add(createFontMenu(frame));
        menu.add(createWindowMenu(frame));
        menu.add(createLanguageMenu(frame));
        menu.addSeparator();
        menu.add(menuItem(I18n.get("menu.settings.console"), 0, null,
                e -> JavaConsoleWindow.open()));
        return menu;
    }

    // Language submenu: persists the choice, switches the locale, and refreshes
    // the whole window (menus + panels) so the change applies without a restart.
    private static JMenu createLanguageMenu(MainFrame frame) {
        var languageMenu = menu(I18n.get("menu.settings.language"), KeyEvent.VK_L);
        var current = I18n.getLocale();
        String selected = LANGUAGES.entrySet().stream()
                .filter(e -> e.getValue().equals(current))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        addRadioGroup(languageMenu, List.copyOf(LANGUAGES.keySet()), selected, label -> {
            var locale = LANGUAGES.get(label);
            AppSettings.setLanguage(locale);
            I18n.setLocale(locale);
            frame.reloadLanguage();
        });
        return languageMenu;
    }

    private static JMenu createWindowMenu(MainFrame frame) {
        var windowMenu = menu(I18n.get("menu.settings.window"), KeyEvent.VK_W);
        var labels = RESOLUTIONS.stream().map(Resolution::label).toList();
        var current = frame.getWidth() + " x " + frame.getHeight();
        addRadioGroup(windowMenu, labels, labels.contains(current) ? current : null,
                label -> RESOLUTIONS.stream()
                        .filter(r -> r.label().equals(label))
                        .findFirst()
                        .ifPresent(r -> frame.applyResolution(r.width(), r.height())));
        return windowMenu;
    }

    private static JMenu createThemeMenu(MainFrame frame) {
        var themeMenu = menu(I18n.get("menu.settings.theme"), KeyEvent.VK_T);
        var currentLaf = UIManager.getLookAndFeel().getClass().getName();
        var infos = UIManager.getInstalledLookAndFeels();

        var names = new String[infos.length];
        String selected = null;
        for (int i = 0; i < infos.length; i++) {
            names[i] = infos[i].getName();
            if (infos[i].getClassName().equals(currentLaf)) {
                selected = names[i];
            }
        }
        addRadioGroup(themeMenu, Arrays.asList(names), selected, name -> {
            for (var info : infos) {
                if (info.getName().equals(name)) {
                    applyLookAndFeel(info.getClassName(), frame);
                    return;
                }
            }
        });
        return themeMenu;
    }

    private static JMenu createFontMenu(MainFrame frame) {
        var fontMenu = menu(I18n.get("menu.settings.font"), KeyEvent.VK_O);
        Set<String> installed = new LinkedHashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        var choices = FONT_CHOICES.stream()
                .filter(f -> installed.contains(f) || isLogicalFont(f))
                .toList();
        var current = UIManager.getFont("Label.font").getFamily();
        addRadioGroup(fontMenu, choices, choices.contains(current) ? current : null,
                family -> {
                    SwingUtils.applyFontFamily(family);
                    frame.pack();
                });
        return fontMenu;
    }

    private static JMenu createHelpMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.help"), KeyEvent.VK_H);
        menu.add(menuItem(I18n.get("menu.help.about"), KeyEvent.VK_A, null,
                e -> new AboutDialog(frame).setVisible(true)));
        return menu;
    }

    // -------------------- helpers --------------------

    private static JMenu menu(String text, int mnemonic) {
        var menu = new JMenu(text);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    // Adds a mutually-exclusive set of radio items, invoking onSelect on click.
    private static void addRadioGroup(JMenu menu, List<String> labels,
                                      String selected, Consumer<String> onSelect) {
        var group = new ButtonGroup();
        for (var label : labels) {
            var item = new JRadioButtonMenuItem(label);
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
            SwingUtils.showError(frame, I18n.get("theme.switch.failed", ex.getMessage()));
        }
    }
}
