package com.diosaraiva.archutils.service;

import com.diosaraiva.archutils.util.JarUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service layer for PlantUML operations: command building and diagram rendering.
 */
public final class PlantUmlService {

    private static final String PLANTUML_JAR = "plantuml/plantuml-1.2026.6.jar";

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
                "-tpng", pumlPath.toAbsolutePath().toString(),
                "-o", tempDir);
        File preview = new File(tempDir, "_preview.png");
        return preview.isFile() ? preview : null;
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
