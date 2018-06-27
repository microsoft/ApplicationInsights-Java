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

package com.microsoft.applicationinsights.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class SessionStateTelemetryTest {
  private static void verifyStateAfterCtor(
      SessionStateTelemetry telemetry, SessionState expectedState) {
    assertNull(telemetry.getTimestamp());
    assertNull(telemetry.getSequence());
    assertEquals(telemetry.getSessionState(), expectedState);
  }

  @Test
  public void testEmptyCtor() {
    SessionStateTelemetry telemetry = new SessionStateTelemetry();
    verifyStateAfterCtor(telemetry, SessionState.Start);
  }

  @Test
  public void testCtor() {
    SessionStateTelemetry telemetry = new SessionStateTelemetry(SessionState.End);
    verifyStateAfterCtor(telemetry, SessionState.End);
  }

  @Test
  public void testSetSessionState() {
    SessionStateTelemetry telemetry = new SessionStateTelemetry(SessionState.Start);
    telemetry.setSessionState(SessionState.End);

    assertEquals(telemetry.getSessionState(), SessionState.End);
  }
}
