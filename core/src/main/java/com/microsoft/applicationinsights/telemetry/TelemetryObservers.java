package com.microsoft.applicationinsights.telemetry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.microsoft.applicationinsights.telemetry.TelemetryObserver;

public enum TelemetryObservers {
    INSTANCE;

    private final List<TelemetryObserver<?>> observers = new CopyOnWriteArrayList<>();


    public void addObserver(TelemetryObserver<?> metricTelemetryTelemetryObserver) {
        observers.add(metricTelemetryTelemetryObserver);
    }

    public List<TelemetryObserver<?>> getObservers() {
        return observers;
    }
}
