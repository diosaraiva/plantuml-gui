package com.diosaraiva.archutils.plantuml;

import java.util.Optional;

// Closed set of PlantUML output formats. Enums are implicitly sealed, so this is
// the idiomatic "sealed" model for a fixed format list. `needsJar` marks formats
// that require invoking the PlantUML jar (raster/vector) vs. plain source passthrough.
public enum PlantUmlFormat {

    PNG("png", "PNG", true),
    SVG("svg", "SVG", true),
    PUML("puml", "PUML", false);

    private final String extension;
    private final String displayName;
    private final boolean needsJar;

    PlantUmlFormat(String extension, String displayName, boolean needsJar) {
        this.extension = extension;
        this.displayName = displayName;
        this.needsJar = needsJar;
    }

    public String extension() { return extension; }

    public String displayName() { return displayName; }

    // True when export must run the PlantUML jar; false for raw .puml source.
    public boolean needsJar() { return needsJar; }

    // PlantUML CLI flag for this format (e.g. -tpng). Only meaningful when needsJar.
    public String cliFlag() { return "-t" + extension; }

    // Resolves a format from a file extension; empty when unrecognised.
    public static Optional<PlantUmlFormat> fromExtension(String ext) {
        if (ext == null) return Optional.empty();
        var normalized = ext.toLowerCase().strip();
        for (var format : values()) {
            if (format.extension.equals(normalized)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
