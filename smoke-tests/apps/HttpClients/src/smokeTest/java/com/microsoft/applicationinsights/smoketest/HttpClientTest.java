// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

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
    verify("http://host.testcontainers.internal:6060/mock/200?q=spaces%2520test");
  }

  private static void verify() throws Exception {
    verify("http://host.testcontainers.internal:6060/mock/200?q=spaces%20test");
  }

  private static void verify(String successUrlWithQueryString) throws Exception {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request
                            .hasSuccess(true)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasNoParent()
                            // TODO (trask) add this check in all smoke tests?
                            .hasNoSampleRate()
                            .hasTag("ai.operation.name", "GET /HttpClients/*"))
                .hasDependencySatisying(
                    dependency ->
                        dependency
                            .hasName("GET /mock/200")
                            .hasData(successUrlWithQueryString)
                            .hasType("Http")
                            .hasTarget("host.testcontainers.internal:6060")
                            .hasResultCode("200")
                            .hasSuccess(true)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasParent(trace.getRequestId(0))
                            .hasNoSampleRate()
                            .hasTag("ai.operation.name", "GET /HttpClients/*"))
                .hasDependencySatisying(
                    dependency ->
                        dependency
                            .hasName("GET /mock/404")
                            .hasData("http://host.testcontainers.internal:6060/mock/404")
                            .hasType("Http")
                            .hasTarget("host.testcontainers.internal:6060")
                            .hasResultCode("404")
                            .hasSuccess(false)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasParent(trace.getRequestId(0))
                            .hasNoSampleRate()
                            .hasTag("ai.operation.name", "GET /HttpClients/*"))
                .hasDependencySatisying(
                    dependency ->
                        dependency
                            .hasName("GET /mock/500")
                            .hasData("http://host.testcontainers.internal:6060/mock/500")
                            .hasType("Http")
                            .hasTarget("host.testcontainers.internal:6060")
                            .hasResultCode("500")
                            .hasSuccess(false)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasParent(trace.getRequestId(0))
                            .hasNoSampleRate()
                            .hasTag("ai.operation.name", "GET /HttpClients/*")));
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

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends HttpClientTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends HttpClientTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends HttpClientTest {}

  @Environment(TOMCAT_8_JAVA_23)
  static class Tomcat8Java23Test extends HttpClientTest {}

  @Environment(TOMCAT_8_JAVA_23_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends HttpClientTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends HttpClientTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends HttpClientTest {}
}
