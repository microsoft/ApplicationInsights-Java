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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public final class ExceptionTelemetryTest {

  private static void testSeverityLevel(SeverityLevel severityLevel) {
    ExceptionTelemetry telemetry = new ExceptionTelemetry(new IllegalArgumentException("mockb"));

    telemetry.setSeverityLevel(severityLevel);
    assertEquals(telemetry.getSeverityLevel(), severityLevel);
  }

  @Test
  public void testSetException() {
    NullPointerException exception = new NullPointerException("mock");
    ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);

    NullPointerException exception1 = new NullPointerException("mock");
    exceptionTelemetry.setException(exception1);

    assertSame(exception1, exceptionTelemetry.getException());
  }

  @Test
  public void testCtor() {
    NullPointerException exception = new NullPointerException("mock");
    ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);

    assertSame(exception, exceptionTelemetry.getException());
    assertTrue(exceptionTelemetry.getProperties().isEmpty());
    assertTrue(exceptionTelemetry.getMetrics().isEmpty());
    assertEquals(exceptionTelemetry.getExceptions().size(), 1);
  }

  @Test
  public void testExceptions() {
    Exception exception = new IOException("mocka", new IllegalArgumentException("mockb"));
    ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);

    assertEquals(exceptionTelemetry.getExceptions().size(), 2);
  }

  @Test
  public void testSetSeverityLevel() {
    testSeverityLevel(SeverityLevel.Error);
  }

  @Test
  public void testSetSeverityLevelWithNull() {
    testSeverityLevel(null);
  }

  @Test
  public void testFirstValueIsNull() {
    ExceptionTelemetry telemetry = new ExceptionTelemetry(new IllegalArgumentException("mockb"));
    assertEquals(telemetry.getSeverityLevel(), null);
  }

  @Test
  public void testGetThrowableOnException() {
    IOException exception = new IOException("mock");
    ExceptionTelemetry telemetry = new ExceptionTelemetry(exception);

    assertSame(exception, telemetry.getThrowable());
    assertSame(exception, telemetry.getException());
  }

  @Test
  public void testError() {
    Error error = new NoSuchMethodError("Method");
    ExceptionTelemetry telemetry = new ExceptionTelemetry(error);

    assertNull(telemetry.getException());
    assertSame(error, telemetry.getThrowable());
  }
}
