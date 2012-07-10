package com.github.rickyclarkson.swingflow;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.github.rickyclarkson.swingflow.Fraction._Fraction;
import static com.github.rickyclarkson.swingflow.ProgressBriefAndDetailed._ProgressBriefAndDetailed;

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

        Timer timer = new Timer(updateEveryXMilliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProgressBriefAndDetailed result = stage.progress().match(new StageProgress.MatchBlock<ProgressBriefAndDetailed>() {
                    @Override
                    public ProgressBriefAndDetailed _case(StageProgress.Success x) {
                        return _ProgressBriefAndDetailed(_Fraction(1, 1), x.brief, x.detailed);
                    }

                    @Override
                    public ProgressBriefAndDetailed _case(StageProgress.InProgress x) {
                        return _ProgressBriefAndDetailed(x.complete, x.brief, x.detailed);
                    }

                    @Override
                    public ProgressBriefAndDetailed _case(StageProgress.Failed x) {
                        return _ProgressBriefAndDetailed(_Fraction(0, 1), x.brief, x.detailed);
                    }
                });

                bar.setValue(result.complete.numerator * 100 / result.complete.denominator);
                bar.setString(result.brief);
                details.setDetails(result.detailed);
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
