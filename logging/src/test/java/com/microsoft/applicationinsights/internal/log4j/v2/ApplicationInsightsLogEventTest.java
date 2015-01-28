package com.microsoft.applicationinsights.internal.log4j.v2;

import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public final class ApplicationInsightsLogEventTest {
    @Test
    public void testFatalSeverityLevel() {
        testSeverityLevel(Level.FATAL, SeverityLevel.Critical);
    }

    @Test
    public void testErrorSeverityLevel() {
        testSeverityLevel(Level.ERROR, SeverityLevel.Error);
    }

    @Test
    public void testInfoSeverityLevel() {
        testSeverityLevel(Level.INFO, SeverityLevel.Information);
    }

    @Test
    public void testWarningSeverityLevel() {
        testSeverityLevel(Level.WARN, SeverityLevel.Warning);
    }

    @Test
    public void testDebugSeverityLevel() {
        testSeverityLevel(Level.DEBUG, SeverityLevel.Verbose);
    }

    @Test
    public void testTraceSeverityLevel() {
        testSeverityLevel(Level.TRACE, SeverityLevel.Verbose);
    }

    @Test
    public void testAllSeverityLevel() {
        testSeverityLevel(Level.ALL, SeverityLevel.Verbose);
    }

    private static void testSeverityLevel(final Level level, SeverityLevel expected) {
        org.apache.logging.log4j.core.LogEvent logEvent = new LogEvent() {
            @Override
            public Map<String, String> getContextMap() {
                return null;
            }

            @Override
            public ThreadContext.ContextStack getContextStack() {
                return null;
            }

            @Override
            public String getLoggerFqcn() {
                return null;
            }

            @Override
            public Level getLevel() {
                return level;
            }

            @Override
            public String getLoggerName() {
                return null;
            }

            @Override
            public Marker getMarker() {
                return null;
            }

            @Override
            public Message getMessage() {
                return null;
            }

            @Override
            public long getTimeMillis() {
                return 0;
            }

            @Override
            public StackTraceElement getSource() {
                return null;
            }

            @Override
            public String getThreadName() {
                return null;
            }

            @Override
            public Throwable getThrown() {
                return null;
            }

            @Override
            public ThrowableProxy getThrownProxy() {
                return null;
            }

            @Override
            public boolean isEndOfBatch() {
                return false;
            }

            @Override
            public boolean isIncludeLocation() {
                return false;
            }

            @Override
            public void setEndOfBatch(boolean endOfBatch) {

            }

            @Override
            public void setIncludeLocation(boolean locationRequired) {

            }
        };
        ApplicationInsightsLogEvent event = new com.microsoft.applicationinsights.internal.log4j.v2.ApplicationInsightsLogEvent(logEvent);

        assertEquals(expected, event.getSeverityLevel());
    }
}