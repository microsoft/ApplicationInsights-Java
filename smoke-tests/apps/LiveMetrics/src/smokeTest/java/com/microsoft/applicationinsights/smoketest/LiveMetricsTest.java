// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.LIBERTY_20_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.DocumentIngress;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.DocumentType;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.Exception;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.MetricPoint;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.MonitoringDataPoint;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.Trace;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class LiveMetricsTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void testTelemetryDataFlow() throws java.lang.Exception {
    Awaitility.await()
        .atMost(Duration.ofSeconds(60))
        .until(() -> testing.mockedIngestion.getCountForType("RequestData") == 1);

    PostBodyVerifier postBodyVerifier = new PostBodyVerifier();

    assertThat(testing.mockedIngestion.isPingReceived()).isTrue();

    List<String> postBodies = testing.mockedIngestion.getPostBodies();
    assertThat(postBodies).hasSizeGreaterThan(0); // should post at least once

    for (String postBody : postBodies) {
      postBodyVerifier.searchPostBody(postBody);
    }

    assertThat(postBodyVerifier.hasExceptionDoc()).isTrue();
    assertThat(postBodyVerifier.hasTraceDoc()).isTrue();
    assertThat(postBodyVerifier.hasDependency()).isTrue();
    assertThat(postBodyVerifier.hasRequest()).isTrue();
  }

  class PostBodyVerifier {
    boolean foundExceptionDoc = false;
    boolean foundTraceDoc = false;
    boolean foundDependency = false;
    boolean foundRequest = false;

    public void searchPostBody(String postBody) {
      // Each post body is a list with a singular MonitoringDataPoint
      List<MonitoringDataPoint> dataPoints = new ArrayList<>();
      try {
        JsonReader reader = JsonProviders.createReader(postBody);
        dataPoints = reader.readArray(MonitoringDataPoint::fromJson);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to parse post request body", e);
      }

      // Because the mock ping/posts should succeed, we should only have one MonitoringDataPoint per
      // post
      assertThat(dataPoints).hasSize(1);
      MonitoringDataPoint dataPoint = dataPoints.get(0);

      List<DocumentIngress> docs = dataPoint.getDocuments();
      List<MetricPoint> metrics = dataPoint.getMetrics();

      confirmDocsAreFiltered(docs);
      confirmPerfCountersNonZero(metrics);
      foundExceptionDoc = foundExceptionDoc || hasException(docs);
      foundTraceDoc = foundTraceDoc || hasTrace(docs);
      foundDependency = foundDependency || hasDependency(metrics);
      foundRequest = foundRequest || hasRequest(metrics);
    }

    public boolean hasExceptionDoc() {
      return foundExceptionDoc;
    }

    public boolean hasTraceDoc() {
      return foundTraceDoc;
    }

    public boolean hasDependency() {
      return foundDependency;
    }

    public boolean hasRequest() {
      return foundRequest;
    }

    private void confirmDocsAreFiltered(List<DocumentIngress> docs) {
      for (DocumentIngress doc : docs) {
        assertThat(doc.getDocumentType()).isNotEqualTo(DocumentType.REMOTE_DEPENDENCY);
        assertThat(doc.getDocumentType()).isNotEqualTo(DocumentType.REQUEST);
      }
    }

    private boolean hasException(List<DocumentIngress> docs) {
      for (DocumentIngress doc : docs) {
        if (doc.getDocumentType().equals(DocumentType.EXCEPTION)
            && ((Exception) doc).getExceptionMessage().equals("Fake Exception")) {
          return true;
        }
      }
      return false;
    }

    private boolean hasTrace(List<DocumentIngress> docs) {
      for (DocumentIngress doc : docs) {
        if (doc.getDocumentType().equals(DocumentType.TRACE)
            && ((Trace) doc).getMessage().equals("This message should generate a trace")) {
          return true;
        }
      }
      return false;
    }

    private boolean hasDependency(List<MetricPoint> metrics) {
      for (MetricPoint metric : metrics) {
        String name = metric.getName();
        double value = metric.getValue();
        if (name.equals("\\ApplicationInsights\\Dependency Calls/Sec")) {
          return value == 1;
        }
      }
      return false;
    }

    private boolean hasRequest(List<MetricPoint> metrics) {
      for (MetricPoint metric : metrics) {
        String name = metric.getName();
        double value = metric.getValue();
        if (name.equals("\\ApplicationInsights\\Requests/Sec")) {
          return value == 1;
        }
      }
      return false;
    }

    private void confirmPerfCountersNonZero(List<MetricPoint> metrics) {
      for (MetricPoint metric : metrics) {
        String name = metric.getName();
        double value = metric.getValue();
        if (name.equals("\\Process\\Physical Bytes")
            || name.equals("\\% Process\\Processor Time Normalized")) {
          assertThat(value).isNotEqualTo(0);
        }
      }
    }
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends LiveMetricsTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends LiveMetricsTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends LiveMetricsTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends LiveMetricsTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends LiveMetricsTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends LiveMetricsTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends LiveMetricsTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends LiveMetricsTest {}

  @Environment(TOMCAT_8_JAVA_23)
  static class Tomcat8Java23Test extends LiveMetricsTest {}

  @Environment(TOMCAT_8_JAVA_23_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends LiveMetricsTest {}

  @Environment(LIBERTY_20_JAVA_8)
  static class Liberty20Java8Test extends LiveMetricsTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends LiveMetricsTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends LiveMetricsTest {}
}
