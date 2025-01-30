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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.DocumentIngress;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.Exception;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.Trace;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.DocumentType;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.MetricPoint;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.swagger.models.MonitoringDataPoint;

@UseAgent
abstract class LiveMetricsTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void testTelemetryDataFlow() throws java.lang.Exception {
    Awaitility.await()
        .atMost(Duration.ofSeconds(20));


    assertThat(testing.mockedIngestion.isPingReceived()).isTrue();


    List<String> postBodies = testing.mockedIngestion.getPostBodies();
    assertThat(postBodies).hasSizeGreaterThan(0); // should post at least once

    boolean foundExceptionDoc = false;
    boolean foundTraceDoc = false;
    boolean foundDependency = false;
    boolean foundRequest = false;

    for (String postBody : postBodies) {
      // Verify that the telemetry data is in the last post body
      List<MonitoringDataPoint> dataPoints = new ArrayList<>();
      try {
        JsonReader reader = JsonProviders.createReader(postBody);
        dataPoints = reader.readArray(MonitoringDataPoint::fromJson);
      } catch (IOException e) {
        throw new java.lang.Exception("Failed to parse post request body", e);
      }

      assertThat(dataPoints).hasSize(1);
      MonitoringDataPoint dataPoint = dataPoints.get(0);
      List<DocumentIngress> docs = dataPoint.getDocuments();
      List<MetricPoint> metrics = dataPoint.getMetrics();
      // check that the expected documents are present
      // With the default filtering configuration, we should only see the exception and trace documents.
      // Request/Dep did not fail, so there should not be documents for those.

      for (DocumentIngress doc : docs) {
        if (doc.getDocumentType().equals(DocumentType.EXCEPTION) &&
            ((Exception) doc).getExceptionMessage().equals("Fake Exception")) {
          foundExceptionDoc = true;
        } else if (doc.getDocumentType().equals(DocumentType.TRACE) &&
            ((Trace) doc).getMessage().equals("This message should generate a trace")) {
          foundTraceDoc = true;
        }
      }

      // check that the expected metrics have the correct values

      for (MetricPoint metric : metrics) {
        String name = metric.getName();
        double value = metric.getValue();
        if (name.equals("\\ApplicationInsights\\Dependency Calls/Sec") && value == 1) {
          foundDependency = true;
        } else if (name.equals("\\ApplicationInsights\\Requests/Sec") && value == 1) {
          foundRequest = true;
        }
      }

    }

    assertThat(foundExceptionDoc).isTrue();
    assertThat(foundTraceDoc).isTrue();
    assertThat(foundDependency).isTrue();
    assertThat(foundRequest).isTrue();

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
