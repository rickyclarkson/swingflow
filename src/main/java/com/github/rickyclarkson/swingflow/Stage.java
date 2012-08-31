package com.github.rickyclarkson.swingflow;

import fj.data.Option;

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class Stage<T> extends SwingWorker<Progress<T>, Progress<T>> implements Iterable<Stage<T>> {
    private final String name;
    public final List<T> possibleValues;
    private final Option<Stage<T>> next;

    public final List<ProgressListener<T>> listeners = new ArrayList<ProgressListener<T>>();

    public Stage(String name, List<T> possibleValues, Option<Stage<T>> next) {
        this.name = name;
        this.possibleValues = possibleValues;
        this.next = next;
    }

    public String name() {
        return name;
    }

    protected final void process(List<Progress<T>> chunks) {
        for (ProgressListener<T> listener: listeners)
            listener.process(chunks);
    }

    void addProgressListener(ProgressListener<T> listener) {
        listeners.add(listener);
    }

    public void push(Progress<T> update) {
        publish(update); // gives a warning, find a better way and I'll congratulate you.
    }

    protected final Progress<T> doInBackground() {
        Progress<T> result = call();
        result._switch(new Progress.SwitchBlock<T>() {
            @Override
            public void _case(Progress.InProgress<T> x) {
                throw new IllegalStateException(x.toString());
            }

            @Override
            public void _case(Progress.Complete<T> x) {
                for (Stage n: next)
                    n.execute();
            }

            @Override
            public void _case(Progress.Failed<T> x) {
            }
        });
        return result;
    }

    protected abstract Progress<T> call();

    @Override
    public Iterator<Stage<T>> iterator() {
        return new Iterator<Stage<T>>() {
            Stage<T> current = null;
            @Override
            public boolean hasNext() {
                return current == null || current.next.isSome();
            }

            @Override
            public Stage<T> next() {
                if (current == null)
                    return current = Stage.this;

                if (current.next.isSome())
                    return current = current.next.some();

                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}