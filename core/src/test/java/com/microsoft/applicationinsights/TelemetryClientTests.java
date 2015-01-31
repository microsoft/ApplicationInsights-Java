/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.LinkedList;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.telemetry.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.junit.Ignore;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

// TODO: Some of the tests should be expanded. currently we just doing sanity checks by verified that
// all events are sent, without validating their values that added by the client.
/**
 * Tests the interface of the telemetry client.
 */
public final class TelemetryClientTests {

    // region Members

    private TelemetryConfiguration configuration;
    private List<Telemetry> eventsSent;
    private TelemetryClient client;
    private TelemetryChannel channel;

    // endregion Members

    // region Initialization

    @Before
    public void testInitialize() {
        configuration = new TelemetryConfiguration();
        configuration.setInstrumentationKey("00000000-0000-0000-0000-000000000000");
        channel = mock(TelemetryChannel.class);
        configuration.setChannel(channel);

        eventsSent = new LinkedList<Telemetry>();
        // Setting the channel to add the sent telemetries to a collection, so they could be verified in tests.
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Telemetry telemetry = ((Telemetry) invocation.getArguments()[0]);
                eventsSent.add(telemetry);

                return null;
            }
        }).when(channel).send(Matchers.any(Telemetry.class));

        client = new TelemetryClient(configuration);
    }

    // endregion Initialization

    // region Track tests

    @Test(expected = IllegalArgumentException.class)
    public void testNullTrackTelemetry() {
        client.track(null);
    }

    @Test
    public void testTrackTelemetryWithDisabled() {
        configuration.setTrackingIsDisabled(true);
        Telemetry mockTelemetry = Mockito.mock(Telemetry.class);

        client.track(mockTelemetry);

        Mockito.verifyZeroInteractions(channel, mockTelemetry);
    }

    @Test
    public void testUseConfigurationInstrumentationKeyWithNull() {
        testUseConfigurationInstrumentatonKey(null);
    }

    @Test
    public void testUseConfigurationInstrumentationKeyWithEmpty() {
        testUseConfigurationInstrumentatonKey("");
    }

    @Test
    public void testMethodsOnTelemetryAreCalledWhenTracking() {
        TelemetryChannel mockChannel = Mockito.mock(TelemetryChannel.class);
        configuration.setChannel(mockChannel);

        TelemetryContext mockContext = new TelemetryContext();
        Telemetry mockTelemetry = Mockito.mock(Telemetry.class);
        Mockito.doReturn(mockContext).when(mockTelemetry).getContext();

        TelemetryClient telemetryClient = new TelemetryClient(configuration);

        telemetryClient.track(mockTelemetry);

        Mockito.verify(mockChannel, Mockito.times(1)).send(mockTelemetry);

        Mockito.verify(mockTelemetry, Mockito.times(1)).setTimestamp(any(Date.class));
        Mockito.verify(mockTelemetry, Mockito.times(1)).sanitize();
    }

    @Test
    public void testTelemetryContextsAreCalled() {
        ContextInitializer mockContextInitializer = Mockito.mock(ContextInitializer.class);
        configuration.getContextInitializers().add(mockContextInitializer);
        TelemetryInitializer mockTelemetryInitializer = Mockito.mock(TelemetryInitializer.class);
        configuration.getTelemetryInitializers().add(mockTelemetryInitializer);

        TelemetryContext mockContext = new TelemetryContext();
        Telemetry mockTelemetry = Mockito.mock(Telemetry.class);
        Mockito.doReturn(mockContext).when(mockTelemetry).getContext();
        client.track(mockTelemetry);

        Mockito.verify(mockContextInitializer, Mockito.times(1)).initialize(any(TelemetryContext.class));
        Mockito.verify(mockTelemetryInitializer, Mockito.times(1)).initialize(mockTelemetry);
    }

    @Test
    public void testTrackEventWithPropertiesAndMetrics() {
        Map<String, String> properties = new HashMap<String, String>() {{ put("key", "value"); }};
        Map<String, Double> metrics = new HashMap<String, Double>() {{ put("key", 1d); }};

        client.trackEvent("Event", properties, metrics);

        EventTelemetry telemetry = (EventTelemetry) verifyAndGetLastEventSent();
        Assert.assertTrue("Expected telemetry property not found", telemetry.getProperties().get("key").equalsIgnoreCase("value"));
        Assert.assertTrue("Expected telemetry property not found", 1d == telemetry.getMetrics().get("key"));
    }

    @Test
    public void testTrackEventWithName() {
        client.trackEvent("Event");

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackEventWithEventTelemetry() {
        EventTelemetry eventTelemetry = new EventTelemetry("Event");
        client.trackEvent(eventTelemetry);

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackTraceAll() {
        Map<String, String> properties = new HashMap<String, String>() {{ put("key", "value"); }};
        client.trackTrace("Trace", SeverityLevel.Error, properties);

        Telemetry telemetry = verifyAndGetLastEventSent();
        verifyTraceTelemetry(telemetry, SeverityLevel.Error, properties);
    }

    @Test
    public void testTrackTraceWithProperties() {
        Map<String, String> properties = new HashMap<String, String>() {{ put("key", "value"); }};
        client.trackTrace("Trace", null, properties);

        Telemetry telemetry = verifyAndGetLastEventSent();
        verifyTraceTelemetry(telemetry, null, properties);
    }

    @Test
    public void testTrackTraceWithSeverityLevel() {
        client.trackTrace("Trace", SeverityLevel.Critical);

        Telemetry telemetry = verifyAndGetLastEventSent();
        verifyTraceTelemetry(telemetry, SeverityLevel.Critical, null);
    }

    @Test
    public void testTrackTraceWithName() {
        client.trackTrace("Trace");

        verifyAndGetLastEventSent();}

    @Test
    public void testTrackTraceWithTraceTelemetry() {
        TraceTelemetry telemetry = new TraceTelemetry("Trace");
        client.trackTrace(telemetry);

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackMetricWithExpandedValues() {
        Map<String, String> properties = new HashMap<String, String>() {{ put("key", "value"); }};
        client.trackMetric("Metric", 1, 1, 1, 1, properties);

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackMetricWithNameAndValue() {
        client.trackMetric("Metric", 1);

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackMetricWithMetricTelemetry() {
        MetricTelemetry telemetry = new MetricTelemetry("Metric", 1.0);
        client.trackMetric(telemetry);

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackExceptionWithPropertiesAndMetrics() {
        Exception exception = new Exception("Exception");
        Map<String, String> properties = new HashMap<String, String>() {{ put("key", "value"); }};
        Map<String, Double> metrics = new HashMap<String, Double>() {{ put("key", 1d); }};

        client.trackException(exception, properties, metrics);

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackExceptionWithExceptionTelemetry() {
        ExceptionTelemetry telemetry = new ExceptionTelemetry(new Exception("Exception"));

        client.trackException(telemetry);

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackException() {
        Exception exception = new Exception("Exception");

        client.trackException(exception);

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackHttpRequest() {
        client.trackHttpRequest("Name", new Date(), 1, "200", true);

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackHttpRequestWithHttpRequestTelemetry() {
        HttpRequestTelemetry telemetry = new HttpRequestTelemetry("Name", new Date(), 1, "200", true);
        client.trackHttpRequest(telemetry);

        verifyAndGetLastEventSent();
    }

    @Test
    @Ignore("Not supported yet.")
    public void testTrackRemoteDependency(){ }

    @Test
    public void testTrackPageViewWithName() {
        client.trackPageView("PageName");

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrackPageViewWithPageViewTelemetry() {
        PageViewTelemetry telemetry = new PageViewTelemetry("PageName");
        client.trackPageView(telemetry);

        verifyAndGetLastEventSent();
    }

    @Test
    public void testTrack() {
        TraceTelemetry telemetry = new TraceTelemetry("test");
        client.track(telemetry);

        verifyAndGetLastEventSent();
    }

    // endregion Track tests

    // region Private methods

    private static void verifyTraceTelemetry(Telemetry telemetry, SeverityLevel expectedSeverityLevel, Map<String, String> expectedProperties) {
        assertNotNull(telemetry);
        assertTrue(telemetry instanceof TraceTelemetry);

        TraceTelemetry traceTelemetry = (TraceTelemetry)telemetry;
        assertEquals(traceTelemetry.getSeverityLevel(), expectedSeverityLevel);
        if (expectedProperties != null) {
            assertEquals(traceTelemetry.getContext().getProperties(), expectedProperties);
        }
    }

    private void testUseConfigurationInstrumentatonKey(String contextInstrumentationKey) {
        TelemetryConfiguration configuration = new TelemetryConfiguration();
        TelemetryChannel mockChannel = Mockito.mock(TelemetryChannel.class);
        configuration.setChannel(mockChannel);
        configuration.setInstrumentationKey("00000000-0000-0000-0000-000000000000");

        TelemetryContext mockContext = new TelemetryContext();
        mockContext.setInstrumentationKey(contextInstrumentationKey);
        Telemetry mockTelemetry = Mockito.mock(Telemetry.class);
        Mockito.doReturn(mockContext).when(mockTelemetry).getContext();

        TelemetryClient telemetryClient = new TelemetryClient(configuration);

        telemetryClient.track(mockTelemetry);

        Mockito.verify(mockChannel, Mockito.times(1)).send(mockTelemetry);
        assertEquals(mockContext.getInstrumentationKey(), "00000000-0000-0000-0000-000000000000");
    }

    private Telemetry verifyAndGetLastEventSent() {
        verify(channel, times(1)).send(any(Telemetry.class));

        return eventsSent.get(0);
    }

    // endregion Private methods
}