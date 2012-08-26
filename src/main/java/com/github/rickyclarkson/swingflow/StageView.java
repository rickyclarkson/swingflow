package com.github.rickyclarkson.swingflow;

import com.github.rickyclarkson.monitorablefutures.MonitorableFuture;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StageView extends VerticalPanel {
    private final Timer timer;

    private StageView(Stage stage, JProgressBar bar, DetailsButton details, Timer timer) {
        super(new JLabel(stage.name()), bar, details);
        this.timer = timer;
    }

    public static StageView stageView(final Stage stage, int updateEveryXMilliseconds) {
        final JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setStringPainted(true);
        bar.setString(stage.longestString + "wwww");
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

                        private void displayProgress(int numerator, int denominator, String brief, String detail) {
                            bar.setValue(numerator * 100 / denominator);
                            if (brief.length() > stage.longestString.length())
                                throw new IllegalStateException("The provided string [" + brief + "] is longer than the provided maximum length string [" + stage.longestString + "] - we reject this to prevent display corruption.");
                            bar.setString(brief);
                            details.setDetails(detail);
                        }

                    });
                }
            }
        });

        timer.start();

        return new StageView(stage, bar, details, timer);
    }

    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

}
