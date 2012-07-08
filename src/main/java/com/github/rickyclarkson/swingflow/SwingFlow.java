package com.github.rickyclarkson.swingflow;

import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import static com.github.rickyclarkson.swingflow.Fraction._Fraction;
import static com.github.rickyclarkson.swingflow.Option._None;
import static com.github.rickyclarkson.swingflow.StageProgress._InProgress;
import static com.github.rickyclarkson.swingflow.StageProgress._Success;

public class SwingFlow {
    private final Stage[] stages;

    public SwingFlow(Stage... stages) {
        this.stages = stages;
    }

    @EDT
    public JComponent view(int updateEveryXMilliseconds) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Must be called on the event dispatch thread.");

        JPanel panel = new JPanel(new MigLayout());

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
        SwingFlow flow = new SwingFlow(sleep(1), sleep(2), sleep(4), sleep(8));
        JFrame frame = new JFrame();
        frame.add(flow.view(500));
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        flow.start();
    }

    private void start() {
        new StageWorker(stages, 0).execute();
    }

    private static Stage sleep(final int seconds) {
        return new Stage() {
            private Option<Long> startTimeMillis = _None();

            @Override
            public StageProgress progress() {
                return startTimeMillis.match(new Option.MatchBlock<Long, StageProgress>() {
                    @Override
                    public StageProgress _case(Option.Some<Long> startTimeMillis) {
                        final int slept = (int) ((System.currentTimeMillis() - startTimeMillis.t) / 1000);
                        final int remaining = seconds - slept;
                        if (remaining <= 0)
                            return _Success("Slept for " + seconds + " seconds", Option.<String>_None());

                        return _InProgress(_Fraction(slept, seconds), "Slept for " + slept + "; " + remaining + " to go", Option.<String>_None());
                    }

                    @Override
                    public StageProgress _case(Option.None<Long> x) {
                        return _InProgress(_Fraction(0, seconds), "Sleep for " + seconds, Option._Some("In the middle of a deep, deep " + seconds + " second sleep."));
                    }
                });
            }

            @Override
            public void start() {
                startTimeMillis = Option._Some(System.currentTimeMillis());
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException ignored) {
                }
                System.out.println("Slept for " + seconds + " seconds.");
            }

            @Override
            public String name() {
                return "Sleep for " + seconds + " seconds";
            }
        };
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
    }
}
