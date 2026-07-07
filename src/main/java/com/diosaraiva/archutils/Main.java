package com.diosaraiva.archutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

import com.diosaraiva.archutils.i18n.I18n;
import com.diosaraiva.archutils.plantuml.PlantUmlConsole;
import com.diosaraiva.archutils.ui.MainFrame;

// Application entry point. Installs the global console tee first so the Java
// Console captures every System.out/err write from startup, restores the saved
// language, then shows the main window using the platform default look and feel.
public class Main {

    private static final Path TEMP_DIR =
            Path.of(System.getProperty("user.dir"), "temp");

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Arch Utils");

        // Tee System.out/err before anything logs, so nothing is missed.
        PlantUmlConsole.global().install();
        // Apply persisted language so every window builds with the right locale.
        I18n.setLocale(AppSettings.getLanguage());

        cleanTempDir();
        Runtime.getRuntime().addShutdownHook(new Thread(Main::cleanTempDir));
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    // Best-effort cleanup of the temp render folder on start and shutdown.
    private static void cleanTempDir() {
        if (!Files.isDirectory(TEMP_DIR)) {
            return;
        }
        try (var files = Files.list(TEMP_DIR)) {
            files.forEach(Main::deleteQuietly);
        } catch (IOException ignored) {
            // Ignore failures; the folder is transient.
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Ignore individual delete failures.
        }
    }
}