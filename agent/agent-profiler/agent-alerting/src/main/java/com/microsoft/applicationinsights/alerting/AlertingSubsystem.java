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
package com.microsoft.applicationinsights.alerting;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.alert.AlertMetricType;
import com.microsoft.applicationinsights.alerting.analysis.AlertPipelines;
import com.microsoft.applicationinsights.alerting.analysis.TelemetryDataPoint;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfigurationBuilder;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for the alerting subsystem.
 * - Configures alerts according to a provided configuration
 * - Receives telemetry data, feeds it into the appropriate alert pipeline and if necessary
 * issue an alert.
 */
public class AlertingSubsystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertingSubsystem.class);

    // Downstream observer of alerts produced by the alerting system
    private final Consumer<AlertBreach> alertHandler;

    // Execution context of the alerting subsytem
    private final ExecutorService executorService;

    // queue of telemetry data to be processed
    private final LinkedBlockingQueue<TelemetryDataPoint> workQueue = new LinkedBlockingQueue<>();

    // List of manual triggers that have already been processed
    private final Set<String> manualTriggersExecuted = new HashSet<>();

    private final AlertPipelines alertPipelines;
    private final TimeSource timeSource;

    // Current configuration of the alerting subsystem
    private AlertingConfiguration alertConfig;

    // monitor to guard the notification that the work list has been processed
    private final Object monitor = new Object();

    protected AlertingSubsystem(Consumer<AlertBreach> alertHandler, ExecutorService executorService) {
        this.alertHandler = alertHandler;
        alertPipelines = new AlertPipelines(alertHandler);
        this.executorService = executorService;
        timeSource = TimeSource.DEFAULT;
    }

    public static AlertingSubsystem create(Consumer<AlertBreach> alertHandler, ExecutorService executorService) {
        AlertingSubsystem alertingSubsystem = new AlertingSubsystem(alertHandler, executorService);
        //init with disabled config
        alertingSubsystem.initialize(new AlertingConfiguration(
                new AlertConfigurationBuilder().setType(AlertMetricType.CPU).setEnabled(false).setThreshold(0).setProfileDuration(0).setCooldown(0).createAlertConfiguration(),
                new AlertConfigurationBuilder().setType(AlertMetricType.MEMORY).setEnabled(false).setThreshold(0).setProfileDuration(0).setCooldown(0).createAlertConfiguration(),
                new DefaultConfiguration(false, 0, 0),
                new CollectionPlanConfiguration(false, EngineMode.immediate, ZonedDateTime.now(), 0, "")));
        return alertingSubsystem;
    }

    /**
     * Create alerting pipelines with default configuration
     */
    public void initialize(AlertingConfiguration alertConfig) {

        updateConfiguration(alertConfig);

        executorService
                .execute(() -> {
                    while (true) {
                        try {
                            process(workQueue.take());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        } catch (RuntimeException e) {
                            LOGGER.error("Exception while evaluating alert", e);
                        } catch (Error e) {
                            LOGGER.error("Exception while evaluating alert", e);
                            throw e;
                        }

                        synchronized (monitor) {
                            monitor.notifyAll();
                        }
                    }
                });
    }

    /**
     * Add telemetry to alert processing pipeline
     */
    public void track(AlertMetricType type, Number value) {
        if (type != null && value != null) {
            workQueue.add(new TelemetryDataPoint(type, timeSource.getNow(), value.doubleValue()));
        }
    }

    /**
     * Block until work queue is empty
     */
    public void awaitQueueFlush() {
        while (workQueue.size() > 0) {
            synchronized (monitor) {
                try {
                    if (workQueue.size() > 0) {
                        monitor.wait();
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    /**
     * Deliver data to pipelines
     */
    public void process(TelemetryDataPoint telemetryDataPoint) {
        if (telemetryDataPoint == null) {
            return;
        }
        LOGGER.trace("Tracking " + telemetryDataPoint.getType().name() + " " + telemetryDataPoint.getValue());
        alertPipelines.process(telemetryDataPoint);
    }

    /**
     * Apply given configuration to the alerting pipelines
     */
    public void updateConfiguration(AlertingConfiguration alertingConfig) {
        if (this.alertConfig == null || !this.alertConfig.equals(alertingConfig)) {
            AlertConfiguration oldCpuConfig = this.alertConfig == null ? null : this.alertConfig.getCpuAlert();
            updatePipelineConfig(alertingConfig.getCpuAlert(), oldCpuConfig);

            AlertConfiguration oldMemoryConfig = this.alertConfig == null ? null : this.alertConfig.getMemoryAlert();
            updatePipelineConfig(alertingConfig.getMemoryAlert(), oldMemoryConfig);

            evaluateManualTrigger(alertingConfig);
            this.alertConfig = alertingConfig;
        }
    }

    /**
     * If the config has changed update the pipeline
     */
    private void updatePipelineConfig(AlertConfiguration newAlertConfig, AlertConfiguration oldAlertConfig) {
        if (oldAlertConfig == null || !oldAlertConfig.equals(newAlertConfig)) {
            alertPipelines.updateAlertConfig(newAlertConfig);
        }
    }

    /**
     * Determine if a manual alert has been requested
     */
    private void evaluateManualTrigger(AlertingConfiguration alertConfig) {
        CollectionPlanConfiguration config = alertConfig.getCollectionPlanConfiguration();

        boolean shouldTrigger = config.isSingle() &&
                config.getMode() == EngineMode.immediate &&
                ZonedDateTime.now().isBefore(config.getExpiration()) &&
                !manualTriggersExecuted.contains(config.getSettingsMoniker());

        if (shouldTrigger) {
            manualTriggersExecuted.add(config.getSettingsMoniker());
            AlertBreach alertBreach = new AlertBreach(AlertMetricType.MANUAL,
                    0.0,
                    new AlertConfigurationBuilder()
                            .setType(AlertMetricType.MANUAL)
                            .setEnabled(true)
                            .setProfileDuration(config.getImmediateProfilingDuration())
                            .setThreshold(0.0f)
                            .setCooldown(0)
                            .createAlertConfiguration());
            alertHandler.accept(alertBreach);
        }
    }

}
