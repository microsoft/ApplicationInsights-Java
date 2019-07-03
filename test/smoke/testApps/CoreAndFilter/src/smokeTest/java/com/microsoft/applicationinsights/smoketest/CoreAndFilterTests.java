package com.microsoft.applicationinsights.smoketest;

import java.util.Comparator;
import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import com.microsoft.applicationinsights.internal.schemav2.PageViewData;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers;
import com.microsoft.applicationinsights.smoketest.matchers.PageViewDataMatchers;
import com.microsoft.applicationinsights.smoketest.matchers.TraceDataMatchers;
import com.microsoft.applicationinsights.telemetry.Duration;
import org.junit.Ignore;
import org.junit.Test;

import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.ExceptionDetailsMatchers.withMessage;
import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.hasException;
import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.hasMeasurement;
import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.hasSeverityLevel;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasDuration;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasName;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasResponseCode;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasSuccess;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasUrl;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@UseAgent
public class CoreAndFilterTests extends AiSmokeTest {

    @Test
    @TargetUri("/trackDependency")
    public void trackDependency() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        final String expectedName = "DependencyTest";
        final String expectedData = "commandName";
        final Duration expectedDuration = new Duration(0, 0, 1, 1, 1);

        assertEquals(expectedName, rdd.getName());
        assertEquals(expectedData, rdd.getData());
        assertEquals(expectedDuration, rdd.getDuration());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    @Test
    @TargetUri("/trackEvent")
    public void testTrackEvent() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> edList = mockedIngestion.waitForItems("EventData", 2);

        Envelope rdEnvelope = rdList.get(0);
        Envelope edEnvelope1 = edList.get(0);
        Envelope edEnvelope2 = edList.get(1);

        List<EventData> events = mockedIngestion.getTelemetryDataByType("EventData");
        events.sort(new Comparator<EventData>() {
            @Override
            public int compare(EventData o1, EventData o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        EventData ed1 = events.get(0);
        EventData ed2 = events.get(1);

        assertEquals("EventDataPropertyTest", ed1.getName());
        assertEquals("value", ed1.getProperties().get("key"));
        assertEquals((Double) 1.0, ed1.getMeasurements().get("key"));

        assertEquals("EventDataTest", ed2.getName());

        assertSameOperationId(rdEnvelope, edEnvelope1);
        assertSameOperationId(rdEnvelope, edEnvelope2);
    }

    @Ignore
    @Test
    @TargetUri("/trackException")
    public void testTrackException() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> edList = mockedIngestion.waitForItems("ExceptionData", 3);

        Envelope rdEnvelope = rdList.get(0);
        Envelope edEnvelope1 = edList.get(0);
        Envelope edEnvelope2 = edList.get(1);
        Envelope edEnvelope3 = edList.get(2);

        final String expectedName = "This is track exception.";
        final String expectedProperties = "value";
        final Double expectedMetrice = 1d;

        final List<ExceptionData> exceptions = mockedIngestion.getTelemetryDataByType("ExceptionData");
        assertThat(exceptions, hasItem(hasException(withMessage(expectedName))));
        assertThat(exceptions, hasItem(allOf(
                hasException(withMessage(expectedName)),
                ExceptionDataMatchers.hasProperty("key", expectedProperties),
                hasMeasurement("key", expectedMetrice))));
        assertThat(exceptions, hasItem(allOf(
                hasException(withMessage(expectedName)),
                hasSeverityLevel(SeverityLevel.Error)
        )));

        assertSameOperationId(rdEnvelope, edEnvelope1);
        assertSameOperationId(rdEnvelope, edEnvelope2);
        assertSameOperationId(rdEnvelope, edEnvelope3);
    }

    @Ignore
    @Test
    @TargetUri("/trackHttpRequest")
    public void testHttpRequest() throws Exception {
        mockedIngestion.waitForItems("RequestData", 5);

        int totalItems = mockedIngestion.getItemCount();
        int expectedItems = 5;
        assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
                expectedItems, totalItems);

        // TODO get HttpRequest data envelope and verify value
        final List<Domain> requests = mockedIngestion.getTelemetryDataByType("RequestData");
        //true
        assertThat(requests, hasItem(allOf(
                hasName("HttpRequestDataTest"),
                hasResponseCode("200"),
                hasDuration(new Duration(4711)),
                hasSuccess(true))));
        assertThat(requests, hasItem(allOf(
                hasName("PingTest"),
                hasResponseCode("200"),
                hasDuration(new Duration(1)),
                hasSuccess(true),
                hasUrl("http://tempuri.org/ping")
        )));

        //false
        assertThat(requests, hasItem(allOf(
                hasName("FailedHttpRequest"),
                hasResponseCode("404"),
                hasDuration(new Duration(6666)),
                hasSuccess(false)
        )));
        assertThat(requests, hasItem(allOf(
                hasName("FailedHttpRequest2"),
                hasResponseCode("505"),
                hasDuration(new Duration(8888)),
                hasSuccess(false),
                hasUrl("https://www.bingasdasdasdasda.com/")
        )));
    }

