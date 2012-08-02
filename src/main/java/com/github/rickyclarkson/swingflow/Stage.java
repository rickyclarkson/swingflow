package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;
import com.github.rickyclarkson.monitorablefutures.MonitorableRunnable;
import fj.data.Option;

public final class Stage {
    private final MonitorableExecutorService executorService;
    private final String name;
    private final MonitorableRunnable<ProgressBriefAndDetailed> command;
    private Option<MonitorableFuture<Void, ProgressBriefAndDetailed>> future = Option.none();

    public Stage(MonitorableExecutorService executorService, String name, MonitorableRunnable<ProgressBriefAndDetailed> command) {
        this.executorService = executorService;
        this.name = name;
        this.command = command;
    }

    public MonitorableRunnable<ProgressBriefAndDetailed> command() {
        return command;
    }

    public void start() {
        future = Option.some(executorService.submit(command));
    }

    public String name() {
        return name;
    }

    public Option<MonitorableFuture<Void, ProgressBriefAndDetailed>> future() {
        return future;
    }
}