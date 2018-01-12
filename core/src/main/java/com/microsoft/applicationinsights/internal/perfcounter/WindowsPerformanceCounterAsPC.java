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
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Built-in Windows performance counters that are sent as {@link com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry}
 *
 * Created by gupele on 3/30/2015.
 */
public final class WindowsPerformanceCounterAsPC extends AbstractWindowsPerformanceCounter {

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

        register(Constants.TOTAL_CPU_PC_CATEGORY_NAME, Constants.CPU_PC_COUNTER_NAME, Constants.INSTANCE_NAME_TOTAL);
        register(Constants.TOTAL_MEMORY_PC_CATEGORY_NAME, Constants.TOTAL_MEMORY_PC_COUNTER_NAME, "");
        register(Constants.PROCESS_CATEGORY, Constants.PROCESS_IO_PC_COUNTER_NAME, JniPCConnector.translateInstanceName(JniPCConnector.PROCESS_SELF_INSTANCE_NAME));

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
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable e) {
                try {
                    InternalLogger.INSTANCE.error("Failed to send performance counter for '%s': '%s'", entry.getValue().displayName, e.getMessage());
                    InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t2) {
                    // chomp
                }
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
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable e) {
                try {
                    InternalLogger.INSTANCE.error("Exception while registering windows performance counter as PC");
                    InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t2) {
                    // chomp
                }
            }
        }
    }
}
