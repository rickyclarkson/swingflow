package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;
import fj.data.Option;

import java.util.List;

public interface UntypedStage extends Iterable<UntypedStage> {
    List<String> possibleValues();

    Option<UntypedStage> next();
    void start();

    String name();

    Option<MonitorableFuture<Progress>> future();
}
