package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import com.github.rickyclarkson.monitorablefutures.Monitorable;
import fj.data.Option;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService.monitorable;
import static com.github.rickyclarkson.swingflow.Fraction._Fraction;
import static com.github.rickyclarkson.swingflow.ProgressBriefAndDetailed._ProgressBriefAndDetailed;

public class SwingFlow {
    private final Stage[] stages;

    public SwingFlow(Stage... stages) {
        this.stages = stages;
    }

    @EDT
    public JComponent view(int updateEveryXMilliseconds) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Must be called on the event dispatch thread.");

        final JPanel panel = new JPanel(new MigLayout());

        for (Stage stage: stages)
            panel.add(StageView.stageView(stage, updateEveryXMilliseconds));

        panel.setPreferredSize(panel.getPreferredSize());
        return new JScrollPane(panel);
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
        final MonitorableExecutorService executorService = monitorable(Executors.newSingleThreadExecutor());
        final SwingFlow flow = new SwingFlow(sleep(executorService, 1), sleep(executorService, 2), sleep(executorService, 4), sleep(executorService, 8));
        final JFrame frame = new JFrame();
        frame.add(flow.view(500));
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        flow.start();
    }

    private void start() {
        new StageWorker(stages, 0).execute();
    }

    private static Stage sleep(final MonitorableExecutorService executorService, final int seconds) {
        final Monitorable<ProgressBriefAndDetailed> command = new Monitorable<ProgressBriefAndDetailed>() {
            @Override
            public ProgressBriefAndDetailed call() {
                for (int a = 0; a < seconds; a++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    updates.offer(_ProgressBriefAndDetailed(_Fraction(a + 1, seconds), "Slept for " + (a + 1) + " seconds.", Option.some("Still sleeping, a = " + a)));
                }
                return _ProgressBriefAndDetailed(_Fraction(seconds, seconds), "Finished", Option.some("fart"));
            }
        };

        return new Stage(executorService, "sleep(" + seconds + ")", command);
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
