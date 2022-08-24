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

        if (!Objects.equals(
                newRpConfiguration.sampling.percentage, rpConfiguration.sampling.percentage)
            || !Objects.equals(
                newRpConfiguration.sampling.limitPerSecond,
                rpConfiguration.sampling.limitPerSecond)) {
          logger.debug("Updating sampling percentage");
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
