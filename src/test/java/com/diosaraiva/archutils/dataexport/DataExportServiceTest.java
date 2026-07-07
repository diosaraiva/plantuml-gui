package com.diosaraiva.archutils.dataexport;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

// Self-verifying tests for the sealed ExportService implementations and
// TableData parsing. Run via main(); exits non-zero on any failure.
public final class DataExportServiceTest {

    private static int failures;

    public static void main(String[] args) throws Exception {
        parsesCsvIntoTable();
        parsesJsonIntoTable();
        csvServiceRoundTrips();
        jsonServiceProducesArray();
        markdownServiceProducesTable();
        exportWritesFile();
        formatsAreDistinct();

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void parsesCsvIntoTable() {
        var data = TableData.fromCsv("name,age\nAlice,30\nBob,25");
        check("csv headers", data.headers().equals(List.of("name", "age")));
        check("csv row count", data.rows().size() == 2);
        check("csv cell value", data.rows().get(0).equals(List.of("Alice", "30")));
    }

    private static void parsesJsonIntoTable() {
        var data = TableData.fromJson("[{\"name\":\"Alice\",\"age\":\"30\"}]");
        check("json headers", data.headers().equals(List.of("name", "age")));
        check("json row value", data.rows().get(0).equals(List.of("Alice", "30")));
    }

    private static void csvServiceRoundTrips() {
        var service = new CsvExportService();
        var data = TableData.fromCsv("name,city\nAlice,New York");
        var csv = service.format(data);
        check("csv service target", service.target() == DataExportFormat.CSV);
        check("csv service header line", csv.startsWith("name,city"));
        // Values with commas are quoted.
        check("csv quotes commas", service.format(
                TableData.fromJson("[{\"a\":\"x,y\"}]")).contains("\"x,y\""));
    }

    private static void jsonServiceProducesArray() {
        var service = new JsonExportService();
        var json = service.format(TableData.fromCsv("name,age\nAlice,30"));
        check("json service target", service.target() == DataExportFormat.JSON);
        check("json is array", json.strip().startsWith("[") && json.strip().endsWith("]"));
        check("json has key", json.contains("\"name\""));
        // Numeric-looking values are emitted unquoted by the writer.
        check("json numeric unquoted", json.contains(":30"));
    }

    private static void markdownServiceProducesTable() {
        var service = new MarkdownExportService();
        var md = service.format(TableData.fromCsv("name,age\nAlice,30"));
        check("md service target", service.target() == DataExportFormat.MARKDOWN);
        check("md header row", md.contains("| name | age |"));
        check("md divider row", md.contains("| --- | --- |"));
        check("md data row", md.contains("| Alice | 30 |"));
    }

    private static void exportWritesFile() throws Exception {
        var service = new JsonExportService();
        var data = TableData.fromCsv("name\nAlice");
        File tmp = File.createTempFile("dataexport-test", ".json");
        tmp.deleteOnExit();
        service.export(data, tmp.toPath());
        var content = Files.readString(tmp.toPath());
        check("export file non-empty", !content.isBlank());
        check("export file matches format", content.equals(service.format(data)));
    }

    private static void formatsAreDistinct() {
        check("csv ext", DataExportFormat.CSV.extension().equals("csv"));
        check("json mime", DataExportFormat.JSON.mimeType().equals("application/json"));
        check("md ext", DataExportFormat.MARKDOWN.extension().equals("md"));
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            System.out.println("PASS: " + name);
        } else {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private DataExportServiceTest() { }
}
