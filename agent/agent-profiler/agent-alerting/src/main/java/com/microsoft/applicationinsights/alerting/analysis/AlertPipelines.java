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
package com.microsoft.applicationinsights.alerting.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.alert.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains analysis pipelines for all metric types
 */
public class AlertPipelines {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertPipelines.class);

    // List of alert analysis pipelines for each metric type, entrypoint
    // for the pipeline is a rolling average
    private final Map<AlertMetricType, AlertPipeline> alertPipelines = new HashMap<>();

    //Handler to notify when a breach happens
    private final Consumer<AlertBreach> alertHandler;

    public AlertPipelines(Consumer<AlertBreach> alertHandler) {
        this.alertHandler = alertHandler;
    }

    public OptionalDouble getAverage(AlertMetricType type) {
        AlertPipeline pipeline = alertPipelines.get(type);
        if (pipeline != null) {
            return pipeline.calculateAverage();
        } else {
            return OptionalDouble.empty();
        }
    }

    public void updateAlertConfig(AlertConfiguration newAlertConfig) {
        AlertPipeline pipeline = alertPipelines.get(newAlertConfig.getType());
        if (pipeline == null) {
            pipeline = AlertPipeline.create(new RollingAverage(), newAlertConfig, this::dispatchAlert);
            alertPipelines.put(newAlertConfig.getType(), pipeline);
        } else {
            pipeline.updateConfig(newAlertConfig);
        }

        LOGGER.debug("Set alert configuration for {}: {}", newAlertConfig.getType(), newAlertConfig.toString());
    }

    /**
     * Ensure that alerts contain the required metrics and notify upstream handler
     */
    private void dispatchAlert(AlertBreach alert) {
        alertHandler.accept(addMetricData(alert));
    }

    //Ensure that cpu and memory values are set on the breach
    private AlertBreach addMetricData(AlertBreach alert) {
        if (alert.getMemoryUsage() == 0.0) {
            OptionalDouble memory = getAverage(AlertMetricType.MEMORY);
            if (memory.isPresent()) {
                alert = alert.withMemoryMetric(memory.getAsDouble());
            }
        }

        if (alert.getCpuMetric() == 0.0) {
            OptionalDouble cpu = getAverage(AlertMetricType.CPU);
            if (cpu.isPresent()) {
                alert = alert.withCpuMetric(cpu.getAsDouble());
            }
        }
        return alert;
    }

    /**
     * Route telemetry to the appropriate pipeline
     */
    public void process(TelemetryDataPoint telemetryDataPoint) {
        AlertPipeline pipeline = alertPipelines.get(telemetryDataPoint.getType());
        if (pipeline != null) {
            pipeline.track(telemetryDataPoint);
        }
    }
}
