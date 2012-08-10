package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.Monitorable;
import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;
import fj.data.Option;

import java.util.concurrent.TimeUnit;

public final class Stage {
    private final MonitorableExecutorService executorService;
    private final String name;
    private final Monitorable<ProgressBriefAndDetailed> command;
    private Option<MonitorableFuture<ProgressBriefAndDetailed>> future = Option.none();

    public Stage(MonitorableExecutorService executorService, String name, final Monitorable<ProgressBriefAndDetailed> command) {
        this.executorService = executorService;
        this.name = name;
        this.command = new Monitorable<ProgressBriefAndDetailed>(command.updates) {
            @Override
            public ProgressBriefAndDetailed call() throws Exception {
                final ProgressBriefAndDetailed result = command.call();
                if (!updates.offer(result, 10, TimeUnit.SECONDS)) {
                    final IllegalStateException exception = new IllegalStateException("Could not give " + result + " to the updates queue.");
                    exception.printStackTrace();
                    throw exception;
                }
                return result;
            }
        };
    }

    public Monitorable<ProgressBriefAndDetailed> command() {
        return command;
    }

    public void start() {
        future = Option.some(executorService.submit(command));
    }

    public String name() {
        return name;
    }

    public Option<MonitorableFuture<ProgressBriefAndDetailed>> future() {
        return future;
    }
}