package com.microsoft.applicationinsights.internal.statsbeat;

enum OperatingSystem {

    OS_WINDOWS("Windows"), OS_LINUX("Linux"), OS_UNKNOWN("unknown");

    private final String value;

    OperatingSystem(String value) {
        this.value = value;
    }

    String getValue() {
        return value;
    }
}
