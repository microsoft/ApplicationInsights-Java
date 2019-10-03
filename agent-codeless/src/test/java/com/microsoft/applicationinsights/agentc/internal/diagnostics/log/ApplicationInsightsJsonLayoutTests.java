package com.microsoft.applicationinsights.agentc.internal.diagnostics.log;

import com.microsoft.applicationinsights.agentc.internal.diagnostics.DiagnosticsValueFinder;
import org.hamcrest.Matchers;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ApplicationInsightsJsonLayoutTests {

    public static final String UNKNOWN_VALUE = ApplicationInsightsJsonLayout.UNKNOWN_VALUE;

    private ApplicationInsightsJsonLayout ourLayout;
    private Map<String, Object> jsonMap = new HashMap<>();

    @Before
    public void setup() {
        ourLayout = new ApplicationInsightsJsonLayout();
    }

    @After
    public void tearDown() {
        ourLayout = null;
        jsonMap.clear();
    }

    @Test
    public void layoutAddsDataFromFinders() {
        ourLayout.valueFinders.clear();
        final String key = "mock-finder";
        final String value = "mock-value";

        final DiagnosticsValueFinder mockFinder = mock(DiagnosticsValueFinder.class);
        when(mockFinder.getName()).thenReturn(key);
        when(mockFinder.getValue()).thenReturn(value);
        ourLayout.valueFinders.add(mockFinder);

        ourLayout.addCustomDataToJsonMap(jsonMap, null);

        verify(mockFinder, atLeastOnce()).getName();
        verify(mockFinder, atLeastOnce()).getValue();
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(key, value));
    }

    @Test
    public void nullOrEmptyValueWritesUnknownValue() {
        ourLayout.valueFinders.clear();
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

        ourLayout.addCustomDataToJsonMap(jsonMap, null);

        verify(nullValueFinder, atLeastOnce()).getName();
        verify(nullValueFinder, atLeastOnce()).getValue();
        verify(emptyValueFinder, atLeastOnce()).getName();
        verify(emptyValueFinder, atLeastOnce()).getValue();
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(eKey, UNKNOWN_VALUE));
        assertThat(jsonMap, Matchers.<String, Object>hasEntry(nKey, UNKNOWN_VALUE));
    }

}
