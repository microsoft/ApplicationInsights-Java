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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@UseAgent
public class SpringbootSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri("/basic/trackEvent")
    public void trackEvent() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);
        mockedIngestion.waitForItems("EventData", 2);

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
    public void testResultCodeWhenRestControllerThrows() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> edList = mockedIngestion.waitForItems("ExceptionData", 1);

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
    @TargetUri("/asyncDependencyCall")
    public void testAsyncDependencyCall() throws Exception {
        commonValidation();
    }

    private static void commonValidation() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

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
