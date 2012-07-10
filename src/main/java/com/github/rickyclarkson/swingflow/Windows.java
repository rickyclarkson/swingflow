package com.github.rickyclarkson.swingflow;

import javax.swing.JWindow;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Windows {
    public static <T> Option<T> findAncestor(Component component, Class<T> ancestorClass) {
        while (component != null) {
            if (ancestorClass.isInstance(component))
                return Option._Some(ancestorClass.cast(component));

            component = component.getParent();
        }

        return Option._None();
    }

    public static ActionListener close(final JWindow window) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.setVisible(false);
            }
        };
    }
}
