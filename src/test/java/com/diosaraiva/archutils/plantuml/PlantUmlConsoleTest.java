package com.diosaraiva.archutils.plantuml;

// Self-verifying tests for the console capture/tee: buffering, System.out/err
// redirection, listener notification and clear. Uses a fresh instance (not the
// global one) and always restores the original streams.
public final class PlantUmlConsoleTest {

    private static int failures;

    public static void main(String[] args) {
        capturesAndTeesStreams();
        appendAndClearBuffer();
        appendBlockAddsNewline();

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void capturesAndTeesStreams() {
        var console = new PlantUmlConsole();
        var received = new StringBuilder();
        console.addListener(chunk -> { if (chunk != null) received.append(chunk); });

        var realOut = System.out;
        console.install();
        try {
            System.out.println("hello-out");
            System.err.println("hello-err");
        } finally {
            console.uninstall();
        }
        // Streams restored; safe to assert/print normally now.
        check("streams restored", System.out == realOut);
        check("captures stdout", console.getText().contains("hello-out"));
        check("captures stderr", console.getText().contains("hello-err"));
        check("listener received output", received.toString().contains("hello-out"));
    }

    private static void appendAndClearBuffer() {
        var console = new PlantUmlConsole();
        var cleared = new boolean[1];
        console.addListener(chunk -> { if (chunk == null) cleared[0] = true; });
        console.append("abc");
        console.append("def");
        check("buffer accumulates", console.getText().equals("abcdef"));
        console.clear();
        check("clear empties buffer", console.getText().isEmpty());
        check("clear signals listeners with null", cleared[0]);
    }

    private static void appendBlockAddsNewline() {
        var console = new PlantUmlConsole();
        console.appendBlock("line");
        check("appendBlock terminates line",
                console.getText().equals("line" + System.lineSeparator()));
        console.appendBlock("   ");
        check("appendBlock ignores blank",
                console.getText().equals("line" + System.lineSeparator()));
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            System.out.println("PASS: " + name);
        } else {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private PlantUmlConsoleTest() { }
}
