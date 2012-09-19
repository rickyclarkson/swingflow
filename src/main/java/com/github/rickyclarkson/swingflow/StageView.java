package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.MonitorableExecutorService;
import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;
import fj.data.Option;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class StageView {
    public final JProgressBar progressBar;
    public final DetailsButton detailsButton;
    public final JButton cancelButton;
    public final Option<JButton> retryButton;
    public final Timer timer;

    private StageView(Timer timer, JProgressBar progressBar, DetailsButton detailsButton, JButton cancelButton, Option<JButton> retryButton) {
        this.timer = timer;
        this.progressBar = progressBar;
        this.detailsButton = detailsButton;
        this.cancelButton = cancelButton;
        this.retryButton = retryButton;
    }

    public static <T> StageView stageView(final MonitorableExecutorService executorService, final TypedStage<T> stage, int updateEveryXMilliseconds) {
        final JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setStringPainted(true);
        String longest = "";
        for (String s: stage.possibleValues())
            longest = longest.length() > s.length() ? longest : s;
        bar.setString(longest + "wwww");
        bar.setPreferredSize(bar.getPreferredSize());
        bar.setString("");

        final DetailsButton details = new DetailsButton();

        final Timer timer = new Timer(updateEveryXMilliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (MonitorableFuture<Progress> future: stage.future()) {
                    final Progress result = future.updates().poll();
                    if (result == null)
                        return;

                    result._switch(new Progress.SwitchBlock() {
                        @Override
                        public void _case(Progress.InProgress x) {
                            displayProgress(x.numerator, x.denominator, x.brief, x.detail);
                        }

                        @Override
                        public void _case(Progress.Complete x) {
                            displayProgress(100, 100, x.brief, x.detail);
                        }

                        @Override
                        public void _case(Progress.Failed x) {
                            displayProgress(x.numerator, x.denominator, x.brief, x.detail);
                        }
                    });
                }
            }

            private void displayProgress(int numerator, int denominator, String brief, String detail) {
                bar.setValue(numerator * 100 / denominator);
                if (!stage.possibleValues().contains(brief))
                    throw new IllegalArgumentException("The argument [" + brief + "] was provided but is not in the list of possible values for stage " + stage.name);
                bar.setString(brief);
                details.setDetails(detail);
            }
        });

        final JButton cancelButton = new JButton(new ImageIcon(StageView.class.getResource("stop.png")));
        cancelButton.setToolTipText("Stop");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Option<MonitorableFuture<Progress>> futureOption = stage.future();
                if (futureOption.isNone() || futureOption.some().isDone())
                    JOptionPane.showMessageDialog(cancelButton, "Cannot cancel a task that is not currently running.");
                else
                    futureOption.some().cancel(true);
            }
        });

        Option<JButton> retryButtonOption;
        if (stage.rerun == Rerun.ALLOWED) {
            final JButton retryButton = new JButton(new ImageIcon(StageView.class.getResource("rerun.png")));
            retryButton.setToolTipText("Rerun");
            retryButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (stage.future().isNone() || !stage.future().some().isDone()) {
                        JOptionPane.showMessageDialog(retryButton, "The stage " + stage.name + " cannot be rerun until it has been executed at least once.");
                        return;
                    }

                    final Option<List<Stage>> problemStages = stage.rerun(executorService);
                    for (List<Stage> stages: problemStages) {
                        final StringBuilder builder = new StringBuilder();

                        for (Stage stage: stages)
                            builder.append(stage.name()).append(", ");

                        if (builder.length() != 0)
                            builder.setLength(builder.length() - ", ".length());

                        JOptionPane.showMessageDialog(retryButton, "Cannot run " + stage.name + " without a successful run of " + builder);
                    }
                }
            });
            retryButtonOption = Option.some(retryButton);
        } else
            retryButtonOption = Option.none();

        timer.start();
        return new StageView(timer, bar, details, cancelButton, retryButtonOption);
    }
}
