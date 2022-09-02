// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.logbackpatch;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class FixedWindowRollingPolicyTest {

  @Test
  public void shouldFailWithOriginalFixedWindowRollingPolicy() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
    appender.setContext(loggerContext);
    appender.setName("FILE");
    appender.setFile("test.log");

    ch.qos.logback.core.rolling.FixedWindowRollingPolicy rollingPolicy =
        new ch.qos.logback.core.rolling.FixedWindowRollingPolicy();
    rollingPolicy.setContext(loggerContext);
    rollingPolicy.setFileNamePattern(
        "/test%20-test/applicationinsights-extension-%d{yyyy-MM-dd}.%i.log.old");
    rollingPolicy.setMinIndex(1);
    rollingPolicy.setMaxIndex(10);
    rollingPolicy.setParent(appender);

    Assertions.assertThrows(NumberFormatException.class, rollingPolicy::start);
  }

  @Test
  public void shouldNotFailWithPatchedFixedWindowRollingPolicy() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
    appender.setContext(loggerContext);
    appender.setName("FILE");
    appender.setFile("test.log");

    FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
    rollingPolicy.setContext(loggerContext);
    rollingPolicy.setFileNamePattern(
        "/test%20-test/applicationinsights-extension-%d{yyyy-MM-dd}.%i.log.old");
    rollingPolicy.setMinIndex(1);
    rollingPolicy.setMaxIndex(10);
    rollingPolicy.setParent(appender);

    rollingPolicy.start();
  }
}
