// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class DefaultOtelResourceAttributesTest {

  private static final List<String> EXPECTED_RESOURCE_ATTRIBUTES =
      Arrays.asList(
          "telemetry.sdk.language",
          "service.name",
          "service.instance.id",
          "telemetry.sdk.version",
          "telemetry.sdk.name");

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "/app")
  void testApp() {
    List<Envelope> metricsEnvelops = testing.mockedIngestion.getItemsEnvelopeDataType("MetricData");
    List<Envelope> otelResourceCustomMetrics = new ArrayList<>();
    for (Envelope envelope : metricsEnvelops) {
      MetricData metricData = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      String name = metricData.getMetrics().get(0).getName();
      if ("_OTELRESOURCE_".equals(name)) {
        otelResourceCustomMetrics.add(envelope);
      }
    }

    verify(otelResourceCustomMetrics);
  }

  private static void verify(List<Envelope> metrics) {
    for (Envelope envelope : metrics) {
      validateTags(envelope);
      MetricData md1 = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      validateMetricData(md1);
    }
  }

  private static void validateTags(Envelope envelope) {
    Map<String, String> tags = envelope.getTags();
    assertThat(tags).containsOnlyKeys("ai.internal.sdkVersion", "ai.cloud.roleInstance");
  }

  private static void validateMetricData(MetricData metricData) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    Map<String, String> properties = metricData.getProperties();
    assertThat(properties.keySet())
        .containsExactlyInAnyOrderElementsOf(EXPECTED_RESOURCE_ATTRIBUTES);
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends DefaultOtelResourceAttributesTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends DefaultOtelResourceAttributesTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends DefaultOtelResourceAttributesTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends DefaultOtelResourceAttributesTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends DefaultOtelResourceAttributesTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends DefaultOtelResourceAttributesTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends DefaultOtelResourceAttributesTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends DefaultOtelResourceAttributesTest {}
}
