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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

@UseAgent
public class HttpClientSmokeTest extends AiWarSmokeTest {

  @Test
  @TargetUri("/apacheHttpClient4")
  public void testApacheHttpClient4() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/apacheHttpClient4WithResponseHandler")
  public void testApacheHttpClient4WithResponseHandler() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/apacheHttpClient3")
  public void testApacheHttpClient3() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/apacheHttpAsyncClient")
  public void testApacheHttpAsyncClient() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/okHttp3")
  public void testOkHttp3() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/okHttp2")
  public void testOkHttp2() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/httpUrlConnection")
  public void testHttpUrlConnection() throws Exception {
    verify();
  }

  @Test
  @TargetUri("/springWebClient")
  public void testSpringWebClient() throws Exception {
    // TODO investigate why %2520 is captured instead of %20
    verify("https://mock.codes/200?q=spaces%2520test");
  }

  private static void verify() throws Exception {
    verify("https://mock.codes/200?q=spaces%20test");
  }

  private static void verify(String successUrlWithQueryString) throws Exception {
    Telemetry telemetry = getTelemetry(3);

    assertTrue(telemetry.rd.getProperties().isEmpty());
    assertTrue(telemetry.rd.getSuccess());
    // TODO (trask) add this check in all smoke tests?
    assertNull(telemetry.rdEnvelope.getSampleRate());

    assertEquals("GET /200", telemetry.rdd1.getName());
    assertEquals(successUrlWithQueryString, telemetry.rdd1.getData());
    assertEquals("Http", telemetry.rdd1.getType());
    assertEquals("mock.codes", telemetry.rdd1.getTarget());
    assertEquals("200", telemetry.rdd1.getResultCode());
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();
    assertNull(telemetry.rddEnvelope1.getSampleRate());

    assertEquals("GET /404", telemetry.rdd2.getName());
    assertEquals("https://mock.codes/404", telemetry.rdd2.getData());
    assertEquals("Http", telemetry.rdd2.getType());
    assertEquals("mock.codes", telemetry.rdd2.getTarget());
    assertEquals("404", telemetry.rdd2.getResultCode());
    assertThat(telemetry.rdd2.getProperties()).isEmpty();
    assertFalse(telemetry.rdd2.getSuccess());
    assertNull(telemetry.rddEnvelope2.getSampleRate());

    assertEquals("GET /500", telemetry.rdd3.getName());
    assertEquals("https://mock.codes/500", telemetry.rdd3.getData());
    assertEquals("Http", telemetry.rdd3.getType());
    assertEquals("mock.codes", telemetry.rdd3.getTarget());
    assertEquals("500", telemetry.rdd3.getResultCode());
    assertThat(telemetry.rdd3.getProperties()).isEmpty();
    assertFalse(telemetry.rdd3.getSuccess());
    assertNull(telemetry.rddEnvelope3.getSampleRate());

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /HttpClients/*");
    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope2, "GET /HttpClients/*");
    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope3, "GET /HttpClients/*");
  }
}