    @Test
    @TargetUri("/trackMetric")
    public void trackMetric() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> mdList = mockedIngestion.waitForItems("MetricData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope mdEnvelope = mdList.get(0);

        MetricData md = (MetricData) ((Data) mdEnvelope.getData()).getBaseData();

        List<DataPoint> metrics = md.getMetrics();
        assertEquals(1, metrics.size());
        DataPoint dp = metrics.get(0);

        final double expectedValue = 111222333.0;
        final double epsilon = Math.ulp(expectedValue);
        assertEquals(DataPointType.Measurement, dp.getKind());
        assertEquals(expectedValue, dp.getValue(), epsilon);
        assertEquals("TimeToRespond", dp.getName());

        assertNull("getCount was non-null", dp.getCount());
        assertNull("getMin was non-null", dp.getMin());
        assertNull("getMax was non-null", dp.getMax());
        assertNull("getStdDev was non-null", dp.getStdDev());

        assertSameOperationId(rdEnvelope, mdEnvelope);
    }

    @Ignore
    @Test
    @TargetUri("/trackTrace")
    public void testTrackTrace() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> mdList = mockedIngestion.waitForItems("MessageData", 3);

        Envelope rdEnvelope = rdList.get(0);
        Envelope mdEnvelope1 = mdList.get(0);
        Envelope mdEnvelope2 = mdList.get(1);
        Envelope mdEnvelope3 = mdList.get(2);

        final List<MessageData> messages = mockedIngestion.getTelemetryDataByType("MessageData");
        assertThat(messages, hasItem(
                TraceDataMatchers.hasMessage("This is first trace message.")
        ));

        assertThat(messages, hasItem(allOf(
                TraceDataMatchers.hasMessage("This is second trace message."),
                TraceDataMatchers.hasSeverityLevel(SeverityLevel.Error)
        )));

        assertThat(messages, hasItem(allOf(
                TraceDataMatchers.hasMessage("This is third trace message."),
                TraceDataMatchers.hasSeverityLevel(SeverityLevel.Information),
                TraceDataMatchers.hasProperty("key", "value")
        )));

