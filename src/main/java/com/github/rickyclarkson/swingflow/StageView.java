package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StageView {
    public final JProgressBar progressBar;
    public final DetailsButton detailsButton;
    public final JButton cancelButton;
    public final JButton retryButton;
    public final Timer timer;

    private StageView(Timer timer, JProgressBar progressBar, DetailsButton detailsButton, JButton cancelButton, JButton retryButton) {
        this.timer = timer;
        this.progressBar = progressBar;
        this.detailsButton = detailsButton;
        this.cancelButton = cancelButton;
        this.retryButton = retryButton;
    }

    public static StageView stageView(final UntypedStage stage, int updateEveryXMilliseconds) {
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
                    throw new IllegalArgumentException("The argument [" + brief + "] was provided but is not in the list of possible values for stage " + stage.name());
                bar.setString(brief);
                details.setDetails(detail);
            }
        });

        final JButton cancelButton = new JButton(new ImageIcon(StageView.class.getResource("stop.png")));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stage.future().some().cancel(true);
            }
        });

        final JButton retryButton = new JButton(new ImageIcon(StageView.class.getResource("rerun.png")));
        retryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw null;
            }
        });

        timer.start();
        return new StageView(timer, bar, details, cancelButton, retryButton);
    }
}
