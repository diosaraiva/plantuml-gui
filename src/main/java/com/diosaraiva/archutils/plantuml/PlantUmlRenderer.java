package com.diosaraiva.archutils.plantuml;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.diosaraiva.archutils.util.JarUtils;

/**
 * Core PlantUML rendering: the single wrapper around the bundled PlantUML jar.
 *
 * <p>UI code must not touch PlantUML/JAR invocation directly; it goes through
 * this class (preview) and {@link PlantUmlExporter} (file export).
 */
public final class PlantUmlRenderer {

    private static final String PLANTUML_JAR = "plantuml/plantuml-1.2026.6.jar";

    // Bundled samples double as the include root for `!include` of sibling files.
    private static final String SAMPLES_RESOURCE = "plantuml/samples";
    private static final String SAMPLES_FS = "src" + File.separator + "main"
            + File.separator + "resources" + File.separator + "plantuml"
            + File.separator + "samples";

    private PlantUmlRenderer() { }

    // Outcome of a capturing preview compile: image (null on failure) + console text.
    public record CompileResult(File previewImage, int exitCode, String output) {
        public boolean isSuccess() { return exitCode == 0; }
    }

    // Renders a PNG preview into tempDir, returning the file or null on failure.
    public static File renderPreview(String code, String tempDir)
            throws IOException, InterruptedException {
        var dir = new File(tempDir);
        if (!dir.exists()) { Files.createDirectories(dir.toPath()); }
        Path pumlPath = Paths.get(tempDir, "_preview.puml");
        Files.writeString(pumlPath, code);
        JarUtils.runJar(PLANTUML_JAR, dir,
                includePathOptions(),
                "-tpng", pumlPath.toAbsolutePath().toString(),
                "-o", tempDir);
        var preview = new File(tempDir, "_preview.png");
        return preview.isFile() ? preview : null;
    }

    // Preview compile that captures stdout+stderr so syntax errors reach the
    // console. `-stdrpt:1` forces error reporting instead of only embedding it in
    // the generated image.
    public static CompileResult compilePreview(String code, String tempDir)
            throws IOException, InterruptedException {
        var dir = new File(tempDir);
        if (!dir.exists()) { Files.createDirectories(dir.toPath()); }
        Path pumlPath = Paths.get(tempDir, "_preview.puml");
        Files.writeString(pumlPath, code);

        JarUtils.JarRunResult run = JarUtils.runJarCapture(PLANTUML_JAR, dir,
                includePathOptions(),
                "-tpng", "-stdrpt:1", pumlPath.toAbsolutePath().toString(),
                "-o", tempDir);

        var preview = new File(tempDir, "_preview.png");
        return new CompileResult(preview.isFile() ? preview : null,
                run.exitCode(), run.combinedOutput());
    }

    // Runs PlantUML for a concrete output file/format. Package-visible so only
    // PlantUmlExporter drives file export.
    static void runExport(String code, File target, PlantUmlFormat format)
            throws IOException, InterruptedException {
        ensureParentDir(target);
        String baseName = stripExtension(target.getName());
        Path pumlPath = Paths.get(target.getParent(), baseName + ".puml");
        Files.writeString(pumlPath, code);

        // .puml just writes the source; nothing to render.
        if (!format.needsJar()) { return; }

        JarUtils.runJar(PLANTUML_JAR, target.getParentFile(),
                includePathOptions(),
                format.cliFlag(), pumlPath.toAbsolutePath().toString(),
                "-o", target.getParent());
    }

    // Headless JVM options: keep the subprocess off the macOS Dock and point
    // plantuml.include.path at the samples dir so relative `!include`s resolve.
    private static List<String> includePathOptions() {
        var opts = new ArrayList<String>();
        opts.add("-Djava.awt.headless=true");
        opts.add("-Dapple.awt.UIElement=true");
        var samplesDir = resolveSamplesDir();
        if (samplesDir != null) {
            opts.add("-Dplantuml.include.path=" + samplesDir.getAbsolutePath());
        }
        return opts;
    }

    private static File resolveSamplesDir() {
        URL url = PlantUmlRenderer.class.getClassLoader().getResource(SAMPLES_RESOURCE);
        if (url != null && "file".equals(url.getProtocol())) {
            var dir = new File(url.getPath());
            if (dir.isDirectory()) { return dir; }
        }
        var fsDir = new File(SAMPLES_FS);
        return fsDir.isDirectory() ? fsDir : null;
    }

    private static void ensureParentDir(File target) throws IOException {
        var parentDir = target.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            Files.createDirectories(parentDir.toPath());
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