        assertSameOperationId(rdEnvelope, mdEnvelope1);
        assertSameOperationId(rdEnvelope, mdEnvelope2);
        assertSameOperationId(rdEnvelope, mdEnvelope3);
    }

    @Test
    @TargetUri("/trackPageView")
    public void testTrackPageView() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> pvdList = mockedIngestion.waitForItems("PageViewData", 2);

        Envelope rdEnvelope = rdList.get(0);
        Envelope pvdEnvelope1 = pvdList.get(0);
        Envelope pvdEnvelope2 = pvdList.get(1);

        final List<Domain> pageViews = mockedIngestion.getTelemetryDataByType("PageViewData");
        assertThat(pageViews, hasItem(allOf(
                PageViewDataMatchers.hasName("test-page"),
                PageViewDataMatchers.hasDuration(new Duration(0))
        )));

        assertThat(pageViews, hasItem(allOf(
                PageViewDataMatchers.hasName("test-page-2"),
                PageViewDataMatchers.hasDuration(new Duration(123456)),
                PageViewDataMatchers.hasProperty("key", "value")
        )));

        assertSameOperationId(rdEnvelope, pvdEnvelope1);
        assertSameOperationId(rdEnvelope, pvdEnvelope2);
    }

    @Test
    @TargetUri("/doPageView.jsp")
    public void testTrackPageView_JSP() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> pvdList = mockedIngestion.waitForItems("PageViewData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope pvdEnvelope = pvdList.get(0);

        PageViewData pv = (PageViewData) ((Data) pvdEnvelope.getData()).getBaseData();
        assertEquals("doPageView", pv.getName());
        assertEquals(new Duration(0), pv.getDuration());

        assertSameOperationId(rdEnvelope, pvdEnvelope);
    }

    @Test
    @TargetUri("/autoFailedRequestWithResultCode")
    public void testAutoFailedRequestWithResultCode() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        assertEquals(false, rd.getSuccess());
        assertEquals("404", rd.getResponseCode());

        assertSameOperationId(rdEnvelope, rdEnvelope);
    }

    @Test
    @TargetUri(value="/requestSlow?sleeptime=25", timeout=35_000) // the servlet sleeps for 25 seconds
    public void testRequestSlowWithResponseTime() throws Exception {
        validateSlowTest(25);
    }

    @Test
    @TargetUri(value="/slowLoop?responseTime=25", timeout=35_000) // the servlet sleeps for 20 seconds
    public void testSlowRequestUsingCpuBoundLoop() throws Exception {
        validateSlowTest(25);
    }

    @Test
    @TargetUri("/autoExceptionWithFailedRequest")
    public void testAutoExceptionWithFailedRequest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> edList = mockedIngestion.waitForItems("ExceptionData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope edEnvelope = edList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        ExceptionData ed = (ExceptionData) ((Data) edEnvelope.getData()).getBaseData();

        assertEquals(false, rd.getSuccess());

        ExceptionDetails eDetails = getExceptionDetails(ed);
        assertEquals("This is a auto thrown exception !", eDetails.getMessage());
    }

    @Test
    @TargetUri("/index.jsp")
    public void testRequestJSP() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);
    }

    private static ExceptionDetails getExceptionDetails(ExceptionData exceptionData) {
        List<ExceptionDetails> details = exceptionData.getExceptions();
        ExceptionDetails ex = details.get(0);
        return ex;
    }

    private void validateSlowTest(int expectedDurationSeconds) throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        long actual = rd.getDuration().getTotalMilliseconds();
        long expected = (new Duration(0, 0, 0, expectedDurationSeconds, 0).getTotalMilliseconds());
        long tolerance = 2 * 1000; // 2 seconds

        final long min = expected - tolerance;
        final long max = expected + tolerance;

        System.out.printf("Slow response time: expected=%d, actual=%d%n", expected, actual);
        assertThat(actual, both(greaterThanOrEqualTo(min)).and(lessThan(max)));

        assertSameOperationId(rdEnvelope, rdEnvelope);
    }

    private static void assertSameOperationId(Envelope rdEnvelope, Envelope otherEnvelope) {
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        String operationParentId = rdEnvelope.getTags().get("ai.operation.parentId");

        assertNotNull(operationId);
        assertNotNull(operationParentId);

        assertEquals(operationId, otherEnvelope.getTags().get("ai.operation.id"));
        assertEquals(operationParentId, otherEnvelope.getTags().get("ai.operation.parentId"));
    }
}