package com.microsoft.applicationinsights.telemetry;

public abstract class TelemetryObserver<T extends Telemetry> {

    private final Class<T> clazz;

    public TelemetryObserver(Class<T> clazz) {
        this.clazz = clazz;
    }

    protected abstract void process(T telemetry);

    public void consume(Telemetry telemetry) {
        if (telemetry.getClass().isAssignableFrom(clazz)) {
            process((T) telemetry);
        }
    }
}
