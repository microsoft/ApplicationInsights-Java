package com.microsoft.applicationinsights.telemetry;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;

public abstract class TelemetryObserver<T extends TelemetryItem> {

    private final Class<T> clazz;

    public TelemetryObserver(Class<T> clazz) {
        this.clazz = clazz;
    }

    protected abstract void process(T telemetry);

    public void consume(TelemetryItem telemetry) {
        if (telemetry.getClass().isAssignableFrom(clazz)) {
            process(clazz.cast(telemetry));
        }
    }
}
