package com.diosaraiva.plantumlgui.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class SampleLoader {

    private static final String SAMPLES_DIR = "src" + File.separator
            + "main" + File.separator + "resources" + File.separator
            + "plantuml" + File.separator + "samples";

    private SampleLoader() { }

    public static String load(String fileName) throws IOException {
        InputStream in = SampleLoader.class.getClassLoader()
                .getResourceAsStream("plantuml/samples/" + fileName);
        if (in != null) {
            return readStream(in);
        }
        File file = new File(SAMPLES_DIR, fileName);
        if (file.isFile()) {
            return readFile(file);
        }
        throw new IOException("Sample not found: " + fileName);
    }

    private static String readStream(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, "UTF-8"))) {
            return readAll(reader);
        }
    }

    private static String readFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new FileReader(file))) {
            return readAll(reader);
        }
    }

    private static String readAll(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        return sb.toString();
    }
}
