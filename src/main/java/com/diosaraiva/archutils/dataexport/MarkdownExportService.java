package com.diosaraiva.archutils.dataexport;

// Renders TableData as a GitHub-flavoured Markdown table.
public final class MarkdownExportService implements ExportService<TableData> {

    @Override
    public String format(TableData data) {
        if (data.isEmpty()) return "";
        var headers = data.headers();
        var sb = new StringBuilder();
        sb.append("| ").append(String.join(" | ", headers)).append(" |\n");
        sb.append("|").append(" --- |".repeat(headers.size())).append('\n');
        for (var row : data.rows()) {
            sb.append("| ");
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0) sb.append(" | ");
                sb.append(i < row.size() ? row.get(i) : "");
            }
            sb.append(" |\n");
        }
        return sb.toString().strip();
    }

    @Override
    public DataExportFormat target() { return DataExportFormat.MARKDOWN; }
}
