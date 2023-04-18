// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.EventData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class SpringBootTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/basic/trackEvent")
  void trackEvent() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");

    testing.mockedIngestion.waitForItemsInOperation("EventData", 2, operationId);

    // TODO get event data envelope and verify value
    List<EventData> data = testing.mockedIngestion.getTelemetryDataByTypeInRequest("EventData");

    assertThat(data).anySatisfy(ed -> assertThat(ed.getName()).isEqualTo("EventDataTest"));

    assertThat(data)
        .anySatisfy(
            ed -> {
              assertThat(ed.getName()).isEqualTo("EventDataPropertyTest");
              assertThat(ed.getProperties()).containsEntry("key", "value");
              assertThat(ed.getMeasurements()).containsEntry("key", 1.0);
            });
  }

  @Test
  @TargetUri("/throwsException")
  void testResultCodeWhenRestControllerThrows() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItems(
            input -> {
              if (!"ExceptionData".equals(input.getData().getBaseType())) {
                return false;
              }
              if (!operationId.equals(input.getTags().get("ai.operation.id"))) {
                return false;
              }
              // lastly, filter out ExceptionData captured from tomcat logger
              ExceptionData data = (ExceptionData) ((Data<?>) input.getData()).getBaseData();
              return !data.getProperties().containsKey("LoggerName");
            },
            1,
            10,
            TimeUnit.SECONDS);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope edEnvelope1 = edList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(edEnvelope1.getSampleRate()).isNull();

    RequestData rd = testing.getTelemetryDataForType(0, "RequestData");

    assertThat(rd.getName()).isEqualTo("GET /SpringBoot/throwsException");
    assertThat(rd.getResponseCode()).isEqualTo("500");
    assertThat(rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd.getSuccess()).isFalse();

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope1, "GET /SpringBoot/throwsException");
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

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(rddEnvelope1.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();

    assertThat(rd.getName()).isEqualTo("GET /SpringBoot/asyncDependencyCall");
    assertThat(rd.getResponseCode()).isEqualTo("200");
    assertThat(rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd.getSuccess()).isTrue();

    assertThat(rdd1.getName()).isEqualTo("GET /");
    assertThat(rdd1.getData()).isEqualTo("https://www.bing.com");
    assertThat(rdd1.getTarget()).isEqualTo("www.bing.com");
    assertThat(rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, rddEnvelope1, "GET /SpringBoot/asyncDependencyCall");
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

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends SpringBootTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends SpringBootTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends SpringBootTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends SpringBootTest {}
}
