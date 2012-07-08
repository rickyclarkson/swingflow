package com.github.rickyclarkson.swingflow;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StageView extends VerticalPanel {
    private final Timer timer;

    private StageView(Stage stage, JProgressBar bar, Timer timer) {
        super(new JLabel(stage.title()), bar);
        this.timer = timer;
    }

    public static StageView stageView(final Stage stage, final int originalEstimate, int updateEveryXMilliseconds) {
        final JProgressBar bar = new JProgressBar(0, originalEstimate);

        Timer timer = new Timer(updateEveryXMilliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bar.setValue(originalEstimate - stage.estimateSecondsLeft());
            }
        });

        timer.start();

        return new StageView(stage, bar, timer);
    }

    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }
}
