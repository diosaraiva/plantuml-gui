package com.diosaraiva.plantumlgui.util;

import java.io.File;
import java.util.Locale;

// Shared file-name arithmetic; the only place extensions are added, stripped or read.
public final class FileNames {

    private FileNames() { }

    public static String baseName(String path) {
        int dot = path.lastIndexOf('.');
        return dot > 0 ? path.substring(0, dot) : path;
    }

    public static String baseName(File file) {
        return baseName(file.getName());
    }

    // Lower-case extension without the dot, empty when there is none.
    public static String extension(String path) {
        int dot = path.lastIndexOf('.');
        return dot > 0 ? path.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    public static String withExtension(String path, String extension) {
        return baseName(path) + "." + extension;
    }
}
