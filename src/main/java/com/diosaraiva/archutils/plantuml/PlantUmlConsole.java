package com.diosaraiva.archutils.plantuml;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Application-wide console capture used both by PlantUML compilation reporting
 * and by the global JDK console window.
 *
 * <p>This is the single capture mechanism referenced across the app: it holds an
 * in-memory buffer and can optionally tee {@link System#out}/{@link System#err}
 * so JVM output is mirrored to both the real terminal (for {@code java} CLI
 * visibility) and this buffer. Consumers pull via {@link #getText()} or subscribe
 * for incremental chunks.
 */
public final class PlantUmlConsole {

    // Shared instance backing the global console window and the tee redirection.
    private static final PlantUmlConsole GLOBAL = new PlantUmlConsole();

    public static PlantUmlConsole global() { return GLOBAL; }

    private final StringBuilder buffer = new StringBuilder();
    // CopyOnWrite so listeners can be notified without holding the append lock.
    private final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    private PrintStream originalOut;
    private PrintStream originalErr;
    private boolean installed;

    public PlantUmlConsole() { }

    // Redirects System.out/err through a tee that mirrors to the real stream and
    // this buffer. Idempotent; safe to call once at startup. Not reversed except
    // via uninstall(), so keep the originals for restoration.
    public synchronized void install() {
        if (installed) return;
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(tee(originalOut));
        System.setErr(tee(originalErr));
        installed = true;
    }

    // Restores the original streams; used by tests to avoid leaking the tee.
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

    // Appends captured text and notifies subscribers with the same chunk.
    public void append(String text) {
        if (text == null || text.isEmpty()) return;
        synchronized (this) { buffer.append(text); }
        for (var listener : listeners) { listener.accept(text); }
    }

    // Appends a labelled, newline-terminated block (used by compile reporting).
    public void appendBlock(String text) {
        if (text == null || text.isBlank()) return;
        append(text.strip() + System.lineSeparator());
    }

    public synchronized String getText() { return buffer.toString(); }

    // Clears the buffer; subscribers receive a null chunk meaning "reset".
    public void clear() {
        synchronized (this) { buffer.setLength(0); }
        for (var listener : listeners) { listener.accept(null); }
    }

    public void addListener(Consumer<String> listener) { listeners.add(listener); }

    public void removeListener(Consumer<String> listener) { listeners.remove(listener); }
}
