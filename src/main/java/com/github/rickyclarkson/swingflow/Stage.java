package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.Monitorable;
import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;
import fj.data.Option;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Stage implements Iterable<Stage> {
    private final MonitorableExecutorService executorService;
    private final String name;
    private final Monitorable<Progress> command;
    private Option<MonitorableFuture<Progress>> future = Option.none();
    private final List<String> possibleValues;
    public Option<Stage> next;
    private final List<Stage> prereqs = new ArrayList<Stage>();

    public static <T> Stage stage(MonitorableExecutorService executorService, String name, final Monitorable<Progress> command, List<T> possibleValues, T onException, final Option<Stage> next) {
        if (!possibleValues.contains(onException))
            throw new IllegalArgumentException("The onException parameter [" + onException + "] needs to be included in the list of possible values [" + possibleValues + ']');

        return new Stage(executorService, name, command, mapToString(possibleValues), onException.toString(), next);
    }

    private static <T> List<String> mapToString(List<T> list) {
        final List<String> results = new ArrayList<String>();
        for (T t: list)
            results.add(t.toString());
        return results;
    }

    private Stage(MonitorableExecutorService executorService, String name, final Monitorable<Progress> command, List<String> possibleValues, final String onException, final Option<Stage> next) {
        this.executorService = executorService;
        this.name = name;
        this.command = new Monitorable<Progress>(command.updates) {
            @Override
            public Progress call() throws Exception {
                Progress result;
                try {
                    result = command.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    result = Progress._Failed(0, 100, onException, e.getMessage());
                }
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
                        for (Stage n: Stage.this.next)
                            for (List<Stage> problemStages: n.start())
                                throw new IllegalStateException("Failed to start " + n.name() + " because of " + problemStages);
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

    public List<String> possibleValues() {
        return possibleValues;
    }

    public Option<List<Stage>> start() {
        final List<Stage> problemStages = new ArrayList<Stage>();
        for (Stage stage: prereqs) {
            if (stage.future.isNone())
                problemStages.add(stage);

            try {
                stage.future.some().get(0, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e); // An interrupt on a 0-wait call would seem to be a bug.
            } catch (ExecutionException e) {
                problemStages.add(stage); // Looks like a prerequisite failed.
            } catch (TimeoutException e) {
                problemStages.add(stage); // Looks like a prerequisite is still running.
            }
        }

        if (!problemStages.isEmpty())
            return Option.some(problemStages);

        future = Option.some(executorService.submit(command));
        return Option.none();
    }

    public String name() {
        return name;
    }

    public Option<MonitorableFuture<Progress>> future() {
        return future;
    }

    @Override
    public Iterator<Stage> iterator() {
        return new Iterator<Stage>() {
            Stage current = null;
            @Override
            public boolean hasNext() {
                return current == null || current.next.isSome();
            }

            @Override
            public Stage next() {
                if (current == null) {
                    current = Stage.this;
                    return current;
                }

                return current = current.next.some();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Option<List<Stage>> rerun() {
        future = Option.none();
        return start();
    }
}