package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.Monitorable;
import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;
import fj.data.Option;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class Stage implements UntypedStage, Iterable<UntypedStage> {
    private final MonitorableExecutorService executorService;
    private final String name;
    private final Monitorable<Progress> command;
    private Option<MonitorableFuture<Progress>> future = Option.none();
    private final List<String> possibleValues;
    private final Option<UntypedStage> next;

    public Stage(MonitorableExecutorService executorService, String name, final Monitorable<Progress> command, List<String> possibleValues, final Option<UntypedStage> next) {
        this.executorService = executorService;
        this.name = name;
        this.command = new Monitorable<Progress>(command.updates) {
            @Override
            public Progress call() throws Exception {
                final Progress result = command.call();
                if (!updates.offer(result, 10, TimeUnit.SECONDS)) {
                    final IllegalStateException exception = new IllegalStateException("Could not give " + result + " to the updates queue.");
                    exception.printStackTrace();
                    throw exception;
                }

                result._switch(new Progress.SwitchBlock() {
                    @Override
                    public void _case(Progress.InProgress x) {
                        throw new IllegalStateException("Should not be able to observe this state.");
                    }

                    @Override
                    public void _case(Progress.Complete x) {
                        for (UntypedStage n: next)
                            n.start();
                    }

                    @Override
                    public void _case(Progress.Failed x) {
                    }
                });
                return result;
            }
        };
        this.possibleValues = possibleValues;
        this.next = next;
    }

    @Override
    public List<String> possibleValues() {
        return possibleValues;
    }

    @Override
    public Option<UntypedStage> next() {
        return next;
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

    @Override
    public Iterator<UntypedStage> iterator() {
        return new Iterator<UntypedStage>() {
            UntypedStage current = null;
            @Override
            public boolean hasNext() {
                return current == null || current.next().isSome();
            }

            @Override
            public UntypedStage next() {
                if (current == null) {
                    current = Stage.this;
                    return current;
                }

                return current = current.next().some();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}