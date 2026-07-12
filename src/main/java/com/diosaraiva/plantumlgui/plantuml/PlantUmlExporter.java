package com.diosaraiva.plantumlgui.plantuml;

import java.io.File;
import java.io.IOException;

public final class PlantUmlExporter {

    private PlantUmlExporter() { }

    public static void export(String code, File targetFile, PlantUmlFormat format)
            throws IOException, InterruptedException {
        PlantUmlRenderer.runExport(code, targetFile, format);
    }

    public static void export(String code, File targetFile)
            throws IOException, InterruptedException {
        var ext = extensionOf(targetFile.getName());
        var format = PlantUmlFormat.fromExtension(ext).orElse(PlantUmlFormat.PNG);
        export(code, targetFile, format);
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1) : "";
    }
}
