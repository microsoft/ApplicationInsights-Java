package com.microsoft.applicationinsights;

import org.checkerframework.checker.nullness.qual.Nullable;

public class InstrumentationKey {

    private static volatile @Nullable String value;

    public static @Nullable String get() {
        return value;
    }

    public static void set(String value) {
        InstrumentationKey.value = value;
    }

    private InstrumentationKey() {}
}
