package com.microsoft.applicationinsights;

import java.util.*;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.telemetry.*;
import org.junit.*;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

// TODO: Some of the tests should be expanded. currently we just doing sanity checks by verified that
// all events are sent, without validating their values that added by the client.
/**
 * Tests the interface of the telemetry client.
 */
public class TelemetryClientTests {

    // region Members

    private List<Telemetry> eventsSent;
    private static DefaultTelemetryClient client;
    private TelemetryChannel channel;

    // endregion Members

    // region Initialization

    @BeforeClass
    public static void classInitialize() {
        TelemetryConfiguration configuration = new TelemetryConfiguration();
        configuration.setInstrumentationKey("IKey");

        client = new DefaultTelemetryClient(configuration);
    }

    @Before
    public void testInitialize() {
        eventsSent = new LinkedList<Telemetry>();
        channel = mock(TelemetryChannel.class);

        // Setting the channel to add the sent telemetries to a collection, so they could be verified in tests.
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Telemetry telemetry = ((Telemetry) invocation.getArguments()[0]);
                eventsSent.add(telemetry);

                return null;
            }
        }).when(channel).send(Matchers.any(Telemetry.class));

        client.setChannel(channel);
    }

    // endregion Initialization

    // region Track tests

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
    public void testTrackTraceWithProperties() {
        Map<String, String> properties = new HashMap<String, String>() {{ put("key", "value"); }};
        client.trackTrace("Trace", properties);

        verifyAndGetLastEventSent();
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
        MetricTelemetry telemetry = new MetricTelemetry("Metric", 1);
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

    private Telemetry verifyAndGetLastEventSent() {
        verify(channel, times(1)).send(any(Telemetry.class));

        return eventsSent.get(0);
    }

    // endregion Private methods
}