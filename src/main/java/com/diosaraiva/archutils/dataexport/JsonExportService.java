package com.diosaraiva.archutils.dataexport;

import com.diosaraiva.archutils.util.SimpleJsonParser;

// Renders TableData as a JSON array of flat objects.
public final class JsonExportService implements ExportService<TableData> {

    @Override
    public String format(TableData data) {
        if (data.isEmpty()) return "[]";
        return SimpleJsonParser.toArray(data.asMaps());
    }

    @Override
    public DataExportFormat target() { return DataExportFormat.JSON; }
}
