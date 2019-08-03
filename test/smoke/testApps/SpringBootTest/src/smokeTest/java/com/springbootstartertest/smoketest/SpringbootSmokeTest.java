package com.springbootstartertest.smoketest;

import java.util.List;
import java.util.Map;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@UseAgent
public class SpringbootSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri("/basic/trackEvent")
    public void trackEvent() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(2, mockedIngestion.getCountForType("EventData"));

        // TODO get event data envelope and verify value
        final List<EventData> data = mockedIngestion.getTelemetryDataByType("EventData");
        assertThat(data, hasItem(new TypeSafeMatcher<EventData>() {
            final String name = "EventDataTest";
            Matcher<String> nameMatcher = Matchers.equalTo(name);
            @Override
            protected boolean matchesSafely(EventData item) {
                return nameMatcher.matches(item.getName());
            }

            @Override
            public void describeTo(Description description) {
                description.appendDescriptionOf(nameMatcher);
            }
        }));

        assertThat(data, hasItem(new TypeSafeMatcher<EventData>() {
            final String expectedKey = "key";
            final String expectedName = "EventDataPropertyTest";
            final String expectedPropertyValue = "value";
            final Double expectedMetricValue = 1d;
            Matcher<Map<? extends String, ? extends Double>> metricMatcher = Matchers.hasEntry(expectedKey, expectedMetricValue);
            Matcher<Map<? extends String, ? extends String>> propertyMatcher = Matchers.hasEntry(expectedKey, expectedPropertyValue);
            Matcher<String> nameMatcher = Matchers.equalTo(expectedName);


            @Override
            public void describeTo(Description description) {
                description.appendDescriptionOf(nameMatcher);
                description.appendDescriptionOf(propertyMatcher);
                description.appendDescriptionOf(metricMatcher);
            }

            @Override
            protected boolean matchesSafely(EventData item) {
                return nameMatcher.matches(item.getName()) && propertyMatcher.matches(item.getProperties()) && metricMatcher.matches(item.getMeasurements());
            }
        }));
    }

    @Test
    @TargetUri("/throwsException")
    public void testResultCodeWhenRestControllerThrows() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> edList = mockedIngestion.getItemsEnvelopeDataType("ExceptionData");

        assertThat(rdList, hasSize(1));
        assertThat(edList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope edEnvelope = edList.get(0);

        RequestData rd = getTelemetryDataForType(0, "RequestData");
        System.out.println("Response code after exception: " + rd.getResponseCode());
        int code = -123;
        try {
            code = Integer.parseInt(rd.getResponseCode());
        } catch (NumberFormatException e) {
            fail("Response code is not a number");
        }
        assertThat(code, greaterThanOrEqualTo(500));

        assertSameOperationId(rdEnvelope, edEnvelope);
    }

    @Test
    @TargetUri("/asyncDependencyCallWithApacheHttpClient4")
    public void testAsyncDependencyCallWithApacheHttpClient4() {
        commonValidation();
    }

    @Test
    @TargetUri("/asyncDependencyCallWithApacheHttpClient3")
    public void testAsyncDependencyCallWithApacheHttpClient3() {
        commonValidation();
    }

    @Test
    @TargetUri("/asyncDependencyCallWithOkHttp3")
    public void testAsyncDependencyCallWithOkHttp3() {
        commonValidation();
    }

    @Test
    @TargetUri("/asyncDependencyCallWithOkHttp2")
    public void testAsyncDependencyCallWithOkHttp2() {
        commonValidation();
    }

    @Test
    @TargetUri("/asyncDependencyCallWithHttpURLConnection")
    public void testAsyncDependencyCallWithHttpURLConnection() {
        commonValidation();
    }

    private static void commonValidation() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData d = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());

        assertTrue(rdd.getId().contains(d.getId()));
        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    private static void assertSameOperationId(Envelope rdEnvelope, Envelope rddEnvelope) {
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        String operationParentId = rdEnvelope.getTags().get("ai.operation.parentId");

        assertNotNull(operationId);
        assertNotNull(operationParentId);

        assertEquals(operationId, rddEnvelope.getTags().get("ai.operation.id"));
        assertEquals(operationParentId, rddEnvelope.getTags().get("ai.operation.parentId"));
    }
}
