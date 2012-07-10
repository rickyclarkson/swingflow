package com.github.rickyclarkson.swingflow;

import java.awt.Component;

public class Windows {
    public static <T> Option<T> findAncestor(Component component, Class<T> ancestorClass) {
        while (component != null) {
            if (ancestorClass.isInstance(component))
                return Option._Some(ancestorClass.cast(component));

            component = component.getParent();
        }

        return Option._None();
    }
}
