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
                            displayProgress(bar, x.numerator, x.denominator, details, x.brief, x.detail);
                        }

                        @Override
                        public void _case(Progress.Complete x) {
                            displayProgress(bar, 100, 100, details, x.brief, x.detail);
                        }

                        @Override
                        public void _case(Progress.Failed x) {
                            displayProgress(bar, x.numerator, x.denominator, details, x.brief, x.detail);
                        }
                    });
                }
            }
        });

        timer.start();

        return new StageView(stage, bar, details, timer);
    }

    private static void displayProgress(JProgressBar bar, int numerator, int denominator, DetailsButton detailButton, String brief, String detail) {
        bar.setValue(numerator * 100 / denominator);
        bar.setString(brief);
        detailButton.setDetails(detail);
    }

    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

}
