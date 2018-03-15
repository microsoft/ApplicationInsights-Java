package com.microsoft.applicationinsights.internal.channel.samplingV2;

public enum TelemetryType {
    Dependency,
    Event,
    Exception,
    PageView,
    Request,
    Trace;

    static TelemetryType valueOfOrNull(String name) {
        try {
            return valueOf(name);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
}
