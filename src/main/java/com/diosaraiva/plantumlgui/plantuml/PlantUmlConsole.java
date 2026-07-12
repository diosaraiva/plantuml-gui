package com.diosaraiva.plantumlgui.plantuml;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class PlantUmlConsole {

    private static final PlantUmlConsole GLOBAL = new PlantUmlConsole();

    public static PlantUmlConsole global() { return GLOBAL; }

    private final StringBuilder buffer = new StringBuilder();

    private final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    private PrintStream originalOut;
    private PrintStream originalErr;
    private boolean installed;

    public PlantUmlConsole() { }

    public synchronized void install() {
        if (installed) return;
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(tee(originalOut));
        System.setErr(tee(originalErr));
        installed = true;
    }

    public synchronized void uninstall() {
        if (!installed) return;
        System.setOut(originalOut);
        System.setErr(originalErr);
        installed = false;
    }

    public boolean isInstalled() { return installed; }

    private PrintStream tee(PrintStream real) {
        var mirror = new OutputStream() {
            @Override public void write(int b) {
                real.write(b);
                append(String.valueOf((char) (b & 0xFF)));
            }
            @Override public void write(byte[] b, int off, int len) {
                real.write(b, off, len);
                append(new String(b, off, len, StandardCharsets.UTF_8));
            }
        };
        return new PrintStream(mirror, true, StandardCharsets.UTF_8);
    }

    public void append(String text) {
        if (text == null || text.isEmpty()) return;
        synchronized (this) { buffer.append(text); }
        for (var listener : listeners) { listener.accept(text); }
    }

    public void appendBlock(String text) {
        if (text == null || text.isBlank()) return;
        append(text.strip() + System.lineSeparator());
    }

    public synchronized String getText() { return buffer.toString(); }

    public void clear() {
        synchronized (this) { buffer.setLength(0); }
        for (var listener : listeners) { listener.accept(null); }
    }

    public void addListener(Consumer<String> listener) { listeners.add(listener); }

    public void removeListener(Consumer<String> listener) { listeners.remove(listener); }
}
