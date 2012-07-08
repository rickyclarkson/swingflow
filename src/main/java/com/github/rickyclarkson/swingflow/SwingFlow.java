package com.github.rickyclarkson.swingflow;

import fj.F;
import fj.P2;
import fj.data.Option;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.HashMap;
import java.util.Map;

import static fj.P.p;

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
        int secondsLeft = 0;
        Map<Stage, Integer> proportions = new HashMap<Stage, Integer>();
        for (Stage stage: stages) {
            final int estimate = stage.estimateSecondsLeft();
            proportions.put(stage, estimate);
            secondsLeft += estimate;
        }

        for (Stage stage: stages) {
            final Integer originalEstimate = proportions.get(stage);
            panel.add(StageView.stageView(stage, originalEstimate, updateEveryXMilliseconds), "width " + originalEstimate * 100 / secondsLeft + "%");
        }

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
        SwingFlow flow = new SwingFlow(sleep(1), sleep(2), sleep(4));
        flow.view(500);
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
            private Option<Long> startTime = Option.none();

            @Override
            public int estimateSecondsLeft() {
                return startTime.option(seconds, new F<Long, Integer>() {
                    @Override
                    public Integer f(Long startTime) {
                        return seconds - (int)((System.currentTimeMillis() - startTime) / 1000);
                    }
                });
            }

            @Override
            public P2<String, String> briefAndDetailedStatuses() {
                return startTime.option(p("Sleep for " + seconds + " seconds.", ""), new F<Long, P2<String, String>>() {
                    @Override
                    public P2<String, String> f(Long startTime) {
                        return p(seconds - (int)((System.currentTimeMillis() - startTime) / 1000) + " seconds of sleep remaining.", "");
                    }
                });
            }

            @Override
            public void start() {
                startTime = Option.some(System.currentTimeMillis());
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException ignored) {
                }
                System.out.println("Slept for " + seconds + " seconds.");
            }

            @Override
            public String title() {
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
