// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

// TODO (trask) ETW: is this really needed? if so, need to restore devtest configuration
public class DiagnosticsLoggerProxy implements Logger {
  // Hardcoded to avoid dependency:
  // com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper.DIAGNOSTICS_LOGGER_NAME
  private static final Logger LOGGER =
      LoggerFactory.getLogger("applicationinsights.extension.diagnostics");

  @Override
  public String getName() {
    return LOGGER.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return LOGGER.isTraceEnabled();
  }

  @Override
  public void trace(String msg) {
    LOGGER.trace(msg);
  }

  @Override
  public void trace(String format, Object arg) {
    LOGGER.trace(format, arg);
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    LOGGER.trace(format, arg1, arg2);
  }

  @Override
  public void trace(String format, Object... arguments) {
    LOGGER.trace(format, arguments);
  }

  @Override
  public void trace(String msg, Throwable t) {
    LOGGER.trace(msg, t);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return LOGGER.isTraceEnabled(marker);
  }

  @Override
  public void trace(Marker marker, String msg) {
    LOGGER.trace(marker, msg);
  }

  @Override
  public void trace(Marker marker, String format, Object arg) {
    LOGGER.trace(marker, format, arg);
  }

  @Override
  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    LOGGER.trace(marker, format, arg1, arg2);
  }

  @Override
  public void trace(Marker marker, String format, Object... argArray) {
    LOGGER.trace(marker, format, argArray);
  }

  @Override
  public void trace(Marker marker, String msg, Throwable t) {
    LOGGER.trace(marker, msg, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return LOGGER.isDebugEnabled();
  }

  @Override
  public void debug(String msg) {
    LOGGER.debug(msg);
  }

  @Override
  public void debug(String format, Object arg) {
    LOGGER.debug(format, arg);
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    LOGGER.debug(format, arg1, arg2);
  }

  @Override
  public void debug(String format, Object... arguments) {
    LOGGER.debug(format, arguments);
  }

  @Override
  public void debug(String msg, Throwable t) {
    LOGGER.debug(msg, t);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return LOGGER.isDebugEnabled(marker);
  }

  @Override
  public void debug(Marker marker, String msg) {
    LOGGER.debug(marker, msg);
  }

  @Override
  public void debug(Marker marker, String format, Object arg) {
    LOGGER.debug(marker, format, arg);
  }

  @Override
  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    LOGGER.debug(marker, format, arg1, arg2);
  }

  @Override
  public void debug(Marker marker, String format, Object... arguments) {
    LOGGER.debug(marker, format, arguments);
  }

  @Override
  public void debug(Marker marker, String msg, Throwable t) {
    LOGGER.debug(marker, msg, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return LOGGER.isInfoEnabled();
  }

  @Override
  public void info(String msg) {
    LOGGER.info(msg);
  }

  @Override
  public void info(String format, Object arg) {
    LOGGER.info(format, arg);
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    LOGGER.info(format, arg1, arg2);
  }

  @Override
  public void info(String format, Object... arguments) {
    LOGGER.info(format, arguments);
  }

  @Override
  public void info(String msg, Throwable t) {
    LOGGER.info(msg, t);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return LOGGER.isInfoEnabled(marker);
  }

  @Override
  public void info(Marker marker, String msg) {
    LOGGER.info(marker, msg);
  }

  @Override
  public void info(Marker marker, String format, Object arg) {
    LOGGER.info(marker, format, arg);
  }

  @Override
  public void info(Marker marker, String format, Object arg1, Object arg2) {
    LOGGER.info(marker, format, arg1, arg2);
  }

  @Override
  public void info(Marker marker, String format, Object... arguments) {
    LOGGER.info(marker, format, arguments);
  }

  @Override
  public void info(Marker marker, String msg, Throwable t) {
    LOGGER.info(marker, msg, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return LOGGER.isWarnEnabled();
  }

  @Override
  public void warn(String msg) {
    LOGGER.warn(msg);
  }

  @Override
  public void warn(String format, Object arg) {
    LOGGER.warn(format, arg);
  }

  @Override
  public void warn(String format, Object... arguments) {
    LOGGER.warn(format, arguments);
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    LOGGER.warn(format, arg1, arg2);
  }

  @Override
  public void warn(String msg, Throwable t) {
    LOGGER.warn(msg, t);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return LOGGER.isWarnEnabled(marker);
  }

  @Override
  public void warn(Marker marker, String msg) {
    LOGGER.warn(marker, msg);
  }

  @Override
  public void warn(Marker marker, String format, Object arg) {
    LOGGER.warn(marker, format, arg);
  }

  @Override
  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    LOGGER.warn(marker, format, arg1, arg2);
  }

  @Override
  public void warn(Marker marker, String format, Object... arguments) {
    LOGGER.warn(marker, format, arguments);
  }

  @Override
  public void warn(Marker marker, String msg, Throwable t) {
    LOGGER.warn(marker, msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return LOGGER.isErrorEnabled();
  }

  @Override
  public void error(String msg) {
    LOGGER.error(msg);
  }

  @Override
  public void error(String format, Object arg) {
    LOGGER.error(format, arg);
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    LOGGER.error(format, arg1, arg2);
  }

  @Override
  public void error(String format, Object... arguments) {
    LOGGER.error(format, arguments);
  }

  @Override
  public void error(String msg, Throwable t) {
    LOGGER.error(msg, t);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return LOGGER.isErrorEnabled(marker);
  }

  @Override
  public void error(Marker marker, String msg) {
    LOGGER.error(marker, msg);
  }

  @Override
  public void error(Marker marker, String format, Object arg) {
    LOGGER.error(marker, format, arg);
  }

  @Override
  public void error(Marker marker, String format, Object arg1, Object arg2) {
    LOGGER.error(marker, format, arg1, arg2);
  }

  @Override
  public void error(Marker marker, String format, Object... arguments) {
    LOGGER.error(marker, format, arguments);
  }

  @Override
  public void error(Marker marker, String msg, Throwable t) {
    LOGGER.error(marker, msg, t);
  }
}
