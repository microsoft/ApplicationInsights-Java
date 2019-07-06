package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
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


import org.junit.*;

import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.ExceptionDetailsMatchers.withMessage;
import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.hasException;
import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.hasMeasurement;
import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.hasSeverityLevel;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Comparator;
import java.util.List;

public class CoreAndFilterTests extends AiSmokeTest {
	
	@Test
    @TargetUri("/trackDependency")
    public void trackDependency() throws Exception {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 2;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
                expectedItems, totalItems);

        // TODO get dependency data envelope and verify value
        RemoteDependencyData d = getTelemetryDataForType(0, "RemoteDependencyData");

        final String expectedName = "DependencyTest";
        final String expectedData = "commandName";
        final Duration expectedDuration = new Duration(0, 0, 1, 1, 1);

        assertEquals(expectedName, d.getName());
        assertEquals(expectedData, d.getData());
        assertEquals(expectedDuration, d.getDuration());
	}

	@Test
	@TargetUri("/trackEvent")
	public void testTrackEvent() throws Exception {
		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
		assertEquals(2, mockedIngestion.getCountForType("EventData"));
		int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 3;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
				expectedItems, totalItems);

		// TODO get event data envelope and verify value
        final List<EventData> events = mockedIngestion.getTelemetryDataByType("EventData");
        events.sort(new Comparator<EventData>() {
            @Override
            public int compare(EventData o1, EventData o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        EventData d = events.get(1);
		final String name = "EventDataTest";
		assertEquals(name, d.getName());

		EventData d2 = events.get(0);

		final String expectedname = "EventDataPropertyTest";
		final String expectedProperties = "value";
		final Double expectedMetrice = 1d;

		assertEquals(expectedname, d2.getName());
		assertEquals(expectedProperties, d2.getProperties().get("key"));
		assertEquals(expectedMetrice, d2.getMeasurements().get("key"));
	}

	@Test
    @TargetUri("/trackException")
    public void testTrackException() throws Exception {

        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(3, mockedIngestion.getCountForType("ExceptionData"));
        int totalItems = mockedIngestion.getItemCount();
        int expectedItems = 4;
        assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
                expectedItems, totalItems);

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
    }

	@Test
    @TargetUri("/trackHttpRequest")
    public void testHttpRequest() throws Exception {
        assertEquals(5, mockedIngestion.getCountForType("RequestData"));

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
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("MetricData"));
        int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 2;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
                expectedItems, totalItems);

        // TODO get Metric data envelope and verify value
        MetricData d = getTelemetryDataForType(0, "MetricData");
        List<DataPoint> metrics = d.getMetrics();
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
	}

	@Test
	@TargetUri("/trackTrace")
	public void testTrackTrace() throws Exception {
		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
		assertEquals(3, mockedIngestion.getCountForType("MessageData"));
		int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 4;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
				expectedItems, totalItems);

        final List<MessageData> messages = mockedIngestion.getTelemetryDataByType("MessageData");
        // TODO get trace data envelope and verify value
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
    }

    @Test
    @TargetUri("/trackPageView")
    public void testTrackPageView() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(2, mockedIngestion.getCountForType("PageViewData"));

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
    }

    @Test
    @TargetUri("/doPageView.jsp")
    public void testTrackPageView_JSP() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("PageViewData"));

        PageViewData pv1 = getTelemetryDataForType(0, "PageViewData");
        assertEquals("doPageView", pv1.getName());
        assertEquals(new Duration(0), pv1.getDuration());
    }

    @Test
    @TargetUri("/autoFailedRequestWithResultCode")
    public void testAutoFailedRequestWithResultCode() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));

        RequestData rd1 = getTelemetryDataForType(0, "RequestData");
        assertEquals(false, rd1.getSuccess());
        assertEquals("404", rd1.getResponseCode());
    }

    @Test
    @TargetUri(value="/requestSlow?sleeptime=25", timeout=35_000) // the servlet sleeps for 25 seconds
    public void testRequestSlowWithResponseTime() {
        validateSlowTest(25);
    }

    @Test
    @TargetUri(value="/slowLoop?responseTime=25", timeout=35_000) // the servlet sleeps for 20 seconds
    public void testSlowRequestUsingCpuBoundLoop() {
        validateSlowTest(25);
    }

    @Ignore // See github issue #600. This should pass when that is fixed.
    @Test
    @TargetUri("/autoExceptionWithFailedRequest")
    public void testAutoExceptionWithFailedRequest() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("ExceptionData"));

        //due to there is a bug, the success for the request data is not correct, so just ignore this case now.
        RequestData rd = getTelemetryDataForType(0, "RequestData");
        assertEquals(false, rd.getSuccess());

        ExceptionData ed = getTelemetryDataForType(0, "ExceptionData");
        ExceptionDetails eDetails = getExceptionDetails(ed);
        final String expectedName = "This is a auto thrown exception !";
        assertEquals(expectedName, eDetails.getMessage());
    }

    @Test
    @TargetUri("/index.jsp")
    public void testRequestJSP() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
    }

    private static ExceptionDetails getExceptionDetails(ExceptionData exceptionData) {
        List<ExceptionDetails> details = exceptionData.getExceptions();
        ExceptionDetails ex = details.get(0);
        return ex;
    }

    private void validateSlowTest(int expectedDurationSeconds) {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));

        RequestData rd1 = getTelemetryDataForType(0, "RequestData");
        long actual = rd1.getDuration().getTotalMilliseconds();
        long expected = (new Duration(0, 0, 0, expectedDurationSeconds, 0).getTotalMilliseconds());
        long tolerance = 2 * 1000; // 2 seconds

        final long min = expected - tolerance;
        final long max = expected + tolerance;

        System.out.printf("Slow response time: expected=%d, actual=%d%n", expected, actual);
        assertThat(actual, both(greaterThanOrEqualTo(min)).and(lessThan(max)));
    }

}