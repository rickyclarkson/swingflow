package com.github.rickyclarkson.swingflow;

import javax.swing.SwingUtilities;

public @interface EDT {
    public class Assert {
        public static void onEdt() {
            if (!SwingUtilities.isEventDispatchThread())
                throw new IllegalStateException("Unsafe use of Swing code detected.");
        }
    }
}
