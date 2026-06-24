package com.diosaraiva.archutils.service;

import com.diosaraiva.archutils.util.JarUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service layer for PlantUML operations: command building and diagram rendering.
 */
public final class PlantUmlService {

    private static final String PLANTUML_JAR = "plantuml/plantuml-1.2026.6.jar";

    /** Resource path of the bundled sample diagrams (also holds shared includes). */
    private static final String SAMPLES_RESOURCE = "plantuml/samples";

    /** Filesystem fallback for the samples directory (running from project root). */
    private static final String SAMPLES_FS = "src" + File.separator + "main"
            + File.separator + "resources" + File.separator + "plantuml"
            + File.separator + "samples";

    private PlantUmlService() { }

    /**
     * Builds a human-readable command string for rendering a diagram.
     */
    public static String buildCommand(String code, String targetFile) {
        File target = new File(targetFile);
        String outputDir = target.getParent() != null ? target.getParent() : ".";
        String baseName = stripExtension(target.getName());
        String sourceFile = outputDir + File.separator + baseName + ".puml";

        StringBuilder sb = new StringBuilder();
        sb.append("# Step 1: Save PlantUML code to source file\n");
        sb.append("# ").append(sourceFile).append("\n\n");
        sb.append("# Step 2: Run PlantUML to generate output\n");
        sb.append("java -jar plantuml.jar");
        appendFormatFlag(sb, target.getName());
        sb.append(" \"").append(sourceFile).append("\"");
        sb.append(" -o \"").append(outputDir).append("\"");
        return sb.toString();
    }

    /**
     * Renders a PlantUML diagram to the target file.
     */
    public static void render(String code, String targetFile)
            throws IOException, InterruptedException {
        File target = new File(targetFile);
        ensureParentDir(target);

        String baseName = stripExtension(target.getName());
        Path pumlPath = Paths.get(target.getParent(), baseName + ".puml");
        Files.write(pumlPath, code.getBytes(StandardCharsets.UTF_8));

        String ext = getExtension(target.getName());
        if ("puml".equals(ext)) {
            return;
        }

        String formatArg = ext.isEmpty() || "txt".equals(ext)
                ? "-tsvg" : "-t" + ext;

        JarUtils.runJar(PLANTUML_JAR, target.getParentFile(),
                includePathOptions(),
                formatArg, pumlPath.toAbsolutePath().toString(),
                "-o", target.getParent());
    }

    /**
     * Renders a temporary PNG preview into the given temp directory.
     * Returns the temp PNG file, or null if generation fails.
     */
    public static File renderPreview(String code, String tempDir)
            throws IOException, InterruptedException {
        File dir = new File(tempDir);
        if (!dir.exists()) { Files.createDirectories(dir.toPath()); }
        Path pumlPath = Paths.get(tempDir, "_preview.puml");
        Files.write(pumlPath, code.getBytes(StandardCharsets.UTF_8));
        JarUtils.runJar(PLANTUML_JAR, dir,
                includePathOptions(),
                "-tpng", pumlPath.toAbsolutePath().toString(),
                "-o", tempDir);
        File preview = new File(tempDir, "_preview.png");
        return preview.isFile() ? preview : null;
    }

    /**
     * Builds the JVM options that let PlantUML resolve {@code !include} of
     * shared sample files (e.g. {@code !include custom_modular_ref.puml}).
     * Because each diagram is rendered from a standalone file written to a
     * temp/output folder, sibling include files would otherwise be missing;
     * pointing {@code plantuml.include.path} at the samples directory makes
     * those relative includes resolvable. Bundled stdlib includes written
     * with angle brackets (e.g. {@code <archimate/Archimate>}) keep working
     * from inside the jar regardless.
     */
    private static List<String> includePathOptions() {
        File samplesDir = resolveSamplesDir();
        if (samplesDir == null) {
            return Collections.emptyList();
        }
        List<String> opts = new ArrayList<>();
        opts.add("-Dplantuml.include.path=" + samplesDir.getAbsolutePath());
        return opts;
    }

    /** Resolves the samples directory on the classpath, then the filesystem. */
    private static File resolveSamplesDir() {
        URL url = PlantUmlService.class.getClassLoader()
                .getResource(SAMPLES_RESOURCE);
        if (url != null && "file".equals(url.getProtocol())) {
            File dir = new File(url.getPath());
            if (dir.isDirectory()) {
                return dir;
            }
        }
        File fsDir = new File(SAMPLES_FS);
        return fsDir.isDirectory() ? fsDir : null;
    }

    private static void ensureParentDir(File target) throws IOException {
        File parentDir = target.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            Files.createDirectories(parentDir.toPath());
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }

    private static void appendFormatFlag(StringBuilder sb, String fileName) {
        String ext = getExtension(fileName);
        if (!ext.isEmpty() && !"puml".equals(ext) && !"txt".equals(ext)) {
            sb.append(" -t").append(ext);
        }
    }
}
