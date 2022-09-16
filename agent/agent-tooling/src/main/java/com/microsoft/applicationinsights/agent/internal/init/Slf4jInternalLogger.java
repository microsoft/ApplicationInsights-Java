// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import io.opentelemetry.javaagent.bootstrap.InternalLogger;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jInternalLogger extends InternalLogger {

  static Slf4jInternalLogger create(String name) {
    return new Slf4jInternalLogger(name);
  }

  private final Logger logger;

  Slf4jInternalLogger(String name) {
    logger = LoggerFactory.getLogger(name);
  }

  @Override
  protected boolean isLoggable(InternalLogger.Level level) {
    switch (level) {
      case TRACE:
        return logger.isTraceEnabled();
      case DEBUG:
        return logger.isDebugEnabled();
      case INFO:
        return logger.isInfoEnabled();
      case WARN:
        return logger.isWarnEnabled();
      case ERROR:
        return logger.isErrorEnabled();
    }
    throw new IllegalStateException("Missing logging level value in switch");
  }

  @Override
  protected void log(InternalLogger.Level level, String message, @Nullable Throwable error) {
    switch (level) {
      case TRACE:
        logger.trace(message, error);
        return;
      case DEBUG:
        logger.debug(message, error);
        return;
      case INFO:
        logger.info(message, error);
        return;
      case WARN:
        logger.warn(message, error);
        return;
      case ERROR:
        logger.error(message, error);
        return;
    }
    throw new IllegalStateException("Missing logging level value in switch");
  }

  @Override
  protected String name() {
    return logger.getName();
  }
}
