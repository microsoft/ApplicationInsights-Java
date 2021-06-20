package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ApplicationInsightsDiagnosticsLogFilterTests {
    private ApplicationInsightsDiagnosticsLogFilter filter;

    private ILoggingEvent mockEvent;

    @BeforeEach
    public void setup() {
        DiagnosticsTestHelper.setIsAppSvcAttachForLoggingPurposes(true);
        filter = new ApplicationInsightsDiagnosticsLogFilter();
        mockEvent = mock(ILoggingEvent.class);
        when(mockEvent.getLevel()).thenReturn(Level.ERROR);
        when(mockEvent.getLoggerName()).thenReturn("test");
    }

    @AfterEach
    public void tearDown() {
        mockEvent = null;
        filter = null;
        DiagnosticsTestHelper.reset();
    }

    @Test
    public void neutralIfError() {
        assertEquals(FilterReply.NEUTRAL, filter.decide(mockEvent));
    }

    @Test
    public void denyIfInfo() {
        when(mockEvent.getLevel()).thenReturn(Level.INFO);
        assertEquals(FilterReply.DENY, filter.decide(mockEvent));
    }

    @Test
    public void neutralIfDiagnosticsLogger() {
        when(mockEvent.getLoggerName()).thenReturn(DiagnosticsHelper.DIAGNOSTICS_LOGGER_NAME);
        assertEquals(FilterReply.NEUTRAL, filter.decide(mockEvent));
    }
}
