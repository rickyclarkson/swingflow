package com.github.rickyclarkson.swingflow;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class StageView {
    public final JProgressBar progressBar;
    public final DetailsButton detailsButton;
    public final JButton cancelButton;
    public final JButton retryButton;

    private StageView(JProgressBar progressBar, DetailsButton detailsButton, JButton cancelButton, JButton retryButton) {
        this.progressBar = progressBar;
        this.detailsButton = detailsButton;
        this.cancelButton = cancelButton;
        this.retryButton = retryButton;
    }

    public static <T> StageView stageView(final Stage<T> stage) {
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

        stage.addProgressListener(new ProgressListener<T>() {
            @Override
            public void process(List<Progress<T>> chunks) {
                chunks.get(chunks.size() - 1)._switch(new Progress.SwitchBlock<T>() {
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
                });
            }

            private void displayProgress(int numerator, int denominator, T brief, String detail) {
                bar.setValue(numerator * 100 / denominator);
                if (!stage.possibleValues.contains(brief))
                    throw new IllegalArgumentException("The argument [" + brief + "] was provided but is not in the list of possible values for stage " + stage.name());
                bar.setString(brief.toString());
                details.setDetails(detail);
            }
        });

        final JButton cancelButton = new JButton(new ImageIcon(StageView.class.getResource("stop.png")));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stage.cancel(true);
            }
        });

        final JButton retryButton = new JButton(new ImageIcon(StageView.class.getResource("rerun.png")));
        retryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw null;
            }
        });
        return new StageView(bar, details, cancelButton, retryButton);
    }
}
