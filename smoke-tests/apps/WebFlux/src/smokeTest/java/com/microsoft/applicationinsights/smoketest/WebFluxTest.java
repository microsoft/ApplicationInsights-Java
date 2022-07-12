/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class WebFluxTest {

  @RegisterExtension static final SmokeTestExtension testing = new SmokeTestExtension();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /test/**");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/test");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getMeasurements()).isEmpty();
  }

  @Test
  @TargetUri("/exception")
  void testException() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /exception");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/exception");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("500");
    assertThat(telemetry.rd.getSuccess()).isFalse();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getMeasurements()).isEmpty();
  }

  @Test
  @TargetUri("/futureException")
  void testFutureException() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /futureException");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/futureException");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("500");
    assertThat(telemetry.rd.getSuccess()).isFalse();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getMeasurements()).isEmpty();
  }

  @Environment(JAVA_8)
  static class Java8Test extends WebFluxTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends WebFluxTest {}

  @Environment(JAVA_11)
  static class Java11Test extends WebFluxTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends WebFluxTest {}

  @Environment(JAVA_17)
  static class Java17Test extends WebFluxTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends WebFluxTest {}
}
