package com.github.rickyclarkson.swingflow;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StageView extends VerticalPanel {
    private final Timer timer;

    private StageView(Stage stage, JProgressBar bar, Timer timer) {
        super(new JLabel(stage.name()), bar);
        this.timer = timer;
    }

    public static StageView stageView(final Stage stage, int updateEveryXMilliseconds) {
        final JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setStringPainted(true);

        Timer timer = new Timer(updateEveryXMilliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bar.setValue(stage.progress().match(new StageProgress.MatchBlock<Integer>() {
                    @Override
                    public Integer _case(StageProgress.Success x) {
                        return 100;
                    }

                    @Override
                    public Integer _case(StageProgress.InProgress x) {
                        final int seconds = stage.originalEstimate().seconds;
                        return (seconds - x.remaining.seconds) * 100 / seconds;
                    }

                    @Override
                    public Integer _case(StageProgress.Failed x) {
                        return 0;
                    }
                }));

                bar.setString(stage.progress().match(new StageProgress.MatchBlock<String>() {
                    @Override
                    public String _case(StageProgress.Success x) {
                        return x.brief;
                    }

                    @Override
                    public String _case(StageProgress.InProgress x) {
                        return x.brief;
                    }

                    @Override
                    public String _case(StageProgress.Failed x) {
                        return x.brief;
                    }
                }));
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
