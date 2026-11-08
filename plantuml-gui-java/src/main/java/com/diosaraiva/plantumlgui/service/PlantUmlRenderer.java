package com.diosaraiva.plantumlgui.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.diosaraiva.plantumlgui.util.FileNames;
import com.diosaraiva.plantumlgui.util.JarUtils;
import com.diosaraiva.plantumlgui.util.JarUtils.JarRunResult;
import com.diosaraiva.plantumlgui.util.ResourceLocator;

// Runs the PlantUML JAR in a child JVM; owns the scratch directory used for previews.
public final class PlantUmlRenderer {

    private static final Logger LOG = Logger.getLogger(PlantUmlRenderer.class.getName());

    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"), "plantuml-gui");
    private static final String PREVIEW_NAME = "_preview";

    private PlantUmlRenderer() { }

    public record CompileResult(File previewImage, JarRunResult run) { }

    // Renders the source to PNG in the scratch directory; previewImage() is null when nothing was produced.
    // Synchronized because every preview shares one scratch file name.
    public static synchronized CompileResult compilePreview(String code) throws IOException, InterruptedException {
        Path dir = tempDir();
        Path puml = dir.resolve(PREVIEW_NAME + ".puml");
        Files.writeString(puml, code);

        JarRunResult run = JarUtils.runJar(resolveJar(), dir.toFile(), jvmOptions(),
                PlantUmlFormat.PNG.cliFlag(), "-stdrpt:1", puml.toString(), "-o", dir.toString());

        File preview = dir.resolve(PREVIEW_NAME + ".png").toFile();
        return new CompileResult(preview.isFile() ? preview : null, run);
    }

    // Writes the source next to the target and, for JAR-backed formats, renders the target itself.
    public static void export(String code, File target, PlantUmlFormat format)
            throws IOException, InterruptedException {
        File parent = target.getAbsoluteFile().getParentFile();
        Files.createDirectories(parent.toPath());
        Path puml = parent.toPath().resolve(FileNames.baseName(target) + ".puml");
        Files.writeString(puml, code);

        if (!format.needsJar()) {
            return;
        }
        JarRunResult run = JarUtils.runJar(resolveJar(), parent, jvmOptions(),
                format.cliFlag(), puml.toString(), "-o", parent.toString());
        if (!run.isSuccess()) {
            throw new IOException(run.output().isEmpty() ? "PlantUML exit code " + run.exitCode() : run.output());
        }
    }

    public static String bundledJarPath() {
        try {
            return bundledJar().getAbsolutePath();
        } catch (IOException ex) {
            LOG.warning(() -> "Bundled PlantUML JAR unavailable: " + ex.getMessage());
            return "";
        }
    }

    // Deletes previous scratch files; call at start-up and from a shutdown hook.
    public static void cleanTempDir() {
        if (!Files.isDirectory(TEMP_DIR)) {
            return;
        }
        try (var files = Files.list(TEMP_DIR)) {
            files.forEach(PlantUmlRenderer::deleteQuietly);
        } catch (IOException ex) {
            LOG.fine(() -> "Could not clean " + TEMP_DIR + ": " + ex.getMessage());
        }
    }

    // Custom JAR from the config file when it exists, otherwise the bundled one.
    private static File resolveJar() throws IOException {
        String custom = AppSettings.getJarPath();
        if (!custom.isEmpty() && new File(custom).isFile()) {
            return new File(custom);
        }
        return bundledJar();
    }

    private static File bundledJar() throws IOException {
        return JarUtils.extractJar(AppSettings.get(AppSettings.BUNDLED_JAR));
    }

    private static Path tempDir() throws IOException {
        return Files.createDirectories(TEMP_DIR);
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException | UncheckedIOException ignored) {
            // Leftovers are harmless; the next run overwrites them.
        }
    }

    // Configured JVM options plus the resolved PlantUML include path.
    private static List<String> jvmOptions() {
        var opts = new ArrayList<String>();
        String configured = AppSettings.get(AppSettings.JVM_OPTIONS);
        if (!configured.isEmpty()) {
            opts.addAll(Arrays.asList(configured.split("\\s+")));
        }
        ResourceLocator.find(AppSettings.get(AppSettings.INCLUDE_DIR))
                .filter(Files::isDirectory)
                .ifPresent(dir -> opts.add("-Dplantuml.include.path=" + dir.toAbsolutePath()));
        return opts;
    }
}