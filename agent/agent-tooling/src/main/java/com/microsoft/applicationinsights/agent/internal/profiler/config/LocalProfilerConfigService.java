// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Checks a local directory for a profiler configuration file. If a valid file exists, it takes
 * precedence over the remote Azure service profiler configuration.
 *
 * <p>The configuration file must be named {@code profiler-config.json} and use the same JSON format
 * as {@link ProfilerConfiguration}.
 */
public class LocalProfilerConfigService {

  private static final Logger logger = LoggerFactory.getLogger(LocalProfilerConfigService.class);

  static final String CONFIG_FILE_NAME = "profiler-config.json";

  private final File configFile;
  private volatile long lastModifiedTime;

  public LocalProfilerConfigService(File configDir) {
    this.configFile = new File(configDir, CONFIG_FILE_NAME);
    this.lastModifiedTime = 0;
  }

  /**
   * Checks for a local profiler configuration file. Returns a configuration if the file exists and
   * has been modified since the last check. Returns empty if no file exists or if it has not
   * changed.
   *
   * @return Mono containing the configuration if changed, or Mono.error if the file is malformed
   */
  public Mono<ProfilerConfiguration> pullSettings() {
    if (!configFile.exists()) {
      return Mono.empty();
    }

    long fileLastModified = configFile.lastModified();
    if (fileLastModified == lastModifiedTime) {
      // File has not changed since last successful read
      return Mono.empty();
    }

    try {
      ProfilerConfiguration config = readConfigFile();
      logger.info(
          "Successfully read local profiler configuration from: {}", configFile.getAbsolutePath());

      // Delete the file after successful read - this is a one-shot configuration mechanism
      if (!configFile.delete()) {
        logger.warn(
            "Failed to delete local profiler configuration file after reading: {}",
            configFile.getAbsolutePath());
      } else {
        logger.info(
            "Deleted local profiler configuration file after successful read: {}",
            configFile.getAbsolutePath());
      }

      lastModifiedTime = fileLastModified;
      return Mono.just(config);
    } catch (Exception e) {
      logger.error(
          "Failed to parse local profiler configuration file: {}. "
              + "Fix or remove the file to restore normal operation.",
          configFile.getAbsolutePath(),
          e);
      return Mono.error(e);
    }
  }

  /**
   * Returns true if a local config file is present (regardless of whether it has changed), meaning
   * local override mode is active.
   */
  public boolean isLocalConfigPresent() {
    return configFile.exists();
  }

  private ProfilerConfiguration readConfigFile() throws IOException {
    try (Reader reader =
            new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8);
        JsonReader jsonReader = JsonProviders.createReader(reader)) {

      ProfilerConfiguration config = ProfilerConfiguration.fromJson(jsonReader);

      // Default lastModified from file timestamp if not provided
      if (config.getLastModified() == null
          || config.getLastModified().compareTo(ProfilerConfiguration.DEFAULT_DATE) == 0) {
        config.setLastModified(new Date(configFile.lastModified()));
      }

      // Default enabledLastModified if not set
      if (config.getEnabledLastModified() == null) {
        config.setEnabledLastModified(config.getLastModified());
      }

      // Default requestTriggerConfiguration to empty list if null
      if (config.getRequestTriggerConfiguration() == null) {
        config.setRequestTriggerConfiguration(new ArrayList<>());
      }

      return config;
    }
  }

  // visible for testing
  @Nullable
  File getConfigFile() {
    return configFile;
  }
}
