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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

@UseAgent("controller_spans_disabled_opentelemetryapisupport")
public class OpenTelemetryApiSupportControllerSpansDisabledTest extends AiSmokeTest {

  @Test
  @TargetUri("/test-api")
  public void testApi() throws Exception {
    Telemetry telemetry = getTelemetry(0);

    assertEquals("myspanname", telemetry.rd.getName());
    assertTrue(
        telemetry.rd.getUrl().matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/test-api"));
    assertEquals("200", telemetry.rd.getResponseCode());
    assertTrue(telemetry.rd.getSuccess());
    assertNull(telemetry.rd.getSource());
    assertEquals(2, telemetry.rd.getProperties().size());
    assertEquals("myvalue1", telemetry.rd.getProperties().get("myattr1"));
    assertEquals("myvalue2", telemetry.rd.getProperties().get("myattr2"));
    assertTrue(telemetry.rd.getMeasurements().isEmpty());

    // ideally want the properties below on rd, but can't get SERVER span yet
    // see
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1726#issuecomment-731890267

    // checking that instrumentation key, cloud role name, cloud role instance, and sdk version are
    // from the agent
    assertEquals("00000000-0000-0000-0000-0FEEDDADBEEF", telemetry.rdEnvelope.getIKey());
    assertEquals("testrolename", telemetry.rdEnvelope.getTags().get("ai.cloud.role"));
    assertEquals("testroleinstance", telemetry.rdEnvelope.getTags().get("ai.cloud.roleInstance"));
    assertTrue(telemetry.rdEnvelope.getTags().get("ai.internal.sdkVersion").startsWith("java:3."));
    assertEquals("myuser", telemetry.rdEnvelope.getTags().get("ai.user.id"));
  }

  @Test
  @TargetUri("/test-overriding-ikey-etc")
  public void testOverridingIkeyEtc() throws Exception {
    Telemetry telemetry = getTelemetry(0);

    assertEquals("GET /OpenTelemetryApiSupport/test-overriding-ikey-etc", telemetry.rd.getName());
    assertTrue(
        telemetry
            .rd
            .getUrl()
            .matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/test-overriding-ikey-etc"));
    assertEquals("200", telemetry.rd.getResponseCode());
    assertTrue(telemetry.rd.getSuccess());
    assertNull(telemetry.rd.getSource());
    assertTrue(telemetry.rd.getProperties().isEmpty());
    assertTrue(telemetry.rd.getMeasurements().isEmpty());

    // checking that instrumentation key, cloud role name, cloud role instance, and sdk version are
    // from the agent
    assertEquals("12341234-1234-1234-1234-123412341234", telemetry.rdEnvelope.getIKey());
    assertEquals("role-name-here", telemetry.rdEnvelope.getTags().get("ai.cloud.role"));
    assertEquals("role-instance-here", telemetry.rdEnvelope.getTags().get("ai.cloud.roleInstance"));
    assertEquals(
        "application-version-here", telemetry.rdEnvelope.getTags().get("ai.application.ver"));
    assertTrue(telemetry.rdEnvelope.getTags().get("ai.internal.sdkVersion").startsWith("java:3."));
  }

  @Test
  @TargetUri("/test-annotations")
  public void testAnnotations() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertEquals("GET /OpenTelemetryApiSupport/test-annotations", telemetry.rd.getName());
    assertTrue(
        telemetry
            .rd
            .getUrl()
            .matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/test-annotations"));
    assertEquals("200", telemetry.rd.getResponseCode());
    assertTrue(telemetry.rd.getSuccess());
    assertNull(telemetry.rd.getSource());
    assertTrue(telemetry.rd.getProperties().isEmpty());
    assertTrue(telemetry.rd.getMeasurements().isEmpty());

    assertEquals("TestController.underAnnotation", telemetry.rdd1.getName());
    assertNull(telemetry.rdd1.getData());
    assertEquals("InProc", telemetry.rdd1.getType());
    assertNull(telemetry.rdd1.getTarget());
    assertTrue(telemetry.rdd1.getProperties().isEmpty());
    assertTrue(telemetry.rdd1.getSuccess());

    assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /OpenTelemetryApiSupport/test-annotations");
  }
}
