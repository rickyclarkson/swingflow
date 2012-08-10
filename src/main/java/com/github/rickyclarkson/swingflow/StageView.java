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
                for (MonitorableFuture<ProgressBriefAndDetailed> future: stage.future()) {
                    final ProgressBriefAndDetailed result = future.updates().poll();
                    if (result == null)
                        return;

                    final Fraction fraction = result.complete;
                    bar.setValue(fraction.numerator * 100 / fraction.denominator);
                    bar.setString(result.brief);
                    details.setDetails(result.detailed);
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
