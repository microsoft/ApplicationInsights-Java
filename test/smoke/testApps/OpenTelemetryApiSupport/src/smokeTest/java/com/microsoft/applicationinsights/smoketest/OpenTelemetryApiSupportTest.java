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

import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import org.junit.Test;

@UseAgent("opentelemetryapisupport")
public class OpenTelemetryApiSupportTest extends AiSmokeTest {

  @Test
  @TargetUri("/test-api")
  public void testApi() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertEquals("GET /OpenTelemetryApiSupport/test-api", telemetry.rd.getName());
    assertTrue(
        telemetry.rd.getUrl().matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/test-api"));
    assertEquals("200", telemetry.rd.getResponseCode());
    assertTrue(telemetry.rd.getSuccess());
    assertNull(telemetry.rd.getSource());
    assertTrue(telemetry.rd.getProperties().isEmpty());
    assertTrue(telemetry.rd.getMeasurements().isEmpty());

    assertEquals("myspanname", telemetry.rdd1.getName());
    assertNull(telemetry.rdd1.getData());
    assertEquals("InProc", telemetry.rdd1.getType());
    assertNull(telemetry.rdd1.getTarget());
    assertEquals(2, telemetry.rdd1.getProperties().size());
    assertEquals("myvalue1", telemetry.rdd1.getProperties().get("myattr1"));
    assertEquals("myvalue2", telemetry.rdd1.getProperties().get("myattr2"));
    assertTrue(telemetry.rdd1.getSuccess());

    // ideally want the properties below on rd, but can't get SERVER span yet
    // see
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1726#issuecomment-731890267

    // checking that instrumentation key, cloud role name, cloud role instance, and sdk version are
    // from the agent
    assertEquals("00000000-0000-0000-0000-0FEEDDADBEEF", telemetry.rddEnvelope1.getIKey());
    assertEquals("testrolename", telemetry.rddEnvelope1.getTags().get("ai.cloud.role"));
    assertEquals("testroleinstance", telemetry.rddEnvelope1.getTags().get("ai.cloud.roleInstance"));
    assertTrue(
        telemetry.rddEnvelope1.getTags().get("ai.internal.sdkVersion").startsWith("java:3."));
    assertEquals("myuser", telemetry.rddEnvelope1.getTags().get("ai.user.id"));

    assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /OpenTelemetryApiSupport/test-api");
  }

  @Test
  @TargetUri("/test-overriding-ikey-etc")
  public void testOverridingIkeyEtc() throws Exception {
    Telemetry telemetry = getTelemetry(1);

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

    // ideally want the properties below on rd, but can't get SERVER span yet, see
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1726#issuecomment-731890267

    // checking that instrumentation key, cloud role name, cloud role instance, and sdk version are
    // from the agent
    assertEquals("12341234-1234-1234-1234-123412341234", telemetry.rddEnvelope1.getIKey());
    assertEquals("role-name-here", telemetry.rddEnvelope1.getTags().get("ai.cloud.role"));
    assertEquals(
        "role-instance-here", telemetry.rddEnvelope1.getTags().get("ai.cloud.roleInstance"));
    assertEquals(
        "application-version-here", telemetry.rddEnvelope1.getTags().get("ai.application.ver"));
    assertTrue(
        telemetry.rddEnvelope1.getTags().get("ai.internal.sdkVersion").startsWith("java:3."));
    assertTrue(telemetry.rdd1.getProperties().isEmpty());
    assertTrue(telemetry.rdd1.getSuccess());

    assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /OpenTelemetryApiSupport/test-overriding-ikey-etc");
  }

  @Test
  @TargetUri("/test-annotations")
  public void testAnnotations() throws Exception {
    Telemetry telemetry = getTelemetry(2);

    if (!telemetry.rdd1.getName().equals("TestController.testAnnotations")) {
      RemoteDependencyData rddTemp = telemetry.rdd1;
      telemetry.rdd1 = telemetry.rdd2;
      telemetry.rdd2 = rddTemp;

      Envelope rddEnvelopeTemp = telemetry.rddEnvelope1;
      telemetry.rddEnvelope1 = telemetry.rddEnvelope2;
      telemetry.rddEnvelope2 = rddEnvelopeTemp;
    }

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

    assertEquals("TestController.testAnnotations", telemetry.rdd1.getName());
    assertNull(telemetry.rdd1.getData());
    assertEquals("InProc", telemetry.rdd1.getType());
    assertNull(telemetry.rdd1.getTarget());
    assertTrue(telemetry.rdd1.getProperties().isEmpty());
    assertTrue(telemetry.rdd1.getSuccess());

    assertEquals("TestController.underAnnotation", telemetry.rdd2.getName());
    assertNull(telemetry.rdd2.getData());
    assertEquals("InProc", telemetry.rdd2.getType());
    assertNull(telemetry.rdd2.getTarget());
    assertTrue(telemetry.rdd2.getProperties().isEmpty());
    assertTrue(telemetry.rdd2.getSuccess());

    assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /OpenTelemetryApiSupport/test-annotations");
    assertParentChild(
        telemetry.rdd1,
        telemetry.rddEnvelope1,
        telemetry.rddEnvelope2,
        "GET /OpenTelemetryApiSupport/test-annotations");
  }
}
