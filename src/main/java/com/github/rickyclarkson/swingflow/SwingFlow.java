package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.Monitorable;
import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import fj.data.Option;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static com.github.rickyclarkson.swingflow.Progress._Complete;
import static com.github.rickyclarkson.swingflow.Progress._InProgress;

public class SwingFlow {
    private final Stage[] stages;

    public SwingFlow(Stage... stages) {
        this.stages = stages;
    }

    @EDT
    public JComponent view(int updateEveryXMilliseconds) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Must be called on the event dispatch thread.");

        final List<Timer> timersToCancel = new ArrayList<Timer>();
        final JPanel panel = new JPanel(new MigLayout(new LC().wrapAfter(1), new AC(), new AC())) {
            @Override
            public void removeNotify() {
                for (Timer timer: timersToCancel)
                    timer.stop();
                super.removeNotify();
            }
        };

        for (Stage<?> stage: stages) {
            final JPanel titlePanel = new JPanel();
            titlePanel.setBorder(BorderFactory.createTitledBorder(stage.name()));
            final StageView view = StageView.stageView(stage, updateEveryXMilliseconds);
            titlePanel.add(view.progressBar);
            final JToolBar invisibleBar = new JToolBar();
            invisibleBar.setFloatable(false);
            invisibleBar.add(view.detailsButton);
            invisibleBar.add(view.cancelButton);
            invisibleBar.add(view.retryButton);
            titlePanel.add(invisibleBar);
            panel.add(titlePanel);
        }

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    realMain();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (UnsupportedLookAndFeelException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @EDT
    private static void realMain() throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final MonitorableExecutorService executorService = MonitorableExecutorService.monitorable(Executors.newSingleThreadExecutor());
        final SwingFlow flow = new SwingFlow(sleep(executorService, 1), sleep(executorService, 2), sleep(executorService, 4), sleep(executorService, 8));

        final JFrame frame = new JFrame();
        frame.add(flow.view(500));
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        flow.start();
    }

    public void start() {
        new StageWorker(stages, 0).execute();
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

    private static Stage<SleepMessages> sleep(final MonitorableExecutorService executorService, final int seconds) {
        final Monitorable<Progress<SleepMessages>> command = new Monitorable<Progress<SleepMessages>>() {
            @Override
            public Progress<SleepMessages> call() throws Exception {
                for (int a = 0; a < seconds; a++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    updates.offer(_InProgress(a + 1, seconds, SleepMessages.SLEEPING, "Still sleeping, a = " + a));
                }
                return _Complete(SleepMessages.COMPLETE, "All sleeping complete");
            }
        };

        return new Stage<SleepMessages>(executorService, "sleep(" + seconds + ")", command, Arrays.asList(SleepMessages.values()));

    }

    private static class StageWorker extends SwingWorker<Void, Void> {
        private final Stage[] stages;
        private final int index;

        public StageWorker(Stage[] stages, int index) {
            this.stages = stages;
            this.index = index;
        }
        @Override
        protected Void doInBackground() throws Exception {
            stages[index].start();
            if (stages.length > index + 1)
                new StageWorker(stages, index + 1).execute();

            return null;
        }

        public void done() {
            try {
                get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
