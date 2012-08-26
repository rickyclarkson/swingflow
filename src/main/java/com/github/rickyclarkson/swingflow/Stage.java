package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.Monitorable;
import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;
import fj.data.Option;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class Stage<T> {
    private final MonitorableExecutorService executorService;
    private final String name;
    private final Monitorable<Progress<T>> command;
    private Option<MonitorableFuture<Progress<T>>> future = Option.none();
    public final List<T> possibleValues;

    public Stage(MonitorableExecutorService executorService, String name, final Monitorable<Progress<T>> command, List<T> possibleValues) {
        this.executorService = executorService;
        this.name = name;
        this.command = new Monitorable<Progress<T>>(command.updates) {
            @Override
            public Progress<T> call() throws Exception {
                final Progress<T> result = command.call();
                if (!updates.offer(result, 10, TimeUnit.SECONDS)) {
                    final IllegalStateException exception = new IllegalStateException("Could not give " + result + " to the updates queue.");
                    exception.printStackTrace();
                    throw exception;
                }
                return result;
            }
        };
        this.possibleValues = possibleValues;
    }

    public Monitorable<Progress<T>> command() {
        return command;
    }

    public void start() {
        future = Option.some(executorService.submit(command));
    }

    public String name() {
        return name;
    }

    public Option<MonitorableFuture<Progress<T>>> future() {
        return future;
    }
}