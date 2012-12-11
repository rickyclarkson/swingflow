package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import fj.data.Option;

import java.awt.Component;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface Stage extends Iterable<Stage> {
    Option<List<Stage>> start(MonitorableExecutorService executorService, Component component);
    String name();
    boolean hasFuture();
    void tryToGetWithoutWaiting() throws InterruptedException, ExecutionException, TimeoutException;
    Option<Stage> next();
    void addPrerequisite(Stage preRequisite);
    StageView view(MonitorableExecutorService executorService, Component component, int updateEveryXMilliseconds);
}
