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
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class PreAggMetricsWithRoleNameOverridesAndSamplingTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  private static final int COUNT = 100;

  @Test
  @TargetUri(value = "/app2", callCount = COUNT)
  void testApp2() throws Exception {
    testApp("app2");
  }

  @Test
  @TargetUri(value = "/app3", callCount = COUNT)
  void testApp3() throws Exception {
    testApp("app3");
  }

  private void testApp(String roleName) throws Exception {
    // super super low chance that number of sampled requests is less than 25
    long start = System.nanoTime();
    while (testing.mockedIngestion.getCountForType("RequestData") < 25
        && NANOSECONDS.toSeconds(System.nanoTime() - start) < 10) {}
    // wait ten more seconds before checking that we didn't receive too many
    Thread.sleep(SECONDS.toMillis(10));

    List<Envelope> requestEnvelopes =
        testing.mockedIngestion.getItemsEnvelopeDataType("RequestData");
    Thread.sleep(SECONDS.toMillis(10));

    List<Envelope> metricsEnvelops = testing.mockedIngestion.getItemsEnvelopeDataType("MetricData");
    List<Envelope> clientMetrics = new ArrayList<>();
    List<Envelope> serverMetrics = new ArrayList<>();
    for (Envelope envelope : metricsEnvelops) {
      MetricData metricData = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      String name = metricData.getMetrics().get(0).getName();
      if ("http.client.duration".equals(name)) {
        clientMetrics.add(envelope);
      } else if ("http.server.duration".equals(name)) {
        serverMetrics.add(envelope);
      }
    }

    verifySamplingRateAndRoleNameOverrides(requestEnvelopes, roleName);
    verifyPreAggMetrics(clientMetrics, roleName, true);
    verifyPreAggMetrics(serverMetrics, roleName, false);
  }

  private static void verifySamplingRateAndRoleNameOverrides(
      List<Envelope> requestEnvelopes, String roleName) throws Exception {
    List<Envelope> messageEnvelopes =
        testing.mockedIngestion.getItemsEnvelopeDataType("MessageData");
    // super super low chance that number of sampled requests/dependencies/events
    // is less than 25 or greater than 75
    assertThat(requestEnvelopes.size()).isGreaterThanOrEqualTo(25);
    assertThat(requestEnvelopes.size()).isLessThanOrEqualTo(75);
    assertThat(messageEnvelopes.size()).isGreaterThanOrEqualTo(25);
    assertThat(messageEnvelopes.size()).isLessThanOrEqualTo(75);

    for (Envelope requestEnvelope : requestEnvelopes) {
      assertThat(requestEnvelope.getSampleRate()).isEqualTo(50);
    }
    for (Envelope messageEnvelope : messageEnvelopes) {
      assertThat(messageEnvelope.getSampleRate()).isEqualTo(50);
    }
    for (Envelope rdEnvelope : requestEnvelopes) {
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

      assertThat(rdEnvelope.getTags()).containsEntry("ai.cloud.role", roleName);
      assertThat(rddEnvelope.getTags()).containsEntry("ai.cloud.role", roleName);
      assertThat(mdEnvelope.getTags()).containsEntry("ai.cloud.role", roleName);

      assertThat(rd.getSuccess()).isTrue();

      assertThat(rdd.getType()).isEqualTo("Http");
      assertThat(rdd.getTarget()).isEqualTo("mock.codes");
      assertThat(rdd.getName()).isEqualTo("GET /200");
      assertThat(rdd.getData()).isEqualTo("https://mock.codes/200");
      assertThat(rdd.getResultCode()).isEqualTo("200");
      assertThat(rdd.getSuccess()).isTrue();

      assertThat(md.getMessage()).isEqualTo("hello");
      assertThat(md.getSeverityLevel()).isEqualTo(SeverityLevel.INFORMATION);
      assertThat(md.getProperties()).containsEntry("SourceType", "Logger");
      assertThat(md.getProperties()).containsEntry("LoggerName", "smoketestapp");
      assertThat(md.getProperties()).containsKey("ThreadName");
      assertThat(md.getProperties()).hasSize(3);

      SmokeTestExtension.assertParentChild(
          rd, rdEnvelope, rddEnvelope, "GET /PreAggMetricsWithRoleNameOverridesAndSampling/*");
      SmokeTestExtension.assertParentChild(
          rd, rdEnvelope, mdEnvelope, "GET /PreAggMetricsWithRoleNameOverridesAndSampling/*");
    }
  }

  private static void verifyPreAggMetrics(
      List<Envelope> metrics, String roleName, boolean isClient) {
    for (Envelope envelope : metrics) {
      validateTags(envelope, roleName);
      MetricData md1 = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      validateMetricData(isClient ? "client" : "server", md1, "200", roleName);
    }
  }

  private static void validateTags(Envelope envelope, String roleName) {
    Map<String, String> tags = envelope.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", roleName);
  }

  private static void validateMetricData(
      String type, MetricData metricData, String resultCode, String roleName) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    DataPoint dataPoint = dataPoints.get(0);
    assertThat(dataPoint.getValue()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    assertThat(dataPoint.getMin()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    assertThat(dataPoint.getMax()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    Map<String, String> properties = metricData.getProperties();
    String expectedSuccess = "200".equals(resultCode) ? "True" : "False";
    if ("client".equals(type)) {
      assertThat(properties).hasSize(9);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("dependencies/duration");
      assertThat(properties.get("dependency/resultCode")).isEqualTo(resultCode);
      assertThat(properties.get("Dependency.Success")).isEqualTo(expectedSuccess);
      assertThat(properties.get("dependency/target")).isEqualTo("mock.codes");
      assertThat(properties.get("Dependency.Type")).isEqualTo("Http");
    } else {
      assertThat(properties).hasSize(7);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("requests/duration");
      assertThat(properties.get("request/resultCode")).isEqualTo(resultCode);
      assertThat(properties.get("Request.Success")).isEqualTo(expectedSuccess);
    }
    assertThat(properties.get("operation/synthetic")).isEqualTo("False");
    assertThat(properties.get("cloud/roleInstance")).isEqualTo("testroleinstance");
    assertThat(properties.get("cloud/roleName")).isEqualTo(roleName);
    assertThat(properties.get("_MS.IsAutocollected")).isEqualTo("True");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends PreAggMetricsWithRoleNameOverridesAndSamplingTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends PreAggMetricsWithRoleNameOverridesAndSamplingTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends PreAggMetricsWithRoleNameOverridesAndSamplingTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends PreAggMetricsWithRoleNameOverridesAndSamplingTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends PreAggMetricsWithRoleNameOverridesAndSamplingTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends PreAggMetricsWithRoleNameOverridesAndSamplingTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends PreAggMetricsWithRoleNameOverridesAndSamplingTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends PreAggMetricsWithRoleNameOverridesAndSamplingTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends PreAggMetricsWithRoleNameOverridesAndSamplingTest {}
}
