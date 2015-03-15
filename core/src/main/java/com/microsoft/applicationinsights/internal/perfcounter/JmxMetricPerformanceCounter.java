package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.jmx.JmxAttributeData;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

import java.util.Collection;

/**
 * Created by gupele on 3/15/2015.
 */
public final class JmxMetricPerformanceCounter extends AbstractJmxPerformanceCounter {
    private final MetricTelemetry telemetry = new MetricTelemetry();

    public JmxMetricPerformanceCounter(String id, String objectName, Collection<JmxAttributeData> attributes) {
        super(id, objectName, attributes);
    }

    @Override
    protected void send(TelemetryClient telemetryClient, String displayName, double value) {
        telemetry.setName(displayName);
        telemetry.setValue(value);
        telemetryClient.track(telemetry);
    }
}
