// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.classicsdk.BytecodeUtilImpl;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.ConfigurationBuilder;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfiguration;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfigurationBuilder;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpConfigurationPolling implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(RpConfigurationPolling.class);

  private volatile RpConfiguration rpConfiguration;
  private final Configuration configuration;
  private final TelemetryClient telemetryClient;
  private final AppIdSupplier appIdSupplier;

  public static void startPolling(
      RpConfiguration rpConfiguration,
      Configuration configuration,
      TelemetryClient telemetryClient,
      AppIdSupplier appIdSupplier) {
    Executors.newSingleThreadScheduledExecutor(
            ThreadPoolUtils.createDaemonThreadFactory(RpConfigurationPolling.class))
        .scheduleWithFixedDelay(
            new RpConfigurationPolling(
                rpConfiguration, configuration, telemetryClient, appIdSupplier),
            60,
            60,
            SECONDS);
  }

  // visible for testing
  RpConfigurationPolling(
      RpConfiguration rpConfiguration,
      Configuration configuration,
      TelemetryClient telemetryClient,
      AppIdSupplier appIdSupplier) {
    this.rpConfiguration = rpConfiguration;
    this.configuration = configuration;
    this.telemetryClient = telemetryClient;
    this.appIdSupplier = appIdSupplier;
  }

  @Override
  public void run() {
    if (rpConfiguration.configPath == null) {
      logger.warn("rp configuration path is null");
      return;
    }
    if (!Files.exists(rpConfiguration.configPath)) {
      logger.warn("rp configuration path doesn't exist: {}", rpConfiguration.configPath);
      return;
    }
    try {
      BasicFileAttributes attributes =
          Files.readAttributes(rpConfiguration.configPath, BasicFileAttributes.class);
      FileTime fileTime = attributes.lastModifiedTime();
      if (rpConfiguration.lastModifiedTime != fileTime.toMillis()) {
        rpConfiguration.lastModifiedTime = fileTime.toMillis();
        RpConfiguration newRpConfiguration =
            RpConfigurationBuilder.loadJsonConfigFile(rpConfiguration.configPath);

        ConfigurationBuilder.overlayFromEnv(newRpConfiguration);

        if (!newRpConfiguration.connectionString.equals(rpConfiguration.connectionString)) {
          logger.debug(
              "Connection string from the JSON config file is overriding the previously configured connection string.");
          configuration.connectionString = newRpConfiguration.connectionString;
          telemetryClient.updateConnectionStrings(
              configuration.connectionString,
              configuration.internal.statsbeat.instrumentationKey,
              configuration.internal.statsbeat.endpoint);
          appIdSupplier.updateAppId();
        }

        boolean changed = false;
        if (!Objects.equals(
            rpConfiguration.sampling.percentage, newRpConfiguration.sampling.percentage)) {
          logger.debug(
              "Updating sampling percentage from {} to {}",
              rpConfiguration.sampling.percentage,
              newRpConfiguration.sampling.percentage);
          changed = true;
        }
        if (!Objects.equals(
            rpConfiguration.sampling.limitPerSecond, newRpConfiguration.sampling.limitPerSecond)) {
          logger.debug(
              "Updating limit per second from {} to {}",
              rpConfiguration.sampling.limitPerSecond,
              newRpConfiguration.sampling.limitPerSecond);
          changed = true;
        }
        if (changed) {
          configuration.sampling.percentage = newRpConfiguration.sampling.percentage;
          configuration.sampling.limitPerSecond = newRpConfiguration.sampling.limitPerSecond;
          DelegatingSampler.getInstance().setDelegate(Samplers.getSampler(configuration));
          if (configuration.sampling.percentage != null) {
            BytecodeUtilImpl.samplingPercentage = configuration.sampling.percentage.floatValue();
          } else {
            BytecodeUtilImpl.samplingPercentage = 100;
          }
        }
        rpConfiguration = newRpConfiguration;
      }
    } catch (IOException e) {
      logger.error("Error occurred when polling json config file: {}", e.getMessage(), e);
    }
  }
}
