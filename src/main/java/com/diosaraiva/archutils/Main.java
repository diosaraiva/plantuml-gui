package com.diosaraiva.archutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

import com.diosaraiva.archutils.i18n.I18n;
import com.diosaraiva.archutils.plantuml.PlantUmlConsole;
import com.diosaraiva.archutils.ui.MainFrame;

public class Main {

    private static final Path TEMP_DIR =
            Path.of(System.getProperty("user.dir"), "temp");

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Arch Utils");

        PlantUmlConsole.global().install();

        I18n.setLocale(AppSettings.getLanguage());

        cleanTempDir();
        Runtime.getRuntime().addShutdownHook(new Thread(Main::cleanTempDir));
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
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
