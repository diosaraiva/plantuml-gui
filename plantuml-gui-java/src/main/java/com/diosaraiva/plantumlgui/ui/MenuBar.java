package com.diosaraiva.plantumlgui.ui;

import static com.diosaraiva.plantumlgui.util.SwingUtils.menuItem;
import static com.diosaraiva.plantumlgui.util.SwingUtils.menuShortcut;

import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.diosaraiva.plantumlgui.service.AppSettings;
import com.diosaraiva.plantumlgui.service.PlantUmlFormat;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;

// Builds the whole menu bar; rebuilt from scratch whenever the language or window size changes.
public final class MenuBar {

    private record Resolution(int width, int height) {

        String label() { return width + " x " + height; }
    }

    private static final List<Resolution> RESOLUTIONS = List.of(
            new Resolution(1024, 600), new Resolution(1280, 720), new Resolution(1366, 768),
            new Resolution(1440, 900), new Resolution(1600, 900), new Resolution(1920, 1080));

    // Always available regardless of the installed fonts.
    private static final Set<String> LOGICAL_FONTS =
            Set.of("Dialog", "DialogInput", "SansSerif", "Serif", "Monospaced");

    private static final List<String> FONT_CHOICES = List.of(
            "Dialog", "SansSerif", "Serif", "Monospaced", "Arial", "Helvetica", "Verdana",
            "Tahoma", "Times New Roman", "Courier New", "Menlo", "Consolas");

    private static final Map<String, Locale> LANGUAGES = new LinkedHashMap<>();
    static {
        LANGUAGES.put("English (US)", I18n.EN_US);
        LANGUAGES.put("Português (BR)", I18n.PT_BR);
        LANGUAGES.put("Español (ES)", I18n.ES_ES);
    }

    private MenuBar() { }

