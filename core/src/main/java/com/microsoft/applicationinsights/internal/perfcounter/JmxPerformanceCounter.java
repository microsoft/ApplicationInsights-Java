/*
 * AppInsights-Java
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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.jmx.JmxAttributeData;
import com.microsoft.applicationinsights.internal.jmx.JmxDataFetcher;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A performance counter that sends {@link com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry}
 *
 * Created by gupele on 3/15/2015.
 */
public final class JmxPerformanceCounter implements PerformanceCounter {
    private final PerformanceCounterTelemetry telemetry;
    private final Map<String, Collection<JmxAttributeData>> objectToAttributes;
    private Map.Entry<String, Collection<JmxAttributeData>> entry;
    private final String id;
    private boolean relevant = true;

    public JmxPerformanceCounter(String categoryName, String counterName, final String objectName, final Collection<JmxAttributeData> attributes) {
        this(categoryName, counterName, new HashMap<String, Collection<JmxAttributeData>>() {{
            put(objectName, attributes);
        }});
    }

    public JmxPerformanceCounter(String categoryName, String counterName, Map<String, Collection<JmxAttributeData>> objectToAttributes) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(categoryName), "categoryName should be a valid non-empty value");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(counterName), "categoryName should be a valid non-empty value");
        Preconditions.checkNotNull(objectToAttributes, "objectToAttributes should be not null");
        Preconditions.checkArgument(!objectToAttributes.isEmpty(), "objectToAttributes should be not be empty");

        id = categoryName + "." + counterName;
        telemetry = new PerformanceCounterTelemetry(categoryName, counterName, SystemInformation.INSTANCE.getProcessId(), Constants.DEFAULT_DOUBLE_VALUE);
        this.objectToAttributes = objectToAttributes;
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public void report(TelemetryClient telemetryClient) {
        if (!relevant) {
            return;
        }

        Map<String, Collection<Object>> data = null;
        if (entry == null) {
            for (Map.Entry<String, Collection<JmxAttributeData>> entry : objectToAttributes.entrySet()) {
                try {
                    data = JmxDataFetcher.fetch(entry.getKey(), entry.getValue());
                    this.entry = entry;
                    break;
                } catch (Exception e) {
                }
            }

            if (entry == null) {
                relevant = false;
                InternalLogger.INSTANCE.error("Could not find JMX data for '%s'. Performance Counter will be ignored.", getId());
                return;
            }
        } else {
            try {
                data = JmxDataFetcher.fetch(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                InternalLogger.INSTANCE.error("Failed to fetch JMX data for '%s'..", getId());
                return;
            }
        }

        if (data == null) {
            return;
        }

        for (Map.Entry<String, Collection<Object>> displayAndValues : data.entrySet()) {
            boolean ok = true;
            double value = 0.0;
            for (Object obj : displayAndValues.getValue()) {
                try {
                    value += Double.parseDouble(String.valueOf(obj));
                } catch (Exception e) {
                    ok = false;
                    InternalLogger.INSTANCE.error("Error while parsing JMX value for '%s:%s': '%s'", getId(), displayAndValues.getKey(), e.getMessage());
                    break;
                }
            }

            if (ok) {
                try {
                    telemetry.setValue(value);
                    System.out.println("Metric: " + telemetry.getCategoryName() + " " + telemetry.getCounterName() + " " + value);
                    InternalLogger.INSTANCE.trace("Metric: %s:%s: %s", telemetry.getCategoryName(), telemetry.getCounterName(), value);
                    telemetryClient.track(telemetry);
                } catch (Exception e) {
                    InternalLogger.INSTANCE.error("Error while sending JMX data for '%s': '%s'", getId(), e.getMessage());
                }
            }
        }
    }
}
