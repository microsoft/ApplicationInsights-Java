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

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionDetails;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-exception-without-sampling-overrides.json")
abstract class ExceptionWithoutSamplingOverridesTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().setSelfDiagnosticsLevel("debug").build();

  @Test
  @TargetUri(value = "/trackExceptionWithoutSamplingOverrides")
  void testExceptionWithoutSamplingOverrides() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    assertThat(rdEnvelope.getTags().get("ai.operation.name"))
        .isEqualTo("GET /SamplingOverrides/trackExceptionWithoutSamplingOverrides");

    List<Envelope> exceptions = testing.mockedIngestion.waitForItems("ExceptionData", 1);
    ExceptionData exceptionData =
        (ExceptionData) ((Data<?>) exceptions.get(0).getData()).getBaseData();
    assertThat(exceptions.size()).isEqualTo(1);
    assertThat(exceptionData.getProperties().size()).isEqualTo(4);
    assertThat(exceptionData.getProperties().get("LoggerName"))
        .isEqualTo(
            "org.apache.catalina.core.ContainerBase.[Catalina].[localhost].[/SamplingOverrides].[com.microsoft.applicationinsights.smoketestapp.ExceptionWithoutSamplingOverridesServlet]");
    assertThat(exceptionData.getProperties().get("Logger Message"))
        .isEqualTo(
            "Servlet.service() for servlet [com.microsoft.applicationinsights.smoketestapp.ExceptionWithoutSamplingOverridesServlet] in context with path [/SamplingOverrides] threw exception");
    assertThat(exceptionData.getProperties().get("SourceType")).isEqualTo("Logger");
    assertThat(exceptionData.getProperties().get("ThreadName")).isEqualTo("http-nio-8080-exec-2");
    ExceptionDetails exceptionDetails = exceptionData.getExceptions().get(0);
    assertThat(exceptionDetails.getStack()).isNotNull();
    assertThat(exceptionDetails.getTypeName()).isEqualTo("java.lang.IllegalArgumentException");
    assertThat(exceptionDetails.getMessage())
        .isEqualTo("this is an expected IllegalArgumentException");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends ExceptionWithoutSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends ExceptionWithoutSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends ExceptionWithoutSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends ExceptionWithoutSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends ExceptionWithoutSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends ExceptionWithoutSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends ExceptionWithoutSamplingOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends ExceptionWithoutSamplingOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends ExceptionWithoutSamplingOverridesTest {}
}
