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
