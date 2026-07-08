package com.diosaraiva.archutils.dataexport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.diosaraiva.archutils.util.SimpleJsonParser;

public record TableData(List<String> headers, List<List<String>> rows) {

    public TableData {
        headers = List.copyOf(headers);
        rows = rows.stream().map(List::copyOf).toList();
    }

    public boolean isEmpty() { return headers.isEmpty(); }

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
