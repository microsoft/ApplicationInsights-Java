package com.microsoft.applicationinsights.internal.perfcounter;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Performance counters that are sent as {@link com.microsoft.applicationinsights.telemetry.MetricTelemetry}
 *
 * Created by gupele on 3/30/2015.
 */
public final class WindowsPerformanceCounterAsMetric implements PerformanceCounter {
    private static final String ID = Constants.PERFORMANCE_COUNTER_PREFIX + "WindowsPerformanceCounterAsMetric";

    private final HashMap<String, String> keyToDisplayName = new HashMap<String, String>();
    private final MetricTelemetry telemetry = new MetricTelemetry("placeholder", Constants.DEFAULT_DOUBLE_VALUE);

    /**
     * Registers the argument's data into performance counters.
     * @param pcsData The performance counters to register for Windows OS.
     * @throws java.lang.Throwable The constructor might throw an Error if the JniPCConnector is not able to properly
     * connect to the native code. or Exception if the constructor is not called under Windows OS.
     */
    public WindowsPerformanceCounterAsMetric(Iterable<WindowsPerformanceCounterData> pcsData) throws Throwable {
        Preconditions.checkState(SystemInformation.INSTANCE.isWindows(), "Must be used under Windows OS.");
        Preconditions.checkNotNull(pcsData, "pcsData must be non-null value.");

        register(pcsData);
        if (keyToDisplayName.isEmpty()) {
            // Failed to register, the performance counter is not needed.
            throw new Exception("No valid data");
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    /**
     * Go through the data that we have and ask for each key its value, and then send using the requested display name.
     * The method might throw an Error if the JniPCConnector is not able to properly connect to the native code.
     * @param telemetryClient The {@link com.microsoft.applicationinsights.TelemetryClient} to use for sending
     */
    @Override
    public void report(TelemetryClient telemetryClient) {
        for (Map.Entry<String, String> entry : keyToDisplayName.entrySet()) {
            try {
                double value = JniPCConnector.getValueOfPerformanceCounter(entry.getKey());
                send(telemetryClient, value, entry.getValue());
                InternalLogger.INSTANCE.trace("Sent performance counter for '%s': '%s'", entry.getValue(), value);
            } catch (Throwable e) {
                InternalLogger.INSTANCE.error("Failed to send performance counter for '%s': '%s'", entry.getValue(), e.getMessage());
            }
        }
    }

    /**
     * Register the requested performance counters using the native code, and storing the display name for future sending of data.
     * @param pcsData The requested performance counters.
     */
    private void register(Iterable<WindowsPerformanceCounterData> pcsData) {
        for (WindowsPerformanceCounterData data : pcsData) {
            try {
                String key = JniPCConnector.addPerformanceCounter(data.categoryName, data.counterName, data.instanceName);
                if (!Strings.isNullOrEmpty(key)) {
                    keyToDisplayName.put(key, data.displayName);
                }
            } catch (Throwable t) {
            }
        }
    }

    protected void send(TelemetryClient telemetryClient, double value, String key) {
        // Using the metric to avoid unneeded allocations
        telemetry.setName(key);
        telemetry.setValue(value);
        telemetryClient.track(telemetry);
    }
}
