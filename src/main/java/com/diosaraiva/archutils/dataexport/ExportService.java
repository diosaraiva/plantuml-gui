package com.diosaraiva.archutils.dataexport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public sealed interface ExportService<T>
        permits CsvExportService, JsonExportService, MarkdownExportService {

    String format(T data);

    DataExportFormat target();

    default void export(T data, Path out) throws IOException {
        Files.writeString(out, format(data));
    }
}
