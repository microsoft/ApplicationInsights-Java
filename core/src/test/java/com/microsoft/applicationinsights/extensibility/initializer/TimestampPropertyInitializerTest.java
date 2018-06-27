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

package com.microsoft.applicationinsights.extensibility.initializer;

import static org.mockito.Matchers.anyObject;

import com.microsoft.applicationinsights.telemetry.Telemetry;
import java.util.Date;
import junit.framework.TestCase;
import org.junit.Test;
import org.mockito.Mockito;

public final class TimestampPropertyInitializerTest extends TestCase {
  private static Telemetry createMockTelemetryAndActivateInitializer(Date mockValue) {
    Telemetry telemetry = Mockito.mock(Telemetry.class);
    Mockito.doReturn(mockValue).when(telemetry).getTimestamp();

    new TimestampPropertyInitializer().initialize(telemetry);

    return telemetry;
  }

  @Test
  public void testSetTimestampWithNull() {
    Telemetry telemetry = createMockTelemetryAndActivateInitializer(null);
    Mockito.verify(telemetry, Mockito.times(1)).setTimestamp((Date) anyObject());
  }

  @Test
  public void testSetTimestampWithNonNull() {
    Telemetry telemetry = createMockTelemetryAndActivateInitializer(new Date());
    Mockito.verify(telemetry, Mockito.never()).setTimestamp((Date) anyObject());
  }
}
