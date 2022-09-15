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
    return logger.isEnabledForLevel(toSlf4jLevel(level));
  }

  @Override
  protected void log(InternalLogger.Level level, String message, @Nullable Throwable error) {
    logger.makeLoggingEventBuilder(toSlf4jLevel(level)).setCause(error).log(message);
  }

  @Override
  protected String name() {
    return logger.getName();
  }

  private static org.slf4j.event.Level toSlf4jLevel(InternalLogger.Level level) {
    switch (level) {
      case ERROR:
        return org.slf4j.event.Level.ERROR;
      case WARN:
        return org.slf4j.event.Level.WARN;
      case INFO:
        return org.slf4j.event.Level.INFO;
      case DEBUG:
        return org.slf4j.event.Level.DEBUG;
      case TRACE:
        return org.slf4j.event.Level.TRACE;
    }
    throw new IllegalStateException("Missing logging level value in switch");
  }
}
