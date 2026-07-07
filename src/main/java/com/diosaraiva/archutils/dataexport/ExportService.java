package com.diosaraiva.archutils.dataexport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Common contract for tabular data exporters.
 *
 * <p>Sealed to a fixed set of formats so callers can exhaustively switch over
 * implementations. {@link #format} renders to a String (used for live in-memory
 * previews); {@link #export} additionally writes that String to a file.
 *
 * @param <T> the data type an exporter accepts (here always {@link TableData})
 */
public sealed interface ExportService<T>
        permits CsvExportService, JsonExportService, MarkdownExportService {

    // Renders data to this service's target format as a String.
    String format(T data);

    // The concrete output format this service produces.
    DataExportFormat target();

    // Writes the rendered output to out (UTF-8). Default suffices for all
    // text-based formats; override only if binary output is ever needed.
    default void export(T data, Path out) throws IOException {
        Files.writeString(out, format(data));
    }
}
