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

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationInsightsDiagnosticsLogFilterTests {

  private ApplicationInsightsDiagnosticsLogFilter filter;

  private ILoggingEvent mockEvent;

  @BeforeEach
  void setup() {
    DiagnosticsTestHelper.setIsAppSvcAttachForLoggingPurposes(true);
    filter = new ApplicationInsightsDiagnosticsLogFilter();
    mockEvent = mock(ILoggingEvent.class);
    when(mockEvent.getLevel()).thenReturn(Level.ERROR);
    when(mockEvent.getLoggerName()).thenReturn("test");
  }

  @AfterEach
  void tearDown() {
    mockEvent = null;
    filter = null;
    DiagnosticsTestHelper.reset();
  }

  @Test
  void neutralIfError() {
    assertThat(filter.decide(mockEvent)).isEqualTo(FilterReply.NEUTRAL);
  }

  @Test
  void denyIfInfo() {
    when(mockEvent.getLevel()).thenReturn(Level.INFO);
    assertThat(filter.decide(mockEvent)).isEqualTo(FilterReply.DENY);
  }

  @Test
  void neutralIfDiagnosticsLogger() {
    when(mockEvent.getLoggerName()).thenReturn(DiagnosticsHelper.DIAGNOSTICS_LOGGER_NAME);
    assertThat(filter.decide(mockEvent)).isEqualTo(FilterReply.NEUTRAL);
  }
}
