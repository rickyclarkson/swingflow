package com.github.rickyclarkson.swingflow;

import fj.data.Option;

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

import static com.github.rickyclarkson.swingflow.Windows.close;

public class DetailsButton extends JButton {
    private final JTextArea popup = new JTextArea(5, 20);

    public DetailsButton() {
        super("Details >>");
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JWindow window = new JWindow();

                final Border border = BorderFactory.createLineBorder(Color.black);
                popup.setBorder(border);
                final JPanel northPanel = gradientPanel(new BorderLayout());
                northPanel.add(new JLabel("Details"), BorderLayout.WEST);
                northPanel.add(closeButton(close(window)), BorderLayout.EAST);
                window.add(northPanel, BorderLayout.NORTH);
                window.add(popup);
                window.setMinimumSize(new Dimension(200, 100));
                window.pack();
                popup.setFocusable(true);
                popup.setEditable(true);
                final Rectangle bounds = window.getGraphicsConfiguration().getBounds();
                final int x = getLocationOnScreen().x;
                final int y = getLocationOnScreen().y + getSize().height;
                final int right = x + window.getWidth();
                final int bottom = y + window.getHeight();

                if (right <= bounds.getX() + bounds.getWidth() && bottom <= bounds.getY() + bounds.getHeight())
                    window.setLocation(x, y);
                else {
                    final int top = getLocationOnScreen().y - getSize().height;
                    if (right <= bounds.getX() + bounds.getWidth() && top >= bounds.getY())
                        window.setLocation(x, top);
                    else
                        window.setLocationByPlatform(true);
                }

                window.setVisible(true);
            }
        });
    }

    private JPanel gradientPanel(LayoutManager layout) {
        return new JPanel(layout) {
            public void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                final Graphics2D g2d = (Graphics2D)graphics.create();
                g2d.setPaint(new GradientPaint(0, 0, averageColor(Color.gray, Color.cyan), 0, getHeight(), Color.white));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
    }

    private static Color averageColor(Color one, Color two) {
        return new Color((one.getRed() + two.getRed()) / 2, (one.getGreen() + two.getGreen()) / 2, (one.getBlue() + two.getBlue()) / 2);
    }

    private static JComponent closeButton(final ActionListener listener) {
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
        if (details.isSome())
            popup.setText(details.some());
        else {
            for (JWindow someWindow: Windows.findAncestor(popup, JWindow.class))
                someWindow.dispose();
        }

        setEnabled(details.isSome());
    }
}
