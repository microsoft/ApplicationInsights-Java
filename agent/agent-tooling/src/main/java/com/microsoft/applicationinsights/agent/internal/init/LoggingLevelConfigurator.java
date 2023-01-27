// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.Locale;

public class LoggingLevelConfigurator {

  private final Level level;

  public LoggingLevelConfigurator(String levelStr) {
    try {
      this.level = Level.valueOf(levelStr.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Unexpected self-diagnostic level: " + levelStr, e);
    }
  }

  public void initLoggerLevels(LoggerContext loggerContext) {
    updateLoggerLevel(loggerContext.getLogger("oshi"));
    updateLoggerLevel(loggerContext.getLogger("reactor.netty"));
    updateLoggerLevel(loggerContext.getLogger("reactor.util"));
    updateLoggerLevel(loggerContext.getLogger("io.netty"));
    updateLoggerLevel(loggerContext.getLogger("io.netty.handler.ssl"));
    updateLoggerLevel(loggerContext.getLogger("io.grpc.Context"));
    updateLoggerLevel(loggerContext.getLogger("io.opentelemetry.javaagent.tooling.VersionLogger"));
    updateLoggerLevel(loggerContext.getLogger("io.opentelemetry.exporter.logging"));
    updateLoggerLevel(
        loggerContext.getLogger("io.opentelemetry.sdk.metrics.internal.state.DeltaMetricStorage"));
    updateLoggerLevel(loggerContext.getLogger("io.opentelemetry"));
    updateLoggerLevel(loggerContext.getLogger("muzzleMatcher"));
    updateLoggerLevel(
        loggerContext.getLogger("com.azure.core.implementation.jackson.MemberNameConverterImpl"));
    // TODO (trask) revisit this after https://github.com/Azure/azure-sdk-for-java/pull/24843
    updateLoggerLevel(
        loggerContext.getLogger("com.azure.core.implementation.jackson.JacksonVersion"));
    updateLoggerLevel(loggerContext.getLogger("com.azure.core"));
    updateLoggerLevel(loggerContext.getLogger("com.microsoft.applicationinsights"));
    updateLoggerLevel(loggerContext.getLogger(ROOT_LOGGER_NAME));
  }

  public void updateLoggerLevel(Logger logger) {
    Level loggerLevel;
    String name = logger.getName();
    if (name.startsWith("oshi")) {
      loggerLevel = getOshiLoggerLevel(level);
    } else if (name.startsWith("io.netty.handler.ssl")) {
      loggerLevel = getDefaultLibraryLevel(level);
    } else if (name.startsWith("reactor.netty") || name.startsWith("io.netty")) {
      loggerLevel = getNettyLevel(level);
    } else if (name.startsWith("reactor.util")) {
      loggerLevel = getDefaultLibraryLevel(level);
    } else if (name.equals("io.grpc.Context")) {
      // never want to log at trace or debug, as it logs confusing stack trace that
      // looks like error but isn't
      loggerLevel = getAtLeastInfoLevel(level);
    } else if (name.equals("io.opentelemetry.javaagent.tooling.VersionLogger")) {
      // TODO (trask) currently otel version relies on manifest so incorrect in this distro
      loggerLevel = getAtLeastWarnLevel(level);
    } else if (name.startsWith("io.opentelemetry.exporter.logging")) {
      // in case user enables OpenTelemetry logging exporters via otel.traces.exporter,
      // otel.metrics.exporter, or otel.logs.exporter
      loggerLevel = level;
    } else if (name.equals("io.opentelemetry.sdk.metrics.internal.state.DeltaMetricStorage")) {
      loggerLevel = getDeltaMetricStorageLoggerLevel(level);
    } else if (name.startsWith("io.opentelemetry")) {
      // OpenTelemetry instrumentation debug log has lots of things that look like errors
      // which has been confusing customers, so only enable it when user configures "trace" level
      loggerLevel = getDefaultLibraryLevel(level);
    } else if (name.equals("com.azure.core.implementation.jackson.MemberNameConverterImpl")) {
      // never want to log at trace or debug, as it logs confusing stack trace that
      // looks like error but isn't
      loggerLevel = getAtLeastInfoLevel(level);
    } else if (name.equals("com.azure.core.implementation.jackson.JacksonVersion")) {
      // TODO (trask) revisit this after https://github.com/Azure/azure-sdk-for-java/pull/24843
      // need to suppress ERROR messages from this class since it cannot find the real jackson
      // version due to shading
      loggerLevel = Level.OFF;
    } else if (name.startsWith("com.azure.core")) {
      loggerLevel = getDefaultLibraryLevel(level);
    } else if (name.startsWith("com.microsoft.applicationinsights")) {
      loggerLevel = level;
    } else {
      loggerLevel = getOtherLibLevel(level);
    }
    logger.setLevel(loggerLevel);
  }

  // never want to log apache http at trace or debug, it's just way to verbose
  private static Level getAtLeastInfoLevel(Level level) {
    return getMaxLevel(level, Level.INFO);
  }

  private static Level getAtLeastWarnLevel(Level level) {
    return getMaxLevel(level, Level.WARN);
  }

  private static Level getNettyLevel(Level level) {
    if (level == Level.DEBUG || level == Level.TRACE) {
      // never want to log reactor netty or netty at trace or debug, it's just way too verbose
      return Level.INFO;
    } else if (level == Level.INFO || level == Level.WARN) {
      // suppress spammy reactor-netty WARN logs
      return Level.ERROR;
    } else {
      return level;
    }
  }

  private static Level getOshiLoggerLevel(Level level) {
    if (level == Level.INFO || level == Level.WARN || level == Level.ERROR) {
      return Level.OFF;
    } else if (level == Level.DEBUG) {
      return Level.INFO;
    }
    return level;
  }

  private static Level getDeltaMetricStorageLoggerLevel(Level level) {
    if (level == Level.INFO || level == Level.WARN) {
      return Level.ERROR;
    }
    return level;
  }

  private static Level getDefaultLibraryLevel(Level level) {
    if (level == Level.INFO) {
      return Level.WARN;
    } else if (level == Level.DEBUG) {
      // use "trace" level to enable verbose debugging
      return Level.INFO;
    } else {
      return level;
    }
  }

  private static Level getOtherLibLevel(Level level) {
    // bump INFO up to WARN, keep all others as-is
    return level == Level.INFO ? Level.WARN : level;
  }

  private static Level getMaxLevel(Level level1, Level level2) {
    return level1.toInt() >= level2.toInt() ? level1 : level2;
  }
}
