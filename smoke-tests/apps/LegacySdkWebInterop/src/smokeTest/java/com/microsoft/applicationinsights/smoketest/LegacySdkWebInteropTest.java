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

import static org.junit.Assert.assertFalse;

import org.junit.Test;

@UseAgent
public class LegacySdkWebInteropTest extends AiWarSmokeTest {

  @Test
  @TargetUri("/test")
  public void doMostBasicTest() throws Exception {
    Telemetry telemetry = getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("myspanname");
    assertThat(telemetry.rd.getSource()).isEqualTo("mysource");
    assertThat(telemetry.rdEnvelope.getTags().get("ai.user.id")).isEqualTo("myuser");
    assertThat(telemetry.rdEnvelope.getTags().get("ai.session.id")).isEqualTo("mysessionid");
    assertThat(telemetry.rdEnvelope.getTags().get("ai.device.os")).isEqualTo("mydeviceos");
    assertThat(telemetry.rdEnvelope.getTags().get("ai.device.osVersion"))
        .isEqualTo("mydeviceosversion");
    assertThat(telemetry.rd.getProperties().get("myattr1")).isEqualTo("myvalue1");
    assertThat(telemetry.rd.getProperties().get("myattr2")).isEqualTo("myvalue2");
    assertThat(telemetry.rd.getProperties()).hasSize(2);

    assertFalse(telemetry.rd.getSuccess());
  }
}
