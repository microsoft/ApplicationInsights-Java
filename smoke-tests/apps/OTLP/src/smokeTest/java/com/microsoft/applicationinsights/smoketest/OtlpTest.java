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
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    classes = {OtlpApplication.class},
    webEnvironment = RANDOM_PORT)
@UseAgent
abstract class OtlpTest {

  @LocalServerPort private int port;

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().useOtlpEndpoint().setSelfDiagnosticsLevel("DEBUG").build();

  @Autowired TestRestTemplate template;

  @Test
  @TargetUri("/ping")
  public void testOtlpTelemetry() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("GET /OTLP/ping");

    List<Envelope> metricList = testing.mockedIngestion.waitForItems("MetricData", 1, true);
    Envelope metricEnvelope = metricList.get(0);
    MetricData metricData = (MetricData) ((Data<?>) metricEnvelope.getData()).getBaseData();
    assertThat(metricData.getMetrics().get(0).getName()).isEqualTo("histogram-test-otlp-exporter");

    template.getForEntity(URI.create("http://host.testcontainers.internal:" + port + "/ping"), String.class);
    testing.mockedOtlpIngestion.verify();
  }

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends OtlpTest {}
}
