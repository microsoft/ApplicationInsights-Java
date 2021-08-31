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

@UseAgent("azuresdk")
public class AzureSdkTest extends AiSmokeTest {

  @Test
  @TargetUri("/test")
  public void test() throws Exception {
    Telemetry telemetry = getTelemetry(2);

    if (!telemetry.rdd1.getName().equals("TestController.test")) {
      RemoteDependencyData rddTemp = telemetry.rdd1;
      telemetry.rdd1 = telemetry.rdd2;
      telemetry.rdd2 = rddTemp;

      Envelope rddEnvelopeTemp = telemetry.rddEnvelope1;
      telemetry.rddEnvelope1 = telemetry.rddEnvelope2;
      telemetry.rddEnvelope2 = rddEnvelopeTemp;
    }

    assertEquals("GET /AzureSdk/test", telemetry.rd.getName());
    assertTrue(telemetry.rd.getUrl().matches("http://localhost:[0-9]+/AzureSdk/test"));
    assertEquals("200", telemetry.rd.getResponseCode());
    assertTrue(telemetry.rd.getSuccess());
    assertNull(telemetry.rd.getSource());
    assertTrue(telemetry.rd.getProperties().isEmpty());
    assertTrue(telemetry.rd.getMeasurements().isEmpty());

    assertEquals("TestController.test", telemetry.rdd1.getName());
    assertNull(telemetry.rdd1.getData());
    assertEquals("InProc", telemetry.rdd1.getType());
    assertNull(telemetry.rdd1.getTarget());
    assertTrue(telemetry.rdd1.getProperties().isEmpty());
    assertTrue(telemetry.rdd1.getSuccess());

    assertEquals("hello", telemetry.rdd2.getName());
    assertNull(telemetry.rdd2.getData());
    assertEquals("InProc", telemetry.rdd2.getType());
    assertNull(telemetry.rdd2.getTarget());
    assertTrue(telemetry.rdd2.getProperties().isEmpty());
    assertTrue(telemetry.rdd2.getSuccess());

    assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /AzureSdk/test");
    assertParentChild(
        telemetry.rdd1, telemetry.rddEnvelope1, telemetry.rddEnvelope2, "GET /AzureSdk/test");
  }
}
