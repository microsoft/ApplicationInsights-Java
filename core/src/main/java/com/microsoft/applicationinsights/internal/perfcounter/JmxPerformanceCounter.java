package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.jmx.JmxAttributeData;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.Collection;

/**
 * Created by gupele on 3/15/2015.
 */
public final class JmxPerformanceCounter extends AbstractJmxPerformanceCounter {
    private final String categoryName;

    public JmxPerformanceCounter(String categoryName, String id, String objectName, Collection<JmxAttributeData> attributes) {
        super(id, objectName, attributes);
        this.categoryName = categoryName;
    }

    @Override
    protected void send(TelemetryClient telemetryClient, String displayName, double value) {
        Telemetry telemetry = new PerformanceCounterTelemetry(categoryName, displayName, "", value);
        telemetryClient.track(telemetry);
    }
}
