// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
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
      SmokeTestExtension.builder().useOtlpEndpoint().setSelfDiagnosticsLevel("DEBUG").build();

  @Test
  @TargetUri("/ping")
  public void testOtlpTelemetry() throws Exception {
    // verify request sent to Application Insights endpoint
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("GET /OTLP/ping");

    // verify metric sent to Application Insights endpoint
    List<Envelope> metricList =
        testing.mockedIngestion.waitForItems("MetricData", OtlpTest::isHistogramMetric, 1);
    Envelope metricEnvelope = metricList.get(0);
    MetricData metricData = (MetricData) ((Data<?>) metricEnvelope.getData()).getBaseData();
    assertThat(metricData.getMetrics().get(0).getName()).isEqualTo("histogram-test-otlp-exporter");

    // verify metrics sent to OTLP endpoint
    testing.mockedOtlpIngestion.verify();
  }

  private static boolean isHistogramMetric(Envelope envelope) {
    if (envelope.getData().getBaseType().equals("MetricData")) {
      MetricData data = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      return data.getMetrics().get(0).getName().equals("histogram-test-otlp-exporter");
    }
    return false;
  }

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends OtlpTest {}
}
