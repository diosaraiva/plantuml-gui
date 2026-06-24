package com.diosaraiva.archutils.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility for running JAR files bundled in src/main/resources.
 * Extracts the JAR to a temp directory and executes it via ProcessBuilder.
 */
public final class JarUtils {

    private JarUtils() { }

    private static final String RESOURCES_DIR = "src" + File.separator
            + "main" + File.separator + "resources";

    /** Locates a resource JAR on the classpath or under src/main/resources. */
    public static File extractJar(String resourcePath) throws IOException {
        // 1. Try classpath (works when built with Maven/Gradle)
        URL url = JarUtils.class.getClassLoader().getResource(resourcePath);
        if (url != null && "file".equals(url.getProtocol())) {
            return new File(url.getPath());
        }

        try (InputStream in = JarUtils.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (in != null) {
                String baseName = resourcePath.replaceAll(".*/", "");
                Path temp = Files.createTempFile(baseName + "-", ".jar");
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                temp.toFile().deleteOnExit();
                return temp.toFile();
            }
        }

        // 2. Fall back to filesystem under src/main/resources
        File fsFile = new File(RESOURCES_DIR, resourcePath);
        if (fsFile.isFile()) {
            return fsFile;
        }

        throw new IOException("Resource not found on classpath or at "
                + fsFile.getAbsolutePath() + ": " + resourcePath);
    }

    /** Runs a resource JAR with the given arguments. */
    public static int runJar(String resourcePath, String... args)
            throws IOException, InterruptedException {
        return runJar(resourcePath, null, args);
    }

    /** Runs a resource JAR with the given arguments in a working directory. */
    public static int runJar(String resourcePath, File workingDir,
                             String... args)
            throws IOException, InterruptedException {
        return runJar(resourcePath, workingDir, Collections.emptyList(), args);
    }

    /**
     * Runs a resource JAR with optional JVM options (e.g. {@code -Dkey=value}),
     * a working directory and program arguments. JVM options are placed before
     * {@code -jar} so they are interpreted by the JVM rather than the program.
     */
    public static int runJar(String resourcePath, File workingDir,
                             List<String> jvmOptions, String... args)
            throws IOException, InterruptedException {
        File jar = extractJar(resourcePath);
        List<String> cmd = new java.util.ArrayList<>();
        cmd.add("java");
        if (jvmOptions != null) { cmd.addAll(jvmOptions); }
        cmd.add("-jar");
        cmd.add(jar.getAbsolutePath());
        cmd.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workingDir != null) { pb.directory(workingDir); }
        pb.inheritIO();
        return pb.start().waitFor();
    }
}
