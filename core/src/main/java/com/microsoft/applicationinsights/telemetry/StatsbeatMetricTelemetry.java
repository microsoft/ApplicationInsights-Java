package com.microsoft.applicationinsights.telemetry;

public class StatsbeatMetricTelemetry extends MetricTelemetry {

    private static final String STATSBEAT_IKEY = "006208d6-a99c-488b-8d75-4420afeec14d";
    private static final String STATSBEAT_TELEMETRY_NAME = "Statsbeat";

    public StatsbeatMetricTelemetry(String name, double value) {
        super(name, value);
        setTelemetryName(STATSBEAT_TELEMETRY_NAME);
        this.getContext().setInstrumentationKey(STATSBEAT_IKEY);
    }
}
