package com.diosaraiva.archutils.plantuml;

import java.io.File;
import java.io.IOException;

/**
 * Format-specific PlantUML file export. The only entry point the UI uses to
 * write diagram files; it delegates the actual jar invocation to
 * {@link PlantUmlRenderer}.
 */
public final class PlantUmlExporter {

    private PlantUmlExporter() { }

    // Exports PlantUML source to targetFile using the given format. The format is
    // authoritative for how the file is produced (see PlantUmlFormat.needsJar).
    public static void export(String code, File targetFile, PlantUmlFormat format)
            throws IOException, InterruptedException {
        PlantUmlRenderer.runExport(code, targetFile, format);
    }

    // Convenience overload that infers the format from the target file extension,
    // defaulting to PNG when the extension is unknown.
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
