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
package com.microsoft.applicationinsights.internal.profiler;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.alert.AlertMetricType;
import com.microsoft.applicationinsights.extensibility.initializer.TelemetryObservers;
import com.microsoft.applicationinsights.internal.perfcounter.jvm.JvmHeapMemoryUsedPerformanceCounter;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.applicationinsights.internal.perfcounter.Constants.TOTAL_CPU_PC_METRIC_NAME;

/**
 * Creates AlertMonitor and wires it up to observe telemetry.
 */
public class AlertingServiceFactory  {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertingServiceFactory.class);

    public static AlertingSubsystem create(Consumer<AlertBreach> alertAction, TelemetryObservers telemetryObservers, ExecutorService executorService) {
        AlertingSubsystem alertingSubsystem = AlertingSubsystem.create(alertAction, executorService);

        addObserver(alertingSubsystem, telemetryObservers);

        return alertingSubsystem;
    }

    private static void addObserver(AlertingSubsystem alertingSubsystem, TelemetryObservers telemetryObservers) {
        telemetryObservers.addObserver(new TelemetryObserver<MetricTelemetry>(MetricTelemetry.class) {
            @Override
            protected void process(MetricTelemetry telemetry) {
                AlertMetricType alertMetricType = null;
                if (telemetry.getName().equals(TOTAL_CPU_PC_METRIC_NAME)) {
                    alertMetricType = AlertMetricType.CPU;
                } else if (telemetry.getName().equals(JvmHeapMemoryUsedPerformanceCounter.HEAP_MEM_USED_PERCENTAGE)) {
                    alertMetricType = AlertMetricType.MEMORY;
                }

                if (alertMetricType != null) {
                    alertingSubsystem.track(alertMetricType, telemetry.getValue());
                }
            }
        });
    }
}
