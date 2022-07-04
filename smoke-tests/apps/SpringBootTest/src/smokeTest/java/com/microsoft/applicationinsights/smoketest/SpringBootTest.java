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

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertFalse;
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
import org.junit.jupiter.api.Test;

@UseAgent
abstract class SpringBootTest {

  @Test
  @TargetUri("/basic/trackEvent")
  void trackEvent() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");

    testing.mockedIngestion.waitForItemsInOperation("EventData", 2, operationId);

    // TODO get event data envelope and verify value
    List<EventData> data = testing.mockedIngestion.getTelemetryDataByTypeInRequest("EventData");
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
              void describeTo(Description description) {
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
              void describeTo(Description description) {
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
  void testResultCodeWhenRestControllerThrows() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItems(
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
            1,
            10,
            TimeUnit.SECONDS);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope edEnvelope1 = edList.get(0);

    RequestData rd = testing.getTelemetryDataForType(0, "RequestData");

    assertThat(rd.getName()).isEqualTo("GET /SpringBootTest/throwsException");
    assertThat(rd.getResponseCode()).isEqualTo("500");
    assertTrue(rd.getProperties().isEmpty());
    assertFalse(rd.getSuccess());

    AiSmokeTest.assertParentChild(
        rd, rdEnvelope, edEnvelope1, "GET /SpringBootTest/throwsException");
  }

  @Test
  @TargetUri("/asyncDependencyCall")
  void testAsyncDependencyCall() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope rddEnvelope1 = rddList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();

    assertThat(rd.getName()).isEqualTo("GET /SpringBootTest/asyncDependencyCall");
    assertThat(rd.getResponseCode()).isEqualTo("200");
    assertTrue(rd.getProperties().isEmpty());
    assertTrue(rd.getSuccess());

    assertThat(rdd1.getName()).isEqualTo("GET /");
    assertThat(rdd1.getData()).isEqualTo("https://www.bing.com");
    assertThat(rdd1.getTarget()).isEqualTo("www.bing.com");
    assertThat(rdd1.getProperties()).isEmpty();
    assertThat(rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        rd, rdEnvelope, rddEnvelope1, "GET /SpringBootTest/asyncDependencyCall");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends SpringBootTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends SpringBootTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends SpringBootTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends SpringBootTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends SpringBootTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends SpringBootTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends SpringBootTest {}
}
