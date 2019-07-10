package com.springbootstartertest.smoketest;

import java.util.List;
import java.util.Map;

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

import static org.hamcrest.Matchers.hasItem;
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
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        List<Envelope> exceptionEnvelopeList = mockedIngestion.getItemsEnvelopeDataType("ExceptionData");
        assertEquals(1, exceptionEnvelopeList.size());

        Envelope exceptionEnvelope = exceptionEnvelopeList.get(0);
        RequestData d = getTelemetryDataForType(0, "RequestData");
        String requestOperationId = d.getId();
        assertTrue(requestOperationId.contains(exceptionEnvelope.getTags().
                getOrDefault("ai.operation.id", null)));
    }

    @Test
    @TargetUri("/asyncDependencyCallWithApacheHttpClient4")
    public void testAsyncDependencyCallWithApacheHttpClient4() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }

    @Test
    @TargetUri("/asyncDependencyCallWithApacheHttpClient3")
    public void testAsyncDependencyCallWithApacheHttpClient3() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }

    @Test
    @TargetUri("/asyncDependencyCallWithOkHttp3")
    public void testAsyncDependencyCallWithOkHttp3() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }

    @Test
    @TargetUri("/asyncDependencyCallWithOkHttp2")
    public void testAsyncDependencyCallWithOkHttp2() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }

    @Test
    @TargetUri("/asyncDependencyCallWithHttpURLConnection")
    public void testAsyncDependencyCallWithHttpURLConnection() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }
}
