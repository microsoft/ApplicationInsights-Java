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
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class HttpClientTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/apacheHttpClient4")
  void testApacheHttpClient4() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/apacheHttpClient4WithResponseHandler")
  void testApacheHttpClient4WithResponseHandler() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/apacheHttpClient3")
  void testApacheHttpClient3() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/apacheHttpAsyncClient")
  void testApacheHttpAsyncClient() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/okHttp3")
  void testOkHttp3() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/okHttp2")
  void testOkHttp2() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/httpUrlConnection")
  void testHttpUrlConnection() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/springWebClient")
  void testSpringWebClient() throws Exception {
    // TODO investigate why %2520 is captured instead of %20
    verify("https://mock.codes/200?q=spaces%2520test");
  }

  private static void verify() throws Exception {
    verify("https://mock.codes/200?q=spaces%20test");
  }

  private static void verify(String successUrlWithQueryString) throws Exception {
    Telemetry telemetry = testing.getTelemetry(3);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();
    // TODO (trask) add this check in all smoke tests?
    assertThat(telemetry.rdEnvelope.getSampleRate()).isNull();

    assertThat(telemetry.rdd1.getName()).isEqualTo("GET /200");
    assertThat(telemetry.rdd1.getData()).isEqualTo(successUrlWithQueryString);
    assertThat(telemetry.rdd1.getType()).isEqualTo("Http");
    assertThat(telemetry.rdd1.getTarget()).isEqualTo("mock.codes");
    assertThat(telemetry.rdd1.getResultCode()).isEqualTo("200");
    assertThat(telemetry.rdd1.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rdd1.getSuccess()).isTrue();
    assertThat(telemetry.rddEnvelope1.getSampleRate()).isNull();

    assertThat(telemetry.rdd2.getName()).isEqualTo("GET /404");
    assertThat(telemetry.rdd2.getData()).isEqualTo("https://mock.codes/404");
    assertThat(telemetry.rdd2.getType()).isEqualTo("Http");
    assertThat(telemetry.rdd2.getTarget()).isEqualTo("mock.codes");
    assertThat(telemetry.rdd2.getResultCode()).isEqualTo("404");
    assertThat(telemetry.rdd2.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rdd2.getSuccess()).isFalse();
    assertThat(telemetry.rddEnvelope2.getSampleRate()).isNull();

    assertThat(telemetry.rdd3.getName()).isEqualTo("GET /500");
    assertThat(telemetry.rdd3.getData()).isEqualTo("https://mock.codes/500");
    assertThat(telemetry.rdd3.getType()).isEqualTo("Http");
    assertThat(telemetry.rdd3.getTarget()).isEqualTo("mock.codes");
    assertThat(telemetry.rdd3.getResultCode()).isEqualTo("500");
    assertThat(telemetry.rdd3.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rdd3.getSuccess()).isFalse();
    assertThat(telemetry.rddEnvelope3.getSampleRate()).isNull();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /HttpClients/*");
    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope2, "GET /HttpClients/*");
    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope3, "GET /HttpClients/*");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends HttpClientTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends HttpClientTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends HttpClientTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends HttpClientTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends HttpClientTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends HttpClientTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends HttpClientTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends HttpClientTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends HttpClientTest {}
}
