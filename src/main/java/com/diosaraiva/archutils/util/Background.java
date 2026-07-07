package com.diosaraiva.archutils.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

/**
 * Runs blocking work (PlantUML rendering/export, file I/O) off the Event
 * Dispatch Thread and marshals the result back onto it.
 *
 * <p>Backed by a virtual-thread-per-task executor: tasks are cheap, so no pool
 * sizing is needed and long renders never starve each other. Success/error
 * callbacks always run on the EDT, so callers may touch Swing directly.
 */
public final class Background {

    // Virtual threads: one per submitted task, unbounded and cheap.
    private static final ExecutorService EXEC = Executors.newVirtualThreadPerTaskExecutor();

    private Background() { }

    // Runs task on a virtual thread; routes result/failure to the EDT.
    public static <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        EXEC.submit(() -> {
            try {
                T result = task.call();
                SwingUtilities.invokeLater(() -> onSuccess.accept(result));
            } catch (Throwable t) {
                SwingUtilities.invokeLater(() -> onError.accept(t));
            }
        });
    }
}
