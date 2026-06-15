// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipeline;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipelines;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingProfileFileTriggerConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for the alerting subsystem. - Configures alerts according to a provided
 * configuration - Receives telemetry data, feeds it into the appropriate alert pipeline and if
 * necessary issue an alert.
 */
public class AlertingSubsystem {

  private static final Logger logger = LoggerFactory.getLogger(AlertingSubsystem.class);
  // Downstream observer of alerts produced by the alerting system
  private final Consumer<AlertBreach> alertHandler;

  // List of manual triggers that have already been processed
  private final Set<String> manualTriggersExecuted = new HashSet<>();

  private final AlertPipelines alertPipelines;
  private final TimeSource timeSource;

  // Current configuration of the alerting subsystem
  private AlertingConfiguration alertConfig;

  /** Configuration controlling the file-based manual profile trigger. */
  private final AlertingProfileFileTriggerConfiguration alertingProfileFileTriggerConfiguration;

  private boolean enableRequestTriggerUpdates;

  protected AlertingSubsystem(
      Consumer<AlertBreach> alertHandler,
      TimeSource timeSource,
      boolean enableRequestTriggerUpdates,
      AlertingProfileFileTriggerConfiguration alertingProfileFileTriggerConfiguration) {
    this.alertHandler = alertHandler;
    this.alertPipelines = new AlertPipelines(alertHandler);
    this.timeSource = timeSource;
    this.enableRequestTriggerUpdates = enableRequestTriggerUpdates;
    this.alertingProfileFileTriggerConfiguration = alertingProfileFileTriggerConfiguration;
  }

  /**
   * Creates and initializes an {@link AlertingSubsystem} with an initially-disabled configuration.
   *
   * @param alertHandler downstream consumer that handles generated alert breaches
   * @param timeSource time source used for alert evaluation windows
   * @param alertingProfileFileTriggerConfiguration configuration for the file-based manual trigger
   * @return a fully initialized alerting subsystem ready to receive configuration updates
   */
  public static AlertingSubsystem create(
      Consumer<AlertBreach> alertHandler,
      TimeSource timeSource,
      AlertingProfileFileTriggerConfiguration alertingProfileFileTriggerConfiguration) {

    AlertingSubsystem alertingSubsystem =
        new AlertingSubsystem(
            alertHandler, timeSource, true, alertingProfileFileTriggerConfiguration);

    // init with disabled config
    alertingSubsystem.initialize(
        AlertingConfiguration.create(
            AlertConfiguration.builder().setType(AlertMetricType.CPU).build(),
            AlertConfiguration.builder().setType(AlertMetricType.MEMORY).build(),
            DefaultConfiguration.builder().build(),
            CollectionPlanConfiguration.builder()
                .setSingle(false)
                .setMode(EngineMode.immediate)
                .setExpiration(Instant.now())
                .setImmediateProfilingDurationSeconds(0)
                .setSettingsMoniker("")
                .build(),
            new ArrayList<>()));
    return alertingSubsystem;
  }

  /** Create alerting pipelines with default configuration. */
  public void initialize(AlertingConfiguration alertConfig) {
    updateConfiguration(alertConfig);
  }

  /** Add telemetry to alert processing pipeline. */
  public void track(@Nullable AlertMetricType type, @Nullable Number value) {
    if (type != null && value != null) {
      trackTelemetryDataPoint(
          TelemetryDataPoint.create(type, timeSource.getNow(), type.name(), value.doubleValue()));
    }
  }

  /** Deliver data to pipelines. */
  public void trackTelemetryDataPoint(@Nullable TelemetryDataPoint telemetryDataPoint) {
    if (telemetryDataPoint == null) {
      return;
    }
    logger.trace(
        "Tracking " + telemetryDataPoint.getType().name() + " " + telemetryDataPoint.getValue());
    alertPipelines.process(telemetryDataPoint);
  }

  /** Apply given configuration to the alerting pipelines. */
  public void updateConfiguration(AlertingConfiguration alertingConfig) {

    if (this.alertConfig == null || !this.alertConfig.equals(alertingConfig)) {
      AlertConfiguration oldCpuConfig =
          this.alertConfig == null ? null : this.alertConfig.getCpuAlert();
      updatePipelineConfig(alertingConfig.getCpuAlert(), oldCpuConfig);

      AlertConfiguration oldMemoryConfig =
          this.alertConfig == null ? null : this.alertConfig.getMemoryAlert();
      updatePipelineConfig(alertingConfig.getMemoryAlert(), oldMemoryConfig);

      if (this.enableRequestTriggerUpdates && alertingConfig.hasRequestAlertConfiguration()) {
        List<AlertConfiguration> oldRequestConfig =
            this.alertConfig == null ? null : this.alertConfig.getRequestAlertConfiguration();
        updateRequestPipelineConfig(
            alertingConfig.getRequestAlertConfiguration(), oldRequestConfig);
      }

      evaluateManualTrigger(alertingConfig);

      this.alertConfig = alertingConfig;
    }
  }

