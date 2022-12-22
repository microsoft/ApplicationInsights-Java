// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.configuration.ConfigurationBuilder;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfiguration;
import com.microsoft.applicationinsights.agent.internal.configuration.RpConfigurationBuilder;
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
  private final DynamicConfigurator dynamicConfigurator;

  public static void startPolling(
      RpConfiguration rpConfiguration, DynamicConfigurator dynamicConfigurator) {
    Executors.newSingleThreadScheduledExecutor(
            ThreadPoolUtils.createDaemonThreadFactory(RpConfigurationPolling.class))
        .scheduleWithFixedDelay(
            new RpConfigurationPolling(rpConfiguration, dynamicConfigurator), 60, 60, SECONDS);
  }

  // visible for testing
  RpConfigurationPolling(RpConfiguration rpConfiguration, DynamicConfigurator dynamicConfigurator) {
    this.rpConfiguration = rpConfiguration;
    this.dynamicConfigurator = dynamicConfigurator;
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

        DynamicConfiguration config = dynamicConfigurator.getCurrentConfigCopy();

        if (!newRpConfiguration.connectionString.equals(rpConfiguration.connectionString)) {
          config.connectionString = newRpConfiguration.connectionString;
        }
        if (!Objects.equals(
            rpConfiguration.sampling.percentage, newRpConfiguration.sampling.percentage)) {
          config.sampling.percentage = newRpConfiguration.sampling.percentage;
        }
        if (!Objects.equals(
            rpConfiguration.sampling.requestsPerSecond,
            newRpConfiguration.sampling.requestsPerSecond)) {
          config.sampling.requestsPerSecond = newRpConfiguration.sampling.requestsPerSecond;
        }

        dynamicConfigurator.applyDynamicConfiguration(config);

        rpConfiguration = newRpConfiguration;
      }
    } catch (IOException e) {
      logger.error("Error occurred when polling json config file: {}", e.getMessage(), e);
    }
  }
}
