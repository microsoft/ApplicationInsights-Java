package com.microsoft.applicationinsights.common;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class Strings {

    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

    private Strings() {}
}
