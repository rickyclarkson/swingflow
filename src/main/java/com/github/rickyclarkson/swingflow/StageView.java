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

    public static <T> StageView stageView(final Stage<T> stage, int updateEveryXMilliseconds) {
        final JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setStringPainted(true);
        String longest = "";
        for (T t: stage.possibleValues)
            longest = longest.length() > t.toString().length() ? longest : t.toString();
        bar.setString(longest + "wwww");
        bar.setPreferredSize(bar.getPreferredSize());
        bar.setString("");

        final DetailsButton details = new DetailsButton();

        final Timer timer = new Timer(updateEveryXMilliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (MonitorableFuture<Progress<T>> future: stage.future()) {
                    final Progress<T> result = future.updates().poll();
                    if (result == null)
                        return;

                    result._switch(new Progress.SwitchBlock<T>() {
                        @Override
                        public void _case(Progress.InProgress<T> x) {
                            displayProgress(x.numerator, x.denominator, x.brief, x.detail);
                        }

                        @Override
                        public void _case(Progress.Complete<T> x) {
                            displayProgress(100, 100, x.brief, x.detail);
                        }

                        @Override
                        public void _case(Progress.Failed<T> x) {
                            displayProgress(x.numerator, x.denominator, x.brief, x.detail);
                        }

                        private void displayProgress(int numerator, int denominator, T brief, String detail) {
                            bar.setValue(numerator * 100 / denominator);
                            if (!stage.possibleValues.contains(brief))
                                throw new IllegalArgumentException("The argument [" + brief + "] was provided but is not in the list of possible values for stage " + stage.name());
                            bar.setString(brief.toString());
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
