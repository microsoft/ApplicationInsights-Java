package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import com.microsoft.applicationinsights.internal.schemav2.PageViewData;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.Duration;

import org.junit.*;

import static org.junit.Assert.*;

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
		EventData d = getTelemetryDataForType(0, "EventData");
		final String name = "EventDataTest";
		assertEquals(name, d.getName());

		EventData d2 = getTelemetryDataForType(1, "EventData");

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

        ExceptionData d = getTelemetryDataForType(0, "ExceptionData");
        ExceptionDetails eDetails = getExceptionDetails(d);
        assertEquals(expectedName, eDetails.getMessage());

        ExceptionData d2 = getTelemetryDataForType(1, "ExceptionData");
        ExceptionDetails eDetails2 = getExceptionDetails(d2);
        assertEquals(expectedName, eDetails2.getMessage());
        assertEquals(expectedProperties, d2.getProperties().get("key"));
        assertEquals(expectedMetrice, d2.getMeasurements().get("key"));

        ExceptionData d3 = getTelemetryDataForType(2, "ExceptionData");
        ExceptionDetails eDetails3 = getExceptionDetails(d3);
        assertEquals(expectedName, eDetails3.getMessage());
        assertEquals(SeverityLevel.Error, d3.getSeverityLevel());
    }

    private ExceptionDetails getExceptionDetails(ExceptionData exceptionData) {
        List<ExceptionDetails> details = exceptionData.getExceptions();
        ExceptionDetails ex = details.get(0);
        return ex;
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
        //true
        RequestData d = getTelemetryDataForType(0, "RequestData");
        
        final String expectedName = "HttpRequestDataTest";
        final String expectedResponseCode = "200";

        assertEquals(expectedName, d.getName());
        assertEquals(expectedResponseCode, d.getResponseCode());
        assertEquals(new Duration(4711), d.getDuration());
        assertEquals(true, d.getSuccess());

        RequestData d1 = getTelemetryDataForType(1, "RequestData");

        final String expectedName1 = "PingTest";
        final String expectedResponseCode1 = "200";
        final String expectedURL = "http://tempuri.org/ping";

        assertEquals(expectedName1, d1.getName());
        assertEquals(expectedResponseCode1, d1.getResponseCode());
        assertEquals(new Duration(1), d1.getDuration());
        assertEquals(true, d1.getSuccess());
        assertEquals(expectedURL, d1.getUrl());

        //false
        RequestData rd1 = getTelemetryDataForType(2, "RequestData");
        assertEquals("FailedHttpRequest", rd1.getName());
        assertEquals("404", rd1.getResponseCode());
        assertEquals(new Duration(6666), rd1.getDuration());
        assertEquals(false, rd1.getSuccess());

        RequestData rd2 = getTelemetryDataForType(3, "RequestData");
        assertEquals("FailedHttpRequest2", rd2.getName());
        assertEquals("505", rd2.getResponseCode());
        assertEquals(new Duration(8888), rd2.getDuration());
        assertEquals(false, rd2.getSuccess());
        assertEquals("https://www.bingasdasdasdasda.com/", rd2.getUrl());

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
		assertEquals(Integer.valueOf(1),  dp.getCount());

		assertNull(dp.getMin());
		assertNull(dp.getMax());
		assertNull(dp.getStdDev());
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

		// TODO get trace data envelope and verify value	
		MessageData d = getTelemetryDataForType(0, "MessageData");
		final String expectedMessage = "This is first trace message.";
		assertEquals(expectedMessage, d.getMessage());

		MessageData d2 = getTelemetryDataForType(1, "MessageData");
		final String expectedMessage2 = "This is second trace message.";
		assertEquals(expectedMessage2, d2.getMessage());
		assertEquals(SeverityLevel.Error, d2.getSeverityLevel());

		MessageData d3 = getTelemetryDataForType(2, "MessageData");
		final String expectedMessage3 = "This is third trace message.";
		final String expectedValue = "value";
		assertEquals(expectedMessage3, d3.getMessage());
		assertEquals(SeverityLevel.Information, d3.getSeverityLevel());
		assertEquals(expectedValue, d3.getProperties().get("key"));
    }
    
    @Test
    @TargetUri("/trackPageView")
    public void testTrackPageView() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(2, mockedIngestion.getCountForType("PageViewData"));
        
        PageViewData pv1 = getTelemetryDataForType(0, "PageViewData");
        assertEquals("test-page", pv1.getName());
        assertEquals(new Duration(0), pv1.getDuration());

        PageViewData pv2 = getTelemetryDataForType(1, "PageViewData");
        assertEquals("test-page-2", pv2.getName());
        assertEquals(new Duration(123456), pv2.getDuration());
        assertEquals("value", pv2.getProperties().get("key"));
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
    @TargetUri("/autoFailedRequestWithCode")
    public void testAutoFailedRequestWithCode() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));

        RequestData rd1 = getTelemetryDataForType(0, "RequestData");
        assertEquals(false, rd1.getSuccess());
        assertEquals("404", rd1.getResponseCode());
    }
}