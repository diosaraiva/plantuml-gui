package com.diosaraiva.plantumlgui.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Locates and executes an external JAR in a child JVM, capturing its output.
public final class JarUtils {

    // The child runs on the same JVM as the app, so a missing or older `java` on the PATH cannot break it.
    private static final String JAVA = currentJava();

    // A packaged JAR is copied out once per run, not once per render.
    private static final Map<String, File> EXTRACTED = new HashMap<>();

    private JarUtils() { }

    public static synchronized File extractJar(String resourcePath) throws IOException {
        File cached = EXTRACTED.get(resourcePath);
        if (cached != null && cached.isFile()) {
            return cached;
        }
        File jar = locateOrCopy(resourcePath);
        EXTRACTED.put(resourcePath, jar);
        return jar;
    }

    // Runs `java [jvmOptions] -jar <jar> [args]` and blocks until it exits.
    public static JarRunResult runJar(File jar, File workingDir, List<String> jvmOptions, String... args)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of(JAVA));
        cmd.addAll(jvmOptions);
        cmd.add("-jar");
        cmd.add(jar.getAbsolutePath());
        cmd.addAll(List.of(args));

        // A single merged pipe keeps messages in order and cannot block on an undrained second stream.
        Process process = new ProcessBuilder(cmd).directory(workingDir).redirectErrorStream(true).start();
        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        }
        return new JarRunResult(process.waitFor(), output);
    }

    public record JarRunResult(int exitCode, String output) {

        public boolean isSuccess() { return exitCode == 0; }
    }

    // Prefers a real file on disk; a resource packaged inside our own JAR is copied to a temp file.
    private static File locateOrCopy(String resourcePath) throws IOException {
        Optional<Path> onDisk = ResourceLocator.find(resourcePath);
        if (onDisk.isPresent()) {
            return onDisk.get().toFile();
        }
        try (InputStream in = ResourceLocator.openStream(resourcePath)) {
            Path temp = Files.createTempFile(Path.of(resourcePath).getFileName() + "-", ".jar");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return temp.toFile();
        }
    }

    private static String currentJava() {
        String exe = File.separatorChar == '\\' ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", exe);
        return Files.isExecutable(java) ? java.toString() : "java";
    }
}
