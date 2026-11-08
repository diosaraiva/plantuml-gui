package com.diosaraiva.plantumlgui.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

// Runs blocking work off the EDT and delivers both outcomes back on the EDT.
public final class Background {

    // Daemon threads only, so a pending task never keeps the JVM alive after the window closes.
    private static final ExecutorService EXEC = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "background");
        thread.setDaemon(true);
        return thread;
    });

    private Background() { }

    public static <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        EXEC.submit(() -> {
            try {
                T result = task.call();
                SwingUtilities.invokeLater(() -> onSuccess.accept(result));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> onError.accept(ex));
            }
        });
    }
}