/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.perfcounter;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;

import com.google.common.base.Strings;

/**
 * Built-in Windows performance counters that are sent as {@link com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry}
 *
 * Created by gupele on 3/30/2015.
 */
public final class WindowsPerformanceCounterAsPC extends AbstractWindowsPerformanceCounter {
    private static final String TOTAL_CPU_CATEGORY_NAME = "Processor";
    private static final String TOTAL_CPU_COUNTER_NAME = "% Processor time";
    private static final String TOTAL_CPU_INSTANCE_NAME = "_Total";

    private static final String TOTAL_MEMORY_CATEGORY_NAME = "Memory";
    private static final String TOTAL_MEMORY_COUNTER_NAME = "Available bytes";


    private static final String PROCESS_IO_DATA_BYTES_CATEGORY_NAME = "Process";
    private static final String PROCESS_IO_DATA_BYTES_COUNTER_NAME = "IO Data Bytes/sec";

    private static final String ID = Constants.PERFORMANCE_COUNTER_PREFIX + "WindowsPerformanceCounterAsPC";

    // Performance counter key and its data that is relevant when sending.
    private final HashMap<String, WindowsPerformanceCounterData> pcs = new HashMap<String, WindowsPerformanceCounterData>();

    /**
     * Registers the 'built-in' Windows performance counters that are not fetched from the JVM JMX.
     *
     * @throws java.lang.Throwable The constructor might throw an Error if the JniPCConnector is not able to properly
     * connect to the native code. or Exception if the constructor is not called under Windows OS.
     */
    public WindowsPerformanceCounterAsPC() throws Throwable {
        Preconditions.checkState(SystemInformation.INSTANCE.isWindows(), "Must be used under Windows OS.");

        register(TOTAL_CPU_CATEGORY_NAME, TOTAL_CPU_COUNTER_NAME, TOTAL_CPU_INSTANCE_NAME);
        register(TOTAL_MEMORY_CATEGORY_NAME, TOTAL_MEMORY_COUNTER_NAME, "");
        register(PROCESS_IO_DATA_BYTES_CATEGORY_NAME, PROCESS_IO_DATA_BYTES_COUNTER_NAME, JniPCConnector.translateInstanceName(JniPCConnector.PROCESS_SELF_INSTANCE_NAME));

        if (pcs.isEmpty()) {
            // Failed to register, the performance counter is not needed.
            throw new Exception("Failed to register all built-in Windows performance counters.");
        }
    }

    @Override
    public void report(TelemetryClient telemetryClient) {
        for (Map.Entry<String, WindowsPerformanceCounterData> entry : pcs.entrySet()) {
            try {
                double value = JniPCConnector.getValueOfPerformanceCounter(entry.getKey());
                if (value < 0) {
                    reportError(value, entry.getValue().displayName);
                } else {
                    send(telemetryClient, value, entry.getValue());
                    WindowsPerformanceCounterData pcData = entry.getValue();
                    InternalLogger.INSTANCE.trace("Sent performance counter for '%s'(%s, %s, %s): '%s'",
                            pcData.displayName, pcData.categoryName, pcData.counterName, pcData.instanceName, value);
                }
            } catch (Throwable e) {
                InternalLogger.INSTANCE.error("Failed to send performance counter for '%s': '%s'", entry.getValue().displayName, e.getMessage());
            }
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    private void send(TelemetryClient telemetryClient, double value, WindowsPerformanceCounterData data) {
        PerformanceCounterTelemetry telemetry = new PerformanceCounterTelemetry(data.categoryName, data.counterName, data.instanceName, value);
        telemetryClient.track(telemetry);
    }

    /**
     * The method will use the {@link com.microsoft.applicationinsights.internal.perfcounter.JniPCConnector} to register a performance counter.
     * The method might throw an Error if the JniPCConnector is not able to properly connect to the native code.
     * @param category The category
     * @param counter The counter
     * @param instance The instnace
     */
    private void register(String category, String counter, String instance) {
        String key = JniPCConnector.addPerformanceCounter(category, counter, instance);
        if (!Strings.isNullOrEmpty(key)) {
            try {
                WindowsPerformanceCounterData data = new WindowsPerformanceCounterData().
                        setCategoryName(category).
                        setCounterName(counter).
                        setInstanceName(instance).
                        setDisplayName(category + " " + counter);
                pcs.put(key, data);
            } catch (Throwable e) {
            }
        }
    }
}
