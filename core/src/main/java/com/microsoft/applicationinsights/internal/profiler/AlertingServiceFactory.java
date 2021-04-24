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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.telemetry.TelemetryObservers;

/**
 * Creates AlertMonitor and wires it up to observe telemetry.
 */
public class AlertingServiceFactory {
    public static AlertingSubsystem create(
            Consumer<AlertBreach> alertAction,
            TelemetryObservers telemetryObservers,
            TelemetryClient telemetryClient,
            ExecutorService executorService,
            GcEventMonitor.GcEventMonitorConfiguration gcEventMonitorConfiguration) {
        AlertingSubsystem alertingSubsystem = AlertingSubsystem.create(alertAction, executorService);

        addObserver(alertingSubsystem, telemetryObservers);

        monitorGcActivity(alertingSubsystem, telemetryClient, executorService, gcEventMonitorConfiguration);
        return alertingSubsystem;
    }

    private static void monitorGcActivity(
            AlertingSubsystem alertingSubsystem,
            TelemetryClient telemetryClient,
            ExecutorService executorService,
            GcEventMonitor.GcEventMonitorConfiguration gcEventMonitorConfiguration) {
        GcEventMonitor.init(alertingSubsystem, telemetryClient, executorService, gcEventMonitorConfiguration);
    }

    private static void addObserver(AlertingSubsystem alertingSubsystem, TelemetryObservers telemetryObservers) {
        // FIXME (trask)
        /*
        telemetryObservers.addObserver(new TelemetryObserver<MetricTelemetry>(MetricTelemetry.class) {
            @Override
            protected void process(MetricTelemetry telemetry) {
                AlertMetricType alertMetricType = null;
                if (telemetry.getName().equals(TOTAL_CPU_PC_METRIC_NAME)) {
                    alertMetricType = AlertMetricType.CPU;
                }

                if (alertMetricType != null) {
                    alertingSubsystem.track(alertMetricType, telemetry.getValue());
                }
            }
        });
         */
    }
}
