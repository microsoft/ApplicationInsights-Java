package com.microsoft.applicationinsights.internal.statsbeat;

enum OperatingSystem {

    OS_WINDOWS("Windows"), OS_LINUX("Linux"), OS_UNKNOWN("unknown");

    private final String id;

    OperatingSystem(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
