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
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = {OtlpApplication.class},
    webEnvironment = RANDOM_PORT)
@UseAgent
abstract class OtlpTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().useOtlpEndpoint().setSelfDiagnosticsLevel("TRACE").build();

  @Test
  @TargetUri("/ping")
  public void testOtlpTelemetry() throws Exception {
    // verify request sent to Application Insights endpoint
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("GET /OtlpMetrics/ping");

    // verify custom histogram metric sent to Application Insights endpoint
    List<Envelope> metricList =
        testing.mockedIngestion.waitForItems("MetricData", OtlpTest::isHistogramMetric, 1);
    Envelope metricEnvelope = metricList.get(0);
    MetricData metricData = (MetricData) ((Data<?>) metricEnvelope.getData()).getBaseData();
    assertThat(metricData.getMetrics().get(0).getName()).isEqualTo("histogram-test-otlp-exporter");

    // verify pre-aggregated standard metric sent to Application Insights endpoint
    List<Envelope> standardMetricsList =
        testing.mockedIngestion.waitForItems("MetricData", OtlpTest::isStandardMetric, 1);
    Envelope standardMetricEnvelope = standardMetricsList.get(0);
    MetricData standardMetricData =
        (MetricData) ((Data<?>) standardMetricEnvelope.getData()).getBaseData();
    assertThat(standardMetricData.getMetrics().get(0).getName()).isEqualTo("http.server.duration");
    assertThat(standardMetricData.getProperties().get("_MS.IsAutocollected")).isEqualTo("True");

    // verify custom histogram metric 'histogram-test-otlp-exporter' and otel metric
    // 'http.server.duration' sent to OTLP endpoint
    testing.mockedOtlpIngestion.verify();
  }

  private static boolean isHistogramMetric(Envelope envelope) {
    if (envelope.getData().getBaseType().equals("MetricData")) {
      MetricData data = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      return data.getMetrics().get(0).getName().equals("histogram-test-otlp-exporter");
    }
    return false;
  }

  private static boolean isStandardMetric(Envelope envelope) {
    if (envelope.getData().getBaseType().equals("MetricData")) {
      MetricData data = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      return data.getMetrics().get(0).getName().equals("http.server.duration");
    }
    return false;
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends OtlpTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends OtlpTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends OtlpTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends OtlpTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends OtlpTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends OtlpTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends OtlpTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends OtlpTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends OtlpTest {}
}
