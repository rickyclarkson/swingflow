package com.github.rickyclarkson.swingflow;

import com.google.common.collect.Iterables;
import fj.data.Option;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.GridLayout;
import java.util.Arrays;

import static com.github.rickyclarkson.swingflow.Progress._Complete;
import static com.github.rickyclarkson.swingflow.Progress._InProgress;

public class SwingFlow {
    private final Stage<?> stage;

    public SwingFlow(Stage stage) {
        this.stage = stage;
    }

    @EDT
    public JComponent view() {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Must be called on the event dispatch thread.");

        final JPanel panel = new JPanel(new GridLayout(1, Iterables.size(stage), 20, 20));

        for (Stage<?> s: stage)
            panel.add(StageView.stageView(s));

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                realMain();
            }
        });
    }

    @EDT
    private static void realMain() {
        final SwingFlow flow = new SwingFlow(sleep(1, Option.some(sleep(2, Option.some(sleep(4, Option.some(sleep(8, Option.<Stage<SleepMessages>>none()))))))));

        final JFrame frame = new JFrame();
        frame.add(flow.view());
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        flow.start();
    }

    public void start() {
        stage.execute();
    }

    private enum SleepMessages {
        SLEEPING("Sleeping"),
        COMPLETE("Complete");

        private final String text;

        SleepMessages(String text) {
            this.text = text;
        }

        public String toString() {
            return text;
        }
    }

    private static Stage<SleepMessages> sleep(final int seconds, Option<Stage<SleepMessages>> next) {
        return new Stage<SleepMessages>("sleep(" + seconds + ")", Arrays.asList(SleepMessages.values()), next) {
            @Override
            public Progress<SleepMessages> call() {
                for (int a = 0; a < seconds; a++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    push(_InProgress(a + 1, seconds, SleepMessages.SLEEPING, "Still sleeping, a = " + a));
                }
                return _Complete(SleepMessages.COMPLETE, "All sleeping complete");
            }
        };
    }
}
