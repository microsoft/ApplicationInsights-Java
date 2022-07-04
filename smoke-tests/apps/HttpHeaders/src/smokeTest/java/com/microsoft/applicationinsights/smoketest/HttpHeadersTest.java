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

import org.junit.Test;

@UseAgent
public class HttpHeadersTest extends AiWarSmokeTest {

  @Test
  @TargetUri("/serverHeaders")
  public void testServerHeaders() throws Exception {
    Telemetry telemetry = getTelemetry(0);

    assertEquals("testing123", telemetry.rd.getProperties().get("http.response.header.abc"));
    assertThat(telemetry.rd.getProperties().get("http.request.header.host")).isNotNull();
    assertThat(telemetry.rd.getProperties()).hasSize(2);
    assertThat(telemetry.rd.getSuccess()).isTrue();
  }

  @Test
  @TargetUri("/clientHeaders")
  public void testClientHeaders() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    assertThat(telemetry.rd.getProperties().get("http.request.header.host")).isNotNull();
    assertThat(telemetry.rd.getProperties()).hasSize(1);
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertEquals("testing123", telemetry.rdd1.getProperties().get("http.request.header.abc"));
    assertThat(telemetry.rdd1.getProperties().get("http.response.header.date")).isNotNull();
    assertThat(telemetry.rdd1.getProperties()).hasSize(2);
    assertThat(telemetry.rdd1.getSuccess()).isTrue();
  }
}
