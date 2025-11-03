package com.toxicstoxm.YAJSI.parsing;

import org.jetbrains.annotations.NotNull;

public record Priority(int priority, boolean isExternalPriority) implements Comparable<Priority> {

    @Override
    public int compareTo(@NotNull Priority o) {
        return isExternalPriority ? (o.isExternalPriority ? Integer.compare(priority, o.priority) : -5) : 5;
    }
}
