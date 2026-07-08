package com.diosaraiva.archutils.dataexport;

public enum DataExportFormat {

    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
    MARKDOWN("md", "text/markdown");

    private final String extension;
    private final String mimeType;

    DataExportFormat(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String extension() { return extension; }

    public String mimeType() { return mimeType; }
}
