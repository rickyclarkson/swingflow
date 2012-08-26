package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.Monitorable;
import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;
import fj.data.Option;

import java.util.concurrent.TimeUnit;

public final class Stage {
    private final MonitorableExecutorService executorService;
    private final String name;
    final String longestString;
    private final Monitorable<Progress> command;
    private Option<MonitorableFuture<Progress>> future = Option.none();

    public Stage(MonitorableExecutorService executorService, String name, final Monitorable<Progress> command, String longestString) {
        this.executorService = executorService;
        this.name = name;
        this.longestString = longestString;
        this.command = new Monitorable<Progress>(command.updates) {
            @Override
            public Progress call() throws Exception {
                final Progress result = command.call();
                if (!updates.offer(result, 10, TimeUnit.SECONDS)) {
                    final IllegalStateException exception = new IllegalStateException("Could not give " + result + " to the updates queue.");
                    exception.printStackTrace();
                    throw exception;
                }
                return result;
            }
        };
    }

    public Monitorable<Progress> command() {
        return command;
    }

    public void start() {
        future = Option.some(executorService.submit(command));
    }

    public String name() {
        return name;
    }

    public Option<MonitorableFuture<Progress>> future() {
        return future;
    }
}