/*
 * ApplicationInsights-Java
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

package com.microsoft.applicationinsights.smoketest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.EventData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

@UseAgent
public class SpringbootSmokeTest extends AiSmokeTest {

  @Test
  @TargetUri("/basic/trackEvent")
  public void trackEvent() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");

    mockedIngestion.waitForItemsInOperation("EventData", 2, operationId);

    // TODO get event data envelope and verify value
    List<EventData> data = mockedIngestion.getTelemetryDataByTypeInRequest("EventData");
    assertThat(
        data,
        hasItem(
            new TypeSafeMatcher<EventData>() {
              final String name = "EventDataTest";
              final Matcher<String> nameMatcher = Matchers.equalTo(name);

              @Override
              protected boolean matchesSafely(EventData item) {
                return nameMatcher.matches(item.getName());
              }

              @Override
              public void describeTo(Description description) {
                description.appendDescriptionOf(nameMatcher);
              }
            }));

    assertThat(
        data,
        hasItem(
            new TypeSafeMatcher<EventData>() {
              final String expectedKey = "key";
              final String expectedName = "EventDataPropertyTest";
              final String expectedPropertyValue = "value";
              final Double expectedMetricValue = 1d;
              final Matcher<Map<? extends String, ? extends Double>> metricMatcher =
                  Matchers.hasEntry(expectedKey, expectedMetricValue);
              final Matcher<Map<? extends String, ? extends String>> propertyMatcher =
                  Matchers.hasEntry(expectedKey, expectedPropertyValue);
              final Matcher<String> nameMatcher = Matchers.equalTo(expectedName);

              @Override
              public void describeTo(Description description) {
                description.appendDescriptionOf(nameMatcher);
                description.appendDescriptionOf(propertyMatcher);
                description.appendDescriptionOf(metricMatcher);
              }

              @Override
              protected boolean matchesSafely(EventData item) {
                return nameMatcher.matches(item.getName())
                    && propertyMatcher.matches(item.getProperties())
                    && metricMatcher.matches(item.getMeasurements());
              }
            }));
  }

  @Test
  @TargetUri("/throwsException")
  public void testResultCodeWhenRestControllerThrows() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> rddList =
        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    List<Envelope> edList =
        mockedIngestion.waitForItems(
            new Predicate<Envelope>() {
              @Override
              public boolean test(Envelope input) {
                if (!"ExceptionData".equals(input.getData().getBaseType())) {
                  return false;
                }
                if (!operationId.equals(input.getTags().get("ai.operation.id"))) {
                  return false;
                }
                // lastly, filter out ExceptionData captured from tomcat logger
                ExceptionData data = (ExceptionData) ((Data<?>) input.getData()).getBaseData();
                return !data.getProperties().containsKey("LoggerName");
              }
            },
            2,
            10,
            TimeUnit.SECONDS);
    assertEquals(0, mockedIngestion.getCountForType("EventData"));

    Envelope rddEnvelope1 = rddList.get(0);
    Envelope edEnvelope1 = edList.get(0);

    RequestData rd = getTelemetryDataForType(0, "RequestData");
    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();

    assertEquals("GET /SpringBootTest/throwsException", rd.getName());
    assertEquals("500", rd.getResponseCode());
    assertTrue(rd.getProperties().isEmpty());
    assertFalse(rd.getSuccess());

    assertEquals("TestController.resultCodeTest", rdd1.getName());
    assertNull(rdd1.getData());
    assertEquals("InProc", rdd1.getType());
    assertNull(rdd1.getTarget());
    assertTrue(rdd1.getProperties().isEmpty());
    assertFalse(rdd1.getSuccess());

    assertParentChild(rd, rdEnvelope, rddEnvelope1, "GET /SpringBootTest/throwsException");
    assertParentChild(rdd1, rddEnvelope1, edEnvelope1, "GET /SpringBootTest/throwsException");
  }

  @Test
  @TargetUri("/asyncDependencyCall")
  public void testAsyncDependencyCall() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> rddList =
        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 3, operationId);
    assertEquals(0, mockedIngestion.getCountForType("EventData"));

    Envelope rddEnvelope1 = rddList.get(0);
    Envelope rddEnvelope2 = rddList.get(1);
    Envelope rddEnvelope3 = rddList.get(2);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd2 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();
    RemoteDependencyData rdd3 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope3.getData()).getBaseData();

    assertEquals("GET /SpringBootTest/asyncDependencyCall", rd.getName());
    assertEquals("200", rd.getResponseCode());
    assertTrue(rd.getProperties().isEmpty());
    assertTrue(rd.getSuccess());

    assertEquals("TestController.asyncDependencyCall", rdd1.getName());
    assertNull(rdd1.getData());
    assertEquals("InProc", rdd1.getType());
    assertNull(rdd1.getTarget());
    assertTrue(rdd1.getProperties().isEmpty());
    assertTrue(rdd1.getSuccess());

    assertEquals("HTTP GET", rdd2.getName());
    assertEquals("https://www.bing.com", rdd2.getData());
    assertEquals("www.bing.com", rdd2.getTarget());
    assertTrue(rdd2.getProperties().isEmpty());
    assertTrue(rdd2.getSuccess());

    // TODO (trask): why is spring-webmvc instrumentation capturing this twice?
    assertEquals("TestController.asyncDependencyCall", rdd3.getName());
    assertTrue(rdd3.getProperties().isEmpty());
    assertTrue(rdd3.getSuccess());

    assertParentChild(rd, rdEnvelope, rddEnvelope1, "GET /SpringBootTest/asyncDependencyCall");
    assertParentChild(rdd1, rddEnvelope1, rddEnvelope2, "GET /SpringBootTest/asyncDependencyCall");
    assertParentChild(rdd1, rddEnvelope1, rddEnvelope3, "GET /SpringBootTest/asyncDependencyCall");
  }
}
