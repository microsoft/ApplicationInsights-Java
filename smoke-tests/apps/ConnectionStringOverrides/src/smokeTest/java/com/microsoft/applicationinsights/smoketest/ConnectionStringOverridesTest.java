// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class ConnectionStringOverridesTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/app2")
  void testApp2() throws Exception {
    testApp("12345678-0000-0000-0000-0FEEDDADBEEF");
  }

  @Test
  @TargetUri("/app3")
  void testApp3() throws Exception {
    testApp("87654321-0000-0000-0000-0FEEDDADBEEF");
  }

  private static void testApp(String iKey) throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    List<Envelope> mdList =
        testing.mockedIngestion.waitForItemsInOperation("MessageData", 1, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope rddEnvelope = rddList.get(0);
    Envelope mdEnvelope = mdList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();
    MessageData md = (MessageData) ((Data<?>) mdEnvelope.getData()).getBaseData();

    assertThat(rdEnvelope.getIKey()).isEqualTo(iKey);
    assertThat(rddEnvelope.getIKey()).isEqualTo(iKey);
    assertThat(mdEnvelope.getIKey()).isEqualTo(iKey);

    assertThat(rd.getSuccess()).isTrue();

    assertThat(rdd.getType()).isEqualTo("Http");
    assertThat(rdd.getTarget()).isEqualTo("host.testcontainers.internal:6060");
    assertThat(rdd.getName()).isEqualTo("GET /mock/200");
    assertThat(rdd.getData()).isEqualTo("http://host.testcontainers.internal:6060/mock/200");
    assertThat(rdd.getResultCode()).isEqualTo("200");
    assertThat(rdd.getSuccess()).isTrue();

    assertThat(md.getMessage()).isEqualTo("hello");
    assertThat(md.getSeverityLevel()).isEqualTo(SeverityLevel.INFORMATION);
    assertThat(md.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(md.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(md.getProperties()).containsKey("ThreadName");
    assertThat(md.getProperties()).hasSize(3);

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, rddEnvelope, "GET /ConnectionStringOverrides/*");
    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, mdEnvelope, "GET /ConnectionStringOverrides/*");

    // and metrics too

    List<Envelope> clientMetrics =
        testing.mockedIngestion.waitForStandardMetricItems("dependencies/duration", 1);
    List<Envelope> serverMetrics =
        testing.mockedIngestion.waitForStandardMetricItems("requests/duration", 1);

    verifyHttpClientPreAggregatedMetrics(clientMetrics, iKey);
    verifyHttpServerPreAggregatedMetrics(serverMetrics, iKey);
  }

  private static void verifyHttpClientPreAggregatedMetrics(List<Envelope> metrics, String iKey) {
    assertThat(metrics.size()).isEqualTo(1);

    Envelope envelope1 = metrics.get(0);
    assertThat(envelope1.getIKey()).isEqualTo(iKey);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData("client", md1, "200");
  }

  private static void verifyHttpServerPreAggregatedMetrics(List<Envelope> metrics, String iKey) {
    assertThat(metrics.size()).isEqualTo(1);

    Envelope envelope1 = metrics.get(0);
    assertThat(envelope1.getIKey()).isEqualTo(iKey);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData("server", md1, "200");
  }

  private static void validateTags(Envelope envelope) {
    Map<String, String> tags = envelope.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
  }

  private static void validateMetricData(String type, MetricData metricData, String resultCode) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    DataPoint dataPoint = dataPoints.get(0);
    assertThat(dataPoint.getCount()).isEqualTo(1);
    assertThat(dataPoint.getValue()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    assertThat(dataPoint.getMin()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    assertThat(dataPoint.getMax()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    Map<String, String> properties = metricData.getProperties();
    String expectedSuccess = "200".equals(resultCode) ? "True" : "False";
    if ("client".equals(type)) {
      assertThat(properties).hasSize(10);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("dependencies/duration");
      assertThat(properties.get("dependency/resultCode")).isEqualTo(resultCode);
      assertThat(properties.get("Dependency.Success")).isEqualTo(expectedSuccess);
      assertThat(properties.get("dependency/target"))
          .isEqualTo("host.testcontainers.internal:6060");
      assertThat(properties.get("Dependency.Type")).isEqualTo("Http");
      assertThat(properties.get("_MS.SentToAMW")).isEqualTo("False");
    } else {
      assertThat(properties).hasSize(8);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("requests/duration");
      assertThat(properties.get("request/resultCode")).isEqualTo(resultCode);
      assertThat(properties.get("Request.Success")).isEqualTo(expectedSuccess);
      assertThat(properties.get("_MS.SentToAMW")).isEqualTo("False");
    }
    assertThat(properties.get("operation/synthetic")).isEqualTo("False");
    assertThat(properties.get("cloud/roleInstance")).isEqualTo("testroleinstance");
    assertThat(properties.get("cloud/roleName")).isEqualTo("testrolename");
    assertThat(properties.get("_MS.IsAutocollected")).isEqualTo("True");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends ConnectionStringOverridesTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends ConnectionStringOverridesTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends ConnectionStringOverridesTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends ConnectionStringOverridesTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends ConnectionStringOverridesTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends ConnectionStringOverridesTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends ConnectionStringOverridesTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends ConnectionStringOverridesTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends ConnectionStringOverridesTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends ConnectionStringOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends ConnectionStringOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends ConnectionStringOverridesTest {}
}
