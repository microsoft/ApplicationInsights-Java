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
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("httpserver4xx_applicationinsights.json")
abstract class HttpServer4xxTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test4xx")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /HttpServer4xx/test4xx");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/HttpServer4xx/test4xx");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("400");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    List<Envelope> serverMetrics =
        testing.mockedIngestion.waitForStandardMetricItems("requests/duration", 1);
    MetricData metricData = (MetricData) ((Data<?>) serverMetrics.get(0).getData()).getBaseData();
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    DataPoint dataPoint = dataPoints.get(0);
    assertThat(dataPoint.getCount()).isEqualTo(1);

    Map<String, String> properties = metricData.getProperties();
    assertThat(properties).hasSize(7);
    assertThat(properties.get("_MS.MetricId")).isEqualTo("requests/duration");
    assertThat(properties.get("request/resultCode")).isEqualTo("400");
    assertThat(properties.get("Request.Success")).isEqualTo("True");
    assertThat(properties.get("cloud/roleInstance")).isEqualTo("testroleinstance");
    assertThat(properties.get("cloud/roleName")).isEqualTo("testrolename");
    assertThat(properties.get("_MS.IsAutocollected")).isEqualTo("True");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends HttpServer4xxTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends HttpServer4xxTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends HttpServer4xxTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends HttpServer4xxTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends HttpServer4xxTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends HttpServer4xxTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends HttpServer4xxTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends HttpServer4xxTest {}
}
