// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import static com.microsoft.applicationinsights.agent.bootstrap.diagnostics.MsgId.FAIL_TO_SEND_STATSBEAT_ERROR;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class StatsbeatModule {

  private static final Logger logger = LoggerFactory.getLogger(BaseStatsbeat.class);

  private final ScheduledExecutorService scheduledExecutor =
      Executors.newSingleThreadScheduledExecutor(
          ThreadPoolUtils.createDaemonThreadFactory(BaseStatsbeat.class));

  private final CustomDimensions customDimensions;
  private final NetworkStatsbeat networkStatsbeat;
  private final AttachStatsbeat attachStatsbeat;
  private final FeatureStatsbeat featureStatsbeat;
  private final FeatureStatsbeat instrumentationStatsbeat;
  private final NonessentialStatsbeat nonessentialStatsbeat;
  private final AzureMetadataService azureMetadataService;

  private final AtomicBoolean started = new AtomicBoolean();

  private final AtomicBoolean shutdown = new AtomicBoolean();

  public StatsbeatModule() {
    customDimensions = new CustomDimensions();
    networkStatsbeat = new NetworkStatsbeat(customDimensions);
    attachStatsbeat = new AttachStatsbeat(customDimensions);
    featureStatsbeat = new FeatureStatsbeat(customDimensions, FeatureType.FEATURE);
    instrumentationStatsbeat = new FeatureStatsbeat(customDimensions, FeatureType.INSTRUMENTATION);
    nonessentialStatsbeat = new NonessentialStatsbeat(customDimensions);
    azureMetadataService = new AzureMetadataService(attachStatsbeat, customDimensions);
  }

  public void start(TelemetryClient telemetryClient, Configuration config) {
    if (telemetryClient.getStatsbeatConnectionString() == null) {
      logger.debug("Don't start StatsbeatModule when statsbeat connection string is null.");
      return;
    }

    if (started.getAndSet(true)) {
      throw new IllegalStateException("initialize already called");
    }

    if (config.internal.statsbeat.disabledAll) {
      // disabledAll is an internal emergency kill-switch to turn off Statsbeat completely when
      // something goes wrong.
      // this happens rarely.
      return;
    }

    long shortIntervalSeconds = config.internal.statsbeat.shortIntervalSeconds;
    long longIntervalSeconds = config.internal.statsbeat.longIntervalSeconds;

    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(networkStatsbeat, telemetryClient),
        shortIntervalSeconds,
        shortIntervalSeconds,
        TimeUnit.SECONDS);
    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(attachStatsbeat, telemetryClient),
        Math.min(60, longIntervalSeconds),
        longIntervalSeconds,
        TimeUnit.SECONDS);
    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(featureStatsbeat, telemetryClient),
        Math.min(60, longIntervalSeconds),
        longIntervalSeconds,
        TimeUnit.SECONDS);
    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(instrumentationStatsbeat, telemetryClient),
        Math.min(60, longIntervalSeconds),
        longIntervalSeconds,
        TimeUnit.SECONDS);

    ResourceProvider rp = customDimensions.getResourceProvider();
    // only turn on AzureMetadataService when the resource provider is VM or UNKNOWN.
    if (rp == ResourceProvider.RP_VM || rp == ResourceProvider.UNKNOWN) {
      // will only reach here the first time, after instance has been instantiated
      azureMetadataService.scheduleWithFixedDelay(longIntervalSeconds);
    }

    featureStatsbeat.trackConfigurationOptions(config);

    if (!config.preview.statsbeat.disabled) {
      scheduledExecutor.scheduleWithFixedDelay(
          new StatsbeatSender(nonessentialStatsbeat, telemetryClient),
          longIntervalSeconds,
          longIntervalSeconds,
          TimeUnit.SECONDS);
    } else {
      logger.debug("Non-essential Statsbeat is disabled.");
    }
  }

  public void shutdown() {
    // guarding against multiple shutdown calls because this can get called if statsbeat shuts down
    // early because it cannot reach breeze and later on real shut down (when running not as agent)
    if (!shutdown.getAndSet(true)) {
      logger.debug("Shutting down Statsbeat scheduler.");
      scheduledExecutor.shutdown();
      azureMetadataService.shutdown();
    }
  }

  public NetworkStatsbeat getNetworkStatsbeat() {
    return networkStatsbeat;
  }

  public FeatureStatsbeat getFeatureStatsbeat() {
    return featureStatsbeat;
  }

  public FeatureStatsbeat getInstrumentationStatsbeat() {
    return instrumentationStatsbeat;
  }

  public NonessentialStatsbeat getNonessentialStatsbeat() {
    return nonessentialStatsbeat;
  }

  /** Runnable which is responsible for calling the send method to transmit Statsbeat telemetry. */
  private static class StatsbeatSender implements Runnable {

    private final BaseStatsbeat statsbeat;
    private final TelemetryClient telemetryClient;

    private StatsbeatSender(BaseStatsbeat statsbeat, TelemetryClient telemetryClient) {
      this.statsbeat = statsbeat;
      this.telemetryClient = telemetryClient;
    }

    @Override
    public void run() {
      try {
        // For Linux Consumption Plan, connection string is lazily set.
        // There is no need to send statsbeat when cikey is empty.
        String customerIkey = telemetryClient.getInstrumentationKey();
        if (customerIkey == null || customerIkey.isEmpty()) {
          return;
        }
        statsbeat.send(telemetryClient);
      } catch (RuntimeException e) {
        try (MDC.MDCCloseable ignored = FAIL_TO_SEND_STATSBEAT_ERROR.makeActive()) {
          logger.error("Error occurred while sending statsbeat", e);
        }
      }
    }
  }
}
