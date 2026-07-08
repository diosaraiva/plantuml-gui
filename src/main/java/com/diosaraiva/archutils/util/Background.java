package com.diosaraiva.archutils.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

public final class Background {

    private static final ExecutorService EXEC = Executors.newVirtualThreadPerTaskExecutor();

    private Background() { }

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
