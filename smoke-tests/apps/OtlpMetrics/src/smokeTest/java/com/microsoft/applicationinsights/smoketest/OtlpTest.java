// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import io.opentelemetry.proto.metrics.v1.Metric;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.model.HttpRequest;
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
    // verify request sent to breeze endpoint
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
    assertThat(standardMetricData.getMetrics().get(0).getName())
        .isEqualTo("http.server.request.duration");
    assertThat(standardMetricData.getProperties().get("_MS.IsAutocollected")).isEqualTo("True");

    // verify Statsbeat sent to the breeze endpoint
    verifyStatsbeatSentToBreezeEndpoint();

    // verify custom histogram metric 'histogram-test-otlp-exporter' and otel metric
    // 'http.server.request.duration' sent to OTLP endpoint
    // verify Statsbeat doesn't get sent to OTLP endpoint
    verifyMetricsSentToOtlpEndpoint();
  }

  @SuppressWarnings("PreferJavaTimeOverload")
  private void verifyMetricsSentToOtlpEndpoint() {
    await()
        .atMost(60, SECONDS)
        .untilAsserted(
            () -> {
              HttpRequest[] requests =
                  testing
                      .mockedOtlpIngestion
                      .getCollectorServer()
                      .retrieveRecordedRequests(request());

              // verify metrics
              List<Metric> metrics =
                  testing.mockedOtlpIngestion.extractMetricsFromRequests(requests);
              assertThat(metrics)
                  .extracting(Metric::getName)
                  .contains("histogram-test-otlp-exporter", "http.server.request.duration")
                  .doesNotContain("Attach", "Feature"); // statsbeat
            });
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
      return data.getMetrics().get(0).getName().equals("http.server.request.duration");
    }
    return false;
  }

  private void verifyStatsbeatSentToBreezeEndpoint() throws Exception {
    List<Envelope> statsbeatMetricList =
        testing.mockedIngestion.waitForItems("MetricData", OtlpTest::isAttachStatsbeat, 1);
    Envelope statsbeatEnvelope = statsbeatMetricList.get(0);
    MetricData statsbeatMetricData =
        (MetricData) ((Data<?>) statsbeatEnvelope.getData()).getBaseData();
    assertThat(statsbeatMetricData.getMetrics().get(0).getName()).isEqualTo("Attach");
    assertThat(statsbeatMetricData.getProperties().get("rp")).isNotNull();
    assertThat(statsbeatMetricData.getProperties().get("attach")).isEqualTo("StandaloneAuto");

    List<Envelope> features =
        testing.mockedIngestion.waitForItems("MetricData", OtlpTest::isFeatureStatsbeat, 2);
    Envelope featureEnvelope = features.get(0);
    MetricData featureMetricData = (MetricData) ((Data<?>) featureEnvelope.getData()).getBaseData();
    assertThat(featureMetricData.getMetrics().get(0).getName()).isEqualTo("Feature");
    assertThat(featureMetricData.getProperties().get("type")).isNotEmpty();

    List<Envelope> requestSuccessCounts =
        testing.mockedIngestion.waitForItems("MetricData", OtlpTest::isRequestSuccessCount, 1);
    Envelope rscEnvelope = requestSuccessCounts.get(0);
    MetricData rscMetricData = (MetricData) ((Data<?>) rscEnvelope.getData()).getBaseData();
    assertThat(rscMetricData.getMetrics().get(0).getName()).isEqualTo("Request_Success_Count");
    assertThat(rscMetricData.getProperties().get("endpoint")).isEqualTo("breeze");

    List<Envelope> requestDurations =
        testing.mockedIngestion.waitForItems("MetricData", OtlpTest::isRequestDuration, 1);
    Envelope rdEnvelope = requestDurations.get(0);
    MetricData rdMetricData = (MetricData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rdMetricData.getMetrics().get(0).getName()).isEqualTo("Request_Duration");
    assertThat(rdMetricData.getProperties().get("endpoint")).isEqualTo("breeze");
  }

  private static boolean isAttachStatsbeat(Envelope envelope) {
    if (envelope.getData().getBaseType().equals("MetricData")) {
      MetricData data = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      return data.getMetrics().get(0).getName().equals("Attach");
    }
    return false;
  }

  private static boolean isFeatureStatsbeat(Envelope envelope) {
    if (envelope.getData().getBaseType().equals("MetricData")) {
      MetricData data = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      return data.getMetrics().get(0).getName().equals("Feature");
    }
    return false;
  }

  private static boolean isRequestSuccessCount(Envelope envelope) {
    if (envelope.getData().getBaseType().equals("MetricData")) {
      MetricData data = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      return data.getMetrics().get(0).getName().equals("Request_Success_Count");
    }
    return false;
  }

  private static boolean isRequestDuration(Envelope envelope) {
    if (envelope.getData().getBaseType().equals("MetricData")) {
      MetricData data = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      return data.getMetrics().get(0).getName().equals("Request_Duration");
    }
    return false;
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends OtlpTest {}
}
