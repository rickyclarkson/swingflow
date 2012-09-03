package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.Monitorable;
import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import fj.F;
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
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import static com.github.rickyclarkson.swingflow.Progress._Complete;
import static com.github.rickyclarkson.swingflow.Progress._InProgress;
import static com.github.rickyclarkson.swingflow.Stage.stage;

public class SwingFlow {
    private final Stage stage;

    public SwingFlow(Stage stage) {
        this.stage = stage;
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

        for (Stage s: stage) {
            final JPanel titlePanel = new JPanel();
            titlePanel.setBorder(BorderFactory.createTitledBorder(s.name()));
            final StageView view = StageView.stageView(s, updateEveryXMilliseconds);
            timersToCancel.add(view.timer);
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
        final SwingFlow flow = new SwingFlow(sleep(executorService, 1, Option.some(sleep(executorService, 2, Option.some(sleep(executorService, 4, Option.some(sleep(executorService, 8, Option.<Stage>none()))))))));

        final JFrame frame = new JFrame();
        frame.add(flow.view(500));
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        flow.start();
    }

    public void start() {
        stage.start();
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

    private static Stage sleep(final MonitorableExecutorService executorService, final int seconds, Option<Stage> next) {
        final Monitorable<Progress> command = new Monitorable<Progress>() {
            @Override
            public Progress call() throws Exception {
                for (int a = 0; a < seconds; a++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    updates.offer(_InProgress(a + 1, seconds, SleepMessages.SLEEPING.toString(), "Still sleeping, a = " + a));
                }
                return _Complete(SleepMessages.COMPLETE.toString(), "All sleeping complete");
            }
        };

        return Stage.stage(executorService, "sleep(" + seconds + ")", command, Arrays.asList(SleepMessages.values()), next);

    }
}
