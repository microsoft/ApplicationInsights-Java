package com.microsoft.applicationinsights.internal.perfcounter;

import java.util.Collection;
import java.util.Map;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.jmx.JmxAttributeData;
import com.microsoft.applicationinsights.internal.jmx.JmxDataFetcher;

/**
 * Created by gupele on 3/15/2015.
 */
public abstract class AbstractJmxPerformanceCounter implements PerformanceCounter {
    private final String id;
    private final String objectName;
    private final Collection<JmxAttributeData> attributes;
    private boolean relevant = true;
    private boolean firstTime = true;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized void report(TelemetryClient telemetryClient) {
        if (!relevant) {
            return;
        }

        try {
            Map<String, Collection<Object>> result =
                    JmxDataFetcher.fetch(objectName, attributes);

            for (Map.Entry<String, Collection<Object>> displayAndValues : result.entrySet()) {
                boolean ok = true;
                double value = 0.0;
                for (Object obj : displayAndValues.getValue()) {
                    try {
                        value += Double.parseDouble(String.valueOf(obj));
                    } catch (Exception e) {
                        ok = false;
                        break;
                    }
                }

                if (ok) {
                    send(telemetryClient, displayAndValues.getKey(), value);
                }
            }
        } catch (Exception e) {
            if (firstTime) {
                relevant = false;
            }
        } finally {
            firstTime = false;
        }
    }

    protected AbstractJmxPerformanceCounter(String id, String objectName, Collection<JmxAttributeData> attributes) {
        this.id = id;
        this.objectName = objectName;
        this.attributes = attributes;
    }

    protected abstract void send(TelemetryClient telemetryClient, String displayName, double value);
}
