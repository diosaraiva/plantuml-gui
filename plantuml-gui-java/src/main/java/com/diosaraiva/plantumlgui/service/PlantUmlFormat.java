package com.diosaraiva.plantumlgui.service;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.diosaraiva.plantumlgui.util.I18n;

// Catalogue of export formats: the only place an extension, CLI flag or format label is defined.
public enum PlantUmlFormat {

    PNG("png", true, null),
    SVG("svg", true, null),
    PUML("puml", false, null),
    ARCHIMATE("xml", false, "format.archimate");

    private final String extension;
    private final boolean needsJar;
    private final String labelKey;

    PlantUmlFormat(String extension, boolean needsJar, String labelKey) {
        this.extension = extension;
        this.needsJar = needsJar;
        this.labelKey = labelKey;
    }

    public String extension() { return extension; }

    // False when the format is produced by this app rather than by the PlantUML JAR.
    public boolean needsJar() { return needsJar; }

    public String cliFlag() { return "-t" + extension; }

    // Localised combo-box text; formats without a key show their enum name.
    public String label() { return labelKey == null ? name() : I18n.get(labelKey); }

    public static PlantUmlFormat fromName(String name, PlantUmlFormat fallback) {
        return find(f -> f.name().equalsIgnoreCase(String.valueOf(name).strip())).orElse(fallback);
    }

    public static Optional<PlantUmlFormat> fromExtension(String extension) {
        return find(f -> f.extension.equalsIgnoreCase(String.valueOf(extension).strip()));
    }

    private static Optional<PlantUmlFormat> find(Predicate<PlantUmlFormat> match) {
        return Stream.of(values()).filter(match).findFirst();
    }
}