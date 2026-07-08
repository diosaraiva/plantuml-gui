package com.diosaraiva.archutils.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimpleJsonParser {

    private SimpleJsonParser() { }

    public static List<Map<String, String>> parseArray(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return result;
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) return result;
        for (String obj : splitObjects(inner)) {
            result.add(parseObject(obj.trim()));
        }
        return result;
    }

    public static String toArray(List<Map<String, String>> rows) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < rows.size(); i++) {
            sb.append("  {");
            Map<String, String> row = rows.get(i);
            List<String> keys = new ArrayList<>(row.keySet());
            for (int j = 0; j < keys.size(); j++) {
                String k = keys.get(j);
                sb.append('"').append(k).append("\":");
                sb.append(jsonValue(row.get(k)));
                if (j < keys.size() - 1) sb.append(',');
            }
            sb.append('}');
            if (i < rows.size() - 1) sb.append(',');
            sb.append('\n');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String jsonValue(String val) {
        try { Double.parseDouble(val); return val; }
        catch (NumberFormatException e) { return "\"" + val + "\""; }
    }

    private static List<String> splitObjects(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            if (depth == 0 && c == ',') {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts;
    }

    private static Map<String, String> parseObject(String obj) {
        Map<String, String> map = new LinkedHashMap<>();
        String inner = obj.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}")) inner = inner.substring(0, inner.length() - 1);
        for (String pair : inner.split(",(?=\\s*\")")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String val = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, val);
            }
        }
        return map;
    }
}
