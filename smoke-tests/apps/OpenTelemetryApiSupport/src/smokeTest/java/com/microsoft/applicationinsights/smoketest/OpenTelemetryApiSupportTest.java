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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

@UseAgent
public class OpenTelemetryApiSupportTest extends AiWarSmokeTest {

  @Test
  @TargetUri("/test-api")
  public void testApi() throws Exception {
    Telemetry telemetry = getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("myspanname");
    assertTrue(
        telemetry.rd.getUrl().matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/test-api"));
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties()).hasSize(2);
    assertThat(telemetry.rd.getProperties().get("myattr1")).isEqualTo("myvalue1");
    assertThat(telemetry.rd.getProperties().get("myattr2")).isEqualTo("myvalue2");
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    // ideally want the properties below on rd, but can't get SERVER span yet
    // see
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1726#issuecomment-731890267

    // checking that instrumentation key, cloud role name, cloud role instance, and sdk version are
    // from the agent
    assertThat(telemetry.rdEnvelope.getIKey()).isEqualTo("00000000-0000-0000-0000-0FEEDDADBEEF");
    assertThat(telemetry.rdEnvelope.getTags().get("ai.cloud.role")).isEqualTo("testrolename");
    assertThat(telemetry.rdEnvelope.getTags().get("ai.cloud.roleInstance"))
        .isEqualTo("testroleinstance");
    assertTrue(telemetry.rdEnvelope.getTags().get("ai.internal.sdkVersion").startsWith("java:3."));
    assertThat(telemetry.rdEnvelope.getTags().get("ai.user.id")).isEqualTo("myuser");
  }

  @Test
  @TargetUri("/test-overriding-ikey-etc")
  public void testOverridingIkeyEtc() throws Exception {
    Telemetry telemetry = getTelemetry(0);

    assertThat(telemetry.rd.getName())
        .isEqualTo("GET /OpenTelemetryApiSupport/test-overriding-ikey-etc");
    assertTrue(
        telemetry
            .rd
            .getUrl()
            .matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/test-overriding-ikey-etc"));
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    // checking that instrumentation key, cloud role name, cloud role instance, and sdk version are
    // from the agent
    assertThat(telemetry.rdEnvelope.getIKey()).isEqualTo("12341234-1234-1234-1234-123412341234");
    assertThat(telemetry.rdEnvelope.getTags().get("ai.cloud.role")).isEqualTo("role-name-here");
    assertThat(telemetry.rdEnvelope.getTags().get("ai.cloud.roleInstance"))
        .isEqualTo("role-instance-here");
    assertEquals(
        "application-version-here", telemetry.rdEnvelope.getTags().get("ai.application.ver"));
    assertTrue(telemetry.rdEnvelope.getTags().get("ai.internal.sdkVersion").startsWith("java:3."));
  }

  @Test
  @TargetUri("/test-annotations")
  public void testAnnotations() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /OpenTelemetryApiSupport/test-annotations");
    assertTrue(
        telemetry
            .rd
            .getUrl()
            .matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/test-annotations"));
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    assertThat(telemetry.rdd1.getName()).isEqualTo("TestController.underAnnotation");
    assertThat(telemetry.rdd1.getData()).isNull();
    assertThat(telemetry.rdd1.getType()).isEqualTo("InProc");
    assertThat(telemetry.rdd1.getTarget()).isNull();
    assertThat(telemetry.rdd1.getProperties().get("message")).isEqualTo("a message");
    assertThat(telemetry.rdd1.getProperties()).hasSize(1);
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /OpenTelemetryApiSupport/test-annotations");
  }
}
