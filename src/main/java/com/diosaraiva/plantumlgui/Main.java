package com.diosaraiva.plantumlgui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

import com.diosaraiva.plantumlgui.service.PlantUmlConsole;
import com.diosaraiva.plantumlgui.ui.main.MainFrame;
import com.diosaraiva.plantumlgui.ui.plantuml.PlantUmlOutputConsolePanel;
import com.diosaraiva.plantumlgui.util.I18n;

public class Main {

    private static final Path TEMP_DIR =
            Path.of(System.getProperty("user.dir"), "temp");

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "PlantUML GUI");

        PlantUmlConsole.global().install();

        I18n.setLocale(AppSettings.getLanguage());

        cleanTempDir();
        Runtime.getRuntime().addShutdownHook(new Thread(Main::cleanTempDir));
        SwingUtilities.invokeLater(() -> {
            // Start the Java console capturing JVM output in the background at launch.
            PlantUmlOutputConsolePanel.startBackground();
            new MainFrame().setVisible(true);
        });
    }

    private static void cleanTempDir() {
        if (!Files.isDirectory(TEMP_DIR)) {
            return;
        }
        try (var files = Files.list(TEMP_DIR)) {
            files.forEach(Main::deleteQuietly);
        } catch (IOException ignored) {

        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {

        }
    }
}
