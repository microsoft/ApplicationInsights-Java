// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.profiler.config;

import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfigurationBuilder;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfigurationBuilder;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import com.microsoft.applicationinsights.alerting.config.DefaultConfigurationBuilder;
import com.microsoft.applicationinsights.profiler.ProfilerConfiguration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** Parses the configuration from the service profiler endpoint. */
public class AlertConfigParser {

  public static AlertingConfiguration parse(
      String cpuConfig, String memoryConfig, String defaultConfig, String collectionPlan) {
    return new AlertingConfiguration(
        parseFromCpu(cpuConfig),
        parseFromMemory(memoryConfig),
        parseDefaultConfiguration(defaultConfig),
        parseCollectionPlan(collectionPlan));
  }

  // --single --mode immediate --immediate-profiling-duration 120  --expiration 5249143304354868449
  // --settings-moniker Portal_b5bd7880-7406-4058-a6f8-3ea0102706b1
  private static CollectionPlanConfiguration parseCollectionPlan(@Nullable String collectionPlan) {
    if (collectionPlan == null) {
      return new CollectionPlanConfiguration(
          false, EngineMode.immediate, Instant.ofEpochMilli(0), 0, "");
    }

    String[] tokens = collectionPlan.split(" ");

    Map<String, ParseConfigValue<CollectionPlanConfigurationBuilder>> parsers = new HashMap<>();
    parsers.put(
        "single",
        new ParseConfigValue<>(false, (config, arg) -> config.setCollectionPlanSingle(true)));
    parsers.put(
        "mode",
        new ParseConfigValue<>(true, (config, arg) -> config.setMode(EngineMode.parse(arg))));
    parsers.put(
        "expiration",
        new ParseConfigValue<>(true, (config, arg) -> config.setExpiration(Long.parseLong(arg))));
    parsers.put(
        "immediate-profiling-duration",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setImmediateProfilingDuration(Long.parseLong(arg))));
    parsers.put(
        "settings-moniker",
        new ParseConfigValue<>(true, (config, arg) -> config.setSettingsMoniker(arg)));

    return parseConfig(new CollectionPlanConfigurationBuilder(), tokens, parsers)
        .createDefaultConfiguration();
  }

  public static DefaultConfiguration parseDefaultConfiguration(@Nullable String defaultConfig) {
    if (defaultConfig == null) {
      return new DefaultConfiguration(false, 0, 0);
    }

    String[] tokens = defaultConfig.split(" ");

    Map<String, ParseConfigValue<DefaultConfigurationBuilder>> parsers = new HashMap<>();
    parsers.put(
        "sampling-profiling-duration",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setSamplingProfileDuration(Long.parseLong(arg))));
    parsers.put(
        "sampling-rate",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setSamplingRate(Double.parseDouble(arg))));
    parsers.put(
        "sampling-enabled",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setSamplingEnabled(Boolean.parseBoolean(arg))));

    return parseConfig(new DefaultConfigurationBuilder(), tokens, parsers)
        .createDefaultConfiguration();
  }

  public static AlertConfiguration parseFromMemory(@Nullable String memoryConfig) {
    if (memoryConfig == null) {
      return new AlertConfiguration(AlertMetricType.MEMORY, false, 0f, 0, 0);
    }
    String[] tokens = memoryConfig.split(" ");

    Map<String, ParseConfigValue<AlertConfigurationBuilder>> parsers = new HashMap<>();
    parsers.put(
        "memory-threshold",
        new ParseConfigValue<>(true, (config, arg) -> config.setThreshold(Float.parseFloat(arg))));
    parsers.put(
        "memory-trigger-cooldown",
        new ParseConfigValue<>(true, (config, arg) -> config.setCooldown(Long.parseLong(arg))));
    parsers.put(
        "memory-trigger-profilingDuration",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setProfileDuration(Long.parseLong(arg))));
    parsers.put(
        "memory-trigger-enabled",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setEnabled(Boolean.parseBoolean(arg))));

    return parseConfig(new AlertConfigurationBuilder(), tokens, parsers)
        .setType(AlertMetricType.MEMORY)
        .createAlertConfiguration();
  }

  public static AlertConfiguration parseFromCpu(@Nullable String cpuConfig) {
    if (cpuConfig == null) {
      return new AlertConfiguration(AlertMetricType.CPU, false, 0f, 0, 0);
    }

    String[] tokens = cpuConfig.split(" ");

    Map<String, ParseConfigValue<AlertConfigurationBuilder>> parsers = new HashMap<>();
    parsers.put(
        "cpu-threshold",
        new ParseConfigValue<>(true, (config, arg) -> config.setThreshold(Float.parseFloat(arg))));
    parsers.put(
        "cpu-trigger-cooldown",
        new ParseConfigValue<>(true, (config, arg) -> config.setCooldown(Long.parseLong(arg))));
    parsers.put(
        "cpu-trigger-profilingDuration",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setProfileDuration(Long.parseLong(arg))));
    parsers.put(
        "cpu-trigger-enabled",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setEnabled(Boolean.parseBoolean(arg))));

    return parseConfig(new AlertConfigurationBuilder(), tokens, parsers)
        .setType(AlertMetricType.CPU)
        .createAlertConfiguration();
  }

  private interface ConfigParser<T> {
    T parse(T config, @Nullable String arg);
  }

  private static class ParseConfigValue<T> {
    private final boolean hasArg;
    private final ConfigParser<T> configParser;

    private ParseConfigValue(boolean hasArg, ConfigParser<T> configParser) {
      this.hasArg = hasArg;
      this.configParser = configParser;
    }
  }

  private static <T> T parseConfig(
      T builder, String[] tokens, Map<String, ParseConfigValue<T>> parsers) {
    for (int index = 0; index < tokens.length; index++) {
      if (tokens[index] != null && !tokens[index].isEmpty()) {
        ParseConfigValue<T> parser = parsers.get(tokens[index].replaceFirst("--", ""));

        if (parser != null) {
          if (parser.hasArg && index + 1 < tokens.length) {
            String arg = tokens[++index];
            parser.configParser.parse(builder, arg);
          } else if (!parser.hasArg) {
            parser.configParser.parse(builder, null);
          }
        }
      }
    }
    return builder;
  }

  public static AlertingConfiguration toAlertingConfig(
      ProfilerConfiguration profilerConfiguration) {
    return AlertConfigParser.parse(
        profilerConfiguration.getCpuTriggerConfiguration(),
        profilerConfiguration.getMemoryTriggerConfiguration(),
        profilerConfiguration.getDefaultConfiguration(),
        profilerConfiguration.getCollectionPlan());
  }

  private AlertConfigParser() {}
}
