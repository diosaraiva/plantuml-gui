package com.diosaraiva.archutils.dataexport;

import com.diosaraiva.archutils.util.SimpleJsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable tabular data shared by the CSV/JSON/Markdown export services.
 *
 * <p>Acts as the neutral intermediate representation: parse once from a source
 * format, then hand to any {@link ExportService} to render another format.
 */
public record TableData(List<String> headers, List<List<String>> rows) {

    // Defensive copies keep the record genuinely immutable.
    public TableData {
        headers = List.copyOf(headers);
        rows = rows.stream().map(List::copyOf).toList();
    }

    public boolean isEmpty() { return headers.isEmpty(); }

    // Row-major view as ordered maps; used by the JSON writer.
    public List<Map<String, String>> asMaps() {
        var result = new ArrayList<Map<String, String>>();
        for (var row : rows) {
            var map = new LinkedHashMap<String, String>();
            for (int i = 0; i < headers.size(); i++) {
                map.put(headers.get(i), i < row.size() ? row.get(i) : "");
            }
            result.add(map);
        }
        return result;
    }

    // Parses simple (unquoted) CSV: first non-empty line is the header row.
    public static TableData fromCsv(String csv) {
        var lines = csv.split("\\r?\\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            return new TableData(List.of(), List.of());
        }
        var headers = List.of(lines[0].split(",", -1));
        var rows = new ArrayList<List<String>>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            rows.add(List.of(lines[i].split(",", -1)));
        }
        return new TableData(headers, rows);
    }

    // Parses a JSON array of flat objects; header order follows the first object.
    public static TableData fromJson(String json) {
        var maps = SimpleJsonParser.parseArray(json);
        if (maps.isEmpty()) {
            return new TableData(List.of(), List.of());
        }
        var headers = new ArrayList<>(maps.getFirst().keySet());
        var rows = new ArrayList<List<String>>();
        for (var map : maps) {
            var row = new ArrayList<String>();
            for (var header : headers) {
                row.add(map.getOrDefault(header, ""));
            }
            rows.add(row);
        }
        return new TableData(headers, rows);
    }
}
