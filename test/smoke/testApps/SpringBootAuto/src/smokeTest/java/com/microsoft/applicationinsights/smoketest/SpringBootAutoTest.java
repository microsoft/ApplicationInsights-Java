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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

@UseAgent
public class SpringBootAutoTest extends AiSmokeTest {

  @Test
  @TargetUri("/test")
  public void doMostBasicTest() throws Exception {
    Telemetry telemetry = getTelemetry(0);

    // TODO verify browser and other envelope tags somewhere else
    assertTrue(
        telemetry.rdEnvelope.getTags().get("ai.user.userAgent").startsWith("Apache-HttpClient/"));
    assertNotNull(telemetry.rdEnvelope.getTags().get("ai.location.ip"));

    assertEquals("GET /SpringBootAuto/test", telemetry.rd.getName());
    assertTrue(telemetry.rd.getUrl().matches("http://localhost:[0-9]+/SpringBootAuto/test"));
    assertEquals("200", telemetry.rd.getResponseCode());
    assertTrue(telemetry.rd.getSuccess());
    assertNull(telemetry.rd.getSource());
    assertTrue(telemetry.rd.getProperties().isEmpty());
    assertTrue(telemetry.rd.getMeasurements().isEmpty());
  }
}
