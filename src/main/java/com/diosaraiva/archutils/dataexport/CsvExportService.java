package com.diosaraiva.archutils.dataexport;

import java.util.stream.Collectors;

public final class CsvExportService implements ExportService<TableData> {

    @Override
    public String format(TableData data) {
        if (data.isEmpty()) return "";
        var sb = new StringBuilder(String.join(",", data.headers()));
        for (var row : data.rows()) {
            sb.append('\n').append(row.stream()
                    .map(CsvExportService::escape)
                    .collect(Collectors.joining(",")));
        }
        return sb.toString();
    }

    @Override
    public DataExportFormat target() { return DataExportFormat.CSV; }

    private static String escape(String value) {
        return value.contains(",") ? "\"" + value + "\"" : value;
    }
}