  /** If the config has changed update the pipeline. */
  private void updatePipelineConfig(
      AlertConfiguration newAlertConfig, @Nullable AlertConfiguration oldAlertConfig) {
    if (oldAlertConfig == null || !oldAlertConfig.equals(newAlertConfig)) {
      alertPipelines.updateAlertConfig(newAlertConfig, timeSource);
    }
  }

  private void updateRequestPipelineConfig(
      List<AlertConfiguration> newAlertConfig, @Nullable List<AlertConfiguration> oldAlertConfig) {
    if (oldAlertConfig == null || !oldAlertConfig.equals(newAlertConfig)) {
      alertPipelines.updateRequestAlertConfig(newAlertConfig, timeSource);
    }
  }

  /**
   * Determine if a manual alert has been requested via any supported mechanism. Currently evaluates
   * both the server-side collection plan and the local file-based trigger.
   */
  private void evaluateManualTrigger(AlertingConfiguration alertConfig) {
    evaluateCollectionPlanTrigger(alertConfig);
    evaluateFileTrigger(alertConfig);
  }

  /** Check if the collection plan configuration requests a manual profile. */
  private void evaluateCollectionPlanTrigger(AlertingConfiguration alertConfig) {
    CollectionPlanConfiguration config = alertConfig.getCollectionPlanConfiguration();

    boolean shouldTrigger =
        config.isSingle()
            && config.getMode() == EngineMode.immediate
            && timeSource.getNow().isBefore(config.getExpiration())
            && !manualTriggersExecuted.contains(config.getSettingsMoniker());

    if (shouldTrigger) {
      manualTriggersExecuted.add(config.getSettingsMoniker());

      AlertBreach alertBreach =
          AlertBreach.builder()
              .setType(AlertMetricType.MANUAL)
              .setAlertValue(0.0)
              .setAlertConfiguration(
                  AlertConfiguration.builder()
                      .setType(AlertMetricType.MANUAL)
                      .setEnabled(true)
                      .setProfileDurationSeconds(config.getImmediateProfilingDurationSeconds())
                      .build())
              .setProfileId(UUID.randomUUID().toString())
              .setCpuMetric(0)
              .setMemoryUsage(0)
              .build();
      alertHandler.accept(alertBreach);
    }
  }

  /**
   * Check if a trigger file is present on the local file system and was recently modified. If so,
   * delete the file and trigger a manual profile. The global cooldown in Profiler prevents
   * overlapping profiles.
   */
  public void evaluateFileTrigger() {
    if (!alertingProfileFileTriggerConfiguration.isEnabled()) {
      return;
    }

    if (alertConfig == null) {
      return;
    }

    evaluateFileTrigger(alertConfig);
  }

  private void evaluateFileTrigger(AlertingConfiguration currentConfig) {
    if (!alertingProfileFileTriggerConfiguration.isEnabled()) {
      return;
    }

    if (currentConfig == null) {
      return;
    }

    File manualTriggerFile = alertingProfileFileTriggerConfiguration.getFilePath();
    if (manualTriggerFile == null || !manualTriggerFile.exists()) {
      return;
    }

    long lastModified = manualTriggerFile.lastModified();
    long age = timeSource.getNow().toEpochMilli() - lastModified;

    if (age < 0
        || age > AlertingProfileFileTriggerConfiguration.MANUAL_TRIGGER_FILE_MAX_AGE_MS) {
      return;
    }

    // Delete the trigger file to prevent re-triggering
    if (!manualTriggerFile.delete()) {
      logger.warn(
          "Failed to delete manual profile trigger file: {}", manualTriggerFile.getAbsolutePath());
      return;
    }

    logger.info("Manual profile trigger file detected, initiating profile recording");

    // Use the collection plan's duration if configured, otherwise fall back to the
    // file trigger's default duration setting.
    CollectionPlanConfiguration collectionPlan = currentConfig.getCollectionPlanConfiguration();
    int durationSeconds = collectionPlan.getImmediateProfilingDurationSeconds();
    if (durationSeconds <= 0) {
      durationSeconds = alertingProfileFileTriggerConfiguration.getDefaultProfileDurationSeconds();
    }

    AlertBreach alertBreach =
        AlertBreach.builder()
            .setType(AlertMetricType.MANUAL)
            .setAlertValue(0.0)
            .setAlertConfiguration(
                AlertConfiguration.builder()
                    .setType(AlertMetricType.MANUAL)
                    .setEnabled(true)
                    .setProfileDurationSeconds(durationSeconds)
                    .build())
            .setProfileId(UUID.randomUUID().toString())
            .setCpuMetric(0)
            .setMemoryUsage(0)
            .build();
    alertHandler.accept(alertBreach);
  }

  public void setPipeline(AlertMetricType type, AlertPipeline alertPipeline) {
    alertPipelines.setAlertPipeline(type, alertPipeline);
  }

  public void setEnableRequestTriggerUpdates(boolean enableRequestTriggerUpdates) {
    this.enableRequestTriggerUpdates = enableRequestTriggerUpdates;
  }
}
