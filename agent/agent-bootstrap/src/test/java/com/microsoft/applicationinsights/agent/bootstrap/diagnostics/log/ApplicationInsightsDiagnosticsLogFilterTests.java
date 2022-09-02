// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsTestHelper;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationInsightsDiagnosticsLogFilterTests {

  @Nullable private ApplicationInsightsDiagnosticsLogFilter filter;

  @Nullable private ILoggingEvent mockEvent;

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
