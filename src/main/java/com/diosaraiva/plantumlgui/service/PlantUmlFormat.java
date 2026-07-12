package com.diosaraiva.plantumlgui.service;

import java.util.Optional;

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

    public boolean needsJar() { return needsJar; }

    public String cliFlag() { return "-t" + extension; }

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
