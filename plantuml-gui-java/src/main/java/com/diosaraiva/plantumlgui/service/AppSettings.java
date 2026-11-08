package com.diosaraiva.plantumlgui.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.ResourceLocator;

// Single source of truth for settings: bundled java_config.ini defaults overlaid by the active copy on disk.
// Edits stay in memory until save() is called, so nothing persists without an explicit Save.
public final class AppSettings {

    private static final Logger LOG = Logger.getLogger(AppSettings.class.getName());

    private static final String FILE_NAME = "java_config.ini";

    private static final String LANGUAGE = "app.language";
    public static final String THEME = "app.theme";
    public static final String FONT = "app.font";
    private static final String WINDOW_WIDTH = "app.window.width";
    private static final String WINDOW_HEIGHT = "app.window.height";
    public static final String AUTO_PREVIEW = "app.autoPreview";
    public static final String PREVIEW_DELAY_MS = "app.previewDelayMs";
    private static final String JAR_PATH = "plantuml.jarPath";
    public static final String BUNDLED_JAR = "plantuml.bundledJar";
    public static final String INCLUDE_DIR = "plantuml.includeDir";
    public static final String JVM_OPTIONS = "plantuml.jvmOptions";
    private static final String OUTPUT_DIR = "plantuml.outputDir";
    public static final String EXPORT_FORMAT = "export.format";
    public static final String EXPORT_TARGET = "export.targetFile";

    private static final int DEFAULT_WINDOW_WIDTH = 1024;
    private static final int DEFAULT_WINDOW_HEIGHT = 600;

    // Ordered key/value view of the active configuration.
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    // Bundled template, read once; keeps comments and key order when writing.
    private static final List<String> TEMPLATE = readTemplate();

    // Notified whenever a value or the unsaved state changes, so the UI can show it.
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    // True when memory holds edits that were never written to the active file.
    private static boolean dirty;

    private static Path activeFile;

    static { load(); }

    private AppSettings() { }

    // Loads bundled defaults, then overlays the active file (creating it when absent).
    public static synchronized void load() {
        VALUES.clear();
        VALUES.putAll(parse(TEMPLATE));

        activeFile = locateActiveFile();
        if (!Files.isRegularFile(activeFile)) {
            write(activeFile, parse(TEMPLATE));
        }
        try {
            VALUES.putAll(parse(Files.readAllLines(activeFile, StandardCharsets.UTF_8)));
        } catch (IOException ex) {
            LOG.warning(() -> "Could not read " + activeFile + ": " + ex.getMessage());
        }
        markClean();
    }

    // Writes current values back to the active file. The only call that persists edits.
    public static synchronized void save() {
        write(activeFile, VALUES);
        markClean();
    }

    // Overwrites the active file with the bundled defaults, then reloads.
    public static synchronized void resetToDefaults() {
        write(activeFile, parse(TEMPLATE));
        load();
    }

    // Drops unsaved edits by re-reading the active file.
    public static synchronized void discardChanges() {
        load();
    }

    public static boolean isDirty() { return dirty; }

    public static void addChangeListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static Path file() { return activeFile; }

    public static String get(String key) { return VALUES.getOrDefault(key, "").trim(); }

    public static int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        String value = get(key);
        return value.isEmpty() ? fallback : Boolean.parseBoolean(value);
    }

    // Updates memory only; call save() to make it survive a restart.
    public static void set(String key, String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.equals(VALUES.get(key))) {
            return;
        }
        VALUES.put(key, normalized);
        dirty = true;
        fireChanged();
    }

    public static void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    public static void set(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    public static Locale getLanguage() {
        Locale locale = Locale.forLanguageTag(get(LANGUAGE));
        return locale.getLanguage().isEmpty() ? I18n.EN_US : locale;
    }

    public static void setLanguage(Locale locale) {
        set(LANGUAGE, locale.toLanguageTag());
    }

    public static int windowWidth() { return getInt(WINDOW_WIDTH, DEFAULT_WINDOW_WIDTH); }

    public static int windowHeight() { return getInt(WINDOW_HEIGHT, DEFAULT_WINDOW_HEIGHT); }

    public static void setWindowSize(int width, int height) {
        set(WINDOW_WIDTH, width);
        set(WINDOW_HEIGHT, height);
    }

    // Custom JAR path, or an empty string when the bundled JAR should be used.
    public static String getJarPath() { return get(JAR_PATH); }

    public static void setJarPath(String path) { set(JAR_PATH, path); }

    // Absolute directory that relative export targets resolve against.
    public static Path outputDir() {
        return Path.of(System.getProperty("user.dir"), get(OUTPUT_DIR));
    }

    private static void markClean() {
        dirty = false;
        fireChanged();
    }

    private static void fireChanged() {
        LISTENERS.forEach(Runnable::run);
    }

    // Active file: an existing java_config.ini up the tree, else the project's parent folder.
    private static Path locateActiveFile() {
        String override = System.getProperty("plantumlgui.config", "").trim();
        if (!override.isEmpty()) {
            return Path.of(override).toAbsolutePath();
        }
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(FILE_NAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        Path parent = dir.getParent();
        return (parent != null ? parent : dir).resolve(FILE_NAME);
    }

    // Logical lines of the bundled template; the trailing newline is dropped so that
    // rewriting the template reproduces it byte for byte.
    private static List<String> readTemplate() {
        try {
            var lines = new ArrayList<>(List.of(ResourceLocator.readString(FILE_NAME).split("\\R", -1)));
            if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
                lines.remove(lines.size() - 1);
            }
            return List.copyOf(lines);
        } catch (IOException ex) {
            LOG.warning(() -> "Bundled " + FILE_NAME + " not found: " + ex.getMessage());
            return List.of();
        }
    }

    private static Map<String, String> parse(List<String> lines) {
        var map = new LinkedHashMap<String, String>();
        for (String line : lines) {
            String trimmed = line.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0 && !isComment(trimmed)) {
                map.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
            }
        }
        return map;
    }

    private static boolean isComment(String trimmed) {
        return trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";");
    }

    // Renders the template with the given values, appending any keys the template does not know.
    // Writing the unmodified values must reproduce the template byte for byte.
    private static void write(Path target, Map<String, String> values) {
        var out = new ArrayList<String>();
        var written = new ArrayList<String>();
        for (String line : TEMPLATE) {
            String trimmed = line.trim();
            int eq = trimmed.indexOf('=');
            if (eq <= 0 || isComment(trimmed)) {
                out.add(line);
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            out.add(entry(key, values.getOrDefault(key, "")));
            written.add(key);
        }
        values.forEach((k, v) -> {
            if (!written.contains(k)) { out.add(entry(k, v)); }
        });
        try {
            if (target.getParent() != null) { Files.createDirectories(target.getParent()); }
            Files.write(target, out, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOG.warning(() -> "Could not write " + target + ": " + ex.getMessage());
        }
    }

    // Empty values are written without a trailing space, matching the template style.
    private static String entry(String key, String value) {
        return value.isEmpty() ? key + " =" : key + " = " + value;
    }
}