// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** Parses the configuration from the service profiler endpoint. */
public class AlertConfigParser {

  static AlertingConfiguration parse(
      String cpuConfig, String memoryConfig, String defaultConfig, String collectionPlan) {
    return AlertingConfiguration.create(
        parseFromCpu(cpuConfig),
        parseFromMemory(memoryConfig),
        parseDefaultConfiguration(defaultConfig),
        parseCollectionPlan(collectionPlan));
  }

  // --single --mode immediate --immediate-profiling-duration 120  --expiration 5249143304354868449
  // --settings-moniker Portal_b5bd7880-7406-4058-a6f8-3ea0102706b1
  private static CollectionPlanConfiguration parseCollectionPlan(@Nullable String collectionPlan) {
    if (collectionPlan == null || collectionPlan.isEmpty()) {
      return CollectionPlanConfiguration.builder()
          .setSingle(false)
          .setMode(EngineMode.immediate)
          .setExpiration(Instant.ofEpochMilli(0))
          .setImmediateProfilingDurationSeconds(0)
          .setSettingsMoniker("")
          .build();
    }

    String[] tokens = collectionPlan.split(" ");

    Map<String, ParseConfigValue<CollectionPlanConfiguration.Builder>> parsers = new HashMap<>();
    parsers.put("single", new ParseConfigValue<>(false, (config, arg) -> config.setSingle(true)));
    parsers.put(
        "mode",
        new ParseConfigValue<>(true, (config, arg) -> config.setMode(EngineMode.parse(arg))));
    parsers.put(
        "expiration",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setExpiration(parseBinaryDate(Long.parseLong(arg)))));
    parsers.put(
        "immediate-profiling-duration",
        new ParseConfigValue<>(
            true,
            (config, arg) -> config.setImmediateProfilingDurationSeconds(Integer.parseInt(arg))));
    parsers.put(
        "settings-moniker",
        new ParseConfigValue<>(true, (config, arg) -> config.setSettingsMoniker(arg)));

    return parseConfig(CollectionPlanConfiguration.builder(), tokens, parsers).build();
  }

  static DefaultConfiguration parseDefaultConfiguration(@Nullable String defaultConfig) {
    if (defaultConfig == null) {
      return DefaultConfiguration.builder()
          .setSamplingEnabled(false)
          .setSamplingRate(0)
          .setSamplingProfileDuration(0)
          .build();
    }

    String[] tokens = defaultConfig.split(" ");

    Map<String, ParseConfigValue<DefaultConfiguration.Builder>> parsers = new HashMap<>();
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

    return parseConfig(DefaultConfiguration.builder(), tokens, parsers).build();
  }

  static AlertConfiguration parseFromMemory(@Nullable String memoryConfig) {
    if (memoryConfig == null) {

      return AlertConfiguration.builder()
          .setType(AlertMetricType.MEMORY)
          .setEnabled(false)
          .setThreshold(0f)
          .setProfileDurationSeconds(0)
          .setCooldownSeconds(0)
          .build();
    }
    String[] tokens = memoryConfig.split(" ");

    Map<String, ParseConfigValue<AlertConfiguration.Builder>> parsers = new HashMap<>();
    parsers.put(
        "memory-threshold",
        new ParseConfigValue<>(true, (config, arg) -> config.setThreshold(Float.parseFloat(arg))));
    parsers.put(
        "memory-trigger-cooldown",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setCooldownSeconds(Integer.parseInt(arg))));
    parsers.put(
        "memory-trigger-profilingDuration",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setProfileDurationSeconds(Integer.parseInt(arg))));
    parsers.put(
        "memory-trigger-enabled",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setEnabled(Boolean.parseBoolean(arg))));

    return parseConfig(AlertConfiguration.builder(), tokens, parsers)
        .setType(AlertMetricType.MEMORY)
        .build();
  }

  static AlertConfiguration parseFromCpu(@Nullable String cpuConfig) {

    if (cpuConfig == null) {
      return AlertConfiguration.builder()
          .setType(AlertMetricType.CPU)
          .setEnabled(false)
          .setThreshold(0f)
          .setProfileDurationSeconds(0)
          .setCooldownSeconds(0)
          .build();
    }

    String[] tokens = cpuConfig.split(" ");

    Map<String, ParseConfigValue<AlertConfiguration.Builder>> parsers = new HashMap<>();
    parsers.put(
        "cpu-threshold",
        new ParseConfigValue<>(true, (config, arg) -> config.setThreshold(Float.parseFloat(arg))));
    parsers.put(
        "cpu-trigger-cooldown",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setCooldownSeconds(Integer.parseInt(arg))));
    parsers.put(
        "cpu-trigger-profilingDuration",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setProfileDurationSeconds(Integer.parseInt(arg))));
    parsers.put(
        "cpu-trigger-enabled",
        new ParseConfigValue<>(
            true, (config, arg) -> config.setEnabled(Boolean.parseBoolean(arg))));

    return parseConfig(AlertConfiguration.builder(), tokens, parsers)
        .setType(AlertMetricType.CPU)
        .build();
  }

  private interface ConfigParser<T> {
    void parse(T config, @Nullable String arg);
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

  // visible for testing
  static Instant parseBinaryDate(long expiration) {
    long ticks = expiration & 0x3fffffffffffffffL;
    long seconds = ticks / 10000000L;
    long nanos = (ticks % 10000000L) * 100L;
    long offset = OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
    return Instant.ofEpochSecond(seconds + offset, nanos);
  }

  private AlertConfigParser() {}
}
