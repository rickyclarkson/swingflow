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
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import static com.github.rickyclarkson.swingflow.Progress._Complete;
import static com.github.rickyclarkson.swingflow.Progress._InProgress;

public class SwingFlow {
    private final Stage stage;
    private final MonitorableExecutorService executorService;

    public SwingFlow(MonitorableExecutorService executorService, Stage stage) {
        this.executorService = executorService;
        this.stage = stage;
    }

    @EDT
    public JComponent view(int updateEveryXMilliseconds) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Must be called on the event dispatch thread.");

        final List<Timer> timersToCancel = new ArrayList<Timer>();
        final JPanel panel = new JPanel(new MigLayout(new LC().wrapAfter(1).fill(), new AC().fill(), new AC())) {
            @Override
            public void removeNotify() {
                for (Timer timer: timersToCancel)
                    timer.stop();
                super.removeNotify();
            }
        };

        for (Stage s: stage) {
            final JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.setBorder(BorderFactory.createTitledBorder(s.name));
            final StageView view = StageView.stageView(executorService, s, updateEveryXMilliseconds);
            timersToCancel.add(view.timer);
            titlePanel.add(view.progressBar, BorderLayout.CENTER);
            final JToolBar invisibleBar = new JToolBar();
            invisibleBar.setFloatable(false);
            invisibleBar.add(view.detailsButton);
            invisibleBar.add(view.cancelButton);
            invisibleBar.add(view.retryButton);
            titlePanel.add(invisibleBar, BorderLayout.EAST);
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
        final Stage sleep8 = sleep("Short", "Really long silly name", 8, Option.<Stage>none());
        final Stage sleep4 = sleep("Very very very very long", "Short name", 4, Option.some(sleep8));
        final Stage sleep2 = sleep("", "Tiny", 2, Option.some(sleep4));
        final Stage sleep1 = sleep("sdfkjsdflkjsdfljsdf", "Kind of medium name", 1, Option.some(sleep2));

        sleep2.addPrerequisite(sleep1);
        sleep4.addPrerequisite(sleep2);
        sleep8.addPrerequisite(sleep4);

        final SwingFlow flow = new SwingFlow(MonitorableExecutorService.monitorable(Executors.newSingleThreadExecutor()), sleep1);

        final JFrame frame = new JFrame();
        frame.add(flow.view(500));
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        flow.start();
    }

    public void start() {
        for (List<Stage> problemStages: stage.start(executorService))
            throw new IllegalStateException("Stage " + stage.name + " could not start because of " + problemStages);
    }

    private enum SleepMessages {
        SLEEPING("Sleeping"),
        COMPLETE("Complete"),
        INTERRUPTED("Interrupted");

        private final String text;

        SleepMessages(String text) {
            this.text = text;
        }

        public String toString() {
            return text;
        }
    }

    private static Stage sleep(final String extra, final String name, final int seconds, Option<Stage> next) {
        final Monitorable<Progress> command = new Monitorable<Progress>() {
            @Override
            public Progress call(MonitorableExecutorService executorService) {
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

        return Stage.stage(name, command, Arrays.asList(extra, SleepMessages.COMPLETE.toString(), SleepMessages.INTERRUPTED.toString(), SleepMessages.SLEEPING.toString()), SleepMessages.INTERRUPTED.toString(), next);

    }
}
