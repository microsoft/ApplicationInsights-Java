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

@UseAgent
@WithDependencyContainers(@DependencyContainer(value = "redis", portMapping = "6379"))
public class JedisTest extends AiSmokeTest {

  @Test
  @TargetUri("/jedis")
  public void jedis() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertEquals("GET /Jedis/*", telemetry.rd.getName());
    assertTrue(telemetry.rd.getUrl().matches("http://localhost:[0-9]+/Jedis/jedis"));
    assertEquals("200", telemetry.rd.getResponseCode());
    assertTrue(telemetry.rd.getSuccess());
    assertNull(telemetry.rd.getSource());
    assertTrue(telemetry.rd.getProperties().isEmpty());
    assertTrue(telemetry.rd.getMeasurements().isEmpty());

    assertEquals("GET", telemetry.rdd1.getName());
    assertEquals("GET test", telemetry.rdd1.getData());
    assertEquals("redis", telemetry.rdd1.getType());
    assertTrue(telemetry.rdd1.getTarget().matches("dependency[0-9]+"));
    assertTrue(telemetry.rdd1.getProperties().isEmpty());
    assertTrue(telemetry.rdd1.getSuccess());

    assertParentChild(telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jedis/*");
  }
}
