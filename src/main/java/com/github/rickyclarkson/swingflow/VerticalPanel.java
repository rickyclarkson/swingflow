package com.github.rickyclarkson.swingflow;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.GridLayout;

public class VerticalPanel extends JPanel {
    @EDT
    public VerticalPanel(JComponent... items) {
        super(new GridLayout(items.length, 1));
        setBackground(Color.orange);
        setOpaque(true);
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Must be called on the event dispatch thread");

        for (JComponent item: items)
            add(item);
    }
}
