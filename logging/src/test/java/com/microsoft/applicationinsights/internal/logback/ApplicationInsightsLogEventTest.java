package com.microsoft.applicationinsights.internal.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.junit.Test;
import org.slf4j.Marker;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ApplicationInsightsLogEventTest {
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

    // The deprecated method is not used here
    @SuppressWarnings("deprecation")
    private static void testSeverityLevel(final Level level, SeverityLevel expected) {
        ILoggingEvent loggingEvent = new ILoggingEvent() {
            @Override
            public String getThreadName() {
                return null;
            }

            @Override
            public Level getLevel() {
                return level;
            }

            @Override
            public String getMessage() {
                return null;
            }

            @Override
            public Object[] getArgumentArray() {
                return new Object[0];
            }

            @Override
            public String getFormattedMessage() {
                return null;
            }

            @Override
            public String getLoggerName() {
                return null;
            }

            @Override
            public LoggerContextVO getLoggerContextVO() {
                return null;
            }

            @Override
            public IThrowableProxy getThrowableProxy() {
                return null;
            }

            @Override
            public StackTraceElement[] getCallerData() {
                return new StackTraceElement[0];
            }

            @Override
            public boolean hasCallerData() {
                return false;
            }

            @Override
            public Marker getMarker() {
                return null;
            }

            @Override
            public Map<String, String> getMDCPropertyMap() {
                return null;
            }

            @Override
            public Map<String, String> getMdc() {
                return null;
            }

            @Override
            public long getTimeStamp() {
                return 0;
            }

            @Override
            public void prepareForDeferredProcessing() {

            }
        };
        ApplicationInsightsLogEvent event = new com.microsoft.applicationinsights.internal.logback.ApplicationInsightsLogEvent(loggingEvent);

        assertEquals(expected, event.getNormalizedSeverityLevel());
    }
}