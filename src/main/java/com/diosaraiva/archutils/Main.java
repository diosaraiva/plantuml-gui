package com.diosaraiva.archutils;

import com.diosaraiva.archutils.ui.MainFrame;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entry point for the Arch Utils application.
 */
public class Main {

    private static final Path TEMP_DIR =
            Path.of(System.getProperty("user.dir"), "temp");

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Arch Utils");

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
            // Best-effort cleanup; ignore failures.
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
