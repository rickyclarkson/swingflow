package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.Monitorable;
import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;
import fj.F;
import fj.data.Option;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class TypedStage<T> implements Stage {
    public final String name;
    private final F<Component, Monitorable<Progress<T>>> command;
    private Option<MonitorableFuture<Progress<T>>> future = Option.none();
    private final List<T> possibleValues;
    public final Option<Stage> next;
    private final List<Stage> prereqs = new ArrayList<Stage>();
    public final Rerun rerun;

    public static <T extends Enum<T>> TypedStage<T> stage(Rerun rerun, String name, final F<Component, Monitorable<Progress<T>>> command, List<T> possibleValues, T onException, final Option<Stage> next) {
        if (!possibleValues.contains(onException))
            throw new IllegalArgumentException("The onException parameter [" + onException + "] needs to be included in the list of possible values [" + possibleValues + ']');

        return new TypedStage<T>(rerun, name, command, possibleValues, onException, next);
    }

    private TypedStage(Rerun rerun, String name, final F<Component, Monitorable<Progress<T>>> commandFunction, List<T> possibleValues, final T onException, final Option<Stage> next) {
        this.rerun = rerun;
        this.name = name;
        this.command = new F<Component, Monitorable<Progress<T>>>() {
            @Override
            public Monitorable<Progress<T>> f(final Component component) {
                final Monitorable<Progress<T>> command = commandFunction.f(component);
                return new Monitorable<Progress<T>>(command.updates) {
                    @Override
                    public Progress<T> call(final MonitorableExecutorService executorService) {
                        Progress<T> result;
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

                        result._switch(new Progress.SwitchBlock<T>() {
                            @Override
                            public void _case(Progress.InProgress<T> x) {
                                throw new IllegalStateException("Should not be able to observe this state.");
                            }

                            @Override
                            public void _case(Progress.Complete<T> x) {
                                for (final Stage n: TypedStage.this.next)
                                    executorService.submit(new Monitorable<Void>() {
                                        @Override
                                        public Void call(MonitorableExecutorService executorService) {
                                            for (List<Stage> problemStages: n.start(executorService, component))
                                                throw new IllegalStateException("Failed to start " + n.name() + " because of " + problemStages);
                                            return null;
                                        }
                                    });
                            }

                            @Override
                            public void _case(Progress.Failed<T> x) {
                            }
                        });
                        return result;
                    }
                };
            }
        };
        this.possibleValues = possibleValues;
        this.next = next;
    }

    public List<T> possibleValues() {
        return possibleValues;
    }

    public Option<List<Stage>> start(MonitorableExecutorService executorService, Component component) {
        final List<Stage> problemStages = new ArrayList<Stage>();
        for (Stage stage: prereqs) {
            try {
                stage.tryToGetWithoutWaiting();
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

        future = Option.some(executorService.submit(command.f(component)));
        return Option.none();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean hasFuture() {
        return future.isSome();
    }

    @Override
    public void tryToGetWithoutWaiting() throws InterruptedException, ExecutionException, TimeoutException {
        future.some().get(0, TimeUnit.MILLISECONDS);
    }

    @Override
    public Option<Stage> next() {
        return next;
    }

    public Option<MonitorableFuture<Progress<T>>> future() {
        return future;
    }

    @Override
    public Iterator<Stage> iterator() {
        return new Iterator<Stage>() {
            Stage current = null;
            @Override
            public boolean hasNext() {
                return current == null || current.next().isSome();
            }

            @Override
            public Stage next() {
                if (current == null) {
                    current = TypedStage.this;
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

    public Option<List<Stage>> rerun(MonitorableExecutorService executorService, Component component) {
        future = Option.none();
        return start(executorService, component);
    }

    public void addPrerequisite(Stage prerequisite) {
        prereqs.add(prerequisite);
    }

    @Override
    public StageView view(MonitorableExecutorService executorService, Component component, int updateEveryXMilliseconds) {
        return StageView.stageView(executorService, component, this, updateEveryXMilliseconds);
    }
}