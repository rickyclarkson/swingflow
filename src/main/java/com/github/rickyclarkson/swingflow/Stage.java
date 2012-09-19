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
    public final String name;
    private final Monitorable<Progress> command;
    private Option<MonitorableFuture<Progress>> future = Option.none();
    private final List<String> possibleValues;
    public final Option<Stage> next;
    private final List<Stage> prereqs = new ArrayList<Stage>();
    public final Rerun rerun;

    public static <T extends Enum<T>> Stage stage(Rerun rerun, String name, final Monitorable<Progress> command, List<T> possibleValues, T onException, final Option<Stage> next) {
        if (!possibleValues.contains(onException))
            throw new IllegalArgumentException("The onException parameter [" + onException + "] needs to be included in the list of possible values [" + possibleValues + ']');

        return new Stage(rerun, name, command, mapToString(possibleValues), onException.toString(), next);
    }

    private static <T> List<String> mapToString(List<T> list) {
        final List<String> results = new ArrayList<String>();
        for (T t: list)
            results.add(t.toString());
        return results;
    }

    private Stage(Rerun rerun, String name, final Monitorable<Progress> command, List<String> possibleValues, final String onException, final Option<Stage> next) {
        this.rerun = rerun;
        this.name = name;
        this.command = new Monitorable<Progress>(command.updates) {
            @Override
            public Progress call(final MonitorableExecutorService executorService) {
                Progress result;
                try {
                    result = command.call(executorService);
                } catch (Exception e) {
                    e.printStackTrace();
                    result = Progress._Failed(0, 100, onException, e.getMessage());
                }
                try {
                    if (!updates.offer(result, 10, TimeUnit.SECONDS)) {
                        final IllegalStateException exception = new IllegalStateException("Could not give " + result + " to the updates queue.");
                        exception.printStackTrace();
                        throw exception;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                result._switch(new Progress.SwitchBlock() {
                    @Override
                    public void _case(Progress.InProgress x) {
                        throw new IllegalStateException("Should not be able to observe this state.");
                    }

                    @Override
                    public void _case(Progress.Complete x) {
                        for (final Stage n: Stage.this.next)
                            executorService.submit(new Monitorable<Void>() {
                                @Override
                                public Void call(MonitorableExecutorService executorService) {
                                    for (List<Stage> problemStages: n.start(executorService))
                                        throw new IllegalStateException("Failed to start " + n.name + " because of " + problemStages);
                                    return null;
                                }
                            });
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

    public Option<List<Stage>> start(MonitorableExecutorService executorService) {
        final List<Stage> problemStages = new ArrayList<Stage>();
        for (Stage stage: prereqs) {
            if (stage.future.isNone())
                problemStages.add(stage);

            try {
                stage.future.some().get(0, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                e.printStackTrace();
                problemStages.add(stage); // Looks like a prerequisite failed.
            } catch (TimeoutException e) {
                e.printStackTrace();
                problemStages.add(stage); // Looks like a prerequisite is still running.
            }
        }

        if (!problemStages.isEmpty())
            return Option.some(problemStages);

        future = Option.some(executorService.submit(command));
        return Option.none();
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

    public Option<List<Stage>> rerun(MonitorableExecutorService executorService) {
        future = Option.none();
        return start(executorService);
    }

    public void addPrerequisite(Stage prerequisite) {
        prereqs.add(prerequisite);
    }
}