    public static JMenuBar create(MainFrame frame) {
        var bar = new JMenuBar();
        bar.add(createFileMenu(frame));
        bar.add(createEditMenu(frame));
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

    private static JMenu createEditMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.edit"), KeyEvent.VK_E);
        var panel = frame.getPlantUmlPanel();
        var input = panel.getInputPanel();
        int mod = menuShortcut();

        var undo = menuItem(I18n.get("menu.edit.undo"), KeyEvent.VK_U,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod), e -> input.undo());
        var redo = menuItem(I18n.get("menu.edit.redo"), KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod | InputEvent.SHIFT_DOWN_MASK), e -> input.redo());
        var cut = menuItem(I18n.get("menu.edit.cut"), KeyEvent.VK_C,
                KeyStroke.getKeyStroke(KeyEvent.VK_X, mod), e -> panel.cut());
        var copy = menuItem(I18n.get("menu.edit.copyText"), KeyEvent.VK_T,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, mod), e -> panel.copyText());
        var paste = menuItem(I18n.get("menu.edit.paste"), KeyEvent.VK_P,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, mod), e -> panel.paste());

        menu.add(undo);
        menu.add(redo);
        menu.addSeparator();
        menu.add(cut);
        menu.add(copy);
        menu.add(menuItem(I18n.get("menu.edit.copyImage"), KeyEvent.VK_I,
                KeyStroke.getKeyStroke(KeyEvent.VK_C, mod | InputEvent.SHIFT_DOWN_MASK),
                e -> panel.copyImageToClipboard()));
        menu.add(paste);
        menu.add(menuItem(I18n.get("menu.edit.selectAll"), KeyEvent.VK_A,
                KeyStroke.getKeyStroke(KeyEvent.VK_A, mod), e -> panel.selectAll()));

        // Follows the active tab, the selection and the undo history of the working area.
        panel.setEditStateListener(() -> {
            undo.setEnabled(input.canUndo());
            redo.setEnabled(input.canRedo());
            cut.setEnabled(panel.canCut());
            copy.setEnabled(panel.canCopyText());
            paste.setEnabled(panel.canPaste());
        });
        return menu;
    }

    private static JMenu createSettingsMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.settings"), KeyEvent.VK_S);
        menu.add(createThemeMenu(frame));
        menu.add(createFontMenu(frame));
        menu.add(createWindowMenu(frame));
        menu.add(createLanguageMenu(frame));
        return menu;
    }

    private static JMenu createThemeMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.settings.theme"), KeyEvent.VK_T);
        LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
        String currentClass = UIManager.getLookAndFeel().getClass().getName();
        List<String> names = Arrays.stream(infos).map(LookAndFeelInfo::getName).toList();
        String selected = Arrays.stream(infos)
                .filter(info -> info.getClassName().equals(currentClass))
                .map(LookAndFeelInfo::getName).findFirst().orElse(null);

        addRadioGroup(menu, names, selected, name -> Arrays.stream(infos)
                .filter(info -> info.getName().equals(name))
                .findFirst()
                .ifPresent(info -> applyLookAndFeel(info.getClassName(), frame)));
        return menu;
    }

    private static JMenu createFontMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.settings.font"), KeyEvent.VK_O);
        var installed = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        List<String> choices = FONT_CHOICES.stream()
                .filter(f -> LOGICAL_FONTS.contains(f) || installed.contains(f)).toList();
        String current = UIManager.getFont("Label.font").getFamily();

        addRadioGroup(menu, choices, choices.contains(current) ? current : null, family -> {
            SwingUtils.applyFontFamily(family);
            AppSettings.set(AppSettings.FONT, family);
            frame.pack();
        });
        return menu;
    }

    private static JMenu createWindowMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.settings.window"), KeyEvent.VK_W);
        List<String> labels = RESOLUTIONS.stream().map(Resolution::label).toList();
        String current = new Resolution(frame.getSelectedWidth(), frame.getSelectedHeight()).label();

        addRadioGroup(menu, labels, labels.contains(current) ? current : null,
                label -> RESOLUTIONS.stream()
                        .filter(r -> r.label().equals(label))
                        .findFirst()
                        .ifPresent(r -> frame.applyResolution(r.width(), r.height())));
        return menu;
    }

    private static JMenu createLanguageMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.settings.language"), KeyEvent.VK_L);
        Locale current = I18n.getLocale();
        String selected = LANGUAGES.entrySet().stream()
                .filter(e -> e.getValue().equals(current))
                .map(Map.Entry::getKey).findFirst().orElse(null);

        addRadioGroup(menu, List.copyOf(LANGUAGES.keySet()), selected, label -> {
            Locale locale = LANGUAGES.get(label);
            AppSettings.setLanguage(locale);
            I18n.setLocale(locale);
            frame.reloadLanguage();
        });
        return menu;
    }

    private static JMenu createHelpMenu(MainFrame frame) {
        var menu = menu(I18n.get("menu.help"), KeyEvent.VK_H);
        menu.add(menuItem(I18n.get("menu.help.about"), KeyEvent.VK_A, null,
                e -> new AboutDialog(frame).setVisible(true)));
        return menu;
    }

    private static void onOpenFile(MainFrame frame) {
        var filter = new FileNameExtensionFilter(I18n.get("dialog.open.filter"), PlantUmlFormat.PUML.extension());
        SwingUtils.chooseFile(frame, I18n.get("dialog.open.title"), filter).ifPresent(file -> {
            try {
                frame.getPlantUmlPanel().getInputPanel().setCode(Files.readString(file.toPath()));
            } catch (IOException ex) {
                SwingUtils.showError(frame, I18n.get("app.title"), I18n.get("file.open.failed", ex.getMessage()));
            }
        });
    }

    private static void applyLookAndFeel(String className, MainFrame frame) {
        try {
            SwingUtils.applyLookAndFeel(className);
            AppSettings.set(AppSettings.THEME, className);
            frame.pack();
        } catch (Exception ex) {
            SwingUtils.showError(frame, I18n.get("app.title"), I18n.get("theme.switch.failed", ex.getMessage()));
        }
    }

    private static JMenu menu(String text, int mnemonic) {
        var menu = new JMenu(text);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    // Mutually exclusive options rendered as radio items; every settings submenu uses this.
    private static void addRadioGroup(JMenu menu, List<String> labels,
                                      String selected, Consumer<String> onSelect) {
        var group = new ButtonGroup();
        for (String label : labels) {
            var item = new JRadioButtonMenuItem(label);
            item.setSelected(label.equals(selected));
            item.addActionListener(e -> onSelect.accept(label));
            group.add(item);
            menu.add(item);
        }
    }
}