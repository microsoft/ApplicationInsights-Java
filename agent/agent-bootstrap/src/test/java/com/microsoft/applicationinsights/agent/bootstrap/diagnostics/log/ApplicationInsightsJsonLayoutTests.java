package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsValueFinder;
import org.hamcrest.Matchers;
import org.junit.*;

import static com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log.ApplicationInsightsJsonLayout.CUSTOM_FIELDS_PROP_NAME;
import static com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log.ApplicationInsightsJsonLayout.FORMATTED_MESSAGE_ATTR_NAME;
import static com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log.ApplicationInsightsJsonLayout.LOGGER_ATTR_NAME;
import static com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log.ApplicationInsightsJsonLayout.TIMESTAMP_PROP_NAME;
import static com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log.ApplicationInsightsJsonLayout.UNKNOWN_VALUE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ApplicationInsightsJsonLayoutTests {

    private static final String LOG_MESSAGE = "test message";
    private static final String LOGGER_NAME = "test.logger";
    private static final long TIMESTAMP_VALUE = System.currentTimeMillis();

    private ApplicationInsightsJsonLayout ourLayout;

    private ILoggingEvent logEvent;

    @Before
    public void setup() {
        ourLayout = new ApplicationInsightsJsonLayout();
        ourLayout.valueFinders.clear();

        logEvent = mock(ILoggingEvent.class);
        when(logEvent.getLevel()).thenReturn(Level.ERROR);
        when(logEvent.getFormattedMessage()).thenReturn(LOG_MESSAGE);
        when(logEvent.getLoggerName()).thenReturn(LOGGER_NAME);
        when(logEvent.getThrowableProxy()).thenReturn(new ThrowableProxy(new Exception("testing")));
        when(logEvent.getTimeStamp()).thenReturn(TIMESTAMP_VALUE);
    }

    @After
    public void tearDown() {
        ourLayout = null;
    }

    @Test
    public void topLevelIncludesRequiredFields() {
        final Map<String, Object> jsonMap = ourLayout.toJsonMap(logEvent);
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(TIMESTAMP_PROP_NAME, String.valueOf(TIMESTAMP_VALUE))); // there is no timestamp format specified, so it just uses the raw long value.
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(LOGGER_ATTR_NAME, LOGGER_NAME));
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(FORMATTED_MESSAGE_ATTR_NAME, LOG_MESSAGE));
    }

    @Test
    public void addsDataFromFinders() {
        final String key = "mock-finder";
        final String value = "mock-value";

        final DiagnosticsValueFinder mockFinder = mock(DiagnosticsValueFinder.class);
        when(mockFinder.getName()).thenReturn(key);
        when(mockFinder.getValue()).thenReturn(value);
        ourLayout.valueFinders.add(mockFinder);

        final Map<String, Object> jsonMap = ourLayout.toJsonMap(logEvent);

        verify(mockFinder, atLeastOnce()).getName();
        verify(mockFinder, atLeastOnce()).getValue();
        assertThat((Map<String, Object>) jsonMap.get(CUSTOM_FIELDS_PROP_NAME), Matchers.<String, Object>hasEntry(key, value));
    }

    @Test
    public void nullOrEmptyValueWritesUnknownValue() {
        final String nKey = "f-null";
        final String eKey = "f-empty";

        final DiagnosticsValueFinder nullValueFinder = mock(DiagnosticsValueFinder.class);
        when(nullValueFinder.getName()).thenReturn(nKey);
        when(nullValueFinder.getValue()).thenReturn(null);
        ourLayout.valueFinders.add(nullValueFinder);

        final DiagnosticsValueFinder emptyValueFinder = mock(DiagnosticsValueFinder.class);
        when(emptyValueFinder.getName()).thenReturn(eKey);
        when(emptyValueFinder.getValue()).thenReturn("");
        ourLayout.valueFinders.add(emptyValueFinder);

        final Map<String, Object> jsonMap = ourLayout.toJsonMap(logEvent);

        Map<String, Object> propMap = (Map<String, Object>) jsonMap.get(CUSTOM_FIELDS_PROP_NAME);

        verify(nullValueFinder, atLeastOnce()).getName();
        verify(nullValueFinder, atLeastOnce()).getValue();
        verify(emptyValueFinder, atLeastOnce()).getName();
        verify(emptyValueFinder, atLeastOnce()).getValue();
        assertThat(propMap, Matchers.<String, Object>hasEntry(eKey, UNKNOWN_VALUE));
        assertThat(propMap, Matchers.<String, Object>hasEntry(nKey, UNKNOWN_VALUE));
    }

    @Test
    public void mdcOperationNameAppearsInProperties() {
        Map<String, String> map = new HashMap<>();
        map.put(DiagnosticsHelper.MDC_PROP_OPERATION, "test");
        when(logEvent.getMDCPropertyMap()).thenReturn(map);
        final Map<String, Object> jsonMap = (Map<String, Object>) ourLayout.toJsonMap(logEvent).get("properties");
        assertThat(jsonMap, Matchers.<String, Object>hasEntry("operation", "test"));
    }

}
