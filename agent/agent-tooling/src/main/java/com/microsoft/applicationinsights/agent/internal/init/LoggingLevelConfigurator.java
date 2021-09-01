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
    updateLoggerLevel(loggerContext.getLogger("reactor.netty"));
    updateLoggerLevel(loggerContext.getLogger("io.netty"));
    updateLoggerLevel(loggerContext.getLogger("io.grpc.Context"));
    updateLoggerLevel(loggerContext.getLogger("io.opentelemetry"));
    updateLoggerLevel(loggerContext.getLogger("muzzleMatcher"));
    updateLoggerLevel(loggerContext.getLogger("com.microsoft.applicationinsights"));
    updateLoggerLevel(loggerContext.getLogger(ROOT_LOGGER_NAME));
  }

  public void updateLoggerLevel(Logger logger) {
    Level loggerLevel;
    String name = logger.getName();
    if (name.startsWith("reactor.netty") || name.startsWith("io.netty")) {
      // never want to log reactor netty or netty at trace or debug, it's just way too verbose
      loggerLevel = getAtLeastInfoLevel(level);
    } else if (name.startsWith("io.grpc.Context")) {
      // never want to log io.grpc.Context at trace or debug, as it logs confusing stack trace that
      // looks like error but isn't
      loggerLevel = getAtLeastInfoLevel(level);
    } else if (name.startsWith("io.opentelemetry")) {
      // OpenTelemetry instrumentation debug log has lots of things that look like errors
      // which has been confusing customers, so only enable it when user configures "trace" level
      loggerLevel = getOpenTelemetryLevel(level);
    } else if (name.startsWith("muzzleMatcher")) {
      // muzzleMatcher logs at WARN level, so by default this is OFF, but enabled when DEBUG logging
      // is enabled
      loggerLevel = getMuzzleMatcherLevel(level);
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

  private static Level getOpenTelemetryLevel(Level level) {
    // bump DEBUG up to INFO, keep all others as-is
    return level == Level.DEBUG ? Level.INFO : level;
  }

  private static Level getOtherLibLevel(Level level) {
    // bump INFO up to WARN, keep all others as-is
    return level == Level.INFO ? Level.WARN : level;
  }

  // TODO need something more reliable, currently will log too much WARN if "muzzleMatcher" logger
  // name changes
  // muzzleMatcher logs at WARN level in order to make them visible, but really should only be
  // enabled when debugging
  private static Level getMuzzleMatcherLevel(Level level) {
    return level.toInt() <= Level.DEBUG.toInt() ? level : getMaxLevel(level, Level.ERROR);
  }

  private static Level getMaxLevel(Level level1, Level level2) {
    return level1.toInt() >= level2.toInt() ? level1 : level2;
  }
}
