package com.github.rickyclarkson.swingflow;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.github.rickyclarkson.swingflow.Option._None;
import static com.github.rickyclarkson.swingflow.Option._Some;
import static com.github.rickyclarkson.swingflow.Windows.close;

public class DetailsButton extends JButton {
    private Option<JTextArea> popup = _None();

    public DetailsButton() {
        super("Details >>");
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popup._switch(new Option.SwitchBlock<JTextArea>() {
                    @Override
                    public void _case(Option.Some<JTextArea> someTextArea) {
                        JWindow window = new JWindow();

                        final Border border = BorderFactory.createLineBorder(Color.black);
                        someTextArea.t.setBorder(border);
                        final JPanel northPanel = gradientPanel(new BorderLayout());
                        northPanel.add(new JLabel("Details"), BorderLayout.WEST);
                        northPanel.add(closeButton(close(window)), BorderLayout.EAST);
                        window.add(northPanel, BorderLayout.NORTH);
                        window.add(someTextArea.t);
                        window.setMinimumSize(new Dimension(200, 100));
                        window.pack();
                        someTextArea.t.setFocusable(true);
                        someTextArea.t.setEditable(true);
                        Rectangle bounds = window.getGraphicsConfiguration().getBounds();
                        int x = getLocationOnScreen().x;
                        int y = getLocationOnScreen().y + getSize().height;
                        int right = x + window.getWidth();
                        int bottom = y + window.getHeight();

                        if (right <= bounds.getX() + bounds.getWidth() && bottom <= bounds.getY() + bounds.getHeight())
                            window.setLocation(x, y);
                        else {
                            int top = getLocationOnScreen().y - getSize().height;
                            if (right <= bounds.getX() + bounds.getWidth() && top >= bounds.getY())
                                window.setLocation(x, top);
                            else
                                window.setLocationByPlatform(true);
                        }

                        window.setVisible(true);
                    }

                    @Override
                    public void _case(Option.None<JTextArea> x) {
                        throw new IllegalStateException();
                    }
                });
            }
        });
    }

    private JPanel gradientPanel(LayoutManager layout) {
        return new JPanel(layout) {
            public void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2d = (Graphics2D)graphics.create();
                g2d.setPaint(new GradientPaint(0, 0, averageColor(Color.gray, Color.cyan), 0, getHeight(), Color.white));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
    }

    private Color averageColor(Color one, Color two) {
        return new Color((one.getRed() + two.getRed()) / 2, (one.getGreen() + two.getGreen()) / 2, (one.getBlue() + two.getBlue()) / 2);
    }

    private JComponent closeButton(final ActionListener listener) {
        final JLabel button = new JLabel(" X ");
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                listener.actionPerformed(null);
            }
        });
        return button;
    }

    @EDT
    public void setDetails(final Option<String> details) {
        EDT.Assert.onEdt();
        popup._switch(new Option.SwitchBlock<JTextArea>() {
            @Override
            public void _case(final Option.Some<JTextArea> someTextArea) {
                details._switch(new Option.SwitchBlock<String>() {
                    @Override
                    public void _case(Option.Some<String> x) {
                        someTextArea.t.setText(x.t);
                    }

                    @Override
                    public void _case(Option.None<String> x) {
                        Windows.findAncestor(someTextArea.t, JWindow.class)._switch(new Option.SwitchBlock<JWindow>() {
                            @Override
                            public void _case(Option.Some<JWindow> someWindow) {
                                someWindow.t.dispose();
                            }

                            @Override
                            public void _case(Option.None<JWindow> noWindow) {
                            }
                        });
                        popup = _None();
                    }
                });
            }

            @Override
            public void _case(Option.None<JTextArea> x) {
                details._switch(new Option.SwitchBlock<String>() {
                    @Override
                    public void _case(Option.Some<String> someString) {
                        final JTextArea textArea = new JTextArea(someString.t);
                        popup = _Some(textArea);
                    }

                    @Override
                    public void _case(Option.None<String> x) {
                    }
                });
            }
        });

        setEnabled(popup.match(new Option.MatchBlock<JTextArea, Boolean>() {
            @Override
            public Boolean _case(Option.Some<JTextArea> x) {
                return true;
            }

            @Override
            public Boolean _case(Option.None<JTextArea> x) {
                return false;
            }
        }));
    }
}
