package com.github.rickyclarkson.swingflow;

import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;

public class VerticalPanel extends JPanel {
    @EDT
    public VerticalPanel(JComponent... items) {
        super(new MigLayout());
        setBackground(Color.orange);
        setOpaque(true);
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Must be called on the event dispatch thread");

        for (JComponent item: items)
            add(item, "width 100%, wrap");
    }
}
